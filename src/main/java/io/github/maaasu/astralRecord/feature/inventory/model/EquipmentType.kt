package io.github.maaasu.astralRecord.feature.inventory.model

import io.github.maaasu.astralRecord.feature.item.model.ItemEquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory

enum class EquipmentType {
    MAIN_HAND,
    HEAD,
    CHEST,
    LEGS,
    FEET,
    UNSUPPORTED,
    ;

    fun applyTo(inventory: PlayerInventory, itemStack: ItemStack?) {
        when (this) {
            MAIN_HAND -> inventory.setItemInMainHand(itemStack)
            HEAD -> inventory.helmet = itemStack
            CHEST -> inventory.chestplate = itemStack
            LEGS -> inventory.leggings = itemStack
            FEET -> inventory.boots = itemStack
            UNSUPPORTED -> Unit
        }
    }

    companion object {
        @JvmStatic
        fun fromItemEquipmentSlot(slot: ItemEquipmentSlot?): EquipmentType =
            when (slot) {
                ItemEquipmentSlot.WEAPON -> MAIN_HAND
                ItemEquipmentSlot.HEAD -> HEAD
                ItemEquipmentSlot.CHEST -> CHEST
                ItemEquipmentSlot.LEGS -> LEGS
                ItemEquipmentSlot.FEET -> FEET
                else -> UNSUPPORTED
            }
    }
}
