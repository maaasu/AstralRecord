package io.github.maaasu.astralRecord.feature.player.command;

import io.github.maaasu.astralRecord.feature.item.model.ItemModel;
import io.github.maaasu.astralRecord.feature.item.service.ItemService;
import io.github.maaasu.astralRecord.feature.player.model.AstPlayer;
import io.github.maaasu.astralRecord.infrastructure.command.AstCommand;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

/**
 * /temp コマンドの実装クラス。
 * <p>
 * permission 99 以上のプレイヤーのみ実行可能な管理者向けコマンドです。
 * <p>
 * 使用方法:
 * <pre>
 * /temp &lt;equipmentId&gt; - 指定 equipment の情報を表示
 * </pre>
 */
public class TempCommand extends AstCommand {

    /** このコマンドの実行に必要な最低権限レベル */
    private static final int REQUIRED_PERMISSION = 99;

    private static final DecimalFormat NUMBER_FORMAT =
        new DecimalFormat("#,##0.####", DecimalFormatSymbols.getInstance(Locale.US));

    private final ItemService itemService;

    /**
     * TempCommand を初期化します。
     */
    public TempCommand(@NotNull ItemService itemService) {
        super("temp", "管理者向けテンポラリコマンド", "/temp <equipmentId>", true, REQUIRED_PERMISSION);
        this.itemService = itemService;
    }

    @Override
    protected void executePlayerCommand(@NotNull AstPlayer player, @NotNull String[] args) {
        if (args.length < 1) {
            sendUsage(player.getBukkit());
            return;
        }

        String equipmentId = args[0];
        ItemModel item = itemService.findLoadedById(equipmentId);
        if (item == null) {
            item = itemService.loadItem(equipmentId, "equipment");
        }

        if (item == null) {
            sendError(player.getBukkit(), "equipment が見つかりません: " + equipmentId);
            return;
        }

        if (!"equipment".equalsIgnoreCase(item.getCategory()) || item.getEquipment() == null) {
            sendError(
                player.getBukkit(),
                "指定された ID は equipment ではありません: " + item.getId() + " (category=" + item.getCategory() + ")"
            );
            return;
        }

        showEquipmentDebug(player, item);
    }

    private void showEquipmentDebug(@NotNull AstPlayer player, @NotNull ItemModel item) {
        var sender = player.getBukkit();
        var equipment = item.getEquipment();
        if (equipment == null) {
            sendError(sender, "equipment 情報が存在しません: " + item.getId());
            return;
        }

        sendSuccess(sender, "=== equipment debug: " + item.getId() + " ===");
        sendInfo(sender, "item.schemaVersion=" + item.getSchemaVersion());
        sendInfo(sender, "item.id=" + item.getId());
        sendInfo(sender, "item.category=" + item.getCategory());
        sendInfo(sender, "item.name=" + item.getName());
        sendInfo(sender, "item.icon=" + item.getIcon());
        sendInfo(sender, "item.rarity=" + item.getRarity());
        sendInfo(sender, "item.maxStack=" + item.getMaxStack());
        sendInfo(sender, "item.saleValue=" + item.getSaleValue());
        sendInfo(sender, "item.customModelData=" + valueOrDash(item.getCustomModelData()));
        sendInfo(sender, "item.unTradeable=" + item.getUnTradeable());
        sendInfo(sender, "item.unSellable=" + item.getUnSellable());
        sendInfo(sender, "item.lore.count=" + item.getLore().size());
        showIndexedLines(sender, "item.lore", item.getLore());

        sendInfo(sender, "equipment.slot=" + valueOrDash(equipment.getSlot()));
        sendInfo(sender, "equipment.handType=" + valueOrDash(equipment.getHandType()));
        sendInfo(sender, "equipment.requiredLevel=" + equipment.getRequiredLevel());
        sendInfo(sender, "equipment.requiredClasses=" + formatList(equipment.getRequiredClasses()));
        sendInfo(sender, "equipment.setId=" + valueOrDash(equipment.getSetId()));
        sendInfo(sender, "equipment.skills=" + formatList(equipment.getSkills()));
        sendInfo(sender, "equipment.stats.count=" + equipment.getStats().size());

        for (int i = 0; i < equipment.getStats().size(); i++) {
            var stat = equipment.getStats().get(i);
            sendInfo(
                sender,
                "equipment.stats[" + i + "]="
                    + "status=" + stat.getStatus()
                    + ", type=" + stat.getType()
                    + ", min=" + formatNumber(stat.getMin())
                    + ", max=" + formatNumber(stat.getMax())
                    + ", display=" + stat.displayValue()
            );
        }

        if (equipment.getDurability() == null) {
            sendInfo(sender, "equipment.durability=null");
        } else {
            sendInfo(sender, "equipment.durability.max=" + equipment.getDurability().getMax());
            sendInfo(sender, "equipment.durability.consume=" + equipment.getDurability().getConsume());
        }

        if (equipment.getOnUse() == null) {
            sendInfo(sender, "equipment.onUse=null");
        } else {
            sendInfo(
                sender,
                "equipment.onUse.leftClickCooldownTicks="
                    + valueOrDash(equipment.getOnUse().getLeftClickCooldownTicks())
            );
            sendInfo(
                sender,
                "equipment.onUse.leftClickSkillId=" + valueOrDash(equipment.getOnUse().getLeftClickSkillId())
            );
            sendInfo(
                sender,
                "equipment.onUse.rightClickCooldownTicks="
                    + valueOrDash(equipment.getOnUse().getRightClickCooldownTicks())
            );
            sendInfo(
                sender,
                "equipment.onUse.rightClickSkillId=" + valueOrDash(equipment.getOnUse().getRightClickSkillId())
            );
        }
    }

    private void showIndexedLines(@NotNull CommandSender sender, @NotNull String prefix, @NotNull List<String> lines) {
        if (lines.isEmpty()) {
            sendInfo(sender, prefix + "=[]");
            return;
        }

        for (int i = 0; i < lines.size(); i++) {
            sendInfo(sender, prefix + "[" + i + "]=" + lines.get(i));
        }
    }

    private @NotNull String formatList(@NotNull List<String> values) {
        if (values.isEmpty()) {
            return "[]";
        }
        return "[" + String.join(", ", values) + "]";
    }

    private @NotNull String valueOrDash(Object value) {
        if (value == null) {
            return "-";
        }

        String text = value.toString();
        return text.isBlank() ? "-" : text;
    }

    private @NotNull String formatNumber(double value) {
        return NUMBER_FORMAT.format(value);
    }
}
