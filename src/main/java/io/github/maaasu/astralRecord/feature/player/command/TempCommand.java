package io.github.maaasu.astralRecord.feature.player.command;

import io.github.maaasu.astralRecord.feature.player.PlayerMsgId;
import io.github.maaasu.astralRecord.feature.player.model.AstPlayer;
import io.github.maaasu.astralRecord.infrastructure.command.AstCommand;
import org.jetbrains.annotations.NotNull;

/**
 * /temp コマンドの実装クラス。
 * <p>
 * permission 99 以上のプレイヤーのみ実行可能な管理者向けコマンドです。
 * <p>
 * 使用方法:
 * <pre>
 * /temp - テンポラリコマンドを実行
 * </pre>
 */
public class TempCommand extends AstCommand {

    /** このコマンドの実行に必要な最低権限レベル */
    private static final int REQUIRED_PERMISSION = 99;

    /**
     * TempCommand を初期化します。
     */
    public TempCommand() {
        super("temp", "管理者向けテンポラリコマンド", "/temp", true, REQUIRED_PERMISSION);
    }

    @Override
    protected void executePlayerCommand(@NotNull AstPlayer player, @NotNull String[] args) {
        player.sendMessage(PlayerMsgId.P_5080, player.getUser().getMcid());
    }
}
