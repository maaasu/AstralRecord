package io.github.maaasu.astralRecord.feature.menu.event;

import io.github.maaasu.astralRecord.AstralRecord;
import io.github.maaasu.astralRecord.core.event.AbstractEventHandler;
import io.github.maaasu.astralRecord.feature.account.model.AccountMode;
import io.github.maaasu.astralRecord.feature.inventory.model.InventoryType;
import io.github.maaasu.astralRecord.feature.inventory.service.InventoryService;
import io.github.maaasu.astralRecord.feature.menu.model.MenuScreen;
import io.github.maaasu.astralRecord.feature.menu.model.MenuShortcutAction;
import io.github.maaasu.astralRecord.feature.menu.model.MenuShortcutSettings;
import io.github.maaasu.astralRecord.feature.menu.repository.MenuShortcutRepository;
import io.github.maaasu.astralRecord.feature.menu.sound.MenuSound;
import io.github.maaasu.astralRecord.feature.menu.view.MenuView;
import io.github.maaasu.astralRecord.feature.player.AstPlayerCache;
import io.github.maaasu.astralRecord.feature.player.model.AstPlayer;
import io.github.maaasu.astralRecord.infrastructure.logging.LogId;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * プレイヤーインベントリのクラフト欄からメニューとショートカットを開くイベントハンドラ。
 */
public class MenuOpenEventHandler extends AbstractEventHandler {

    private final AstralRecord plugin;
    private final MenuView menuView;
    private final MenuShortcutRepository shortcutRepository;
    private final InventoryService inventoryService;
    private final Set<UUID> craftRenderSuppressed = ConcurrentHashMap.newKeySet();

    /**
     * メニュー起動イベントハンドラを生成します。
     *
     * @param plugin プラグインインスタンス
     * @param menuView メニュー表示ビュー
     * @param shortcutRepository ショートカット設定リポジトリ
     * @param inventoryService インベントリ表示サービス
     */
    public MenuOpenEventHandler(
        @NotNull AstralRecord plugin,
        @NotNull MenuView menuView,
        @NotNull MenuShortcutRepository shortcutRepository,
        @NotNull InventoryService inventoryService
    ) {
        this.plugin = plugin;
        this.menuView = menuView;
        this.shortcutRepository = shortcutRepository;
        this.inventoryService = inventoryService;
    }

    /**
     * プレイヤーインベントリのクラフト欄にメニューショートカットを表示します。
     *
     * @param event クラフト準備イベント
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        runSafely(() -> {
            if (!isPlayerCraftingInventory(event.getInventory())) {
                return;
            }
            if (!(event.getView().getPlayer() instanceof Player player)) {
                return;
            }
            renderCraftShortcuts(player);
        }, LogId.E_5600, "prepare");
    }

    /**
     * インベントリを開いた直後にクラフト欄ショートカットを再描画します。
     *
     * @param event インベントリオープンイベント
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        runSafely(() -> {
            if (event.getPlayer() instanceof Player player) {
                scheduleCraftShortcutRender(player);
            }
        }, LogId.E_5600, event.getPlayer().getName());
    }

    /**
     * クラフト欄を閉じるときにショートカット表示アイテムを消去します。
     *
     * @param event インベントリクローズイベント
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        runSafely(() -> {
            if (event.getInventory() instanceof CraftingInventory inventory) {
                menuView.clearCraftShortcuts(inventory);
            }
        }, LogId.E_5600, event.getPlayer().getName());
    }

    /**
     * クラフト成果物スロット、ショートカット、メニュー画面内のクリックを処理します。
     *
     * @param event インベントリクリックイベント
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        runSafely(() -> {
            if (menuView.isMenuInventory(event.getView().getTopInventory())) {
                event.setCancelled(true);
                handleMenuClick(event);
                return;
            }

            if (!isCraftMenuClick(event)) {
                return;
            }
            if (!(event.getWhoClicked() instanceof Player player) || !isPlayerMode(player)) {
                return;
            }

            event.setCancelled(true);
            if (event.getRawSlot() == MenuView.CRAFT_RESULT_RAW_SLOT) {
                MenuSound.OPEN.play(player);
                openMainMenu(player);
                return;
            }

            int shortcutIndex = menuView.getCraftShortcutIndex(event.getRawSlot());
            if (shortcutIndex >= 0) {
                MenuShortcutAction action = settings(player).getAction(shortcutIndex);
                executeShortcutAction(player, action);
            }
        }, LogId.E_5600, event.getWhoClicked().getName());
    }

    /**
     * メニュー画面内やショートカットクラフト欄へのドラッグ操作を抑止します。
     *
     * @param event インベントリドラッグイベント
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        runSafely(() -> {
            if (menuView.isMenuInventory(event.getView().getTopInventory())) {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player player) {
                    MenuSound.DENY.play(player);
                }
                return;
            }

            if (!(event.getWhoClicked() instanceof Player player) || !isPlayerMode(player)) {
                return;
            }
            for (int rawSlot : event.getRawSlots()) {
                if (rawSlot >= MenuView.CRAFT_RESULT_RAW_SLOT
                    && rawSlot < MenuView.CRAFT_RESULT_RAW_SLOT + craftMenuSlotCount()) {
                    event.setCancelled(true);
                    scheduleCraftShortcutRender(player);
                    return;
                }
            }
        }, LogId.E_5600, event.getWhoClicked().getName());
    }

    /**
     * メニュー GUI 内のクリック内容に応じて画面遷移または操作を実行します。
     *
     * @param event インベントリクリックイベント
     */
    private void handleMenuClick(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getRawSlot() == MenuView.CLOSE_SLOT) {
            MenuSound.CLOSE.play(player);
            player.closeInventory();
            return;
        }

