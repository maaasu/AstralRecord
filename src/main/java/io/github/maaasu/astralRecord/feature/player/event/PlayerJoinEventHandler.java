package io.github.maaasu.astralRecord.feature.player.event;

import io.github.maaasu.astralRecord.core.event.AbstractEventHandler;
import io.github.maaasu.astralRecord.feature.player.model.AstPlayer;
import io.github.maaasu.astralRecord.feature.player.service.PlayerService;
import io.github.maaasu.astralRecord.infrastructure.logging.LogId;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * プレイヤーのログイン・ログアウト処理を行うイベントハンドラー。
 * <p>
 * ログイン時に {@link PlayerService#onPlayerJoin(org.bukkit.entity.Player)} を呼び出して
 * {@link AstPlayer} をキャッシュに登録します。OP権限の付与・剥奪は
 * {@link AstPlayer#applyPermission(io.github.maaasu.astralRecord.feature.user.model.UserModel)} が行います。
 * ログアウト時は {@link PlayerService#onPlayerQuit(org.bukkit.entity.Player)} でキャッシュを削除します。
 */
public class PlayerJoinEventHandler extends AbstractEventHandler {

    private final PlayerService playerService;

    public PlayerJoinEventHandler(PlayerService playerService) {
        this.playerService = playerService;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        runSafely(() -> playerService.onPlayerJoin(event.getPlayer()), LogId.E_5070, event.getPlayer().getName());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        runSafely(() -> playerService.onPlayerQuit(event.getPlayer()), LogId.E_5070, event.getPlayer().getName());
    }
}


