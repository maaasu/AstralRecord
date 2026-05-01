package io.github.maaasu.astralRecord.core;

import io.github.maaasu.astralRecord.AstralRecord;
import io.github.maaasu.astralRecord.feature.player.command.TempCommand;
import io.github.maaasu.astralRecord.feature.player.command.TempTabCompleter;
import io.github.maaasu.astralRecord.feature.player.command.TestCommand;
import io.github.maaasu.astralRecord.feature.player.command.TestTabCompleter;
import io.github.maaasu.astralRecord.feature.item.command.ItemCommand;
import io.github.maaasu.astralRecord.feature.item.command.ItemTabCompleter;
import io.github.maaasu.astralRecord.feature.item.service.ItemService;
import io.github.maaasu.astralRecord.feature.item.service.ItemStackFactory;
import io.github.maaasu.astralRecord.feature.status.command.StatusCommand;
import io.github.maaasu.astralRecord.feature.status.command.StatusTabCompleter;
import io.github.maaasu.astralRecord.core.command.ReloadCommand;
import io.github.maaasu.astralRecord.infrastructure.command.CommandManager;

/**
 * プラグインのコマンドを一括登録するクラス。
 * {@link AstralRecord#onLoad()} 内で {@link CommandManager#initialize(AstralRecord)} より前に呼び出してください。
 */
public class CommandRegister {
    private final ItemService itemService;
    private final ItemStackFactory itemStackFactory;

    public CommandRegister(ItemService itemService, ItemStackFactory itemStackFactory) {
        this.itemService = itemService;
        this.itemStackFactory = itemStackFactory;
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

        // /status — 基本ステータス表示
        cm.registerCommand("status", new StatusCommand(), new StatusTabCompleter());

        // /item — アイテム定義の参照・取得
        cm.registerCommand("item", new ItemCommand(itemService, itemStackFactory), new ItemTabCompleter(itemService));

        // /astreload — プラグインホットリロード（permission 99 以上必要）
        cm.registerCommand("astreload", new ReloadCommand());
    }
}