        MenuScreen screen = menuView.getMenuScreen(event.getView().getTopInventory());
        if (screen == null) {
            return;
        }

        switch (screen) {
            case MAIN -> handleMainMenuClick(player, event.getRawSlot());
            case INVENTORY_SELECTOR -> handleInventorySelectorClick(player, event.getRawSlot());
            case SHORTCUT_SLOT_SELECTOR -> handleShortcutSlotSelectorClick(player, event.getRawSlot());
            case SHORTCUT_ACTION_SELECTOR -> handleShortcutActionSelectorClick(player, event.getRawSlot());
        }
    }

    /**
     * メインメニューのクリックを処理します。
     *
     * @param player 操作プレイヤー
     * @param rawSlot クリックされた raw slot
     */
    private void handleMainMenuClick(@NotNull Player player, int rawSlot) {
        if (rawSlot == MenuView.INVENTORY_SELECTOR_SLOT) {
            MenuSound.SELECT.play(player);
            menuView.openInventorySelector(player);
            return;
        }
        if (rawSlot == MenuView.SHORTCUT_SETTINGS_SLOT) {
            MenuSound.SELECT.play(player);
            menuView.openShortcutSlotSelector(player, settings(player));
            return;
        }
        MenuSound.DENY.play(player);
    }

    /**
     * インベントリ選択メニューのクリックを処理します。
     *
     * @param player 操作プレイヤー
     * @param rawSlot クリックされた raw slot
     */
    private void handleInventorySelectorClick(@NotNull Player player, int rawSlot) {
        if (rawSlot == MenuView.BACK_SLOT) {
            MenuSound.SELECT.play(player);
            menuView.open(player);
            return;
        }

        InventoryType inventoryType = menuView.getInventoryTypeAtSlot(rawSlot);
        if (inventoryType == null) {
            MenuSound.DENY.play(player);
            return;
        }

        applyInventoryShortcut(player, inventoryType);
    }

    /**
     * ショートカットスロット選択メニューのクリックを処理します。
     *
     * @param player 操作プレイヤー
     * @param rawSlot クリックされた raw slot
     */
    private void handleShortcutSlotSelectorClick(@NotNull Player player, int rawSlot) {
        if (rawSlot == MenuView.BACK_SLOT) {
            MenuSound.SELECT.play(player);
            menuView.open(player);
            return;
        }

        int shortcutSlot = menuView.getShortcutSettingSlotAtSlot(rawSlot);
        if (shortcutSlot < 0) {
            MenuSound.DENY.play(player);
            return;
        }

        MenuSound.SELECT.play(player);
        menuView.openShortcutActionSelector(player, shortcutSlot, settings(player).getAction(shortcutSlot));
    }

    /**
     * ショートカット項目選択メニューのクリックを処理します。
     *
     * @param player 操作プレイヤー
     * @param rawSlot クリックされた raw slot
     */
    private void handleShortcutActionSelectorClick(@NotNull Player player, int rawSlot) {
        if (rawSlot == MenuView.BACK_SLOT) {
            MenuSound.SELECT.play(player);
            menuView.openShortcutSlotSelector(player, settings(player));
            return;
        }

        MenuShortcutAction action = menuView.getShortcutActionAtSlot(rawSlot);
        if (action == null) {
            MenuSound.DENY.play(player);
            return;
        }

        AstPlayer astPlayer = AstPlayerCache.get(player);
        if (astPlayer == null) {
            MenuSound.DENY.play(player);
            return;
        }

        int shortcutSlot = menuView.getShortcutSlotIndex(player.getOpenInventory().getTopInventory());
        shortcutRepository.updateSlot(astPlayer.getAccount().getUuid(), shortcutSlot, action);
        MenuSound.SELECT.play(player);
        scheduleCraftShortcutRender(player);
        menuView.openShortcutSlotSelector(player, settings(player));
    }

    /**
     * ショートカット項目を実行します。
     *
     * @param player 操作プレイヤー
     * @param action 実行するショートカット項目
     */
    private void executeShortcutAction(@NotNull Player player, @NotNull MenuShortcutAction action) {
        if (action == MenuShortcutAction.NONE) {
            MenuSound.SELECT.play(player);
            menuView.openShortcutSlotSelector(player, settings(player));
            return;
        }
        if (action == MenuShortcutAction.MAIN_MENU) {
            MenuSound.OPEN.play(player);
            openMainMenu(player);
            return;
        }
        if (action.getInventoryType() != null) {
            applyInventoryShortcut(player, action.getInventoryType());
            return;
        }
        MenuSound.DENY.play(player);
    }

    /**
     * 指定インベントリタイプをプレイヤー GUI へ反映します。
     *
     * @param player 操作プレイヤー
     * @param inventoryType 表示するインベントリタイプ
     */
    private void applyInventoryShortcut(@NotNull Player player, @NotNull InventoryType inventoryType) {
        AstPlayer astPlayer = AstPlayerCache.get(player);
        if (astPlayer == null || !astPlayer.getAccount().getMode().shouldReflectInventoryToGui()) {
            MenuSound.DENY.play(player);
            return;
        }

        MenuSound.SELECT.play(player);
        suppressCraftRendering(player);
        menuView.clearCraftShortcuts(player);
        inventoryService.applyInventoryToGui(astPlayer, inventoryType);
        player.closeInventory();
        plugin.getServer().getScheduler().runTask(plugin, () -> resumeCraftRendering(player));
    }

    /**
     * クラフト欄表示アイテムを消去してメインメニューを開きます。
     *
     * @param player 操作プレイヤー
     */
    private void openMainMenu(@NotNull Player player) {
        suppressCraftRendering(player);
        menuView.clearCraftShortcuts(player);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            menuView.open(player);
            resumeCraftRendering(player);
        });
    }

    /**
     * 次 tick にクラフトショートカットを再描画します。
     *
     * @param player 表示対象プレイヤー
     */
    private void scheduleCraftShortcutRender(@NotNull Player player) {
        plugin.getServer().getScheduler().runTask(plugin, () -> renderCraftShortcuts(player));
    }

    /**
     * クラフトショートカットを現在の設定で描画します。
     *
     * @param player 表示対象プレイヤー
     */
    private void renderCraftShortcuts(@NotNull Player player) {
        if (craftRenderSuppressed.contains(player.getUniqueId())) {
            return;
        }
        AstPlayer astPlayer = AstPlayerCache.get(player);
        if (astPlayer == null || astPlayer.getAccount().getMode() != AccountMode.PLAYER) {
            return;
        }
        menuView.renderCraftShortcuts(player, shortcutRepository.findByAccountId(astPlayer.getAccount().getUuid()));
    }

    /**
     * 指定プレイヤーのクラフト欄ショートカット再描画を一時停止します。
     *
     * @param player 対象プレイヤー
     */
    private void suppressCraftRendering(@NotNull Player player) {
        craftRenderSuppressed.add(player.getUniqueId());
    }

    /**
     * 指定プレイヤーのクラフト欄ショートカット再描画を再開します。
     *
     * @param player 対象プレイヤー
     */
    private void resumeCraftRendering(@NotNull Player player) {
        craftRenderSuppressed.remove(player.getUniqueId());
    }

    /**
     * 指定プレイヤーのショートカット設定を取得します。
     *
     * @param player 対象プレイヤー
     * @return ショートカット設定
     */
    private @NotNull MenuShortcutSettings settings(@NotNull Player player) {
        AstPlayer astPlayer = AstPlayerCache.get(player);
        if (astPlayer == null) {
            return MenuShortcutSettings.defaults();
        }
        return shortcutRepository.findByAccountId(astPlayer.getAccount().getUuid());
    }

    /**
     * プレイヤーインベントリのクラフト欄クリックか判定します。
     *
     * @param event インベントリクリックイベント
     * @return メニュー起動またはショートカット実行対象なら true
     */
    private boolean isCraftMenuClick(@NotNull InventoryClickEvent event) {
        return event.getView().getType() == org.bukkit.event.inventory.InventoryType.CRAFTING
            && event.getRawSlot() >= MenuView.CRAFT_RESULT_RAW_SLOT
            && event.getRawSlot() < MenuView.CRAFT_RESULT_RAW_SLOT + craftMenuSlotCount();
    }

    /**
     * 成果物スロット1つと 2x2 クラフトスロット4つを合わせた数を返します。
     *
     * @return raw slot 判定対象数
     */
    private int craftMenuSlotCount() {
        return MenuShortcutSettings.SLOT_COUNT + 1;
    }

    /**
     * プレイヤー自身のクラフトインベントリか判定します。
     *
     * @param inventory 判定対象インベントリ
     * @return プレイヤークラフトインベントリなら true
     */
    private boolean isPlayerCraftingInventory(@NotNull CraftingInventory inventory) {
        return inventory.getType() == org.bukkit.event.inventory.InventoryType.CRAFTING;
    }

    /**
     * 通常プレイヤーモードか判定します。
     *
     * @param player 判定対象プレイヤー
     * @return 通常プレイヤーモードなら true
     */
    private boolean isPlayerMode(@Nullable Player player) {
        if (player == null) {
            return false;
        }
        var astPlayer = AstPlayerCache.get(player);
        return astPlayer != null && astPlayer.getAccount().getMode() == AccountMode.PLAYER;
    }

}
