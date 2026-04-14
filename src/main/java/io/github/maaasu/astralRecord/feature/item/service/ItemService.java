package io.github.maaasu.astralRecord.feature.item.service;

import io.github.maaasu.astralRecord.feature.item.model.ItemCategory;
import io.github.maaasu.astralRecord.feature.item.model.ItemModel;
import io.github.maaasu.astralRecord.feature.item.model.ItemSummary;
import io.github.maaasu.astralRecord.feature.item.repository.ItemRepository;
import io.github.maaasu.astralRecord.infrastructure.logging.LogId;
import io.github.maaasu.astralRecord.infrastructure.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * アイテム機能の最小サービス。
 * APIから取得したアイテムをメモリに保持し、一覧/詳細参照に使用します。
 */
public class ItemService {

    private final ItemRepository itemRepository;
    private final Map<String, ItemModel> loadedItems;

    public ItemService() {
        this.itemRepository = new ItemRepository();
        this.loadedItems = new LinkedHashMap<>();
    }

    /**
     * 全カテゴリのアイテムを API から一括取得してキャッシュへ登録します。
     * 起動時の初期ロードに使用します。
     *
     * @return ロードしたアイテムの総件数
     */
    public int loadAll() {
        int total = 0;
        Map<String, Integer> categoryCounts = new HashMap<>();

        try {
            List<ItemSummary> summaries = itemRepository.findAll();
            for (ItemSummary summary : summaries) {
                ItemModel item = itemRepository.findById(summary.getId());
                if (item == null) {
                    continue;
                }

                loadedItems.put(normalize(item.getId()), item);
                categoryCounts.merge(item.getCategory().toLowerCase(Locale.ROOT), 1, Integer::sum);
                total++;
            }
        } catch (Exception e) {
            Logger.log(LogId.E_5202, e, "loadAll");
        }

        for (Map.Entry<String, Integer> entry : categoryCounts.entrySet()) {
            Logger.log(LogId.I_5202, entry.getKey(), entry.getValue());
        }

        Logger.log(LogId.I_5203, total);
        return total;
    }

    /**
     * 指定カテゴリのアイテムを API から一括取得してキャッシュへ登録します。
     *
     * @param category カテゴリ
     * @return ロードしたアイテム件数
     */
    public int loadAllByCategory(@NotNull String category) {
        String normalizedCategory = normalize(category);
        if (normalizedCategory.isBlank()) {
            return 0;
        }

        try {
            List<io.github.maaasu.astralRecord.feature.item.model.ItemModel> items =
                itemRepository.findAllByCategory(normalizedCategory);
            for (io.github.maaasu.astralRecord.feature.item.model.ItemModel item : items) {
                loadedItems.put(normalize(item.getId()), item);
            }
            Logger.log(LogId.I_5202, normalizedCategory, items.size());
            return items.size();
        } catch (Exception e) {
            Logger.log(LogId.E_5202, e, normalizedCategory);
            return 0;
        }
    }

    /**
     * アイテムをAPIから取得してロード済みキャッシュへ登録します。
     */
    public @Nullable ItemModel loadItem(@NotNull String itemId) {
        String normalizedId = normalize(itemId);
        if (normalizedId.isBlank()) {
            return null;
        }

        ItemModel item = itemRepository.findById(itemId);
        if (item == null) {
            return null;
        }

        loadedItems.put(normalize(item.getId()), item);
        return item;
    }

    /**
     * ロード済みアイテムの一覧を返します。
     */
    public @NotNull List<ItemModel> getLoadedItems() {
        return loadedItems.values().stream()
            .sorted(Comparator.comparing(ItemModel::getCategory).thenComparing(ItemModel::getId))
            .toList();
    }

    /**
     * 指定カテゴリでロード済みアイテムを絞り込みます。
     */
    public @NotNull List<ItemModel> getLoadedItemsByCategory(@NotNull String category) {
        String normalized = normalize(category);
        if (normalized.isBlank()) {
            return List.of();
        }

        return getLoadedItems().stream()
            .filter(item -> item.getCategory().equalsIgnoreCase(normalized))
            .toList();
    }

    /**
     * IDでロード済みアイテムを検索します。
     */
    public @Nullable ItemModel findLoadedById(@NotNull String itemId) {
        String normalizedId = normalize(itemId);
        if (normalizedId.isBlank()) {
            return null;
        }

        return loadedItems.get(normalizedId);
    }

    public @NotNull List<String> getLoadedCategories() {
        return getLoadedItems().stream()
            .map(ItemModel::getCategory)
            .distinct()
            .toList();
    }

    public @NotNull List<String> getLoadedItemIds() {
        return getLoadedItems().stream()
            .map(ItemModel::getId)
            .distinct()
            .toList();
    }

    public boolean isSupportedCategory(@NotNull String category) {
        String normalized = normalize(category);
        return ItemCategory.supportedApiValues().stream()
            .anyMatch(supported -> supported.equals(normalized));
    }

    public @NotNull List<String> getSupportedCategories() {
        return ItemCategory.supportedApiValues();
    }

    private @NotNull String normalize(@NotNull String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}



