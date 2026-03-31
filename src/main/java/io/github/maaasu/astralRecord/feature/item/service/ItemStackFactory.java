package io.github.maaasu.astralRecord.feature.item.service;

import io.github.maaasu.astralRecord.feature.item.model.ItemEquipment;
import io.github.maaasu.astralRecord.feature.item.model.ItemEquipmentStat;
import io.github.maaasu.astralRecord.feature.item.model.ItemModel;
import io.github.maaasu.astralRecord.feature.loot.model.LootEntry;
import io.github.maaasu.astralRecord.feature.loot.model.LootModel;
import io.github.maaasu.astralRecord.feature.loot.service.LootService;
import io.github.maaasu.astralRecord.infrastructure.logging.LogId;
import io.github.maaasu.astralRecord.infrastructure.logging.Logger;
import io.github.maaasu.astralRecord.infrastructure.util.ColorCodeUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link ItemModel} からサーバ側 {@link ItemStack} を生成するファクトリ。
 * <p>
 * サーバ側の base Material は常に {@link Material#PAPER} とし、
 * プレイヤーへの見た目（icon）は {@link PersistentDataContainer} に埋め込んで
 * パケットアダプタ（ItemStackPacketAdapter）がパケット書き換えで適用します。
 * <p>
 * 同一 {@link ItemModel} から何度も ItemStack を生成するケースを想定し、
 * テンプレート（プロトタイプ）を {@link ConcurrentHashMap} でキャッシュします。
 * {@link #create} は clone + 個数セットのみで済むため軽量です。
 */
public class ItemStackFactory {

    /** サーバ側ベースマテリアル */
    private static final Material BASE_MATERIAL = Material.PAPER;

    /** レガシーカラーコード（§）→ Adventure Component 変換用シリアライザ */
    private static final LegacyComponentSerializer LEGACY_SERIALIZER =
            LegacyComponentSerializer.legacySection();

    /** PDC キー: AstralRecord アイテムID */
    private static final NamespacedKey KEY_ITEM_ID =
            new NamespacedKey("astralrecord", "item_id");

    /** PDC キー: プレイヤーへ表示する icon Material 名 */
    private static final NamespacedKey KEY_ICON =
            new NamespacedKey("astralrecord", "icon");

    /** PDC キー: カテゴリ */
    private static final NamespacedKey KEY_CATEGORY =
            new NamespacedKey("astralrecord", "category");

    /** PDC キー: レアリティ */
    private static final NamespacedKey KEY_RARITY =
            new NamespacedKey("astralrecord", "rarity");

    /** テンプレートキャッシュ (category:id → プロトタイプ ItemStack) */
    private final Map<String, ItemStack> templateCache = new ConcurrentHashMap<>();

    /** ルートテーブル参照用（nullable: 未初期化時は Lore に含めない） */
    private final LootService lootService;

    /**
     * ItemStackFactory を初期化します。
     *
     * @param lootService ルートテーブルサービス（bundle の lootTableId 解決に使用）
     */
    public ItemStackFactory(@NotNull LootService lootService) {
        this.lootService = lootService;
    }

    // region --- public API ---

    /**
     * {@link ItemModel} から ItemStack を 1 個生成します。
     *
     * @param model アイテム定義
     * @return 生成された ItemStack（サーバ側は PAPER）
     */
    public @NotNull ItemStack create(@NotNull ItemModel model) {
        return create(model, 1);
    }

    /**
     * {@link ItemModel} から指定個数の ItemStack を生成します。
     *
     * @param model  アイテム定義
     * @param amount 個数（1 ～ maxStack）
     * @return 生成された ItemStack（サーバ側は PAPER）
     */
    public @NotNull ItemStack create(@NotNull ItemModel model, int amount) {
        String key = cacheKey(model);
        ItemStack template = templateCache.computeIfAbsent(key, k -> buildTemplate(model));
        ItemStack item = template.clone();
        item.setAmount(Math.clamp(amount, 1, model.getMaxStack()));
        return item;
    }

    /**
     * テンプレートキャッシュをクリアします。
     * アイテム定義のリロード時に呼び出してください。
     */
    public void clearCache() {
        templateCache.clear();
        Logger.log(LogId.D_5210);
    }

    /**
     * キャッシュ済みテンプレート数を返します。
     *
     * @return キャッシュ数
     */
    public int cacheSize() {
        return templateCache.size();
    }

    // endregion

    // region --- PDC 読み取りユーティリティ (PacketAdapter 用) ---

    /**
     * ItemStack に埋め込まれた AstralRecord アイテムIDを取得します。
     *
     * @param item 判定対象
     * @return アイテムID。AstralRecord アイテムでなければ {@code null}
     */
    public static @Nullable String getAstralItemId(@NotNull ItemStack item) {
        if (!item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer()
                .get(KEY_ITEM_ID, PersistentDataType.STRING);
    }

    /**
     * ItemStack に埋め込まれた icon Material 名を取得します。
     *
     * @param item 判定対象
     * @return icon 名。未設定なら {@code null}
     */
    public static @Nullable String getIconName(@NotNull ItemStack item) {
        if (!item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer()
                .get(KEY_ICON, PersistentDataType.STRING);
    }

    // endregion

    // region --- テンプレート構築 ---

    /**
     * ItemModel からプロトタイプ ItemStack を構築します（キャッシュ用）。
     */
    private @NotNull ItemStack buildTemplate(@NotNull ItemModel model) {
        ItemStack item = new ItemStack(BASE_MATERIAL, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        // --- 表示名 ---
        String rarityColor = rarityToColor(model.getRarity());
        meta.displayName(LEGACY_SERIALIZER.deserialize(
                rarityColor + model.getName() + ColorCodeUtil.RESET));

        // --- Lore 構築 ---
        List<String> loreStrings = buildLore(model);
        meta.lore(loreStrings.stream()
                .map(LEGACY_SERIALIZER::deserialize)
                .map(c -> (Component) c)
                .toList());

        // --- ItemModel（リソースパック側のアイテムモデル参照） ---
        if (model.getCustomModelData() != null) {
            meta.setItemModel(new NamespacedKey("astralrecord", "item/" + model.getCustomModelData()));
        }

        // --- PDC にメタ情報を格納 ---
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(KEY_ITEM_ID, PersistentDataType.STRING, model.getId());
        pdc.set(KEY_ICON, PersistentDataType.STRING, model.getIcon().toUpperCase(Locale.ROOT));
        pdc.set(KEY_CATEGORY, PersistentDataType.STRING, model.getCategory());
        pdc.set(KEY_RARITY, PersistentDataType.STRING, model.getRarity());

        item.setItemMeta(meta);

        Logger.log(LogId.D_5211, model.getCategory(), model.getId());
        return item;
    }

    // endregion

    // region --- Lore 構築 ---

    /**
     * ItemModel の情報を元に Lore 行リストを構築します。
     */
    private @NotNull List<String> buildLore(@NotNull ItemModel model) {
        List<String> lore = new ArrayList<>();

        // レアリティ・カテゴリ行
        lore.add(ColorCodeUtil.DARK_GRAY + model.getCategory() + " | " + model.getRarity());
        lore.add("");

        // ユーザー定義 lore
        if (!model.getLore().isEmpty()) {
            for (String line : model.getLore()) {
                lore.add(ColorCodeUtil.GRAY + ColorCodeUtil.translateAlternateColorCodes(line));
            }
            lore.add("");
        }

        // equipment ステータス（APIデータをそのまま表示）
        if (model.getEquipment() != null) {
            appendEquipmentLore(lore, model.getEquipment());
        }

        // bundle の Loot 情報
        if (model.getBundle() != null && model.getBundle().getLootTableId() != null) {
            appendBundleLootLore(lore, model.getBundle().getLootTableId());
        }

        // フッター
        lore.add(ColorCodeUtil.DARK_GRAY + "ID: " + model.getId());

        // 取引不可 / 売却不可 フラグ
        if (model.getUnTradeable()) {
            lore.add(ColorCodeUtil.RED + "取引不可");
        }
        if (model.getUnSellable()) {
            lore.add(ColorCodeUtil.RED + "売却不可");
        }

        return lore;
    }

    /**
     * Equipment 情報を Lore に追加します。
     * 現時点では API から取得した親データの表示のみ。
     */
    private void appendEquipmentLore(@NotNull List<String> lore, @NotNull ItemEquipment equipment) {
        lore.add(ColorCodeUtil.GOLD + "--- Equipment ---");

        // スロット / ハンドタイプ
        if (equipment.getSlot() != null) {
            lore.add(ColorCodeUtil.GRAY + "スロット: " + ColorCodeUtil.WHITE + equipment.getSlot().name());
        }
        lore.add(ColorCodeUtil.GRAY + "ハンドタイプ: " + ColorCodeUtil.WHITE + equipment.getHandType().name());

        // 装備条件
        if (equipment.getRequiredLevel() > 0) {
            lore.add(ColorCodeUtil.GRAY + "必要Lv: " + ColorCodeUtil.YELLOW + equipment.getRequiredLevel());
        }
        if (!equipment.getRequiredClasses().isEmpty()) {
            lore.add(ColorCodeUtil.GRAY + "必要クラス: " + ColorCodeUtil.WHITE
                    + String.join(", ", equipment.getRequiredClasses()));
        }

        // ステータス
        if (!equipment.getStats().isEmpty()) {
            lore.add("");
            for (ItemEquipmentStat stat : equipment.getStats()) {
                String prefix = stat.getType().name().equals("SCALAR") ? "×" : "+";
                lore.add(ColorCodeUtil.AQUA + " " + prefix + stat.getValue()
                        + " " + ColorCodeUtil.WHITE + stat.getStatus());
            }
        }

        // 耐久値
        if (equipment.getDurability() != null) {
            lore.add(ColorCodeUtil.GRAY + "耐久値: " + ColorCodeUtil.WHITE
                    + equipment.getDurability().getMax());
        }

        lore.add("");
    }

    /**
     * Bundle に紐付く Loot テーブルの内容を Lore に追加します。
     * LootService にキャッシュ済みのデータのみを参照し、API リクエストは発行しません。
     */
    private void appendBundleLootLore(@NotNull List<String> lore, @NotNull String lootTableId) {
        LootModel lootModel = lootService.getLoaded(lootTableId);
        if (lootModel == null) {
            lore.add(ColorCodeUtil.DARK_GRAY + "Loot: " + lootTableId + " (未ロード)");
            return;
        }

        lore.add(ColorCodeUtil.GOLD + "--- Loot: " + lootModel.getName() + " ---");
        for (LootEntry entry : lootModel.getEntries()) {
            String amountText = entry.getMinAmount() == entry.getMaxAmount()
                    ? String.valueOf(entry.getMinAmount())
                    : entry.getMinAmount() + "~" + entry.getMaxAmount();
            String weightText = entry.getWeight() >= 100.0
                    ? ""
                    : ColorCodeUtil.DARK_GRAY + " (" + String.format("%.1f%%", entry.getWeight()) + ")";
            lore.add(ColorCodeUtil.GRAY + " " + entry.getItemId()
                    + ColorCodeUtil.DARK_GRAY + " [" + entry.getCategory() + "]"
                    + ColorCodeUtil.WHITE + " x" + amountText + weightText);
        }
        lore.add("");
    }

    // endregion

    // region --- ユーティリティ ---

    /**
     * レアリティ文字列から Minecraft カラーコードを返します。
     */
    private @NotNull String rarityToColor(@NotNull String rarity) {
        return switch (rarity.toLowerCase(Locale.ROOT)) {
            case "common"    -> ColorCodeUtil.WHITE;
            case "uncommon"  -> ColorCodeUtil.GREEN;
            case "rare"      -> ColorCodeUtil.AQUA;
            case "epic"      -> ColorCodeUtil.LIGHT_PURPLE;
            case "legendary" -> ColorCodeUtil.GOLD;
            case "mythic"    -> ColorCodeUtil.RED;
            default          -> ColorCodeUtil.GRAY;
        };
    }

    /**
     * テンプレートキャッシュのキーを生成します。
     */
    private @NotNull String cacheKey(@NotNull ItemModel model) {
        return model.getCategory().toLowerCase(Locale.ROOT)
                + ":" + model.getId().toLowerCase(Locale.ROOT);
    }

    // endregion
}



