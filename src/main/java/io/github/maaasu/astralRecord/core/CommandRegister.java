package io.github.maaasu.astralRecord.core;

import io.github.maaasu.astralRecord.AstralRecord;
import io.github.maaasu.astralRecord.feature.player.command.TempCommand;
import io.github.maaasu.astralRecord.feature.player.command.TempTabCompleter;
import io.github.maaasu.astralRecord.feature.player.command.TestCommand;
import io.github.maaasu.astralRecord.feature.player.command.TestTabCompleter;
import io.github.maaasu.astralRecord.core.command.ReloadCommand;
import io.github.maaasu.astralRecord.infrastructure.command.CommandManager;

/**
 * プラグインのコマンドを一括登録するクラス。
 * {@link AstralRecord#onLoad()} 内で {@link CommandManager#initialize(AstralRecord)} より前に呼び出してください。
 */
public class CommandRegister {
    private final AstralRecord instance;

    public CommandRegister(AstralRecord plugin) {
        this.instance = plugin;
        registerCommand();
    }

    /**
     * すべてのコマンドを CommandManager に登録します。
     */
    public final void registerCommand() {
        CommandManager cm = CommandManager.getInstance();

        // /test — プレイヤー情報確認（テスト用）
        cm.registerCommand("test", new TestCommand(), new TestTabCompleter());

        // /temp — 管理者向けテンポラリコマンド（permission 99 以上必要）
        cm.registerCommand("temp", new TempCommand(), new TempTabCompleter());

        // /astreload — プラグインホットリロード（permission 99 以上必要）
        cm.registerCommand("astreload", new ReloadCommand());
    }

    private AstralRecord getInstance() {
        return instance;
    }
}
