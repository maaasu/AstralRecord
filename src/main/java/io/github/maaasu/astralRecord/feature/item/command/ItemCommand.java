package io.github.maaasu.astralRecord.feature.item.command;

import io.github.maaasu.astralRecord.feature.item.model.ItemModel;
import io.github.maaasu.astralRecord.feature.item.service.ItemService;
import io.github.maaasu.astralRecord.feature.item.service.ItemStackFactory;
import io.github.maaasu.astralRecord.feature.player.PlayerMsgId;
import io.github.maaasu.astralRecord.feature.player.model.AstPlayer;
import io.github.maaasu.astralRecord.infrastructure.command.AstCommand;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

/**
 * /item コマンドの実装クラス。
 *
 * アイテム定義のロード・一覧参照・詳細参照を提供します。
 */
public class ItemCommand extends AstCommand {

    private static final DecimalFormat NUMBER_FORMAT =
        new DecimalFormat("#,##0.####", DecimalFormatSymbols.getInstance(Locale.US));

    private final ItemService itemService;
    private final ItemStackFactory itemStackFactory;

    /**
     * ItemCommand を初期化します。
     */
    public ItemCommand(@NotNull ItemService itemService, @NotNull ItemStackFactory itemStackFactory) {
        super("item", "アイテム定義をロード・参照・取得します", "/item <load|list|info|get> ...", true);
        this.itemService = itemService;
        this.itemStackFactory = itemStackFactory;
    }

    @Override
    protected void executePlayerCommand(@NotNull AstPlayer player, @NotNull String[] args) {
        if (args.length == 0) {
            sendUsage(player.getBukkit());
            return;
        }

        String action = args[0].toLowerCase();
        if (action.equals("load")) {
            handleLoad(player, args);
            return;
        }

        if (action.equals("list")) {
            handleList(player, args);
            return;
        }

        if (action.equals("info")) {
            handleInfo(player, args);
            return;
        }

        if (action.equals("get")) {
            handleGet(player, args);
            return;
        }

        sendUsage(player.getBukkit());
    }

    private void handleLoad(@NotNull AstPlayer player, @NotNull String[] args) {
        if (args.length < 2) {
            sendUsage(player.getBukkit());
            return;
        }

        String itemId = args[1];
        ItemModel loaded = itemService.loadItem(itemId);
        if (loaded == null) {
            player.sendMessage(PlayerMsgId.P_5201, itemId);
            return;
        }

        player.sendMessage(PlayerMsgId.P_5209, loaded.getCategory(), loaded.getId(), loaded.getName());
    }

    private void handleList(@NotNull AstPlayer player, @NotNull String[] args) {
        if (args.length >= 2 && !itemService.isSupportedCategory(args[1])) {
            player.sendMessage(PlayerMsgId.P_5200, args[1]);
            return;
        }

        List<ItemModel> items = args.length >= 2
            ? itemService.getLoadedItemsByCategory(args[1])
            : itemService.getLoadedItems();

        if (items.isEmpty()) {
            player.sendMessage(PlayerMsgId.P_5210);
            return;
        }

        player.sendMessage(PlayerMsgId.P_5211, items.size());
        for (ItemModel item : items) {
            player.sendMessage(PlayerMsgId.P_5212, item.getCategory(), item.getId(), item.getName());
        }
    }

    private void handleInfo(@NotNull AstPlayer player, @NotNull String[] args) {
        if (args.length < 2) {
            sendUsage(player.getBukkit());
            return;
        }

        String itemId = args[1];
        ItemModel item = itemService.findLoadedById(itemId);

        if (item == null) {
            player.sendMessage(PlayerMsgId.P_5213, itemId);
            return;
        }

        showItemDetail(player, item);
    }

