package io.github.maaasu.astralRecord.feature.inventory.service;

import io.github.maaasu.astralRecord.feature.inventory.model.EquipmentType;
import io.github.maaasu.astralRecord.feature.inventory.model.EquipmentLoadoutModel;
import io.github.maaasu.astralRecord.feature.inventory.model.EquipmentLoadoutSlotModel;
import io.github.maaasu.astralRecord.feature.inventory.model.InventoryEntryDraft;
import io.github.maaasu.astralRecord.feature.inventory.model.InventoryEntryModel;
import io.github.maaasu.astralRecord.feature.inventory.model.InventoryInstanceType;
import io.github.maaasu.astralRecord.feature.inventory.model.InventoryModel;
import io.github.maaasu.astralRecord.feature.inventory.model.InventoryProfile;
import io.github.maaasu.astralRecord.feature.inventory.model.InventoryType;
import io.github.maaasu.astralRecord.feature.inventory.repository.EquipmentLoadoutRepository;
import io.github.maaasu.astralRecord.feature.inventory.repository.InventoryRepository;
import io.github.maaasu.astralRecord.feature.item.model.EquipmentInstance;
import io.github.maaasu.astralRecord.feature.item.model.ItemCategory;
import io.github.maaasu.astralRecord.feature.item.model.ItemEquipmentSlot;
import io.github.maaasu.astralRecord.feature.item.model.ItemModel;
import io.github.maaasu.astralRecord.feature.item.model.RuneInstance;
import io.github.maaasu.astralRecord.feature.item.service.ItemService;
import io.github.maaasu.astralRecord.feature.item.service.ItemStackFactory;
import io.github.maaasu.astralRecord.feature.player.model.AstPlayer;
import io.github.maaasu.astralRecord.infrastructure.logging.LogId;
import io.github.maaasu.astralRecord.infrastructure.logging.Logger;
import io.github.maaasu.astralRecord.infrastructure.util.ColorCodeUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InventoryService {
    private static final InventoryProfile DEFAULT_PROFILE = InventoryProfile.GAME;
    private static final String DEFAULT_LOADOUT_NAME = "Default";
    private static final String SLOT_TYPE_HEAD = "HEAD";
    private static final String SLOT_TYPE_CHEST = "CHEST";
    private static final String SLOT_TYPE_LEGS = "LEGS";
    private static final String SLOT_TYPE_FEET = "FEET";
    private static final String SLOT_TYPE_ACCESSORY = "ACCESSORY";

    private final InventoryRepository inventoryRepository;
    private final EquipmentLoadoutRepository equipmentLoadoutRepository;
    private final ItemService itemService;
    private final InventoryItemStackResolver itemStackResolver;
    private final InventorySnapshotCodec snapshotCodec;
    private final Map<UUID, InventoryType> displayedInventoryTypes = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Integer, InventoryEntryModel>> hotbarEntryCache = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> selectedHotbarSlots = new ConcurrentHashMap<>();

    public InventoryService(
        InventoryRepository inventoryRepository,
        EquipmentLoadoutRepository equipmentLoadoutRepository,
        ItemService itemService,
        ItemStackFactory itemStackFactory
    ) {
        this.inventoryRepository = inventoryRepository;
        this.equipmentLoadoutRepository = equipmentLoadoutRepository;
        this.itemService = itemService;
        this.itemStackResolver = new InventoryItemStackResolver(itemService, itemStackFactory);
        this.snapshotCodec = new InventorySnapshotCodec();
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
            .filter(this::isDefaultProfile)
            .findFirst()
            .orElseGet(() -> inventoryRepository.create(
                accountId,
                inventoryType,
                slotCapacity,
                createdBy,
                DEFAULT_PROFILE,
                null
            ));
    }

    public InventoryEntryModel addEntry(
        UUID inventoryId,
        InventoryEntryDraft draft,
        UUID createdBy
    ) {
        return inventoryRepository.createEntry(inventoryId, draft, createdBy);
    }

    /**
     * 通常インベントリへアイテムをデータとして追加します。
     * <p>
     * 追加対象スロットは 9〜35（上段 3x9 の 27 マス）のみです。
     * 0〜8（ツールバー）は追加判定から除外します。
     *
     * @param accountId 追加対象アカウントID
     * @param model 追加するアイテム定義
     * @param amount 追加希望個数（1未満は1として扱う）
     * @return 実際に追加できた個数
     */
    public int addItemToNormalInventory(
        @NotNull UUID accountId,
        @NotNull ItemModel model,
        int amount
    ) {
        var safeAmount = Math.max(1, amount);
        InventoryType inventoryType = resolveTargetInventoryType(model);
        var targetInventory = ensureInventory(accountId, inventoryType, resolveSlotCapacity(inventoryType), accountId);
        Set<Integer> usedSlots = targetInventory.getInventoryType().isSlotted()
            ? NormalInventoryLayout.collectUsedSlots(getEntries(targetInventory.getInventoryId()))
            : Set.of();

        return switch (ItemCategory.fromApiValue(model.getCategory())) {
            case EQUIPMENT -> addInstanceItems(targetInventory, model, safeAmount, InventoryInstanceType.EQUIPMENT, usedSlots, accountId);
            case RUNE -> addInstanceItems(targetInventory, model, safeAmount, InventoryInstanceType.RUNE, usedSlots, accountId);
            default -> addStackedItems(targetInventory, model, safeAmount, usedSlots, accountId);
        };
    }

    /**
     * 通常インベントリへアイテムを追加し、現在の Bukkit インベントリ状態も考慮して空きスロットを決定します。
     * <p>
     * オンラインプレイヤーへコマンドなどで即時付与する場合はこちらを使用します。
     *
     * @param astPlayer 追加対象プレイヤー
     * @param model 追加するアイテム定義
     * @param amount 追加希望個数（1未満は1として扱う）
     * @return 実際に追加できた個数
     */
    public int addItemToNormalInventory(
        @NotNull AstPlayer astPlayer,
        @NotNull ItemModel model,
        int amount
    ) {
        var accountId = astPlayer.getAccount().getUuid();
        int safeAmount = Math.max(1, amount);
        InventoryType inventoryType = resolveTargetInventoryType(model);
        var targetInventory = ensureInventory(accountId, inventoryType, resolveSlotCapacity(inventoryType), accountId);

        Set<Integer> usedSlots = targetInventory.getInventoryType().isSlotted()
            ? NormalInventoryLayout.collectUsedSlots(getEntries(targetInventory.getInventoryId()))
            : Set.of();

        return switch (ItemCategory.fromApiValue(model.getCategory())) {
            case EQUIPMENT -> addInstanceItems(targetInventory, model, safeAmount, InventoryInstanceType.EQUIPMENT, usedSlots, accountId);
            case RUNE -> addInstanceItems(targetInventory, model, safeAmount, InventoryInstanceType.RUNE, usedSlots, accountId);
            default -> addStackedItems(targetInventory, model, safeAmount, usedSlots, accountId);
        };
    }

    public void applyInventoriesToGui(AstPlayer astPlayer) {
        applyInventoryToGui(astPlayer, InventoryType.NORMAL);
        if (!applyActiveEquipmentLoadoutToGui(astPlayer)) {
            applyEquipSlotInventoryToGui(astPlayer);
            applyAccessorySlotInventoryToGui(astPlayer);
        }
        applyHotbarInventoryToGui(astPlayer);
    }

    public List<EquipmentLoadoutModel> getEquipmentLoadouts(UUID accountId) {
        return equipmentLoadoutRepository.findByAccountId(accountId, DEFAULT_PROFILE);
    }

    public @Nullable EquipmentLoadoutModel getActiveEquipmentLoadout(UUID accountId) {
        try {
            return equipmentLoadoutRepository.findByAccountId(accountId, DEFAULT_PROFILE).stream()
                .filter(loadout -> loadout.isActive() && !loadout.isDeleted())
                .findFirst()
                .orElse(null);
        } catch (RuntimeException e) {
            Logger.warn(LogId.W_5252, accountId, e.getMessage());
            return null;
        }
    }

    public @Nullable EquipmentLoadoutModel ensureActiveEquipmentLoadout(UUID accountId) {
        EquipmentLoadoutModel active = getActiveEquipmentLoadout(accountId);
        if (active != null) {
            return active;
        }

        try {
            List<EquipmentLoadoutModel> loadouts = equipmentLoadoutRepository.findByAccountId(accountId, DEFAULT_PROFILE);
            if (!loadouts.isEmpty()) {
                EquipmentLoadoutModel activated = equipmentLoadoutRepository.activate(loadouts.get(0).getEquipmentLoadoutId(), accountId);
                if (activated != null) {
                    return activated;
                }
            }

            return equipmentLoadoutRepository.create(
                accountId,
                DEFAULT_LOADOUT_NAME,
                accountId,
                DEFAULT_PROFILE,
                0,
                true,
                null
            );
        } catch (RuntimeException e) {
            Logger.warn(LogId.W_5253, accountId, e.getMessage());
            return null;
        }
    }

    public InventoryType resolveInventoryType(@NotNull ItemModel model) {
        return resolveTargetInventoryType(model);
    }

    public void applyInventoryToGui(AstPlayer astPlayer, InventoryType inventoryType) {
        if (!astPlayer.getAccount().getMode().shouldReflectInventoryToGui()) {
            return;
        }

        saveEquipSlotSnapshot(astPlayer);
        saveAccessorySlotSnapshot(astPlayer);
        syncCurrentEquipmentState(astPlayer);

        displayedInventoryTypes.put(astPlayer.getAccount().getUuid(), inventoryType);

        var bukkitPlayer = astPlayer.getBukkit();

        var inventories = inventoryRepository.findByAccountId(astPlayer.getAccount().getUuid());
        var selectedInventory = inventories.stream()
            .filter(this::isDefaultProfile)
            .filter(inventory -> inventory.getInventoryType() == inventoryType)
            .findFirst()
            .orElse(null);
        applyInventoryToGui(bukkitPlayer, selectedInventory);

        if (!applyActiveEquipmentLoadoutToGui(astPlayer)) {
            applyEquipSlotInventoryToGui(astPlayer);
            applyAccessorySlotInventoryToGui(astPlayer);
        }
        applyHotbarInventoryToGui(astPlayer);
    }

    public @NotNull InventoryType getDisplayedInventoryType(@NotNull UUID accountId) {
        return displayedInventoryTypes.getOrDefault(accountId, InventoryType.NORMAL);
    }

    public void clearGuiInventory(@NotNull AstPlayer astPlayer) {
        clearGuiInventory(astPlayer.getBukkit());
        astPlayer.getBukkit().updateInventory();
    }

    private void applyInventoryToGui(Player bukkitPlayer, @Nullable InventoryModel inventory) {
        if (inventory == null || !inventory.isEnabled()) {
            clearManagedStorageSlots(bukkitPlayer);
            return;
        }

        if (inventory.getInventoryType() == InventoryType.NORMAL) {
            applySlottedInventory(bukkitPlayer, inventory);
            return;
        }

        if (inventory.getInventoryType().isSlotted()) {
            applySlottedInventory(bukkitPlayer, inventory);
        }
    }

    private void applySlottedInventory(Player bukkitPlayer, InventoryModel inventory) {
        var entries = inventoryRepository.findEntries(inventory.getInventoryId());
        var playerInventory = bukkitPlayer.getInventory();
        Map<Integer, ItemStack> itemByGuiSlot = new HashMap<>();

        for (InventoryEntryModel entry : entries) {
            Integer slotIndex = entry.getSlotIndex();
            if (slotIndex == null || !NormalInventoryLayout.isManagedSlot(slotIndex)) {
                continue;
            }
            int guiSlotIndex = NormalInventoryLayout.toGuiSlotIndex(slotIndex);
            if (guiSlotIndex < 0 || guiSlotIndex >= playerInventory.getStorageContents().length) {
                continue;
            }

            ItemStack itemStack = itemStackResolver.resolve(entry);
            if (itemStack == null) {
                continue;
            }

            itemByGuiSlot.put(guiSlotIndex, itemStack);
        }

        for (int dbSlot = NormalInventoryLayout.DB_SLOT_START; dbSlot <= NormalInventoryLayout.DB_SLOT_END; dbSlot++) {
            int guiSlot = NormalInventoryLayout.toGuiSlotIndex(dbSlot);
            setStorageItemIfChanged(playerInventory, guiSlot, itemByGuiSlot.get(guiSlot));
        }
    }

    // ---------------------------------------------------------------
    // EQUIP_SLOT
    // ---------------------------------------------------------------

    /**
     * EQUIP_SLOT インベントリをプレイヤーの装備スロット（防具・メインハンド）へ反映します。
     * <p>
     * DB にエントリが存在する場合はエントリを優先して装備します。
     * エントリがない場合でも、スナップショットが保存されていれば復元します。
     */
    public void applyEquipSlotInventoryToGui(@NotNull AstPlayer astPlayer) {
        if (!astPlayer.getAccount().getMode().shouldReflectInventoryToGui()) {
            return;
        }

        var accountId = astPlayer.getAccount().getUuid();
        var bukkitPlayer = astPlayer.getBukkit();
        var inventories = inventoryRepository.findByAccountId(accountId);

        inventories.stream()
            .filter(this::isDefaultProfile)
            .filter(inv -> inv.getInventoryType() == InventoryType.EQUIP_SLOT)
            .findFirst()
            .ifPresent(inventory -> {
                var entries = inventoryRepository.findEntries(inventory.getInventoryId());
                if (inventory.getMetadataJson() != null && !inventory.getMetadataJson().isBlank()) {
                    ItemStack[] snapshot = snapshotCodec.decode(inventory.getMetadataJson());
                    if (snapshot != null) {
                        EquipSlotLayout.applySnapshot(bukkitPlayer, snapshot);
                    }
                } else if (!entries.isEmpty()) {
                    EquipSlotLayout.applyEntriesToPlayer(bukkitPlayer, entries, itemStackResolver::resolve);
                }
            });

        bukkitPlayer.updateInventory();
    }

    /**
     * プレイヤーの現在の装備スロット（防具・メインハンド）をスナップショットとして保存します。
     *
     * @param astPlayer 保存対象プレイヤー
     */
    public void saveEquipSlotSnapshot(@NotNull AstPlayer astPlayer) {
        var accountId = astPlayer.getAccount().getUuid();
        var inventory = ensureInventory(
            accountId,
            InventoryType.EQUIP_SLOT,
            EquipSlotLayout.SLOT_MAX,
            accountId
        );
        var snapshot = EquipSlotLayout.createSnapshot(astPlayer.getBukkit());
        var metadataJson = snapshotCodec.encode(snapshot);
        inventoryRepository.updateMetadata(inventory.getInventoryId(), metadataJson, accountId);
    }

    /**
     * 指定エントリの装備アイテムを EQUIP_SLOT の装備部位へ移動します。
     * <p>
     * エントリを EQUIP_SLOT インベントリの entry として登録し、Bukkit インベントリへも反映します。
     * 対象スロットに既存エントリがある場合は上書きします。
     *
     * @param astPlayer  対象プレイヤー
     * @param entry      移動元エントリ（EQUIPMENT インベントリのエントリ）
     * @param slotIndex  EQUIP_SLOT のスロット番号（1=メインハンド, 2=頭, 3=胴, 4=脚, 5=足）
     */
    public void equipToSlot(
        @NotNull AstPlayer astPlayer,
        @NotNull InventoryEntryModel entry,
        int slotIndex
    ) {
        if (!EquipSlotLayout.isManagedSlot(slotIndex)) {
            return;
        }

        var accountId = astPlayer.getAccount().getUuid();
        var equipInventory = ensureInventory(
            accountId,
            InventoryType.EQUIP_SLOT,
            EquipSlotLayout.SLOT_MAX,
            accountId
        );

        var existingEntries = getEntries(equipInventory.getInventoryId());
        existingEntries.stream()
            .filter(e -> e.getSlotIndex() != null && e.getSlotIndex() == slotIndex && !e.isDeleted())
            .findFirst()
            .ifPresent(existing -> inventoryRepository.updateEntry(
                existing.getInventoryEntryId(),
                new InventoryEntryDraft(
                    existing.getSlotIndex(),
                    entry.getItemCategory(),
                    entry.getItemId(),
                    entry.getInstanceType(),
                    entry.getInstanceId(),
                    entry.getQuantity(),
                    entry.getMetadataJson()
                ),
                accountId
            ));

        if (existingEntries.stream().noneMatch(e -> e.getSlotIndex() != null && e.getSlotIndex() == slotIndex && !e.isDeleted())) {
            addEntry(
                equipInventory.getInventoryId(),
                new InventoryEntryDraft(
                    slotIndex,
                    entry.getItemCategory(),
                    entry.getItemId(),
                    entry.getInstanceType(),
                    entry.getInstanceId(),
                    entry.getQuantity(),
                    entry.getMetadataJson()
                ),
                accountId
            );
        }

        EquipmentType.fromEquipSlotIndex(slotIndex)
            .applyTo(astPlayer.getBukkit().getInventory(), itemStackResolver.resolve(entry));
        astPlayer.getBukkit().updateInventory();
    }

    // ---------------------------------------------------------------
    // HOTBAR
    // ---------------------------------------------------------------

    /**
     * HOTBAR インベントリのスナップショットをプレイヤーのホットバー（スロット 0〜8）へ反映します。
     */
    public void applyHotbarInventoryToGui(@NotNull AstPlayer astPlayer) {
        if (!astPlayer.getAccount().getMode().shouldReflectInventoryToGui()) {
            return;
        }

        var accountId = astPlayer.getAccount().getUuid();
        var hotbarInventory = ensureInventory(accountId, InventoryType.HOTBAR, HotbarLayout.CAPACITY, accountId);
        cacheHotbarEntries(accountId, inventoryRepository.findEntries(hotbarInventory.getInventoryId()));
        renderHotbarInventory(astPlayer);
    }

    /**
     * プレイヤーの現在のホットバー（スロット 0〜8）をスナップショットとして保存します。
     *
     * @param astPlayer 保存対象プレイヤー
     */
    public void saveHotbarSnapshot(@NotNull AstPlayer astPlayer) {
        var accountId = astPlayer.getAccount().getUuid();
        var inventory = ensureInventory(
            accountId,
            InventoryType.HOTBAR,
            HotbarLayout.CAPACITY,
            accountId
        );
        cacheHotbarEntries(accountId, inventoryRepository.findEntries(inventory.getInventoryId()));
    }

    /**
     * ホットバースロットのクリックを処理します。
     * <p>
     * アイテム設定済みのスロットは HOTBAR から外して元のインベントリへ戻し、空スロットは選択状態にします。
     *
     * @param astPlayer 対象プレイヤー
     * @param hotbarSlotIndex HOTBAR の DB slot_index（1〜9）
     * @return 操作を処理できた場合 true
     */
    public boolean handleHotbarSlotClick(@NotNull AstPlayer astPlayer, int hotbarSlotIndex) {
        if (!HotbarLayout.isManagedSlot(hotbarSlotIndex)) {
            return false;
        }
        if (!astPlayer.getAccount().getMode().shouldReflectInventoryToGui()) {
            return false;
        }

        var accountId = astPlayer.getAccount().getUuid();
        InventoryEntryModel entry = getCachedHotbarEntry(accountId, hotbarSlotIndex);
        if (entry != null) {
            selectedHotbarSlots.remove(accountId);
            boolean returned = returnHotbarEntryToInventory(astPlayer, entry);
            renderHotbarInventory(astPlayer);
            return returned;
        }

        Integer currentSelected = selectedHotbarSlots.get(accountId);
        if (Integer.valueOf(hotbarSlotIndex).equals(currentSelected)) {
            selectedHotbarSlots.remove(accountId);
            renderHotbarInventory(astPlayer);
            return true;
        }

        selectedHotbarSlots.put(accountId, hotbarSlotIndex);
        astPlayer.getBukkit().getInventory().setHeldItemSlot(HotbarLayout.toBukkitSlot(hotbarSlotIndex));
        renderHotbarInventory(astPlayer);
        return true;
    }

    /**
     * 指定エントリのアイテムをホットバーの指定スロットへ移動し、Bukkit インベントリへ反映します。
     *
     * @param astPlayer      対象プレイヤー
     * @param entry          移動元エントリ
     * @param hotbarSlotIndex ホットバーの DB slot_index（1〜9）
     */
    public void moveToHotbar(
        @NotNull AstPlayer astPlayer,
        @NotNull InventoryEntryModel entry,
        int hotbarSlotIndex
    ) {
        if (!HotbarLayout.isManagedSlot(hotbarSlotIndex)) {
            return;
        }

        ItemStack itemStack = itemStackResolver.resolve(entry);
        if (itemStack == null) {
            return;
        }

        upsertHotbarEntry(astPlayer, entry, hotbarSlotIndex);
        renderHotbarInventory(astPlayer);
    }

    public boolean equipOrAssignClickedItem(
        @NotNull AstPlayer astPlayer,
        @NotNull ItemStack clickedItem,
        int sourceBukkitSlot
    ) {
        if (clickedItem.getType() == Material.AIR) {
            return false;
        }

        InventoryEntryModel sourceEntry = findDisplayedEntryAtBukkitSlot(astPlayer, sourceBukkitSlot);
        if (sourceEntry == null) {
            return false;
        }

        ItemStack sourceItem = itemStackResolver.resolve(sourceEntry);
        if (sourceItem == null || sourceItem.getType() == Material.AIR) {
            return false;
        }

        ItemCategory category = ItemCategory.fromApiValue(sourceEntry.getItemCategory());
        ItemModel model = resolveItemModel(sourceEntry);
        if (model == null) {
            return false;
        }

        if (category == ItemCategory.EQUIPMENT) {
            if (model.getEquipment() == null) {
                return false;
            }

            ItemEquipmentSlot itemSlot = model.getEquipment().getSlot();
            if (itemSlot == ItemEquipmentSlot.HEAD
                || itemSlot == ItemEquipmentSlot.CHEST
                || itemSlot == ItemEquipmentSlot.LEGS
                || itemSlot == ItemEquipmentSlot.FEET) {
                return equipArmorItem(astPlayer, sourceItem, sourceBukkitSlot, EquipmentType.fromItemEquipmentSlot(itemSlot));
            }
            if (itemSlot == ItemEquipmentSlot.ACCESSORY) {
                return equipAccessoryItem(astPlayer, sourceItem, sourceBukkitSlot);
            }
            if (itemSlot == ItemEquipmentSlot.WEAPON || itemSlot == ItemEquipmentSlot.TOOL) {
                return assignHotbarItem(astPlayer, sourceEntry, sourceBukkitSlot);
            }
            return false;
        }

        if (category == ItemCategory.BUNDLE || category == ItemCategory.CONSUMABLE) {
            return assignHotbarItem(astPlayer, sourceEntry, sourceBukkitSlot);
        }

        return false;
    }

    private boolean equipArmorItem(
        @NotNull AstPlayer astPlayer,
        @NotNull ItemStack clickedItem,
        int sourceBukkitSlot,
        @NotNull EquipmentType equipmentType
    ) {
        if (equipmentType == EquipmentType.UNSUPPORTED) {
            return false;
        }

        PlayerInventory inventory = astPlayer.getBukkit().getInventory();
        ItemStack previous = getEquipmentItem(inventory, equipmentType);
        equipmentType.applyTo(inventory, clickedItem.clone());
        inventory.setItem(sourceBukkitSlot, emptyToAir(previous));

        removeDisplayedEntryAtBukkitSlot(astPlayer, sourceBukkitSlot);
        saveDisplayedStorageSnapshotIfNormal(astPlayer);
        saveEquipSlotSnapshot(astPlayer);
        syncCurrentEquipmentState(astPlayer);
        astPlayer.getBukkit().updateInventory();
        return true;
    }

    private boolean equipAccessoryItem(
        @NotNull AstPlayer astPlayer,
        @NotNull ItemStack clickedItem,
        int sourceBukkitSlot
    ) {
        int accessorySlot = findAccessoryTargetSlot(astPlayer);
        if (!AccessorySlotLayout.isManagedSlot(accessorySlot)) {
            return false;
        }

        PlayerInventory inventory = astPlayer.getBukkit().getInventory();
        if (accessorySlot == AccessorySlotLayout.SLOT_OFF_HAND) {
            ItemStack previous = inventory.getItemInOffHand();
            inventory.setItemInOffHand(clickedItem.clone());
            inventory.setItem(sourceBukkitSlot, emptyToAir(previous));
            saveAccessorySlotSnapshot(astPlayer);
        } else {
            if (getAccessorySnapshotItem(astPlayer, accessorySlot) != null) {
                return false;
            }
            inventory.setItem(sourceBukkitSlot, new ItemStack(Material.AIR));
            updateAccessorySnapshotSlot(astPlayer, accessorySlot, clickedItem.clone());
        }

        removeDisplayedEntryAtBukkitSlot(astPlayer, sourceBukkitSlot);
        saveDisplayedStorageSnapshotIfNormal(astPlayer);
        syncCurrentEquipmentState(astPlayer);
        astPlayer.getBukkit().updateInventory();
        return true;
    }

    private boolean assignHotbarItem(
        @NotNull AstPlayer astPlayer,
        @NotNull InventoryEntryModel sourceEntry,
        int sourceBukkitSlot
    ) {
        var accountId = astPlayer.getAccount().getUuid();
        Integer selectedHotbarSlotIndex = selectedHotbarSlots.remove(accountId);
        int targetDbSlot = selectedHotbarSlotIndex != null && HotbarLayout.isManagedSlot(selectedHotbarSlotIndex)
            ? selectedHotbarSlotIndex
            : findNextHotbarSlot(accountId);
        if (!HotbarLayout.isManagedSlot(targetDbSlot)) {
            return false;
        }

        upsertHotbarEntry(astPlayer, sourceEntry, targetDbSlot);
        deleteDisplayedEntryAtBukkitSlot(astPlayer, sourceBukkitSlot);
        applyDisplayedInventoryToGui(astPlayer);
        renderHotbarInventory(astPlayer);
        return true;
    }

    private int findNextHotbarSlot(@NotNull UUID accountId) {
        Map<Integer, InventoryEntryModel> entries = hotbarEntryCache.computeIfAbsent(accountId, key -> new ConcurrentHashMap<>());
        for (int slot = HotbarLayout.DB_SLOT_START; slot <= HotbarLayout.DB_SLOT_END; slot++) {
            if (!entries.containsKey(slot)) {
                return slot;
            }
        }
        return -1;
    }

    /**
     * HOTBAR entry を指定スロットへ保存し、サービス内キャッシュも同時に更新します。
     *
     * @param astPlayer 対象プレイヤー
     * @param sourceEntry 登録する entry
     * @param targetDbSlot HOTBAR の DB slot_index（1〜9）
     */
    private void upsertHotbarEntry(
        @NotNull AstPlayer astPlayer,
        @NotNull InventoryEntryModel sourceEntry,
        int targetDbSlot
    ) {
        var accountId = astPlayer.getAccount().getUuid();
        var hotbarInventory = ensureInventory(accountId, InventoryType.HOTBAR, HotbarLayout.CAPACITY, accountId);
        Map<Integer, InventoryEntryModel> cachedEntries = hotbarEntryCache.computeIfAbsent(accountId, key -> new ConcurrentHashMap<>());
        InventoryEntryModel existing = cachedEntries.get(targetDbSlot);
        InventoryEntryDraft draft = copyEntryDraft(sourceEntry, targetDbSlot);
        InventoryEntryModel saved = existing != null
            ? inventoryRepository.updateEntry(existing.getInventoryEntryId(), draft, accountId)
            : addEntry(hotbarInventory.getInventoryId(), draft, accountId);
        cachedEntries.put(targetDbSlot, saved);
    }

    /**
     * HOTBAR の entry を元の種別のインベントリへ戻します。
     *
     * @param astPlayer 対象プレイヤー
     * @param hotbarEntry HOTBAR から戻す entry
     * @return 戻し先スロットを確保できた場合 true
     */
    private boolean returnHotbarEntryToInventory(
        @NotNull AstPlayer astPlayer,
        @NotNull InventoryEntryModel hotbarEntry
    ) {
        var accountId = astPlayer.getAccount().getUuid();
        InventoryType targetType = resolveTargetInventoryType(hotbarEntry);
        InventoryModel targetInventory = ensureInventory(accountId, targetType, resolveSlotCapacity(targetType), accountId);
        List<InventoryEntryModel> targetEntries = getEntries(targetInventory.getInventoryId());
        Set<Integer> usedSlots = NormalInventoryLayout.collectUsedSlots(targetEntries);
        Integer targetSlot = NormalInventoryLayout.findNextFreeSlot(usedSlots);
        if (targetSlot == null) {
            return false;
        }

        InventoryEntryModel reusable = targetEntries.stream()
            .filter(entry -> entry.getSlotIndex() == null && isSameEntryIdentity(entry, hotbarEntry))
            .findFirst()
            .orElse(null);
        if (reusable != null) {
            inventoryRepository.updateEntry(reusable.getInventoryEntryId(), copyEntryDraft(hotbarEntry, targetSlot), accountId);
        } else {
            addEntry(targetInventory.getInventoryId(), copyEntryDraft(hotbarEntry, targetSlot), accountId);
        }

        inventoryRepository.deleteEntry(hotbarEntry.getInventoryEntryId(), accountId);
        Map<Integer, InventoryEntryModel> cachedEntries = hotbarEntryCache.computeIfAbsent(accountId, key -> new ConcurrentHashMap<>());
        if (hotbarEntry.getSlotIndex() != null) {
            cachedEntries.remove(hotbarEntry.getSlotIndex());
        }
        if (getDisplayedInventoryType(accountId) == targetType) {
            applyDisplayedInventoryToGui(astPlayer);
        }
        return true;
    }

    /**
     * HOTBAR キャッシュから指定スロットの entry を取得します。
     *
     * @param accountId アカウントID
     * @param hotbarSlotIndex HOTBAR の DB slot_index（1〜9）
     * @return キャッシュ済み entry。未設定の場合は null
     */
    private @Nullable InventoryEntryModel getCachedHotbarEntry(@NotNull UUID accountId, int hotbarSlotIndex) {
        Map<Integer, InventoryEntryModel> entries = hotbarEntryCache.computeIfAbsent(accountId, key -> new ConcurrentHashMap<>());
        return entries.get(hotbarSlotIndex);
    }

    /**
     * HOTBAR entries をスロット番号で引ける形にキャッシュします。
     *
     * @param accountId アカウントID
     * @param entries API から取得した HOTBAR entries
     */
    private void cacheHotbarEntries(@NotNull UUID accountId, @NotNull List<InventoryEntryModel> entries) {
        Map<Integer, InventoryEntryModel> slottedEntries = new ConcurrentHashMap<>();
        for (InventoryEntryModel entry : entries) {
            Integer slotIndex = entry.getSlotIndex();
            if (slotIndex != null && HotbarLayout.isManagedSlot(slotIndex) && !entry.isDeleted()) {
                slottedEntries.put(slotIndex, entry);
            }
        }
        hotbarEntryCache.put(accountId, slottedEntries);
    }

    /**
     * HOTBAR キャッシュをプレイヤーのホットバーへ描画します。
     *
     * @param astPlayer 対象プレイヤー
     */
    private void renderHotbarInventory(@NotNull AstPlayer astPlayer) {
        var accountId = astPlayer.getAccount().getUuid();
        Map<Integer, InventoryEntryModel> entries = hotbarEntryCache.computeIfAbsent(accountId, key -> new ConcurrentHashMap<>());
        Integer selectedSlot = selectedHotbarSlots.get(accountId);
        PlayerInventory inventory = astPlayer.getBukkit().getInventory();
        for (int dbSlot = HotbarLayout.DB_SLOT_START; dbSlot <= HotbarLayout.DB_SLOT_END; dbSlot++) {
            InventoryEntryModel entry = entries.get(dbSlot);
            ItemStack itemStack = entry == null ? createHotbarDummyItem(dbSlot) : itemStackResolver.resolve(entry);
            if (itemStack == null || itemStack.getType() == Material.AIR) {
                itemStack = createHotbarDummyItem(dbSlot);
            }
            if (selectedSlot != null && selectedSlot == dbSlot) {
                itemStack = withSelectionGlow(itemStack);
            }
            setStorageItemIfChanged(inventory, HotbarLayout.toBukkitSlot(dbSlot), itemStack);
        }
        astPlayer.getBukkit().updateInventory();
    }

    /**
     * 未設定ホットバースロット用のダミー ItemStack を生成します。
     *
     * @param dbSlot HOTBAR の DB slot_index（1〜9）
     * @return 表示用ダミー ItemStack
     */
    private @NotNull ItemStack createHotbarDummyItem(int dbSlot) {
        ItemStack itemStack = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(ColorCodeUtil.GRAY + "ホットバースロット[" + dbSlot + "]"));
            meta.lore(List.of(Component.text(ColorCodeUtil.GRAY + "アイテム未選択")));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    /**
     * ItemStack に選択中を示す発光表示を付与します。
     *
     * @param itemStack 元の表示 ItemStack
     * @return 発光を付与した clone
     */
    private @NotNull ItemStack withSelectionGlow(@NotNull ItemStack itemStack) {
        ItemStack glowing = itemStack.clone();
        ItemMeta meta = glowing.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            glowing.setItemMeta(meta);
        }
        return glowing;
    }

    private int findAccessoryTargetSlot(@NotNull AstPlayer astPlayer) {
        ItemStack offhand = astPlayer.getBukkit().getInventory().getItemInOffHand();
        if (offhand.getType() == Material.AIR) {
            return AccessorySlotLayout.SLOT_OFF_HAND;
        }
        for (int slot = AccessorySlotLayout.SLOT_NECKLACE; slot <= AccessorySlotLayout.SLOT_RING; slot++) {
            if (getAccessorySnapshotItem(astPlayer, slot) == null) {
                return slot;
            }
        }
        return -1;
    }

    private @Nullable ItemStack getEquipmentItem(
        @NotNull PlayerInventory inventory,
        @NotNull EquipmentType equipmentType
    ) {
        return switch (equipmentType) {
            case MAIN_HAND -> inventory.getItemInMainHand();
            case HEAD -> inventory.getHelmet();
            case CHEST -> inventory.getChestplate();
            case LEGS -> inventory.getLeggings();
            case FEET -> inventory.getBoots();
            case OFF_HAND -> inventory.getItemInOffHand();
            case UNSUPPORTED -> null;
        };
    }

    private void removeDisplayedEntryAtBukkitSlot(@NotNull AstPlayer astPlayer, int sourceBukkitSlot) {
        if (!NormalInventoryLayout.isManagedGuiSlot(sourceBukkitSlot)) {
            return;
        }

        UUID accountId = astPlayer.getAccount().getUuid();
        InventoryType displayedType = getDisplayedInventoryType(accountId);
        int dbSlot = NormalInventoryLayout.toDbSlotIndex(sourceBukkitSlot);

        inventoryRepository.findByAccountId(accountId).stream()
            .filter(this::isDefaultProfile)
            .filter(inventory -> inventory.getInventoryType() == displayedType)
            .findFirst()
            .ifPresent(inventory -> inventoryRepository.findEntries(inventory.getInventoryId()).stream()
                .filter(entry -> entry.getSlotIndex() != null && entry.getSlotIndex() == dbSlot && !entry.isDeleted())
                .findFirst()
                .ifPresent(entry -> inventoryRepository.updateEntry(
                    entry.getInventoryEntryId(),
                    new InventoryEntryDraft(
                        null,
                        entry.getItemCategory(),
                        entry.getItemId(),
                        entry.getInstanceType(),
                        entry.getInstanceId(),
                        entry.getQuantity(),
                        entry.getMetadataJson()
                    ),
                    accountId
                )));
    }

    /**
     * 現在表示中のインベントリから、指定 Bukkit スロットに対応する entry を取得します。
     *
     * @param astPlayer 対象プレイヤー
     * @param sourceBukkitSlot Bukkit storage slot
     * @return 表示中の entry。未設定の場合は null
     */
    private @Nullable InventoryEntryModel findDisplayedEntryAtBukkitSlot(
        @NotNull AstPlayer astPlayer,
        int sourceBukkitSlot
    ) {
        if (!NormalInventoryLayout.isManagedGuiSlot(sourceBukkitSlot)) {
            return null;
        }

        UUID accountId = astPlayer.getAccount().getUuid();
        InventoryType displayedType = getDisplayedInventoryType(accountId);
        int dbSlot = NormalInventoryLayout.toDbSlotIndex(sourceBukkitSlot);

        return inventoryRepository.findByAccountId(accountId).stream()
            .filter(this::isDefaultProfile)
            .filter(inventory -> inventory.getInventoryType() == displayedType)
            .findFirst()
            .flatMap(inventory -> inventoryRepository.findEntries(inventory.getInventoryId()).stream()
                .filter(entry -> entry.getSlotIndex() != null && entry.getSlotIndex() == dbSlot && !entry.isDeleted())
                .findFirst())
            .orElse(null);
    }

    /**
     * 現在表示中のインベントリから、指定 Bukkit スロットに対応する entry を削除します。
     *
     * @param astPlayer 対象プレイヤー
     * @param sourceBukkitSlot Bukkit storage slot
     */
    private void deleteDisplayedEntryAtBukkitSlot(@NotNull AstPlayer astPlayer, int sourceBukkitSlot) {
        InventoryEntryModel entry = findDisplayedEntryAtBukkitSlot(astPlayer, sourceBukkitSlot);
        if (entry != null) {
            inventoryRepository.deleteEntry(entry.getInventoryEntryId(), astPlayer.getAccount().getUuid());
        }
    }

    /**
     * 現在選択されている表示インベントリを Bukkit storage へ再描画します。
     *
     * @param astPlayer 対象プレイヤー
     */
    private void applyDisplayedInventoryToGui(@NotNull AstPlayer astPlayer) {
        UUID accountId = astPlayer.getAccount().getUuid();
        InventoryType displayedType = getDisplayedInventoryType(accountId);
        InventoryModel inventory = inventoryRepository.findByAccountId(accountId).stream()
            .filter(this::isDefaultProfile)
            .filter(inv -> inv.getInventoryType() == displayedType)
            .findFirst()
            .orElse(null);
        applyInventoryToGui(astPlayer.getBukkit(), inventory);
    }

    private void saveDisplayedStorageSnapshotIfNormal(@NotNull AstPlayer astPlayer) {
        // NORMAL inventory は API entry を表示するだけのため、Bukkit ItemStack の JSON 保存は行いません。
    }

    private void updateAccessorySnapshotSlot(
        @NotNull AstPlayer astPlayer,
        int accessorySlot,
        @Nullable ItemStack itemStack
    ) {
        if (!AccessorySlotLayout.isManagedSlot(accessorySlot)) {
            return;
        }
        int loadoutSlotIndex = toAccessoryLoadoutSlotIndex(accessorySlot);
        if (loadoutSlotIndex < 0) {
            return;
        }
        UUID accountId = astPlayer.getAccount().getUuid();
        EquipmentLoadoutModel loadout = ensureActiveEquipmentLoadout(accountId);
        if (loadout == null) {
            return;
        }
        syncLoadoutSlot(loadout, SLOT_TYPE_ACCESSORY, loadoutSlotIndex, itemStack, accountId);
    }

    public void syncCurrentEquipmentState(@NotNull AstPlayer astPlayer) {
        PlayerInventory inventory = astPlayer.getBukkit().getInventory();
        syncActiveEquipmentLoadout(
            astPlayer.getAccount().getUuid(),
            inventory.getHelmet(),
            inventory.getChestplate(),
            inventory.getLeggings(),
            inventory.getBoots(),
            inventory.getItemInOffHand(),
            getAccessorySnapshotItem(astPlayer, AccessorySlotLayout.SLOT_NECKLACE),
            getAccessorySnapshotItem(astPlayer, AccessorySlotLayout.SLOT_RING),
            getAccessorySnapshotItem(astPlayer, AccessorySlotLayout.SLOT_EARRING),
            getAccessorySnapshotItem(astPlayer, AccessorySlotLayout.SLOT_BRACELET),
            getAccessorySnapshotItem(astPlayer, AccessorySlotLayout.SLOT_BELT),
            getAccessorySnapshotItem(astPlayer, AccessorySlotLayout.SLOT_CHARM)
        );
    }

    private @NotNull ItemStack emptyToAir(@Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return new ItemStack(Material.AIR);
        }
        return itemStack;
    }

    // ---------------------------------------------------------------
    // ACCESSORY_SLOT
    // ---------------------------------------------------------------

    /**
     * ACCESSORY_SLOT インベントリをプレイヤーのアクセサリスロットへ反映します。
     * <p>
     * slot_index 1 はオフハンドへ反映します。2 以降は DB 管理のみです。
     */
    public void applyAccessorySlotInventoryToGui(@NotNull AstPlayer astPlayer) {
        if (!astPlayer.getAccount().getMode().shouldReflectInventoryToGui()) {
            return;
        }

        var bukkitPlayer = astPlayer.getBukkit();
        ItemStack[] snapshot = new ItemStack[AccessorySlotLayout.SLOT_MAX + 1];
        snapshot[AccessorySlotLayout.SLOT_OFF_HAND] = itemOrAir(getAccessorySnapshotItem(astPlayer, AccessorySlotLayout.SLOT_OFF_HAND));
        snapshot[AccessorySlotLayout.SLOT_NECKLACE] = itemOrAir(getAccessorySnapshotItem(astPlayer, AccessorySlotLayout.SLOT_NECKLACE));
        snapshot[AccessorySlotLayout.SLOT_RING] = itemOrAir(getAccessorySnapshotItem(astPlayer, AccessorySlotLayout.SLOT_RING));
        snapshot[AccessorySlotLayout.SLOT_EARRING] = itemOrAir(getAccessorySnapshotItem(astPlayer, AccessorySlotLayout.SLOT_EARRING));
        snapshot[AccessorySlotLayout.SLOT_BRACELET] = itemOrAir(getAccessorySnapshotItem(astPlayer, AccessorySlotLayout.SLOT_BRACELET));
        snapshot[AccessorySlotLayout.SLOT_BELT] = itemOrAir(getAccessorySnapshotItem(astPlayer, AccessorySlotLayout.SLOT_BELT));
        snapshot[AccessorySlotLayout.SLOT_CHARM] = itemOrAir(getAccessorySnapshotItem(astPlayer, AccessorySlotLayout.SLOT_CHARM));
        AccessorySlotLayout.applySnapshot(bukkitPlayer, snapshot);

        bukkitPlayer.updateInventory();
    }

    /**
     * プレイヤーの現在のアクセサリスロット（オフハンド）をスナップショットとして保存します。
     *
     * @param astPlayer 保存対象プレイヤー
     */
    public void saveAccessorySlotSnapshot(@NotNull AstPlayer astPlayer) {
        syncCurrentEquipmentState(astPlayer);
    }

    /**
     * 装備 GUI の内容を Bukkit 装備欄と DB スナップショットへ反映します。
     *
     * @param astPlayer 保存対象プレイヤー
     * @param head 頭装備
     * @param chest 胴装備
     * @param legs 脚装備
     * @param feet 足装備
     * @param offHand オフハンド装備
     * @param accessory2 拡張アクセサリ1
     * @param accessory3 拡張アクセサリ2
     */
    public void saveEquipmentGui(
        @NotNull AstPlayer astPlayer,
        @Nullable ItemStack head,
        @Nullable ItemStack chest,
        @Nullable ItemStack legs,
        @Nullable ItemStack feet,
        @Nullable ItemStack offHand,
        @Nullable ItemStack accessory2,
        @Nullable ItemStack accessory3,
        @Nullable ItemStack accessory4,
        @Nullable ItemStack accessory5,
        @Nullable ItemStack accessory6,
        @Nullable ItemStack accessory7
    ) {
        var bukkitInventory = astPlayer.getBukkit().getInventory();
        bukkitInventory.setHelmet(itemOrAir(head));
        bukkitInventory.setChestplate(itemOrAir(chest));
        bukkitInventory.setLeggings(itemOrAir(legs));
        bukkitInventory.setBoots(itemOrAir(feet));
        bukkitInventory.setItemInOffHand(itemOrAir(offHand));

        var accountId = astPlayer.getAccount().getUuid();

        ItemStack[] equipSnapshot = new ItemStack[EquipSlotLayout.SLOT_MAX + 1];
        equipSnapshot[EquipSlotLayout.SLOT_HEAD] = itemOrAir(head);
        equipSnapshot[EquipSlotLayout.SLOT_CHEST] = itemOrAir(chest);
        equipSnapshot[EquipSlotLayout.SLOT_LEGS] = itemOrAir(legs);
        equipSnapshot[EquipSlotLayout.SLOT_FEET] = itemOrAir(feet);
        var equipInventory = ensureInventory(accountId, InventoryType.EQUIP_SLOT, EquipSlotLayout.SLOT_MAX, accountId);
        inventoryRepository.updateMetadata(equipInventory.getInventoryId(), snapshotCodec.encode(equipSnapshot), accountId);

        syncActiveEquipmentLoadout(
            accountId,
            head,
            chest,
            legs,
            feet,
            offHand,
            accessory2,
            accessory3,
            accessory4,
            accessory5,
            accessory6,
            accessory7
        );

        astPlayer.getBukkit().updateInventory();
    }

    private boolean applyActiveEquipmentLoadoutToGui(@NotNull AstPlayer astPlayer) {
        if (!astPlayer.getAccount().getMode().shouldReflectInventoryToGui()) {
            return false;
        }

        EquipmentLoadoutModel loadout = getActiveEquipmentLoadout(astPlayer.getAccount().getUuid());
        if (loadout == null || loadout.getSlots().isEmpty()) {
            return false;
        }

        PlayerInventory inventory = astPlayer.getBukkit().getInventory();
        inventory.setHelmet(new ItemStack(Material.AIR));
        inventory.setChestplate(new ItemStack(Material.AIR));
        inventory.setLeggings(new ItemStack(Material.AIR));
        inventory.setBoots(new ItemStack(Material.AIR));
        inventory.setItemInOffHand(new ItemStack(Material.AIR));

        for (EquipmentLoadoutSlotModel slot : loadout.getSlots()) {
            ItemStack itemStack = itemStackResolver.resolve(toInventoryEntry(slot));
            if (itemStack == null) {
                continue;
            }
            applyLoadoutSlot(inventory, slot.getSlotType(), slot.getSlotIndex(), itemStack);
        }

        astPlayer.getBukkit().updateInventory();
        return true;
    }

    private void syncActiveEquipmentLoadout(
        @NotNull UUID accountId,
        @Nullable ItemStack head,
        @Nullable ItemStack chest,
        @Nullable ItemStack legs,
        @Nullable ItemStack feet,
        @Nullable ItemStack offHand,
        @Nullable ItemStack accessory2,
        @Nullable ItemStack accessory3,
        @Nullable ItemStack accessory4,
        @Nullable ItemStack accessory5,
        @Nullable ItemStack accessory6,
        @Nullable ItemStack accessory7
    ) {
        EquipmentLoadoutModel loadout = ensureActiveEquipmentLoadout(accountId);
        if (loadout == null) {
            return;
        }
        syncLoadoutSlot(loadout, SLOT_TYPE_HEAD, 0, head, accountId);
        syncLoadoutSlot(loadout, SLOT_TYPE_CHEST, 0, chest, accountId);
        syncLoadoutSlot(loadout, SLOT_TYPE_LEGS, 0, legs, accountId);
        syncLoadoutSlot(loadout, SLOT_TYPE_FEET, 0, feet, accountId);
        syncLoadoutSlot(loadout, SLOT_TYPE_ACCESSORY, 0, offHand, accountId);
        syncLoadoutSlot(loadout, SLOT_TYPE_ACCESSORY, 1, accessory2, accountId);
        syncLoadoutSlot(loadout, SLOT_TYPE_ACCESSORY, 2, accessory3, accountId);
        syncLoadoutSlot(loadout, SLOT_TYPE_ACCESSORY, 3, accessory4, accountId);
        syncLoadoutSlot(loadout, SLOT_TYPE_ACCESSORY, 4, accessory5, accountId);
        syncLoadoutSlot(loadout, SLOT_TYPE_ACCESSORY, 5, accessory6, accountId);
        syncLoadoutSlot(loadout, SLOT_TYPE_ACCESSORY, 6, accessory7, accountId);
    }

    private void syncLoadoutSlot(
        @NotNull EquipmentLoadoutModel loadout,
        @NotNull String slotType,
        int slotIndex,
        @Nullable ItemStack itemStack,
        @NotNull UUID updatedBy
    ) {
        UUID equipmentInstanceId = readEquipmentInstanceId(itemStack);
        try {
            if (equipmentInstanceId == null) {
                equipmentLoadoutRepository.deleteSlot(loadout.getEquipmentLoadoutId(), slotType, slotIndex, updatedBy);
                return;
            }

            equipmentLoadoutRepository.upsertSlot(
                loadout.getEquipmentLoadoutId(),
                slotType,
                slotIndex,
                equipmentInstanceId,
                updatedBy
            );
        } catch (RuntimeException e) {
            Logger.warn(LogId.W_5254, loadout.getEquipmentLoadoutId(), slotType, slotIndex, e.getMessage());
        }
    }

    private @Nullable UUID readEquipmentInstanceId(@Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return null;
        }
        String instanceId = ItemStackFactory.getEquipmentInstanceId(itemStack);
        if (instanceId == null || instanceId.isBlank()) {
            return null;
        }
        return parseUuidOrNull(instanceId);
    }

    private InventoryEntryModel toInventoryEntry(@NotNull EquipmentLoadoutSlotModel slot) {
        LocalDateTime now = LocalDateTime.now();
        return new InventoryEntryModel(
            slot.getEquipmentLoadoutSlotId(),
            slot.getEquipmentLoadoutId(),
            slot.getSlotIndex(),
            ItemCategory.EQUIPMENT.name(),
            null,
            InventoryInstanceType.EQUIPMENT.getCode(),
            slot.getEquipmentInstanceId(),
            1L,
            null,
            now,
            now,
            slot.getCreatedBy(),
            slot.getUpdatedBy(),
            false
        );
    }

    private void applyLoadoutSlot(
        @NotNull PlayerInventory inventory,
        @NotNull String slotType,
        int slotIndex,
        @NotNull ItemStack itemStack
    ) {
        switch (slotType.toUpperCase(java.util.Locale.ROOT)) {
            case "WEAPON" -> {
            }
            case SLOT_TYPE_HEAD -> inventory.setHelmet(itemStack);
            case SLOT_TYPE_CHEST -> inventory.setChestplate(itemStack);
            case SLOT_TYPE_LEGS -> inventory.setLeggings(itemStack);
            case SLOT_TYPE_FEET -> inventory.setBoots(itemStack);
            case SLOT_TYPE_ACCESSORY -> {
                if (slotIndex == 0) {
                    inventory.setItemInOffHand(itemStack);
                }
            }
            default -> {
            }
        }
    }

    /**
     * 指定アイテムが装備 GUI の対象スロットへ配置できるか判定します。
     *
     * @param itemStack 判定対象アイテム
     * @param equipmentType Bukkit 装備欄に対応するスロット種別
     * @param extendedAccessory 拡張アクセサリスロットか
     * @return 配置可能なら true
     */
    public boolean canPlaceInEquipmentGuiSlot(
        @Nullable ItemStack itemStack,
        @Nullable EquipmentType equipmentType,
        boolean extendedAccessory
    ) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return true;
        }
        String itemId = ItemStackFactory.getAstralItemId(itemStack);
        String category = ItemStackFactory.getCategory(itemStack);
        if (itemId == null || category == null || ItemCategory.fromApiValue(category) != ItemCategory.EQUIPMENT) {
            return false;
        }
        ItemModel model = resolveItemModel(itemId);
        if (model == null || model.getEquipment() == null) {
            return false;
        }

        ItemEquipmentSlot itemSlot = model.getEquipment().getSlot();
        if (extendedAccessory) {
            return itemSlot == ItemEquipmentSlot.ACCESSORY;
        }
        return EquipmentType.fromItemEquipmentSlot(itemSlot) == equipmentType;
    }

    public @NotNull EquipmentType getEquipmentTypeForItem(@Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return EquipmentType.UNSUPPORTED;
        }
        String itemId = ItemStackFactory.getAstralItemId(itemStack);
        String category = ItemStackFactory.getCategory(itemStack);
        if (itemId == null || category == null || ItemCategory.fromApiValue(category) != ItemCategory.EQUIPMENT) {
            return EquipmentType.UNSUPPORTED;
        }
        ItemModel model = resolveItemModel(itemId);
        if (model == null || model.getEquipment() == null) {
            return EquipmentType.UNSUPPORTED;
        }
        return EquipmentType.fromItemEquipmentSlot(model.getEquipment().getSlot());
    }

    /**
     * 保存済みアクセサリスナップショットから指定スロットのアイテムを取得します。
     *
     * @param astPlayer 対象プレイヤー
     * @param slotIndex ACCESSORY_SLOT の slot_index
     * @return 保存済みアイテム。未保存または空の場合は null
     */
    public @Nullable ItemStack getAccessorySnapshotItem(@NotNull AstPlayer astPlayer, int slotIndex) {
        if (!AccessorySlotLayout.isManagedSlot(slotIndex)) {
            return null;
        }
        int loadoutSlotIndex = toAccessoryLoadoutSlotIndex(slotIndex);
        if (loadoutSlotIndex < 0) {
            return null;
        }
        EquipmentLoadoutModel loadout = getActiveEquipmentLoadout(astPlayer.getAccount().getUuid());
        if (loadout == null || loadout.getSlots().isEmpty()) {
            return null;
        }
        return loadout.getSlots().stream()
            .filter(slot -> SLOT_TYPE_ACCESSORY.equals(slot.getSlotType()))
            .filter(slot -> slot.getSlotIndex() == loadoutSlotIndex)
            .filter(slot -> slot.getEquipmentInstanceId() != null)
            .map(this::toInventoryEntry)
            .map(itemStackResolver::resolve)
            .filter(item -> item != null)
            .filter(item -> item.getType() != Material.AIR)
            .findFirst()
            .orElse(null);
    }

    /**
     * 指定エントリのアクセサリアイテムをアクセサリスロットへ移動します。
     * <p>
     * エントリを ACCESSORY_SLOT インベントリへ登録し、Bukkit インベントリへも反映します。
     *
     * @param astPlayer     対象プレイヤー
     * @param entry         移動元エントリ（EQUIPMENT インベントリのエントリ）
     * @param slotIndex     ACCESSORY_SLOT のスロット番号（1=オフハンド, 2〜3=拡張）
     */
    public void moveToAccessorySlot(
        @NotNull AstPlayer astPlayer,
        @NotNull InventoryEntryModel entry,
        int slotIndex
    ) {
        if (!AccessorySlotLayout.isManagedSlot(slotIndex)) {
            return;
        }

        var accountId = astPlayer.getAccount().getUuid();
        int loadoutSlotIndex = toAccessoryLoadoutSlotIndex(slotIndex);
        EquipmentLoadoutModel loadout = ensureActiveEquipmentLoadout(accountId);
        if (loadout != null && loadoutSlotIndex >= 0) {
            ItemStack itemStack = itemStackResolver.resolve(entry);
            syncLoadoutSlot(loadout, SLOT_TYPE_ACCESSORY, loadoutSlotIndex, itemStack, accountId);
        }

        EquipmentType.fromAccessorySlotIndex(slotIndex)
            .applyTo(astPlayer.getBukkit().getInventory(), itemStackResolver.resolve(entry));
        astPlayer.getBukkit().updateInventory();
    }

    private int toAccessoryLoadoutSlotIndex(int accessorySlot) {
        if (accessorySlot == AccessorySlotLayout.SLOT_OFF_HAND) {
            return 0;
        }
        if (accessorySlot == AccessorySlotLayout.SLOT_NECKLACE) {
            return 1;
        }
        if (accessorySlot == AccessorySlotLayout.SLOT_RING) {
            return 2;
        }
        if (accessorySlot == AccessorySlotLayout.SLOT_EARRING) {
            return 3;
        }
        if (accessorySlot == AccessorySlotLayout.SLOT_BRACELET) {
            return 4;
        }
        if (accessorySlot == AccessorySlotLayout.SLOT_BELT) {
            return 5;
        }
        if (accessorySlot == AccessorySlotLayout.SLOT_CHARM) {
            return 6;
        }
        return -1;
    }

    // ---------------------------------------------------------------

    /**
     * 通常インベントリの管理対象スロットだけをスナップショットとして保存します。
     * <p>
     * ホットバー、装備欄、オフハンドは通常インベントリの表示対象外として保存しません。
     *
     * @param astPlayer 保存対象プレイヤー
     */
    public void saveNormalInventorySnapshot(@NotNull AstPlayer astPlayer) {
        // NORMAL inventory は API entry を表示するだけのため、Bukkit ItemStack の JSON 保存は行いません。
    }

    private @NotNull ItemStack itemOrAir(@Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return new ItemStack(Material.AIR);
        }
        return itemStack;
    }

    private ItemModel resolveItemModel(String itemId) {
        ItemModel loaded = itemService.findLoadedById(itemId);
        if (loaded != null) {
            return loaded;
        }
        return itemService.loadItem(itemId);
    }

    /**
     * Inventory entry から対応する ItemModel を解決します。
     *
     * @param entry 解決対象 entry
     * @return 対応する ItemModel。解決できない場合は null
     */
    private @Nullable ItemModel resolveItemModel(@NotNull InventoryEntryModel entry) {
        if (entry.getItemId() != null && !entry.getItemId().isBlank()) {
            return resolveItemModel(entry.getItemId());
        }
        InventoryInstanceType instanceType = InventoryInstanceType.fromCode(entry.getInstanceType());
        if (instanceType == null || entry.getInstanceId() == null) {
            return null;
        }
        return switch (instanceType) {
            case EQUIPMENT -> {
                EquipmentInstance instance = itemService.findEquipmentInstanceById(entry.getInstanceId().toString());
                yield instance == null ? null : resolveItemModel(instance.getItemId());
            }
            case RUNE -> {
                RuneInstance instance = itemService.findRuneInstanceById(entry.getInstanceId().toString());
                yield instance == null ? null : resolveItemModel(instance.getItemId());
            }
        };
    }

    /**
     * entry のアイテム情報を指定スロットへコピーする draft を生成します。
     *
     * @param entry コピー元 entry
     * @param slotIndex 保存先 slot_index
     * @return API 更新用 draft
     */
    private @NotNull InventoryEntryDraft copyEntryDraft(@NotNull InventoryEntryModel entry, @Nullable Integer slotIndex) {
        return new InventoryEntryDraft(
            slotIndex,
            entry.getItemCategory(),
            entry.getItemId(),
            entry.getInstanceType(),
            entry.getInstanceId(),
            entry.getQuantity(),
            entry.getMetadataJson()
        );
    }

    /**
     * entry が同じアイテム実体を指しているか判定します。
     *
     * @param left 比較元
     * @param right 比較先
     * @return 同一アイテムとみなせる場合 true
     */
    private boolean isSameEntryIdentity(@NotNull InventoryEntryModel left, @NotNull InventoryEntryModel right) {
        return left.getItemCategory().equalsIgnoreCase(right.getItemCategory())
            && java.util.Objects.equals(left.getItemId(), right.getItemId())
            && java.util.Objects.equals(left.getInstanceType(), right.getInstanceType())
            && java.util.Objects.equals(left.getInstanceId(), right.getInstanceId());
    }

    /**
     * 指定アイテムをインスタンスアイテムとして通常インベントリへ追加します。
     */
    private int addInstanceItems(
        @NotNull InventoryModel inventory,
        @NotNull ItemModel model,
        int amount,
        @NotNull InventoryInstanceType instanceType,
        @NotNull Set<Integer> usedSlots,
        @NotNull UUID accountId
    ) {
        int granted = 0;
        for (int i = 0; i < amount; i++) {
            Integer slot = NormalInventoryLayout.findNextFreeSlot(usedSlots);
            if (slot == null) {
                break;
            }

            UUID instanceId = createInstanceId(model, accountId, instanceType);
            if (instanceId == null) {
                break;
            }

            addEntry(
                inventory.getInventoryId(),
                new InventoryEntryDraft(slot, model.getCategory(), null, instanceType.getCode(), instanceId, 1L, null),
                accountId
            );
            usedSlots.add(slot);
            granted++;
        }
        return granted;
    }

    /**
     * 指定アイテムをスタック可能アイテムとして通常インベントリへ追加します。
     */
    private int addStackedItems(
        @NotNull InventoryModel inventory,
        @NotNull ItemModel model,
        int amount,
        @NotNull Set<Integer> usedSlots,
        @NotNull UUID accountId
    ) {
        int granted = 0;
        int remaining = amount;
        int maxStack = Math.max(1, model.getMaxStack());
        List<InventoryEntryModel> entries = getEntries(inventory.getInventoryId());

        for (InventoryEntryModel entry : entries) {
            if (remaining <= 0) {
                break;
            }
            if (!isStackableEntry(entry, model, maxStack)) {
                continue;
            }

            long room = maxStack - entry.getQuantity();
            if (room <= 0) {
                continue;
            }

            int addAmount = (int) Math.min(room, remaining);
            inventoryRepository.updateEntry(
                entry.getInventoryEntryId(),
                new InventoryEntryDraft(
                    entry.getSlotIndex(),
                    entry.getItemCategory(),
                    entry.getItemId(),
                    entry.getInstanceType(),
                    entry.getInstanceId(),
                    entry.getQuantity() + addAmount,
                    entry.getMetadataJson()
                ),
                accountId
            );
            granted += addAmount;
            remaining -= addAmount;
        }

        while (remaining > 0) {
            Integer slot = NormalInventoryLayout.findNextFreeSlot(usedSlots);
            if (slot == null) {
                break;
            }

            int stackAmount = Math.min(maxStack, remaining);
            addEntry(
                inventory.getInventoryId(),
                new InventoryEntryDraft(slot, model.getCategory(), model.getId(), null, null, (long) stackAmount, null),
                accountId
            );
            usedSlots.add(slot);
            granted += stackAmount;
            remaining -= stackAmount;
        }
        return granted;
    }

    private boolean isStackableEntry(
        @NotNull InventoryEntryModel entry,
        @NotNull ItemModel model,
        int maxStack
    ) {
        if (maxStack <= 1) {
            return false;
        }
        if (entry.getItemId() == null || !entry.getItemId().equals(model.getId())) {
            return false;
        }
        if (!entry.getItemCategory().equalsIgnoreCase(model.getCategory())) {
            return false;
        }
        if (entry.getInstanceType() != null || entry.getInstanceId() != null) {
            return false;
        }
        return entry.getQuantity() < maxStack;
    }

    /**
     * インスタンスタイプに応じて API 側の実体を作成し、UUID を返します。
     */
    private @Nullable UUID createInstanceId(
        @NotNull ItemModel model,
        @NotNull UUID accountId,
        @NotNull InventoryInstanceType instanceType
    ) {
        String instanceId = switch (instanceType) {
            case EQUIPMENT -> createEquipmentInstanceId(model, accountId);
            case RUNE -> createRuneInstanceId(model, accountId);
        };
        return instanceId == null ? null : parseUuidOrNull(instanceId);
    }

    /**
     * 装備インスタンスを生成し、生成されたインスタンスIDを返します。
     */
    private @Nullable String createEquipmentInstanceId(@NotNull ItemModel model, @NotNull UUID accountId) {
        EquipmentInstance instance = itemService.createEquipmentInstance(
            model.getId(),
            accountId.toString(),
            "command",
            accountId.toString()
        );
        return instance == null ? null : instance.getEquipmentInstanceId();
    }

    /**
     * ルーンインスタンスを生成し、生成されたインスタンスIDを返します。
     */
    private @Nullable String createRuneInstanceId(@NotNull ItemModel model, @NotNull UUID accountId) {
        RuneInstance instance = itemService.createRuneInstance(
            model.getId(),
            accountId.toString(),
            "command",
            accountId.toString()
        );
        return instance == null ? null : instance.getRuneInstanceId();
    }

    /**
     * UUID文字列をUUIDへ変換します。変換不能な場合は null を返します。
     */
    private UUID parseUuidOrNull(@NotNull String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
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

    /**
     * 通常表示領域（Bukkit storage slot 9〜35）だけを空にします。
     *
     * @param bukkitPlayer 対象プレイヤー
     */
    private void clearManagedStorageSlots(@NotNull Player bukkitPlayer) {
        PlayerInventory inventory = bukkitPlayer.getInventory();
        for (int dbSlot = NormalInventoryLayout.DB_SLOT_START; dbSlot <= NormalInventoryLayout.DB_SLOT_END; dbSlot++) {
            setStorageItemIfChanged(inventory, NormalInventoryLayout.toGuiSlotIndex(dbSlot), null);
        }
    }

    /**
     * 同等の ItemStack が既に入っている場合は Bukkit インベントリを書き換えず、不要な再配置表示を抑制します。
     *
     * @param inventory 対象インベントリ
     * @param bukkitSlot Bukkit storage slot
     * @param itemStack 設定したい ItemStack
     */
    private void setStorageItemIfChanged(
        @NotNull PlayerInventory inventory,
        int bukkitSlot,
        @Nullable ItemStack itemStack
    ) {
        ItemStack next = itemOrAir(itemStack);
        ItemStack current = inventory.getItem(bukkitSlot);
        if (isSameItemStack(current, next)) {
            return;
        }
        inventory.setItem(bukkitSlot, next);
    }

    /**
     * 表示上同一と扱える ItemStack か判定します。
     *
     * @param current 現在の ItemStack
     * @param next 反映予定の ItemStack
     * @return 同一表示であれば true
     */
    private boolean isSameItemStack(@Nullable ItemStack current, @Nullable ItemStack next) {
        boolean currentEmpty = current == null || current.getType() == Material.AIR;
        boolean nextEmpty = next == null || next.getType() == Material.AIR;
        if (currentEmpty || nextEmpty) {
            return currentEmpty == nextEmpty;
        }
        return current.getAmount() == next.getAmount() && current.isSimilar(next);
    }

    private boolean isDefaultProfile(@NotNull InventoryModel inventory) {
        return DEFAULT_PROFILE.getCode().equalsIgnoreCase(inventory.getInventoryProfile());
    }

    private @NotNull InventoryType resolveTargetInventoryType(@NotNull ItemModel model) {
        return switch (ItemCategory.fromApiValue(model.getCategory())) {
            case EQUIPMENT -> InventoryType.EQUIPMENT;
            case RUNE -> InventoryType.RUNE;
            case CURRENCY -> InventoryType.CURRENCY;
            default -> InventoryType.NORMAL;
        };
    }

    /**
     * Inventory entry のカテゴリから戻し先インベントリ種別を解決します。
     *
     * @param entry 対象 entry
     * @return 戻し先の InventoryType
     */
    private @NotNull InventoryType resolveTargetInventoryType(@NotNull InventoryEntryModel entry) {
        return switch (ItemCategory.fromApiValue(entry.getItemCategory())) {
            case EQUIPMENT -> InventoryType.EQUIPMENT;
            case RUNE -> InventoryType.RUNE;
            case CURRENCY -> InventoryType.CURRENCY;
            default -> InventoryType.NORMAL;
        };
    }

    private @Nullable Integer resolveSlotCapacity(@NotNull InventoryType inventoryType) {
        return inventoryType.isSlotted() ? NormalInventoryLayout.CAPACITY : null;
    }
}
