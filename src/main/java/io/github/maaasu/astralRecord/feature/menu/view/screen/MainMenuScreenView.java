package io.github.maaasu.astralRecord.feature.menu.view.screen;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class MainMenuScreenView extends BaseMenuScreenView {
    public static final int STATUS_SLOT = 20;
    public static final int INVENTORY_SELECTOR_SLOT = 21;
    public static final int EQUIPMENT_GUI_SLOT = 22;
    public static final int SHORTCUT_SETTINGS_SLOT = 23;
    public static final int GUIDE_SLOT = 24;

    public void render(@NotNull Inventory inventory) {
        fill(inventory);
        inventory.setItem(STATUS_SLOT, createItem(
            Material.PLAYER_HEAD,
            Component.text("ステータス", NamedTextColor.GREEN),
            List.of(Component.text("キャラクターの状態を確認します。", NamedTextColor.GRAY))
        ));
        inventory.setItem(INVENTORY_SELECTOR_SLOT, createItem(
            Material.CHEST,
            Component.text("インベントリ", NamedTextColor.YELLOW),
            List.of(Component.text("表示するインベントリを選択します。", NamedTextColor.GRAY))
        ));
        inventory.setItem(EQUIPMENT_GUI_SLOT, createItem(
            Material.NETHERITE_CHESTPLATE,
            Component.text("装備", NamedTextColor.GOLD),
            List.of(Component.text("防具・オフハンド・アクセサリーを管理します。", NamedTextColor.GRAY))
        ));
        inventory.setItem(SHORTCUT_SETTINGS_SLOT, createItem(
            Material.REPEATER,
            Component.text("ショートカット", NamedTextColor.AQUA),
            List.of(Component.text("クラフト欄ショートカットを設定します。", NamedTextColor.GRAY))
        ));
        inventory.setItem(GUIDE_SLOT, createItem(
            Material.BOOK,
            Component.text("ガイド", NamedTextColor.LIGHT_PURPLE),
            List.of(Component.text("ヘルプを開きます。", NamedTextColor.GRAY))
        ));
        inventory.setItem(CLOSE_SLOT, closeItem());
    }
}
