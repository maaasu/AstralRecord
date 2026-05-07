package io.github.maaasu.astralRecord.feature.menu.view;

import io.github.maaasu.astralRecord.feature.menu.model.MenuShortcutAction;
import io.github.maaasu.astralRecord.feature.menu.model.MenuShortcutSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

final class CraftShortcutView {
    static final int CRAFT_RESULT_RAW_SLOT = 0;
    static final int CRAFT_SHORTCUT_RAW_SLOT_START = 1;

    private final NamespacedKey craftShortcutKey;
    private final NamespacedKey craftActionKey;

    CraftShortcutView(@NotNull NamespacedKey craftShortcutKey, @NotNull NamespacedKey craftActionKey) {
        this.craftShortcutKey = craftShortcutKey;
        this.craftActionKey = craftActionKey;
    }

    @NotNull ItemStack createCraftResultIcon() {
        ItemStack itemStack = createItem(
            Material.NETHER_STAR,
            Component.text("メニュー", NamedTextColor.AQUA),
            List.of(Component.text("クリックしてメニューを開きます。", NamedTextColor.GRAY))
        );
        markCraftShortcutIcon(itemStack, -1, MenuShortcutAction.MAIN_MENU);
        return itemStack;
    }

    void renderCraftShortcuts(@NotNull Player player, @NotNull MenuShortcutSettings settings) {
        if (!(player.getOpenInventory().getTopInventory() instanceof CraftingInventory inventory)) {
            return;
        }
        if (!canOverwriteCraftMatrix(inventory)) {
            return;
        }

        ItemStack[] matrix = new ItemStack[MenuShortcutSettings.SLOT_COUNT];
        for (int slot = 0; slot < MenuShortcutSettings.SLOT_COUNT; slot++) {
            matrix[slot] = createCraftShortcutIcon(slot, settings.getAction(slot));
        }
        inventory.setMatrix(matrix);
        inventory.setResult(createCraftResultIcon());
        player.updateInventory();
    }

    void clearCraftShortcuts(@NotNull Player player) {
        if (player.getOpenInventory().getTopInventory() instanceof CraftingInventory inventory) {
            clearCraftShortcuts(inventory);
            player.updateInventory();
        }
    }

    void clearCraftShortcuts(@NotNull CraftingInventory inventory) {
        if (!isShortcutMatrix(inventory)) {
            return;
        }
        inventory.setMatrix(new ItemStack[] {
            new ItemStack(Material.AIR),
            new ItemStack(Material.AIR),
            new ItemStack(Material.AIR),
            new ItemStack(Material.AIR)
        });
        inventory.setResult(new ItemStack(Material.AIR));
    }

    void removeCraftShortcutItems(@NotNull Player player) {
        PlayerInventory inventory = player.getInventory();
        removeCraftShortcutItems(inventory);
        if (isCraftShortcutIcon(player.getItemOnCursor())) {
            player.setItemOnCursor(new ItemStack(Material.AIR));
        }
        player.updateInventory();
    }

    int getCraftShortcutIndex(int rawSlot) {
        int index = rawSlot - CRAFT_SHORTCUT_RAW_SLOT_START;
        return index >= 0 && index < MenuShortcutSettings.SLOT_COUNT ? index : -1;
    }

    boolean isCraftShortcutIcon(@Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR || !itemStack.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = itemStack.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(craftShortcutKey, PersistentDataType.INTEGER);
    }

    private @NotNull ItemStack createCraftShortcutIcon(int shortcutSlotIndex, @NotNull MenuShortcutAction action) {
        ItemStack itemStack = createItem(
            action.getMaterial(),
            Component.text(action.getDisplayNameJa(), action.getColor()),
            List.of(Component.text("クリックして実行します。", NamedTextColor.GRAY))
        );
        markCraftShortcutIcon(itemStack, shortcutSlotIndex, action);
        return itemStack;
    }

    private void markCraftShortcutIcon(
        @NotNull ItemStack itemStack,
        int shortcutSlotIndex,
        @NotNull MenuShortcutAction action
    ) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(craftShortcutKey, PersistentDataType.INTEGER, shortcutSlotIndex);
            meta.getPersistentDataContainer().set(craftActionKey, PersistentDataType.STRING, action.getCode());
            itemStack.setItemMeta(meta);
        }
    }

    private boolean canOverwriteCraftMatrix(@NotNull CraftingInventory inventory) {
        for (ItemStack itemStack : inventory.getMatrix()) {
            if (itemStack != null && itemStack.getType() != Material.AIR && !isCraftShortcutIcon(itemStack)) {
                return false;
            }
        }
        return true;
    }

    private boolean isShortcutMatrix(@NotNull CraftingInventory inventory) {
        for (ItemStack itemStack : inventory.getMatrix()) {
            if (itemStack != null && itemStack.getType() != Material.AIR && !isCraftShortcutIcon(itemStack)) {
                return false;
            }
        }
        return true;
    }

    private void removeCraftShortcutItems(@NotNull Inventory inventory) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (isCraftShortcutIcon(inventory.getItem(slot))) {
                inventory.setItem(slot, new ItemStack(Material.AIR));
            }
        }
    }

    private @NotNull ItemStack createItem(
        @NotNull Material material,
        @NotNull Component name,
        @NotNull List<Component> lore
    ) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.values());
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }
}
