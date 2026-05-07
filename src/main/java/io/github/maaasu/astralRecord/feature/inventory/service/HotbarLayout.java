package io.github.maaasu.astralRecord.feature.inventory.service;

import io.github.maaasu.astralRecord.feature.inventory.model.InventoryEntryModel;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * ホットバー領域のスロット変換・反映を扱う補助クラスです。
 */
final class HotbarLayout {

    static final int DB_SLOT_START = 1;
    static final int DB_SLOT_END = 9;
    static final int BUKKIT_SLOT_START = 0;
    static final int BUKKIT_SLOT_END = 8;
    static final int CAPACITY = DB_SLOT_END - DB_SLOT_START + 1;

    private HotbarLayout() {
    }

    /**
     * 指定スロットがホットバー管理対象か判定します。
     *
     * @param dbSlotIndex API側スロット番号
     * @return 管理対象なら true
     */
    static boolean isManagedSlot(int dbSlotIndex) {
        return dbSlotIndex >= DB_SLOT_START && dbSlotIndex <= DB_SLOT_END;
    }

    /**
     * API側スロット番号を Bukkit 側スロット番号へ変換します。
     *
     * @param dbSlotIndex API側スロット番号
     * @return Bukkit側スロット番号
     */
    static int toBukkitSlot(int dbSlotIndex) {
        return BUKKIT_SLOT_START + (dbSlotIndex - DB_SLOT_START);
    }

    /**
     * Bukkit 側スロット番号を API側スロット番号へ変換します。
     *
     * @param bukkitSlot Bukkit側スロット番号
     * @return API側スロット番号
     */
    static int toDbSlot(int bukkitSlot) {
        return DB_SLOT_START + (bukkitSlot - BUKKIT_SLOT_START);
    }

    /**
     * プレイヤーのホットバー状態をスナップショット化します。
     *
     * @param player 取得元プレイヤー
     * @return ホットバー領域のスナップショット
     */
    static @NotNull ItemStack[] createSnapshot(@NotNull Player player) {
        ItemStack[] storage = player.getInventory().getStorageContents();
        ItemStack[] snapshot = new ItemStack[BUKKIT_SLOT_END + 1];
        int len = Math.min(BUKKIT_SLOT_END + 1, storage.length);
        System.arraycopy(storage, BUKKIT_SLOT_START, snapshot, BUKKIT_SLOT_START, len);
        return snapshot;
    }

    /**
     * スナップショット内容をプレイヤーのホットバーへ反映します。
     *
     * @param player 反映先プレイヤー
     * @param snapshot 反映するスナップショット
     */
    static void applySnapshot(@NotNull Player player, @NotNull ItemStack[] snapshot) {
        ItemStack[] storage = player.getInventory().getStorageContents();
        int len = Math.min(BUKKIT_SLOT_END + 1, Math.min(snapshot.length, storage.length));
        for (int i = BUKKIT_SLOT_START; i < len; i++) {
            storage[i] = itemOrAir(snapshot[i]);
        }
        player.getInventory().setStorageContents(storage);
    }

    /**
     * APIエントリをプレイヤーのホットバーへ適用します。
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
        ItemStack[] storage = player.getInventory().getStorageContents();
        for (InventoryEntryModel entry : entries) {
            Integer slotIndex = entry.getSlotIndex();
            if (slotIndex == null || !isManagedSlot(slotIndex) || entry.isDeleted()) {
                continue;
            }
            int bukkitSlot = toBukkitSlot(slotIndex);
            if (bukkitSlot < 0 || bukkitSlot >= storage.length) {
                continue;
            }
            storage[bukkitSlot] = itemOrAir(applier.resolve(entry));
        }
        player.getInventory().setStorageContents(storage);
    }

    private static @NotNull ItemStack itemOrAir(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return new ItemStack(Material.AIR);
        }
        return itemStack;
    }
}
