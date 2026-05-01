package io.github.maaasu.astralRecord.feature.inventory.model

enum class InventoryType(
    val code: String,
    val isSlotted: Boolean,
    val isInstanceBacked: Boolean,
) {
    NORMAL("NORMAL", true, false),
    CURRENCY("CURRENCY", false, false),
    EQUIPMENT("EQUIPMENT", true, true),
    RUNE("RUNE", true, true),
    ;

    companion object {
        fun fromCode(code: String): InventoryType =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unsupported inventory type: $code")
    }
}
