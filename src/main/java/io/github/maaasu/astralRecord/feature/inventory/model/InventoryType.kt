package io.github.maaasu.astralRecord.feature.inventory.model

enum class InventoryType(
    val code: String,
    val displayNameJa: String,
    val isSlotted: Boolean,
    val isInstanceBacked: Boolean,
    private vararg val inputAliases: String,
) {
    NORMAL("NORMAL", "ノーマル", true, false, "normal", "ノーマル"),
    CURRENCY("CURRENCY", "通貨", true, false, "currency", "通貨"),
    EQUIPMENT("EQUIPMENT", "装備", true, true, "equipment", "equip", "装備"),
    RUNE("RUNE", "ルーン", true, true, "rune", "ルーン"),
    ;

    companion object {
        @JvmStatic
        fun fromCode(code: String): InventoryType =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unsupported inventory type: $code")

        @JvmStatic
        fun fromInput(value: String): InventoryType? {
            return entries.firstOrNull { type ->
                type.inputAliases.any { it.equals(value, ignoreCase = true) }
                    || type.name.equals(value, ignoreCase = true)
                    || type.code.equals(value, ignoreCase = true)
            }
        }
    }
}
