package io.github.maaasu.astralRecord.feature.inventory.event;

import io.github.maaasu.astralRecord.core.event.AbstractEventHandler;
import io.github.maaasu.astralRecord.feature.inventory.model.EquipmentType;
import io.github.maaasu.astralRecord.feature.inventory.service.InventoryService;
import io.github.maaasu.astralRecord.feature.menu.model.MenuScreen;
import io.github.maaasu.astralRecord.feature.gui.sound.GuiSound;
import io.github.maaasu.astralRecord.feature.menu.view.MenuView;
import io.github.maaasu.astralRecord.feature.item.service.ItemStackFactory;
import io.github.maaasu.astralRecord.feature.player.AstPlayerCache;
import io.github.maaasu.astralRecord.feature.player.model.AstPlayer;
import io.github.maaasu.astralRecord.infrastructure.logging.LogId;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class InventoryEquipmentGuiEventHandler extends AbstractEventHandler {

    private final MenuView menuView;
    private final InventoryService inventoryService;

    public InventoryEquipmentGuiEventHandler(
        @NotNull MenuView menuView,
        @NotNull InventoryService inventoryService
    ) {
        this.menuView = menuView;
        this.inventoryService = inventoryService;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        runSafely(() -> {
            var topInventory = event.getView().getTopInventory();
            if (menuView.isMenuInventory(topInventory)
                && menuView.getMenuScreen(topInventory) == MenuScreen.EQUIPMENT_GUI) {
                handleEquipmentGuiClick(event, topInventory);
                return;
            }
            handlePlayerInventoryEquipClick(event);
        }, LogId.E_5600, event.getWhoClicked().getName());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        runSafely(() -> {
            if (!(event.getPlayer() instanceof Player player)) {
                return;
            }
            if (!menuView.isMenuInventory(event.getInventory())
                || menuView.getMenuScreen(event.getInventory()) != MenuScreen.EQUIPMENT_GUI) {
                return;
            }
            saveEquipmentGui(player, event.getInventory());
        }, LogId.E_5600, event.getPlayer().getName());
    }

    private void handleEquipmentGuiClick(@NotNull InventoryClickEvent event, @NotNull Inventory topInventory) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getRawSlot() >= topInventory.getSize()) {
            handleEquipmentGuiPlayerInventoryClick(event, topInventory, player);
            return;
        }

        event.setCancelled(true);
        if (event.getRawSlot() == MenuView.CLOSE_SLOT) {
            GuiSound.CLOSE.play(player);
            player.closeInventory();
            return;
        }
        if (event.getRawSlot() == MenuView.BACK_SLOT) {
            saveEquipmentGui(player, topInventory);
            GuiSound.SELECT.play(player);
            menuView.open(player);
            return;
        }
        if (!menuView.isEquipmentItemSlot(event.getRawSlot())) {
            GuiSound.DENY.play(player);
            return;
        }
        if (event.getClick().isShiftClick()) {
            GuiSound.DENY.play(player);
            return;
        }

        ItemStack cursor = event.getCursor();
        EquipmentType equipmentType = menuView.getEquipmentTypeAtSlot(event.getRawSlot());
        boolean extendedAccessory = menuView.isExtendedAccessorySlot(event.getRawSlot());
        if (!inventoryService.canPlaceInEquipmentGuiSlot(cursor, equipmentType, extendedAccessory)) {
            GuiSound.DENY.play(player);
            return;
        }

        ItemStack current = menuView.getEquipmentGuiItem(topInventory, event.getRawSlot());
        boolean hasCursor = cursor != null && cursor.getType() != Material.AIR;
        boolean hasCurrent = current != null;

        if (!hasCursor && !hasCurrent) {
            GuiSound.DENY.play(player);
            return;
        }

        if (!hasCursor) {
            if (!player.getInventory().addItem(current.clone()).isEmpty()) {
                GuiSound.DENY.play(player);
                return;
            }
            ItemStack placeholder = menuView.getEquipmentSlotPlaceholder(event.getRawSlot());
            topInventory.setItem(event.getRawSlot(), placeholder);
            player.updateInventory();
            GuiSound.SELECT.play(player);
            return;
        }

        topInventory.setItem(event.getRawSlot(), cursor.clone());
        player.setItemOnCursor(hasCurrent ? current : new ItemStack(Material.AIR));
        GuiSound.SELECT.play(player);
    }

    private void handleEquipmentGuiPlayerInventoryClick(
        @NotNull InventoryClickEvent event,
        @NotNull Inventory topInventory,
        @NotNull Player player
    ) {
        event.setCancelled(true);
        if (!(event.getClickedInventory() instanceof org.bukkit.inventory.PlayerInventory playerInventory)) {
            GuiSound.DENY.play(player);
            return;
        }

        ItemStack cursor = event.getCursor();
        if (cursor != null && cursor.getType() != Material.AIR) {
            GuiSound.DENY.play(player);
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            GuiSound.DENY.play(player);
            return;
        }

        EquipmentType equipmentType = inventoryService.getEquipmentTypeForItem(clickedItem);
        int targetSlot = equipmentType == EquipmentType.OFF_HAND
            ? menuView.firstEmptyAccessorySlot(topInventory)
            : menuView.getSlotForEquipmentType(equipmentType);
        if (targetSlot < 0) {
            GuiSound.DENY.play(player);
            return;
        }

        boolean extendedAccessory = menuView.isExtendedAccessorySlot(targetSlot);
        EquipmentType targetEquipmentType = menuView.getEquipmentTypeAtSlot(targetSlot);
        if (!inventoryService.canPlaceInEquipmentGuiSlot(clickedItem, targetEquipmentType, extendedAccessory)) {
            GuiSound.DENY.play(player);
            return;
        }

        ItemStack previous = menuView.getEquipmentGuiItem(topInventory, targetSlot);
        topInventory.setItem(targetSlot, clickedItem.clone());
        playerInventory.setItem(event.getSlot(), previous == null ? new ItemStack(Material.AIR) : previous);
        GuiSound.SELECT.play(player);
    }

    private void saveEquipmentGui(@NotNull Player player, @NotNull Inventory inventory) {
        AstPlayer astPlayer = AstPlayerCache.get(player);
        if (astPlayer == null) {
            return;
        }
        ItemStack[] accessories = menuView.getAccessoryItems(inventory);
        inventoryService.saveEquipmentGui(
            astPlayer,
            menuView.getEquipmentGuiItem(inventory, MenuView.EQUIPMENT_HEAD_SLOT),
            menuView.getEquipmentGuiItem(inventory, MenuView.EQUIPMENT_CHEST_SLOT),
            menuView.getEquipmentGuiItem(inventory, MenuView.EQUIPMENT_LEGS_SLOT),
            menuView.getEquipmentGuiItem(inventory, MenuView.EQUIPMENT_FEET_SLOT),
            accessories[1],
            accessories[2],
            accessories[3],
            accessories[4],
            accessories[5],
            accessories[6],
            accessories[7]
        );
    }

    private void handlePlayerInventoryEquipClick(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getView().getType() != org.bukkit.event.inventory.InventoryType.CRAFTING) {
            return;
        }
        if (!(event.getClickedInventory() instanceof org.bukkit.inventory.PlayerInventory)) {
            return;
        }

        AstPlayer astPlayer = AstPlayerCache.get(player);
        if (astPlayer == null || !astPlayer.getAccount().getMode().shouldReflectInventoryToGui()) {
            return;
        }

        int slot = event.getSlot();
        if (slot >= 0 && slot <= 8) {
            event.setCancelled(true);
            if (inventoryService.handleHotbarSlotClick(astPlayer, slot + 1)) {
                GuiSound.SELECT.play(player);
            } else {
                GuiSound.DENY.play(player);
            }
            return;
        }

        if (isArmorOrOffhandSlot(slot)) {
            event.setCancelled(true);
            if (handleArmorOrOffhandClick(event, astPlayer, slot)) {
                GuiSound.SELECT.play(player);
            } else {
                GuiSound.DENY.play(player);
            }
            return;
        }

        if (slot < 9 || slot > 35) {
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        if (ItemStackFactory.getAstralItemId(clickedItem) == null) {
            return;
        }

        event.setCancelled(true);
        if (inventoryService.equipOrAssignClickedItem(astPlayer, clickedItem, slot)) {
            GuiSound.SELECT.play(player);
            return;
        }
        GuiSound.DENY.play(player);
    }

    /**
     * 防具スロットまたはオフハンドのクリック操作（装着・解除・入れ替え）を処理します。
     *
     * @param event クリックイベント
     * @param astPlayer 対象プレイヤー
     * @param slot クリックスロット
     * @return 変更が反映された場合 true
     */
    private boolean handleArmorOrOffhandClick(
        @NotNull InventoryClickEvent event,
        @NotNull AstPlayer astPlayer,
        int slot
    ) {
        if (!(event.getClickedInventory() instanceof org.bukkit.inventory.PlayerInventory inventory)) {
            return false;
        }

        EquipmentType equipmentType = equipmentTypeFromPlayerSlot(slot);
        if (equipmentType == EquipmentType.UNSUPPORTED) {
            return false;
        }

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        boolean hasCurrent = current != null && current.getType() != Material.AIR;
        boolean hasCursor = cursor != null && cursor.getType() != Material.AIR;

        if (!hasCurrent && !hasCursor) {
            return false;
        }

        if (!hasCursor) {
            inventory.setItem(slot, new ItemStack(Material.AIR));
            event.getView().setCursor(current == null ? new ItemStack(Material.AIR) : current.clone());
            inventoryService.saveEquipSlotSnapshot(astPlayer);
            inventoryService.saveAccessorySlotSnapshot(astPlayer);
            inventoryService.syncCurrentEquipmentState(astPlayer);
            astPlayer.getBukkit().updateInventory();
            return true;
        }

        if (!inventoryService.canPlaceInEquipmentGuiSlot(cursor, equipmentType, false)) {
            return false;
        }

        inventory.setItem(slot, cursor.clone());
        event.getView().setCursor(hasCurrent ? current.clone() : new ItemStack(Material.AIR));
        inventoryService.saveEquipSlotSnapshot(astPlayer);
        inventoryService.saveAccessorySlotSnapshot(astPlayer);
        inventoryService.syncCurrentEquipmentState(astPlayer);
        astPlayer.getBukkit().updateInventory();
        return true;
    }

    private boolean isArmorOrOffhandSlot(int slot) {
        return slot >= 36 && slot <= 40;
    }

    private @NotNull EquipmentType equipmentTypeFromPlayerSlot(int slot) {
        return switch (slot) {
            case 36 -> EquipmentType.FEET;
            case 37 -> EquipmentType.LEGS;
            case 38 -> EquipmentType.CHEST;
            case 39 -> EquipmentType.HEAD;
            case 40 -> EquipmentType.OFF_HAND;
            default -> EquipmentType.UNSUPPORTED;
        };
    }
}

