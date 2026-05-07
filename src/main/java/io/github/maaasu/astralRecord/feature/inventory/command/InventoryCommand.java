package io.github.maaasu.astralRecord.feature.inventory.command;

import io.github.maaasu.astralRecord.AstralRecord;
import io.github.maaasu.astralRecord.feature.inventory.model.InventoryType;
import io.github.maaasu.astralRecord.feature.player.PlayerMsgId;
import io.github.maaasu.astralRecord.feature.player.PlayerMsgResource;
import io.github.maaasu.astralRecord.feature.player.model.AstPlayer;
import io.github.maaasu.astralRecord.infrastructure.command.AstCommand;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;

public class InventoryCommand extends AstCommand {

    /**
     * インベントリ表示切替コマンドを初期化します。
     */
    public InventoryCommand() {
        super("inventory", "Switch inventory view.", "/inventory <type>", true);
    }

    /**
     * プレイヤー向けの `/inventory` コマンドを実行します。
     *
     * @param player 実行プレイヤー
     * @param args コマンド引数
     */
    @Override
    protected void executePlayerCommand(@NotNull AstPlayer player, @NotNull String[] args) {
        if (args.length < 1) {
            sendUsage(player.getBukkit());
            return;
        }

        if (!player.getAccount().getMode().shouldReflectInventoryToGui()) {
            sendInfo(player.getBukkit(), PlayerMsgResource.getMessage(PlayerMsgId.P_5250.getId()));
            return;
        }

        InventoryType inventoryType = parseInventoryType(args[0]);
        if (inventoryType == null) {
            sendError(player.getBukkit(), PlayerMsgResource.format(PlayerMsgId.P_5251.getId(), supportedInventoryTypes()));
            return;
        }

        var inventoryService = AstralRecord.getInstance().getInventoryService();
        if (inventoryService == null) {
            sendError(player.getBukkit(), PlayerMsgResource.getMessage(PlayerMsgId.P_5252.getId()));
            return;
        }

        inventoryService.applyInventoryToGui(player, inventoryType);
        sendSuccess(player.getBukkit(), PlayerMsgResource.format(PlayerMsgId.P_5253.getId(), inventoryType.getDisplayNameJa()));
    }

    /**
     * 入力文字列からインベントリ種別を解決します。
     *
     * @param value 入力された種別文字列
     * @return 対応するインベントリ種別。解決できない場合は null
     */
    private InventoryType parseInventoryType(@NotNull String value) {
        return InventoryType.fromInput(value);
    }

    /**
     * 入力可能なインベントリ種別一覧を返します。
     *
     * @return カンマ区切りの種別一覧
     */
    private String supportedInventoryTypes() {
        return InventoryType.getEntries().stream()
            .map(type -> type.toString().toLowerCase())
            .collect(Collectors.joining(", "));
    }
}
