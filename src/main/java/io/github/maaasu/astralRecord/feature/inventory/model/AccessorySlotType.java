package io.github.maaasu.astralRecord.feature.inventory.model;

import org.jetbrains.annotations.NotNull;

public enum AccessorySlotType {
    OFF_HAND(1, "オフハンド"),
    NECKLACE(2, "ネックレス"),
    RING(3, "リング"),
    EARRING(4, "イヤリング"),
    BRACELET(5, "ブレスレット"),
    BELT(6, "ベルト"),
    CHARM(7, "チャーム");

    private final int slotIndex;
    private final String displayName;

    AccessorySlotType(int slotIndex, @NotNull String displayName) {
        this.slotIndex = slotIndex;
        this.displayName = displayName;
    }

    /**
     * API が扱うアクセサリースロット番号を返します。
     *
     * @return 1始まりのスロット番号
     */
    public int getSlotIndex() {
        return slotIndex;
    }

    /**
     * 表示用のスロット名を返します。
     *
     * @return 日本語表示名
     */
    public @NotNull String getDisplayName() {
        return displayName;
    }

    /**
     * スロット番号からアクセサリー種別を返します。
     *
     * @param slotIndex スロット番号
     * @return 対応するアクセサリー種別
     */
    public static @NotNull AccessorySlotType fromSlotIndex(int slotIndex) {
        for (AccessorySlotType type : values()) {
            if (type.slotIndex == slotIndex) {
                return type;
            }
        }
        return CHARM;
    }
}
