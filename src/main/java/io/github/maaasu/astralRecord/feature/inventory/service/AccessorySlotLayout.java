package io.github.maaasu.astralRecord.feature.inventory.service;

import io.github.maaasu.astralRecord.feature.inventory.model.AccessorySlotType;
import io.github.maaasu.astralRecord.feature.inventory.model.EquipmentType;
import io.github.maaasu.astralRecord.feature.inventory.model.InventoryEntryModel;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * アクセサリースロット（オフハンド含む）のレイアウト変換を扱う補助クラスです。
 */
final class AccessorySlotLayout {

    static final int SLOT_OFF_HAND = AccessorySlotType.OFF_HAND.getSlotIndex();
    static final int SLOT_NECKLACE = AccessorySlotType.NECKLACE.getSlotIndex();
    static final int SLOT_RING = AccessorySlotType.RING.getSlotIndex();
    static final int SLOT_EARRING = AccessorySlotType.EARRING.getSlotIndex();
    static final int SLOT_BRACELET = AccessorySlotType.BRACELET.getSlotIndex();
    static final int SLOT_BELT = AccessorySlotType.BELT.getSlotIndex();
    static final int SLOT_CHARM = AccessorySlotType.CHARM.getSlotIndex();
    static final int SLOT_MIN = SLOT_OFF_HAND;
    static final int SLOT_MAX = SLOT_CHARM;
    static final int CAPACITY = SLOT_MAX;

    private AccessorySlotLayout() {
    }

    /**
     * 指定スロットがアクセサリー管理対象か判定します。
     *
     * @param slotIndex API側スロット番号
     * @return 管理対象なら true
     */
    static boolean isManagedSlot(int slotIndex) {
        return slotIndex >= SLOT_MIN && slotIndex <= SLOT_MAX;
    }

    /**
     * APIエントリをプレイヤーのアクセサリースロットへ適用します。
     *
     * @param player 反映先プレイヤー
     * @param entries 反映対象エントリ一覧
     * @param applier エントリを ItemStack へ解決する関数
     */
    static void applyEntriesToPlayer(
        @NotNull Player player,
        @NotNull List<InventoryEntryModel> entries,
        @NotNull EquipSlotLayout.ItemStackApplier applier
    ) {
        PlayerInventory inventory = player.getInventory();
        for (InventoryEntryModel entry : entries) {
            Integer slotIndex = entry.getSlotIndex();
            if (slotIndex == null || !isManagedSlot(slotIndex)) {
                continue;
            }
            EquipmentType equipmentType = EquipmentType.fromAccessorySlotIndex(slotIndex.intValue());
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
     * プレイヤーのアクセサリースロット状態をスナップショット化します。
     *
     * @param player 取得元プレイヤー
     * @return スロット番号を添字とする配列
     */
    static @NotNull ItemStack[] createSnapshot(@NotNull Player player) {
        ItemStack[] snapshot = new ItemStack[SLOT_MAX + 1];
        snapshot[SLOT_OFF_HAND] = player.getInventory().getItemInOffHand();
        return snapshot;
    }

    /**
     * スナップショット内容をプレイヤーのアクセサリースロットへ反映します。
     *
     * @param player 反映先プレイヤー
     * @param snapshot 反映するスナップショット
     */
    static void applySnapshot(@NotNull Player player, @NotNull ItemStack[] snapshot) {
        if (snapshot.length > SLOT_OFF_HAND && snapshot[SLOT_OFF_HAND] != null) {
            player.getInventory().setItemInOffHand(snapshot[SLOT_OFF_HAND]);
            return;
        }
        player.getInventory().setItemInOffHand(new ItemStack(org.bukkit.Material.AIR));
    }
}
