package io.github.maaasu.astralRecord.feature.menu.view;

import io.github.maaasu.astralRecord.AstralRecord;
import io.github.maaasu.astralRecord.feature.inventory.model.EquipmentType;
import io.github.maaasu.astralRecord.feature.inventory.model.InventoryType;
import io.github.maaasu.astralRecord.feature.menu.model.MenuScreen;
import io.github.maaasu.astralRecord.feature.menu.model.MenuShortcutAction;
import io.github.maaasu.astralRecord.feature.menu.model.MenuShortcutSettings;
import io.github.maaasu.astralRecord.feature.menu.view.screen.BaseMenuScreenView;
import io.github.maaasu.astralRecord.feature.menu.view.screen.EquipmentMenuScreenView;
import io.github.maaasu.astralRecord.feature.menu.view.screen.InventorySelectorScreenView;
import io.github.maaasu.astralRecord.feature.menu.view.screen.MainMenuScreenView;
import io.github.maaasu.astralRecord.feature.menu.view.screen.ShortcutSettingsScreenView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * プレイヤーが開くメニュー画面を構築します。
 */
public class MenuView {
    public static final int SIZE = BaseMenuScreenView.SIZE;
    public static final int CLOSE_SLOT = BaseMenuScreenView.CLOSE_SLOT;
    public static final int BACK_SLOT = BaseMenuScreenView.BACK_SLOT;
    public static final int INVENTORY_SELECTOR_SLOT = MainMenuScreenView.INVENTORY_SELECTOR_SLOT;
    public static final int EQUIPMENT_GUI_SLOT = MainMenuScreenView.EQUIPMENT_GUI_SLOT;
    public static final int SHORTCUT_SETTINGS_SLOT = MainMenuScreenView.SHORTCUT_SETTINGS_SLOT;
    public static final int EQUIPMENT_HEAD_SLOT = EquipmentMenuScreenView.EQUIPMENT_HEAD_SLOT;
    public static final int EQUIPMENT_CHEST_SLOT = EquipmentMenuScreenView.EQUIPMENT_CHEST_SLOT;
    public static final int EQUIPMENT_LEGS_SLOT = EquipmentMenuScreenView.EQUIPMENT_LEGS_SLOT;
    public static final int EQUIPMENT_FEET_SLOT = EquipmentMenuScreenView.EQUIPMENT_FEET_SLOT;
    public static final int EQUIPMENT_OFF_HAND_SLOT = EquipmentMenuScreenView.EQUIPMENT_OFF_HAND_SLOT;
    public static final int EQUIPMENT_ACCESSORY_2_SLOT = EquipmentMenuScreenView.EQUIPMENT_ACCESSORY_2_SLOT;
    public static final int EQUIPMENT_ACCESSORY_3_SLOT = EquipmentMenuScreenView.EQUIPMENT_ACCESSORY_3_SLOT;
    public static final int EQUIPMENT_ACCESSORY_4_SLOT = EquipmentMenuScreenView.EQUIPMENT_ACCESSORY_4_SLOT;
    public static final int EQUIPMENT_ACCESSORY_5_SLOT = EquipmentMenuScreenView.EQUIPMENT_ACCESSORY_5_SLOT;
    public static final int EQUIPMENT_ACCESSORY_6_SLOT = EquipmentMenuScreenView.EQUIPMENT_ACCESSORY_6_SLOT;
    public static final int EQUIPMENT_ACCESSORY_7_SLOT = EquipmentMenuScreenView.EQUIPMENT_ACCESSORY_7_SLOT;
    public static final int CRAFT_RESULT_RAW_SLOT = CraftShortcutView.CRAFT_RESULT_RAW_SLOT;
    public static final int CRAFT_SHORTCUT_RAW_SLOT_START = CraftShortcutView.CRAFT_SHORTCUT_RAW_SLOT_START;

    private static final Component MAIN_TITLE = Component.text("AstralRecord メニュー", NamedTextColor.DARK_AQUA);
    private static final Component INVENTORY_TITLE = Component.text("インベントリ選択", NamedTextColor.GOLD);
    private static final Component EQUIPMENT_TITLE = Component.text("装備", NamedTextColor.GOLD);
    private static final Component SHORTCUT_SLOT_TITLE = Component.text("ショートカット設定", NamedTextColor.AQUA);
    private static final Component SHORTCUT_ACTION_TITLE = Component.text("ショートカット項目", NamedTextColor.AQUA);

    private final MainMenuScreenView mainMenuScreenView;
    private final InventorySelectorScreenView inventorySelectorScreenView;
    private final EquipmentMenuScreenView equipmentMenuScreenView;
    private final ShortcutSettingsScreenView shortcutSettingsScreenView;
    private final CraftShortcutView craftShortcutView;

    /**
     * メニュー表示ビューを生成します。
     *
     * @param plugin プラグインインスタンス
     */
    public MenuView(@NotNull AstralRecord plugin) {
        NamespacedKey craftShortcutKey = new NamespacedKey(plugin, "menu_shortcut_slot");
        NamespacedKey craftActionKey = new NamespacedKey(plugin, "menu_shortcut_action");
        NamespacedKey equipmentPlaceholderKey = new NamespacedKey(plugin, "equipment_placeholder");
        this.mainMenuScreenView = new MainMenuScreenView();
        this.inventorySelectorScreenView = new InventorySelectorScreenView();
        this.equipmentMenuScreenView = new EquipmentMenuScreenView(equipmentPlaceholderKey);
        this.shortcutSettingsScreenView = new ShortcutSettingsScreenView();
        this.craftShortcutView = new CraftShortcutView(craftShortcutKey, craftActionKey);
    }

