package io.github.maaasu.astralRecord.feature.player.command;

import io.github.maaasu.astralRecord.infrastructure.command.AstTabCompleter;

/**
 * /temp コマンドのタブ補完実装クラス。
 */
public class TempTabCompleter extends AstTabCompleter {

    /**
     * TempTabCompleter を初期化します。
     */
    public TempTabCompleter() {
        super(true);
    }
}
