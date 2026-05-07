package io.github.maaasu.astralRecord.feature.inventory.service;

import io.github.maaasu.astralRecord.feature.inventory.model.EquipmentType;
import io.github.maaasu.astralRecord.feature.inventory.model.InventoryEntryModel;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 防具スロット（頭・胴・脚・足）のレイアウト変換を扱う補助クラスです。
 */
final class EquipSlotLayout {

    static final int SLOT_MAIN_HAND = 1;
    static final int SLOT_HEAD = 2;
    static final int SLOT_CHEST = 3;
    static final int SLOT_LEGS = 4;
    static final int SLOT_FEET = 5;
    static final int SLOT_MIN = SLOT_HEAD;
    static final int SLOT_MAX = SLOT_FEET;

    private EquipSlotLayout() {
    }

    /**
     * 指定スロットが防具管理対象か判定します。
     *
     * @param slotIndex API側スロット番号
     * @return 管理対象なら true
     */
    static boolean isManagedSlot(int slotIndex) {
        return slotIndex >= SLOT_MIN && slotIndex <= SLOT_MAX;
    }

    /**
     * APIエントリをプレイヤーの防具スロットへ適用します。
     *
     * @param player 反映先プレイヤー
     * @param entries 反映対象エントリ一覧
     * @param applier エントリを ItemStack へ解決する関数
     */
    static void applyEntriesToPlayer(
        @NotNull Player player,
        @NotNull List<InventoryEntryModel> entries,
        @NotNull ItemStackApplier applier
    ) {
        PlayerInventory inventory = player.getInventory();
        for (InventoryEntryModel entry : entries) {
            Integer slotIndex = entry.getSlotIndex();
            if (slotIndex == null || !isManagedSlot(slotIndex)) {
                continue;
            }
            EquipmentType equipmentType = EquipmentType.fromEquipSlotIndex(slotIndex.intValue());
            if (equipmentType == EquipmentType.UNSUPPORTED) {
                continue;
            }
            ItemStack itemStack = applier.resolve(entry);
            if (itemStack == null) {
                continue;
            }
            equipmentType.applyTo(inventory, itemStack);
        }
    }

    /**
     * プレイヤーの防具スロット状態をスナップショット化します。
     *
     * @param player 取得元プレイヤー
     * @return スロット番号を添字とする配列
     */
    static @NotNull ItemStack[] createSnapshot(@NotNull Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] snapshot = new ItemStack[SLOT_MAX + 1];
        snapshot[SLOT_HEAD] = inventory.getHelmet();
        snapshot[SLOT_CHEST] = inventory.getChestplate();
        snapshot[SLOT_LEGS] = inventory.getLeggings();
        snapshot[SLOT_FEET] = inventory.getBoots();
        return snapshot;
    }

    /**
     * スナップショット内容をプレイヤーの防具スロットへ反映します。
     *
     * @param player 反映先プレイヤー
     * @param snapshot 反映するスナップショット
     */
    static void applySnapshot(@NotNull Player player, @NotNull ItemStack[] snapshot) {
        PlayerInventory inventory = player.getInventory();
        inventory.setHelmet(itemOrAir(snapshot, SLOT_HEAD));
        inventory.setChestplate(itemOrAir(snapshot, SLOT_CHEST));
        inventory.setLeggings(itemOrAir(snapshot, SLOT_LEGS));
        inventory.setBoots(itemOrAir(snapshot, SLOT_FEET));
    }

    private static @NotNull ItemStack itemOrAir(@NotNull ItemStack[] snapshot, int slotIndex) {
        if (snapshot.length <= slotIndex) {
            return new ItemStack(org.bukkit.Material.AIR);
        }
        return snapshot[slotIndex];
    }

    /**
     * エントリを ItemStack へ変換する関数インターフェースです。
     */
    @FunctionalInterface
    interface ItemStackApplier {
        @Nullable ItemStack resolve(@NotNull InventoryEntryModel entry);
    }
}
