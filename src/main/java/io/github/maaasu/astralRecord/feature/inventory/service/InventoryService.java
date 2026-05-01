package io.github.maaasu.astralRecord.feature.inventory.service;

import io.github.maaasu.astralRecord.feature.inventory.model.EquipmentType;
import io.github.maaasu.astralRecord.feature.inventory.model.InventoryEntryDraft;
import io.github.maaasu.astralRecord.feature.inventory.model.InventoryEntryModel;
import io.github.maaasu.astralRecord.feature.inventory.model.InventoryModel;
import io.github.maaasu.astralRecord.feature.inventory.model.InventoryType;
import io.github.maaasu.astralRecord.feature.inventory.repository.InventoryRepository;
import io.github.maaasu.astralRecord.feature.item.model.EquipmentInstance;
import io.github.maaasu.astralRecord.feature.item.model.ItemModel;
import io.github.maaasu.astralRecord.feature.item.model.RuneInstance;
import io.github.maaasu.astralRecord.feature.item.service.ItemService;
import io.github.maaasu.astralRecord.feature.item.service.ItemStackFactory;
import io.github.maaasu.astralRecord.feature.player.model.AstPlayer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ItemService itemService;
    private final ItemStackFactory itemStackFactory;

    public InventoryService(
        InventoryRepository inventoryRepository,
        ItemService itemService,
        ItemStackFactory itemStackFactory
    ) {
        this.inventoryRepository = inventoryRepository;
        this.itemService = itemService;
        this.itemStackFactory = itemStackFactory;
    }

    public List<InventoryModel> getInventories(UUID accountId) {
        return inventoryRepository.findByAccountId(accountId);
    }

    public InventoryModel getInventory(UUID inventoryId) {
        return inventoryRepository.findById(inventoryId);
    }

    public List<InventoryEntryModel> getEntries(UUID inventoryId) {
        return inventoryRepository.findEntries(inventoryId);
    }

    public InventoryModel ensureInventory(
        UUID accountId,
        InventoryType inventoryType,
        Integer slotCapacity,
        UUID createdBy
    ) {
        return inventoryRepository.findByAccountId(accountId).stream()
            .filter(inventory -> inventory.getInventoryType() == inventoryType)
            .findFirst()
            .orElseGet(() -> inventoryRepository.create(accountId, inventoryType, slotCapacity, createdBy, null));
    }

    public InventoryEntryModel addEntry(
        UUID inventoryId,
        InventoryEntryDraft draft,
        UUID createdBy
    ) {
        return inventoryRepository.createEntry(inventoryId, draft, createdBy);
    }

    public void applyInventoriesToGui(AstPlayer astPlayer) {
        var bukkitPlayer = astPlayer.getBukkit();
        clearGuiInventory(bukkitPlayer);

        var inventories = inventoryRepository.findByAccountId(astPlayer.getAccount().getUuid());
        inventories.stream()
            .filter(Objects::nonNull)
            .forEach(inventory -> applyInventoryToGui(bukkitPlayer, inventory));

        bukkitPlayer.updateInventory();
    }

    private void applyInventoryToGui(Player bukkitPlayer, InventoryModel inventory) {
        if (!inventory.isEnabled()) {
            return;
        }

        if (inventory.getInventoryType() == InventoryType.NORMAL) {
            applyNormalInventory(bukkitPlayer, inventory);
            return;
        }

        if (inventory.getInventoryType() == InventoryType.EQUIPMENT) {
            applyEquipmentInventory(bukkitPlayer, inventory);
        }
    }

    private void applyNormalInventory(Player bukkitPlayer, InventoryModel inventory) {
        var entries = inventoryRepository.findEntries(inventory.getInventoryId());
        var playerInventory = bukkitPlayer.getInventory();
        var storageContents = playerInventory.getStorageContents();

        for (InventoryEntryModel entry : entries) {
            Integer slotIndex = entry.getSlotIndex();
            if (slotIndex == null || slotIndex < 0 || slotIndex >= storageContents.length) {
                continue;
            }

            ItemStack itemStack = toItemStack(entry);
            if (itemStack == null) {
                continue;
            }

            storageContents[slotIndex] = itemStack;
        }

        playerInventory.setStorageContents(storageContents);
    }

    private void applyEquipmentInventory(Player bukkitPlayer, InventoryModel inventory) {
        var entries = inventoryRepository.findEntries(inventory.getInventoryId());
        var playerInventory = bukkitPlayer.getInventory();

        for (InventoryEntryModel entry : entries) {
            if (!"EQUIPMENT".equalsIgnoreCase(entry.getInstanceType()) || entry.getInstanceId() == null) {
                continue;
            }

            EquipmentInstance equipmentInstance = itemService.findEquipmentInstanceById(entry.getInstanceId().toString());
            if (equipmentInstance == null) {
                continue;
            }

            ItemModel itemModel = resolveItemModel(equipmentInstance.getItemId());
            if (itemModel == null || itemModel.getEquipment() == null) {
                continue;
            }

            ItemStack itemStack = itemStackFactory.create(itemModel, equipmentInstance, 1);
            EquipmentType equipmentType = EquipmentType.fromItemEquipmentSlot(itemModel.getEquipment().getSlot());
            equipmentType.applyTo(playerInventory, itemStack);
        }
    }

    private ItemStack toItemStack(InventoryEntryModel entry) {
        if (entry.getItemId() != null && !entry.getItemId().isBlank()) {
            ItemModel itemModel = resolveItemModel(entry.getItemId());
            if (itemModel == null) {
                return null;
            }
            int amount = normalizeAmount(entry.getQuantity(), itemModel.getMaxStack());
            return itemStackFactory.create(itemModel, amount);
        }

        if ("EQUIPMENT".equalsIgnoreCase(entry.getInstanceType()) && entry.getInstanceId() != null) {
            EquipmentInstance equipmentInstance = itemService.findEquipmentInstanceById(entry.getInstanceId().toString());
            if (equipmentInstance == null) {
                return null;
            }

            ItemModel itemModel = resolveItemModel(equipmentInstance.getItemId());
            if (itemModel == null) {
                return null;
            }
            return itemStackFactory.create(itemModel, equipmentInstance, 1);
        }

        if ("RUNE".equalsIgnoreCase(entry.getInstanceType()) && entry.getInstanceId() != null) {
            RuneInstance runeInstance = itemService.findRuneInstanceById(entry.getInstanceId().toString());
            if (runeInstance == null) {
                return null;
            }

            ItemModel itemModel = resolveItemModel(runeInstance.getItemId());
            if (itemModel == null) {
                return null;
            }
            return itemStackFactory.create(itemModel, runeInstance, 1);
        }

        return null;
    }

    private ItemModel resolveItemModel(String itemId) {
        ItemModel loaded = itemService.findLoadedById(itemId);
        if (loaded != null) {
            return loaded;
        }
        return itemService.loadItem(itemId);
    }

    private int normalizeAmount(long quantity, int maxStack) {
        long normalized = Math.max(1L, quantity);
        long clamped = Math.min(normalized, Math.max(1, maxStack));
        return (int) clamped;
    }

    private void clearGuiInventory(Player bukkitPlayer) {
        var inventory = bukkitPlayer.getInventory();
        inventory.clear();

        var storage = inventory.getStorageContents();
        Arrays.fill(storage, new ItemStack(Material.AIR));
        inventory.setStorageContents(storage);

        inventory.setArmorContents(new ItemStack[] {
            new ItemStack(Material.AIR),
            new ItemStack(Material.AIR),
            new ItemStack(Material.AIR),
            new ItemStack(Material.AIR),
        });
        inventory.setItemInMainHand(new ItemStack(Material.AIR));
        inventory.setItemInOffHand(new ItemStack(Material.AIR));
    }
}
