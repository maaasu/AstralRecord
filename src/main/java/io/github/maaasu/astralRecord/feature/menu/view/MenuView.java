package io.github.maaasu.astralRecord.feature.menu.view;

import io.github.maaasu.astralRecord.AstralRecord;
import io.github.maaasu.astralRecord.feature.inventory.model.InventoryType;
import io.github.maaasu.astralRecord.feature.menu.model.MenuScreen;
import io.github.maaasu.astralRecord.feature.menu.model.MenuShortcutAction;
import io.github.maaasu.astralRecord.feature.menu.model.MenuShortcutSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * プレイヤーが開くメニュー画面を構築します。
 */
public class MenuView {
    public static final int SIZE = 27;
    public static final int CLOSE_SLOT = 22;
    public static final int BACK_SLOT = 18;
    public static final int INVENTORY_SELECTOR_SLOT = 11;
    public static final int SHORTCUT_SETTINGS_SLOT = 15;
    public static final int CRAFT_RESULT_RAW_SLOT = 0;
    public static final int CRAFT_SHORTCUT_RAW_SLOT_START = 1;

    private static final Component MAIN_TITLE = Component.text("AstralRecord Menu", NamedTextColor.DARK_AQUA);
    private static final Component INVENTORY_TITLE = Component.text("Inventory Menu", NamedTextColor.GOLD);
    private static final Component SHORTCUT_SLOT_TITLE = Component.text("Shortcut Settings", NamedTextColor.AQUA);
    private static final Component SHORTCUT_ACTION_TITLE = Component.text("Shortcut Item", NamedTextColor.AQUA);
    private static final int[] INVENTORY_TYPE_SLOTS = {10, 12, 14, 16};
    private static final int[] SHORTCUT_SLOT_SLOTS = {10, 12, 14, 16};
    private static final int[] SHORTCUT_ACTION_SLOTS = {9, 10, 11, 13, 15, 16};

    private final NamespacedKey craftShortcutKey;
    private final NamespacedKey craftActionKey;

    /**
     * メニュー表示ビューを生成します。
     *
     * @param plugin プラグインインスタンス
     */
    public MenuView(@NotNull AstralRecord plugin) {
        this.craftShortcutKey = new NamespacedKey(plugin, "menu_shortcut_slot");
        this.craftActionKey = new NamespacedKey(plugin, "menu_shortcut_action");
    }

    /**
     * メインメニュー画面を開きます。
     *
     * @param player 表示対象プレイヤー
     */
    public void open(@NotNull Player player) {
        Inventory inventory = Bukkit.createInventory(new Holder(MenuScreen.MAIN), SIZE, MAIN_TITLE);
        renderMain(inventory);
        player.openInventory(inventory);
    }

    /**
     * インベントリ選択メニューを開きます。
     *
     * @param player 表示対象プレイヤー
     */
    public void openInventorySelector(@NotNull Player player) {
        Inventory inventory = Bukkit.createInventory(new Holder(MenuScreen.INVENTORY_SELECTOR), SIZE, INVENTORY_TITLE);
        renderInventorySelector(inventory);
        player.openInventory(inventory);
    }

    /**
     * ショートカットスロット選択メニューを開きます。
     *
     * @param player 表示対象プレイヤー
     * @param settings 現在のショートカット設定
     */
    public void openShortcutSlotSelector(@NotNull Player player, @NotNull MenuShortcutSettings settings) {
        Inventory inventory = Bukkit.createInventory(new Holder(MenuScreen.SHORTCUT_SLOT_SELECTOR), SIZE, SHORTCUT_SLOT_TITLE);
        renderShortcutSlotSelector(inventory, settings);
        player.openInventory(inventory);
    }

    /**
     * ショートカット割り当て項目選択メニューを開きます。
     *
     * @param player 表示対象プレイヤー
     * @param shortcutSlotIndex 設定対象のショートカットスロット番号
     * @param currentAction 現在割り当てられている項目
     */
    public void openShortcutActionSelector(
        @NotNull Player player,
        int shortcutSlotIndex,
        @NotNull MenuShortcutAction currentAction
    ) {
        Inventory inventory = Bukkit.createInventory(
            new Holder(MenuScreen.SHORTCUT_ACTION_SELECTOR, shortcutSlotIndex),
            SIZE,
            SHORTCUT_ACTION_TITLE
        );
        renderShortcutActionSelector(inventory, currentAction);
        player.openInventory(inventory);
    }

    /**
     * クラフト成果物スロットに表示するメニュー起動アイテムを作成します。
     *
     * @return メニュー起動用 ItemStack
     */
    public @NotNull ItemStack createCraftResultIcon() {
        return createItem(
            Material.NETHER_STAR,
            Component.text("Menu", NamedTextColor.AQUA),
            List.of(Component.text("クリックしてメニューを開く", NamedTextColor.GRAY))
        );
    }