    /**
     * /item get &lt;itemId&gt; [amount] — ロード済みアイテムを ItemStack としてインベントリに付与します。
     */
    private void handleGet(@NotNull AstPlayer player, @NotNull String[] args) {
        if (args.length < 2) {
            sendUsage(player.getBukkit());
            return;
        }

        String itemId = args[1];
        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException ignored) {
                // デフォルト 1
            }
        }

        ItemModel model = itemService.findLoadedById(itemId);
        if (model == null) {
            player.sendMessage(PlayerMsgId.P_5213, itemId);
            return;
        }

        ItemStack itemStack = itemStackFactory.create(model, amount);

        var result = player.getBukkit().getInventory().addItem(itemStack);
        if (!result.isEmpty()) {
            player.sendMessage(PlayerMsgId.P_5241);
            return;
        }

        player.sendMessage(PlayerMsgId.P_5240, model.getName(), amount);
    }

    private void showItemDetail(@NotNull AstPlayer player, @NotNull ItemModel item) {
        // 共通情報
        player.sendMessage(PlayerMsgId.P_5202, item.getName(), item.getId());
        player.sendMessage(PlayerMsgId.P_5203, item.getCategory(), item.getRarity(), item.getSaleValue());
        player.sendMessage(PlayerMsgId.P_5216, item.getIcon(), item.getSchemaVersion(), item.getCustomModelData());
        player.sendMessage(PlayerMsgId.P_5218, item.getMaxStack());
        player.sendMessage(PlayerMsgId.P_5217, item.getUnTradeable(), item.getUnSellable());

        // カテゴリ別情報（表示順固定）
        showCategoryDetail(player, item);

        // lore（常に最後）
        showLore(player, item);
    }

    private void showCategoryDetail(@NotNull AstPlayer player, @NotNull ItemModel item) {
        String category = item.getCategory().toLowerCase(Locale.ROOT);
        switch (category) {
            case "bundle" -> showBundleDetail(player, item);
            case "currency" -> showCurrencyDetail(player, item);
            case "equipment" -> showEquipmentDetail(player, item);
            case "consumable" -> showConsumableDetail(player, item);
            case "material" -> player.sendMessage(PlayerMsgId.P_5231, item.getCategory());
            default -> player.sendMessage(PlayerMsgId.P_5232, item.getCategory());
        }
    }

    private void showBundleDetail(@NotNull AstPlayer player, @NotNull ItemModel item) {
        if (item.getBundle() == null) {
            player.sendMessage(PlayerMsgId.P_5233, item.getCategory());
            return;
        }

        player.sendMessage(PlayerMsgId.P_5219);
        player.sendMessage(PlayerMsgId.P_5220, valueOrDash(item.getBundle().getLootTableId()));
        if (item.getBundle().getOnUse() != null) {
            player.sendMessage(
                PlayerMsgId.P_5221,
                valueOrDash(item.getBundle().getOnUse().getSound()),
                valueOrDash(item.getBundle().getOnUse().getEffect()),
                valueOrDash(item.getBundle().getOnUse().getParticle())
            );
        }
    }

    private void showCurrencyDetail(@NotNull AstPlayer player, @NotNull ItemModel item) {
        if (item.getCurrency() == null) {
            player.sendMessage(PlayerMsgId.P_5233, item.getCategory());
            return;
        }

        player.sendMessage(PlayerMsgId.P_5222);
        player.sendMessage(
            PlayerMsgId.P_5223,
            valueOrDash(item.getCurrency().getType()),
            valueOrDash(item.getCurrency().getGroup()),
            valueOrDash(item.getCurrency().getExpiresAt())
        );
    }

    private void showEquipmentDetail(@NotNull AstPlayer player, @NotNull ItemModel item) {
        if (item.getEquipment() == null) {
            player.sendMessage(PlayerMsgId.P_5233, item.getCategory());
            return;
        }

        player.sendMessage(PlayerMsgId.P_5224);
        player.sendMessage(
            PlayerMsgId.P_5225,
            valueOrDash(item.getEquipment().getSlot()),
            valueOrDash(item.getEquipment().getHandType()),
            item.getEquipment().getRequiredLevel()
        );

        if (!item.getEquipment().getRequiredClasses().isEmpty()) {
            player.sendMessage(PlayerMsgId.P_5226, String.join(", ", item.getEquipment().getRequiredClasses()));
        }

        for (var stat : item.getEquipment().getStats()) {
            player.sendMessage(PlayerMsgId.P_5227, stat.getStatus(), stat.getType(), stat.getValue());
        }

        if (item.getEquipment().getDurability() != null) {
            player.sendMessage(
                PlayerMsgId.P_5228,
                item.getEquipment().getDurability().getMax(),
                item.getEquipment().getDurability().getConsume()
            );
        }

        if (item.getEquipment().getOnUse() != null) {
            player.sendMessage(
                PlayerMsgId.P_5229,
                valueOrDash(item.getEquipment().getOnUse().getLeftClickCooldownTicks()),
                valueOrDash(item.getEquipment().getOnUse().getLeftClickSkillId()),
                valueOrDash(item.getEquipment().getOnUse().getRightClickCooldownTicks()),
                valueOrDash(item.getEquipment().getOnUse().getRightClickSkillId())
            );
        }

        if (!item.getEquipment().getSkills().isEmpty()) {
            player.sendMessage(PlayerMsgId.P_5230, String.join(", ", item.getEquipment().getSkills()));
        }
    }

    private void showConsumableDetail(@NotNull AstPlayer player, @NotNull ItemModel item) {
        if (item.getConsumable() == null) {
            player.sendMessage(PlayerMsgId.P_5233, item.getCategory());
            return;
        }

        player.sendMessage(PlayerMsgId.P_5234);
        if (item.getConsumable().getOnUse() != null) {
            player.sendMessage(
                PlayerMsgId.P_5235,
                valueOrDash(item.getConsumable().getOnUse().getSound()),
                valueOrDash(item.getConsumable().getOnUse().getEffect()),
                item.getConsumable().getOnUse().getAmount()
            );
        }

        player.sendMessage(PlayerMsgId.P_5236, item.getConsumable().getEffects().size());
        for (var effect : item.getConsumable().getEffects()) {
            player.sendMessage(
                PlayerMsgId.P_5237,
                effect.getType(),
                formatNumber(effect.getRate()),
                valueOrDash(effect.getStatus()),
                valueOrDash(formatNullableNumber(effect.getValue())),
                effect.isPercent(),
                valueOrDash(effect.getBuffId())
            );
        }
    }

    private void showLore(@NotNull AstPlayer player, @NotNull ItemModel item) {
        player.sendMessage(PlayerMsgId.P_5238);

        if (item.getLore().isEmpty()) {
            player.sendMessage(PlayerMsgId.P_5204);
            return;
        }

        for (String line : item.getLore()) {
            player.sendMessage(PlayerMsgId.P_5206, line);
        }
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

    private String formatNullableNumber(Double value) {
        if (value == null) {
            return null;
        }
        return formatNumber(value);
    }
}