    public void open(@NotNull Player player) {
        Inventory inventory = Bukkit.createInventory(new MenuInventoryHolder(MenuScreen.MAIN), SIZE, MAIN_TITLE);
        mainMenuScreenView.render(inventory);
        player.openInventory(inventory);
    }

    public void openInventorySelector(@NotNull Player player) {
        openInventorySelector(player, null);
    }

    /**
     * 現在選択中の種別を強調してインベントリ選択画面を開きます。
     *
     * @param player 対象プレイヤー
     * @param selectedType 現在表示中の種別（未指定なら強調なし）
     */
    public void openInventorySelector(@NotNull Player player, @Nullable InventoryType selectedType) {
        Inventory inventory = Bukkit.createInventory(new MenuInventoryHolder(MenuScreen.INVENTORY_SELECTOR), SIZE, INVENTORY_TITLE);
        inventorySelectorScreenView.render(inventory, selectedType);
        player.openInventory(inventory);
    }

    public void openEquipmentGui(@NotNull Player player) {
        openEquipmentGui(player, new ItemStack[0]);
    }

    public void openEquipmentGui(
        @NotNull Player player,
        @NotNull ItemStack[] accessories
    ) {
        Inventory inventory = Bukkit.createInventory(new MenuInventoryHolder(MenuScreen.EQUIPMENT_GUI), SIZE, EQUIPMENT_TITLE);
        equipmentMenuScreenView.render(inventory, player, accessories);
        player.openInventory(inventory);
    }

    public void openShortcutSlotSelector(@NotNull Player player, @NotNull MenuShortcutSettings settings) {
        Inventory inventory = Bukkit.createInventory(new MenuInventoryHolder(MenuScreen.SHORTCUT_SLOT_SELECTOR), SIZE, SHORTCUT_SLOT_TITLE);
        shortcutSettingsScreenView.renderSlotSelector(inventory, settings);
        player.openInventory(inventory);
    }

    public void openShortcutActionSelector(
        @NotNull Player player,
        int shortcutSlotIndex,
        @NotNull MenuShortcutAction currentAction
    ) {
        Inventory inventory = Bukkit.createInventory(
            new MenuInventoryHolder(MenuScreen.SHORTCUT_ACTION_SELECTOR, shortcutSlotIndex),
            SIZE,
            SHORTCUT_ACTION_TITLE
        );
        shortcutSettingsScreenView.renderActionSelector(inventory, currentAction);
        player.openInventory(inventory);
    }

    public @NotNull ItemStack createCraftResultIcon() {
        return craftShortcutView.createCraftResultIcon();
    }

    public void renderCraftShortcuts(@NotNull Player player, @NotNull MenuShortcutSettings settings) {
        craftShortcutView.renderCraftShortcuts(player, settings);
    }

    public void clearCraftShortcuts(@NotNull Player player) {
        craftShortcutView.clearCraftShortcuts(player);
    }

    public void clearCraftShortcuts(@NotNull CraftingInventory inventory) {
        craftShortcutView.clearCraftShortcuts(inventory);
    }

    public void removeCraftShortcutItems(@NotNull Player player) {
        craftShortcutView.removeCraftShortcutItems(player);
    }

    public boolean isMenuInventory(@Nullable Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof MenuInventoryHolder;
    }

    public @Nullable MenuScreen getMenuScreen(@Nullable Inventory inventory) {
        if (inventory != null && inventory.getHolder() instanceof MenuInventoryHolder holder) {
            return holder.screen();
        }
        return null;
    }

    public int getShortcutSlotIndex(@Nullable Inventory inventory) {
        if (inventory != null && inventory.getHolder() instanceof MenuInventoryHolder holder) {
            return holder.shortcutSlotIndex();
        }
        return -1;
    }

    public @Nullable InventoryType getInventoryTypeAtSlot(int rawSlot) {
        return inventorySelectorScreenView.getInventoryTypeAtSlot(rawSlot);
    }

    public @Nullable EquipmentType getEquipmentTypeAtSlot(int rawSlot) {
        return equipmentMenuScreenView.getEquipmentTypeAtSlot(rawSlot);
    }

    public boolean isExtendedAccessorySlot(int rawSlot) {
        return equipmentMenuScreenView.isExtendedAccessorySlot(rawSlot);
    }

    public boolean isEquipmentItemSlot(int rawSlot) {
        return equipmentMenuScreenView.isEquipmentItemSlot(rawSlot);
    }

    public @Nullable ItemStack getEquipmentGuiItem(@NotNull Inventory inventory, int slot) {
        return equipmentMenuScreenView.getEquipmentGuiItem(inventory, slot);
    }

    public int getSlotForEquipmentType(@NotNull EquipmentType equipmentType) {
        return equipmentMenuScreenView.getSlotForEquipmentType(equipmentType);
    }

    public int firstEmptyAccessorySlot(@NotNull Inventory inventory) {
        return equipmentMenuScreenView.firstEmptyAccessorySlot(inventory);
    }

    public @NotNull ItemStack[] getAccessoryItems(@NotNull Inventory inventory) {
        return equipmentMenuScreenView.getAccessoryItems(inventory);
    }

    public int getShortcutSettingSlotAtSlot(int rawSlot) {
        return shortcutSettingsScreenView.getShortcutSettingSlotAtSlot(rawSlot);
    }

    public @Nullable MenuShortcutAction getShortcutActionAtSlot(int rawSlot) {
        return shortcutSettingsScreenView.getShortcutActionAtSlot(rawSlot);
    }

    public int getCraftShortcutIndex(int rawSlot) {
        return craftShortcutView.getCraftShortcutIndex(rawSlot);
    }
}