    /**
     * バニラ 2x2 クラフト欄へショートカットアイテムを表示します。
     *
     * @param player 表示対象プレイヤー
     * @param settings ショートカット設定
     */
    public void renderCraftShortcuts(@NotNull Player player, @NotNull MenuShortcutSettings settings) {
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

    /**
     * クラフト欄に表示しているショートカットアイテムを消去します。
     *
     * @param player 消去対象プレイヤー
     */
    public void clearCraftShortcuts(@NotNull Player player) {
        if (player.getOpenInventory().getTopInventory() instanceof CraftingInventory inventory) {
            clearCraftShortcuts(inventory);
            player.updateInventory();
        }
    }

    /**
     * 指定クラフトインベントリのショートカット表示を消去します。
     *
     * @param inventory 消去対象クラフトインベントリ
     */
    public void clearCraftShortcuts(@NotNull CraftingInventory inventory) {
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

    /**
     * 指定インベントリがメニュー画面か判定します。
     *
     * @param inventory 判定対象
     * @return メニュー画面なら true
     */
    public boolean isMenuInventory(@Nullable Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof Holder;
    }

    /**
     * 指定インベントリのメニュー画面種別を返します。
     *
     * @param inventory 判定対象
     * @return メニュー画面種別。メニューでない場合は null
     */
    public @Nullable MenuScreen getMenuScreen(@Nullable Inventory inventory) {
        if (inventory != null && inventory.getHolder() instanceof Holder holder) {
            return holder.screen;
        }
        return null;
    }

    /**
     * ショートカット項目選択画面が対象にしているクラフトスロット番号を返します。
     *
     * @param inventory 判定対象
     * @return 0始まりのショートカットスロット番号。対象外の場合は -1
     */
    public int getShortcutSlotIndex(@Nullable Inventory inventory) {
        if (inventory != null && inventory.getHolder() instanceof Holder holder) {
            return holder.shortcutSlotIndex;
        }
        return -1;
    }

    /**
     * インベントリ選択画面のクリック位置から対象タイプを取得します。
     *
     * @param rawSlot クリックされた raw slot
     * @return 対象インベントリタイプ。対象外の場合は null
     */
    public @Nullable InventoryType getInventoryTypeAtSlot(int rawSlot) {
        InventoryType[] types = InventoryType.values();
        for (int i = 0; i < INVENTORY_TYPE_SLOTS.length && i < types.length; i++) {
            if (INVENTORY_TYPE_SLOTS[i] == rawSlot) {
                return types[i];
            }
        }
        return null;
    }

    /**
     * ショートカットスロット選択画面のクリック位置からクラフトスロット番号を取得します。
     *
     * @param rawSlot クリックされた raw slot
     * @return 0始まりのショートカットスロット番号。対象外の場合は -1
     */
    public int getShortcutSettingSlotAtSlot(int rawSlot) {
        for (int i = 0; i < SHORTCUT_SLOT_SLOTS.length; i++) {
            if (SHORTCUT_SLOT_SLOTS[i] == rawSlot) {
                return i;
            }
        }
        return -1;
    }

    /**
     * ショートカット項目選択画面のクリック位置から割り当て項目を取得します。
     *
     * @param rawSlot クリックされた raw slot
     * @return 割り当て項目。対象外の場合は null
     */
    public @Nullable MenuShortcutAction getShortcutActionAtSlot(int rawSlot) {
        MenuShortcutAction[] actions = selectableShortcutActions();
        for (int i = 0; i < SHORTCUT_ACTION_SLOTS.length && i < actions.length; i++) {
            if (SHORTCUT_ACTION_SLOTS[i] == rawSlot) {
                return actions[i];
            }
        }
        return null;
    }

    /**
     * クラフト欄 raw slot からショートカットスロット番号を取得します。
     *
     * @param rawSlot クリックされた raw slot
     * @return 0始まりのショートカットスロット番号。対象外の場合は -1
     */
    public int getCraftShortcutIndex(int rawSlot) {
        int index = rawSlot - CRAFT_SHORTCUT_RAW_SLOT_START;
        return index >= 0 && index < MenuShortcutSettings.SLOT_COUNT ? index : -1;
    }

    /**
     * 指定 ItemStack がメニューのクラフトショートカット表示アイテムか判定します。
     *
     * @param itemStack 判定対象
     * @return ショートカット表示アイテムなら true
     */
    public boolean isCraftShortcutIcon(@Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR || !itemStack.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = itemStack.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(craftShortcutKey, PersistentDataType.INTEGER);
    }

    /**
     * メニュー画面の各スロットへ表示アイテムを配置します。
     *
     * @param inventory 描画対象インベントリ
     */
    private void renderMain(@NotNull Inventory inventory) {
        fill(inventory);
        inventory.setItem(10, createItem(
            Material.PLAYER_HEAD,
            Component.text("Status", NamedTextColor.GREEN),
            List.of(Component.text("ステータス画面", NamedTextColor.GRAY))
        ));
        inventory.setItem(INVENTORY_SELECTOR_SLOT, createItem(
            Material.CHEST,
            Component.text("Inventory", NamedTextColor.YELLOW),
            List.of(Component.text("表示するインベントリを選択", NamedTextColor.GRAY))
        ));
        inventory.setItem(SHORTCUT_SETTINGS_SLOT, createItem(
            Material.REPEATER,
            Component.text("Shortcut", NamedTextColor.AQUA),
            List.of(Component.text("クラフト欄ショートカットを設定", NamedTextColor.GRAY))
        ));
        inventory.setItem(16, createItem(
            Material.BOOK,
            Component.text("Guide", NamedTextColor.LIGHT_PURPLE),
            List.of(Component.text("ガイド", NamedTextColor.GRAY))
        ));
        inventory.setItem(CLOSE_SLOT, closeItem());
    }

    /**
     * インベントリ選択メニューを描画します。
     *
     * @param inventory 描画対象インベントリ
     */
    private void renderInventorySelector(@NotNull Inventory inventory) {
        fill(inventory);
        InventoryType[] types = InventoryType.values();
        for (int i = 0; i < INVENTORY_TYPE_SLOTS.length && i < types.length; i++) {
            MenuShortcutAction action = actionForInventoryType(types[i]);
            inventory.setItem(INVENTORY_TYPE_SLOTS[i], createActionItem(
                action,
                List.of(Component.text("クリックして表示", NamedTextColor.GRAY))
            ));
        }
        inventory.setItem(BACK_SLOT, backItem());
        inventory.setItem(CLOSE_SLOT, closeItem());
    }

    /**
     * ショートカットスロット選択メニューを描画します。
     *
     * @param inventory 描画対象インベントリ
     * @param settings 現在のショートカット設定
     */
    private void renderShortcutSlotSelector(@NotNull Inventory inventory, @NotNull MenuShortcutSettings settings) {
        fill(inventory);
        for (int slot = 0; slot < MenuShortcutSettings.SLOT_COUNT; slot++) {
            MenuShortcutAction action = settings.getAction(slot);
            inventory.setItem(SHORTCUT_SLOT_SLOTS[slot], createItem(
                action.getMaterial(),
                Component.text("Slot " + (slot + 1) + ": " + action.getDisplayNameJa(), action.getColor()),
                List.of(Component.text("クリックして割り当てを変更", NamedTextColor.GRAY))
            ));
        }
        inventory.setItem(BACK_SLOT, backItem());
        inventory.setItem(CLOSE_SLOT, closeItem());
    }

    /**
     * ショートカット項目選択メニューを描画します。
     *
     * @param inventory 描画対象インベントリ
     * @param currentAction 現在割り当てられている項目
     */
    private void renderShortcutActionSelector(@NotNull Inventory inventory, @NotNull MenuShortcutAction currentAction) {
        fill(inventory);
        MenuShortcutAction[] actions = selectableShortcutActions();
        for (int i = 0; i < SHORTCUT_ACTION_SLOTS.length && i < actions.length; i++) {
            MenuShortcutAction action = actions[i];
            Component current = action == currentAction
                ? Component.text("現在の設定", NamedTextColor.GREEN)
                : Component.text("クリックして設定", NamedTextColor.GRAY);
            inventory.setItem(SHORTCUT_ACTION_SLOTS[i], createActionItem(action, List.of(current)));
        }
        inventory.setItem(BACK_SLOT, backItem());
        inventory.setItem(CLOSE_SLOT, closeItem());
    }

    /**
     * クラフトショートカット用の ItemStack を作成します。
     *
     * @param shortcutSlotIndex 0始まりのショートカットスロット番号
     * @param action 表示する項目
     * @return 表示設定済み ItemStack
     */
    private @NotNull ItemStack createCraftShortcutIcon(int shortcutSlotIndex, @NotNull MenuShortcutAction action) {
        ItemStack itemStack = createActionItem(
            action,
            List.of(Component.text("クリックして実行", NamedTextColor.GRAY))
        );
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(craftShortcutKey, PersistentDataType.INTEGER, shortcutSlotIndex);
            meta.getPersistentDataContainer().set(craftActionKey, PersistentDataType.STRING, action.getCode());
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    /**
     * 指定アクションの表示アイテムを作成します。
     *
     * @param action 表示対象アクション
     * @param lore 説明文
     * @return 表示設定済み ItemStack
     */
    private @NotNull ItemStack createActionItem(
        @NotNull MenuShortcutAction action,
        @NotNull List<Component> lore
    ) {
        return createItem(
            action.getMaterial(),
            Component.text(action.getDisplayNameJa(), action.getColor()),
            lore
        );
    }

    /**
     * インベントリタイプに対応するショートカット項目を返します。
     *
     * @param inventoryType インベントリタイプ
     * @return 対応するショートカット項目
     */
    private @NotNull MenuShortcutAction actionForInventoryType(@NotNull InventoryType inventoryType) {
        for (MenuShortcutAction action : MenuShortcutAction.values()) {
            if (action.getInventoryType() == inventoryType) {
                return action;
            }
        }
        return MenuShortcutAction.NONE;
    }

    /**
     * ショートカットへ割り当て可能な項目を返します。
     *
     * @return 割り当て可能項目
     */
    private @NotNull MenuShortcutAction[] selectableShortcutActions() {
        return new MenuShortcutAction[] {
            MenuShortcutAction.NONE,
            MenuShortcutAction.MAIN_MENU,
            MenuShortcutAction.INVENTORY_NORMAL,
            MenuShortcutAction.INVENTORY_EQUIPMENT,
            MenuShortcutAction.INVENTORY_RUNE,
            MenuShortcutAction.INVENTORY_CURRENCY
        };
    }

    /**
     * クラフトマトリクスをショートカット表示で上書きしてよいか判定します。
     *
     * @param inventory 判定対象クラフトインベントリ
     * @return 上書き可能なら true
     */
    private boolean canOverwriteCraftMatrix(@NotNull CraftingInventory inventory) {
        for (ItemStack itemStack : inventory.getMatrix()) {
            if (itemStack != null && itemStack.getType() != Material.AIR && !isCraftShortcutIcon(itemStack)) {
                return false;
            }
        }
        return true;
    }

    /**
     * クラフトマトリクスがショートカット表示だけで構成されているか判定します。
     *
     * @param inventory 判定対象クラフトインベントリ
     * @return 空またはショートカット表示のみなら true
     */
    private boolean isShortcutMatrix(@NotNull CraftingInventory inventory) {
        for (ItemStack itemStack : inventory.getMatrix()) {
            if (itemStack != null && itemStack.getType() != Material.AIR && !isCraftShortcutIcon(itemStack)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 背景スロットを埋めます。
     *
     * @param inventory 描画対象インベントリ
     */
    private void fill(@NotNull Inventory inventory) {
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "), List.of());
        for (int slot = 0; slot < SIZE; slot++) {
            inventory.setItem(slot, filler);
        }
    }

    /**
     * 戻るボタンを作成します。
     *
     * @return 戻るボタン
     */
    private @NotNull ItemStack backItem() {
        return createItem(
            Material.ARROW,
            Component.text("Back", NamedTextColor.WHITE),
            List.of(Component.text("前の画面へ戻る", NamedTextColor.GRAY))
        );
    }

    /**
     * 閉じるボタンを作成します。
     *
     * @return 閉じるボタン
     */
    private @NotNull ItemStack closeItem() {
        return createItem(
            Material.BARRIER,
            Component.text("Close", NamedTextColor.RED),
            List.of(Component.text("閉じる", NamedTextColor.GRAY))
        );
    }

    /**
     * メニュー表示用の ItemStack を作成します。
     *
     * @param material アイテム素材
     * @param name 表示名
     * @param lore 説明文
     * @return 表示設定済み ItemStack
     */
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

    /**
     * Bukkit インベントリをメニュー画面として識別するための Holder。
     */
    private static final class Holder implements InventoryHolder {
        private final MenuScreen screen;
        private final int shortcutSlotIndex;

        /**
         * 指定画面の Holder を生成します。
         *
         * @param screen メニュー画面種別
         */
        private Holder(@NotNull MenuScreen screen) {
            this(screen, -1);
        }

        /**
         * 指定画面とショートカットスロット番号の Holder を生成します。
         *
         * @param screen メニュー画面種別
         * @param shortcutSlotIndex 0始まりのショートカットスロット番号
         */
        private Holder(@NotNull MenuScreen screen, int shortcutSlotIndex) {
            this.screen = screen;
            this.shortcutSlotIndex = shortcutSlotIndex;
        }

        /**
         * Holder の契約を満たすための空インベントリを返します。
         *
         * @return メニュー形式のインベントリ
         */
        @Override
        public @NotNull Inventory getInventory() {
            return Bukkit.createInventory(this, SIZE, MAIN_TITLE);
        }
    }
}
