package io.github.maaasu.astralRecord.feature.inventory.command;

import io.github.maaasu.astralRecord.feature.inventory.model.InventoryType;
import io.github.maaasu.astralRecord.feature.player.model.AstPlayer;
import io.github.maaasu.astralRecord.infrastructure.command.AstTabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class InventoryTabCompleter extends AstTabCompleter {
    /**
     * インベントリコマンド用のタブ補完を初期化します。
     */
    public InventoryTabCompleter() {
        super(true);
    }

    /**
     * プレイヤー実行時の補完候補を返します。
     *
     * @param player 実行プレイヤー
     * @param args 現在の引数
     * @return 補完候補一覧
     */
    @Override
    protected List<String> getPlayerCompletions(@NotNull AstPlayer player, @NotNull String[] args) {
        if (args.length == 1) {
            return InventoryType.getEntries().stream()
                .map(type -> type.toString().toLowerCase())
                .collect(Collectors.toList());
        }
        return List.of();
    }
}
