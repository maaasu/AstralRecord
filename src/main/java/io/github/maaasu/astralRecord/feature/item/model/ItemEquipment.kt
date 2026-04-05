package io.github.maaasu.astralRecord.feature.item.model

/**
 * equipment カテゴリ拡張定義。
 */
data class ItemEquipment(
    val slot: ItemEquipmentSlot?,
    val handType: ItemEquipmentHandType = ItemEquipmentHandType.ONE,
    val requiredLevel: Int = 0,
    val requiredClasses: List<String> = emptyList(),
    val setId: String? = null,
    val stats: List<ItemEquipmentStat> = emptyList(),
    val durability: ItemEquipmentDurability?,
    val onUse: ItemEquipmentOnUse?,
    val skills: List<String> = emptyList(),
)

enum class ItemEquipmentSlot {
    WEAPON,
    HEAD,
    CHEST,
    LEGS,
    FEET,
    ACCESSORY,
    TOOL,
    UNKNOWN,
    ;

    companion object {
        @JvmStatic
        fun fromApiValue(value: String?): ItemEquipmentSlot {
            if (value.isNullOrBlank()) {
                return UNKNOWN
            }
            return try {
                valueOf(value.trim().uppercase())
            } catch (_: IllegalArgumentException) {
                UNKNOWN
            }
        }
    }
}

enum class ItemEquipmentHandType {
    ONE,
    TWO,
    ;

    companion object {
        @JvmStatic
        fun fromApiValue(value: String?): ItemEquipmentHandType {
            if (value.isNullOrBlank()) {
                return ONE
            }
            return try {
                valueOf(value.trim().uppercase())
            } catch (_: IllegalArgumentException) {
                ONE
            }
        }
    }
}

enum class ItemEquipmentStatType {
    FLAT,
    SCALAR,
    ;

    companion object {
        @JvmStatic
        fun fromApiValue(value: String?): ItemEquipmentStatType {
            if (value.isNullOrBlank()) {
                return FLAT
            }
            return try {
                valueOf(value.trim().uppercase())
            } catch (_: IllegalArgumentException) {
                FLAT
            }
        }
    }
}

data class ItemEquipmentStat(
    val status: String,
    val type: ItemEquipmentStatType,
    // スキーマ上は固定値/範囲の両方を受けるため String で保持する。
    val value: String,
)

data class ItemEquipmentDurability(
    val max: Int,
    val consume: Int = 1,
)

data class ItemEquipmentOnUse(
    val leftClickCooldownTicks: Int?,
    val leftClickSkillId: String?,
    val rightClickCooldownTicks: Int?,
    val rightClickSkillId: String?,
)

