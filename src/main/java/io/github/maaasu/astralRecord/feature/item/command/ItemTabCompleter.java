package io.github.maaasu.astralRecord.feature.item.command;

import io.github.maaasu.astralRecord.feature.item.service.ItemService;
import io.github.maaasu.astralRecord.feature.player.model.AstPlayer;
import io.github.maaasu.astralRecord.infrastructure.command.AstTabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * /item コマンドのタブ補完実装クラス。
 */
public class ItemTabCompleter extends AstTabCompleter {

    private final ItemService itemService;

    /**
     * ItemTabCompleter を初期化します。
     */
    public ItemTabCompleter(@NotNull ItemService itemService) {
        super(true);
        this.itemService = itemService;
    }

    @Override
    protected List<String> getPlayerCompletions(@NotNull AstPlayer player, @NotNull String[] args) {
        List<String> subCommands = completeAtPosition(args, 0, "load", "list", "info", "get");
        if (!subCommands.isEmpty()) {
            return subCommands;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("load")) {
            return itemService.getSupportedCategories();
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("load")) {
            return itemService.getLoadedItemIds();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("list")) {
            return itemService.getLoadedCategories();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            return itemService.getLoadedItemIds();
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("info")) {
            return itemService.getLoadedCategories();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("get")) {
            return itemService.getLoadedItemIds();
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("get")) {
            return itemService.getLoadedCategories();
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("get")) {
            return List.of("1", "8", "16", "32", "64");
        }

        return List.of();
    }
}

