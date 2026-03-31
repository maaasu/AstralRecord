package io.github.maaasu.astralRecord.feature.player.service;

import io.github.maaasu.astralRecord.feature.account.model.AccountModel;
import io.github.maaasu.astralRecord.feature.account.service.AccountService;
import io.github.maaasu.astralRecord.feature.player.AstPlayerCache;
import io.github.maaasu.astralRecord.feature.player.model.AstPlayer;
import io.github.maaasu.astralRecord.feature.status.service.StatusService;
import io.github.maaasu.astralRecord.feature.user.model.UserModel;
import io.github.maaasu.astralRecord.feature.user.service.UserService;
import io.github.maaasu.astralRecord.infrastructure.logging.LogId;
import io.github.maaasu.astralRecord.infrastructure.logging.Logger;
import org.bukkit.entity.Player;

/**
 * プレイヤー機能のビジネスロジックを担うサービスクラス。
 * ログイン時の AstPlayer 構築・キャッシュ登録・OP権限付与を管理します。
 */
public class PlayerService {

    /** permission がこの値以上の場合に Minecraft OP 権限を付与する */
    private static final int OP_PERMISSION_THRESHOLD = 99;

    private final UserService userService;
    private final AccountService accountService;
    private final StatusService statusService;

    public PlayerService(UserService userService, AccountService accountService, StatusService statusService) {
        this.userService = userService;
        this.accountService = accountService;
        this.statusService = statusService;
    }

    /**
     * プレイヤーのログイン処理を行います。
     * <p>
     * DB から {@link UserModel} と {@link AccountModel} を取得して {@link AstPlayer} を構築し、
     * {@link AstPlayerCache} に登録します。
     * permission が {@value #OP_PERMISSION_THRESHOLD} 以上の場合は Minecraft OP 権限を付与します。
     *
     * @param player ログインした Bukkit プレイヤー
     */
    public void onPlayerJoin(Player player) {
        UserModel user = userService.getUser(player.getUniqueId());
        if (user == null) {
            Logger.log(LogId.W_5070, player.getName());
            return;
        }

        AccountModel account = accountService.getSelectedAccount(user.getUuid(), user.getAccountId());
        if (account == null) {
            Logger.log(LogId.W_5070, player.getName());
            return;
        }

        AstPlayer astPlayer = new AstPlayer(player, user, account);
        statusService.refreshStatus(astPlayer);
        if (AstPlayerCache.getAll().contains(astPlayer)) {

            return;
        }
        AstPlayerCache.put(astPlayer);
    }

    /**
     * プレイヤーのログアウト処理を行います。
     * {@link AstPlayerCache} からプレイヤーを削除します。
     *
     * @param player ログアウトした Bukkit プレイヤー
     */
    public void onPlayerQuit(Player player) {
        AstPlayerCache.remove(player.getUniqueId());
    }
}

