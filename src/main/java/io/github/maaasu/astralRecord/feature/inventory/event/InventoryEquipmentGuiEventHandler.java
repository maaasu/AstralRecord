package io.github.maaasu.astralRecord.feature.inventory.event;

import io.github.maaasu.astralRecord.core.event.AbstractEventHandler;
import io.github.maaasu.astralRecord.feature.inventory.service.InventoryService;
import io.github.maaasu.astralRecord.feature.menu.view.MenuView;
import io.github.maaasu.astralRecord.infrastructure.logging.LogId;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.jetbrains.annotations.NotNull;

/**
 * インベントリメニュー移行期間中の互換イベントハンドラです。
 * <p>
 * 既存のイベント登録経路を維持するために残しており、現在は処理本体を持ちません。
 */
public class InventoryEquipmentGuiEventHandler extends AbstractEventHandler {

    /**
     * 既存ブートストラップ互換のためのコンストラクタです。
     *
     * @param menuView メニュービュー（将来拡張用）
     * @param inventoryService インベントリサービス（将来拡張用）
     */
    public InventoryEquipmentGuiEventHandler(
        @NotNull MenuView menuView,
        @NotNull InventoryService inventoryService
    ) {
        // Keep constructor signature for current bootstrap wiring.
    }

    /**
     * クリックイベントの互換ハンドラです。
     *
     * @param event インベントリクリックイベント
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        runSafely(() -> {
            // Intentionally no-op during migration.
        }, LogId.E_5600, event.getWhoClicked().getName());
    }

    /**
     * クローズイベントの互換ハンドラです。
     *
     * @param event インベントリクローズイベント
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        runSafely(() -> {
            // Intentionally no-op during migration.
        }, LogId.E_5600, event.getPlayer().getName());
    }
}
