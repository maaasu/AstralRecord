package io.github.maaasu.astralRecord.feature.mob.command;

import io.github.maaasu.astralRecord.feature.mob.service.MobService;
import io.github.maaasu.astralRecord.feature.player.model.AstPlayer;
import io.github.maaasu.astralRecord.infrastructure.command.AstTabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * /mob コマンドのタブ補完実装。
 */
public class MobTabCompleter extends AstTabCompleter {

    private final MobService mobService;

    /**
     * コンストラクタ。
     *
     * @param mobService Mobサービス
     */
    public MobTabCompleter(@NotNull MobService mobService) {
        super(true);
        this.mobService = mobService;
    }

    @Override
    protected List<String> getPlayerCompletions(@NotNull AstPlayer player, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("load", "list", "spawn");
        }
        if (args.length == 2 && "spawn".equalsIgnoreCase(args[0])) {
            return List.copyOf(mobService.getLoadedMobIds());
        }
        return List.of();
    }
}
