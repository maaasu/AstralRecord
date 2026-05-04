package io.github.maaasu.astralRecord.feature.inventory.service;

import io.github.maaasu.astralRecord.feature.inventory.model.InventoryEntryDraft;
import io.github.maaasu.astralRecord.feature.inventory.model.InventoryEntryModel;
import io.github.maaasu.astralRecord.feature.inventory.model.InventoryInstanceType;
import io.github.maaasu.astralRecord.feature.inventory.model.InventoryModel;
import io.github.maaasu.astralRecord.feature.inventory.model.InventoryProfile;
import io.github.maaasu.astralRecord.feature.inventory.model.InventoryType;
import io.github.maaasu.astralRecord.feature.inventory.repository.InventoryRepository;
import io.github.maaasu.astralRecord.feature.item.model.EquipmentInstance;
import io.github.maaasu.astralRecord.feature.item.model.ItemCategory;
import io.github.maaasu.astralRecord.feature.item.model.ItemModel;
import io.github.maaasu.astralRecord.feature.item.model.RuneInstance;
import io.github.maaasu.astralRecord.feature.item.service.ItemService;
import io.github.maaasu.astralRecord.feature.item.service.ItemStackFactory;
import io.github.maaasu.astralRecord.feature.player.model.AstPlayer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class InventoryService {
    private static final InventoryProfile DEFAULT_PROFILE = InventoryProfile.GAME;

    private final InventoryRepository inventoryRepository;
    private final ItemService itemService;
    private final InventoryItemStackResolver itemStackResolver;
    private final InventorySnapshotCodec snapshotCodec;

    public InventoryService(
        InventoryRepository inventoryRepository,
        ItemService itemService,
        ItemStackFactory itemStackFactory
    ) {
        this.inventoryRepository = inventoryRepository;
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
            ? NormalInventoryLayout.collectUsedSlots(astPlayer.getBukkit().getInventory().getStorageContents())
            : Set.of();
        if (targetInventory.getInventoryType().isSlotted()) {
            usedSlots = new java.util.HashSet<>(usedSlots);
            usedSlots.addAll(NormalInventoryLayout.collectUsedSlots(getEntries(targetInventory.getInventoryId())));
        }

        return switch (ItemCategory.fromApiValue(model.getCategory())) {
            case EQUIPMENT -> addInstanceItems(targetInventory, model, safeAmount, InventoryInstanceType.EQUIPMENT, usedSlots, accountId);
            case RUNE -> addInstanceItems(targetInventory, model, safeAmount, InventoryInstanceType.RUNE, usedSlots, accountId);
            default -> addStackedItems(targetInventory, model, safeAmount, usedSlots, accountId);
        };
    }

    public void applyInventoriesToGui(AstPlayer astPlayer) {
        if (!astPlayer.getAccount().getMode().shouldReflectInventoryToGui()) {
            return;
        }

        var bukkitPlayer = astPlayer.getBukkit();
        clearGuiInventory(bukkitPlayer);

        var inventories = inventoryRepository.findByAccountId(astPlayer.getAccount().getUuid());
        inventories.stream()
            .filter(this::isDefaultProfile)
            .forEach(inventory -> applyInventoryToGui(bukkitPlayer, inventory));

        bukkitPlayer.updateInventory();
    }

    public void applyInventoryToGui(AstPlayer astPlayer, InventoryType inventoryType) {
        if (!astPlayer.getAccount().getMode().shouldReflectInventoryToGui()) {
            return;
        }

        var bukkitPlayer = astPlayer.getBukkit();
        clearGuiInventory(bukkitPlayer);

        var inventories = inventoryRepository.findByAccountId(astPlayer.getAccount().getUuid());
        inventories.stream()
            .filter(this::isDefaultProfile)
            .filter(inventory -> inventory.getInventoryType() == inventoryType)
            .findFirst()
            .ifPresent(inventory -> applyInventoryToGui(bukkitPlayer, inventory));

        bukkitPlayer.updateInventory();
    }

    public void clearGuiInventory(@NotNull AstPlayer astPlayer) {
        clearGuiInventory(astPlayer.getBukkit());
        astPlayer.getBukkit().updateInventory();
    }

    private void applyInventoryToGui(Player bukkitPlayer, InventoryModel inventory) {
        if (!inventory.isEnabled()) {
            return;
        }

        if (inventory.getInventoryType() == InventoryType.NORMAL) {
            applyNormalInventoryFromSnapshot(bukkitPlayer, inventory);
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
        var storageContents = playerInventory.getStorageContents();

        for (InventoryEntryModel entry : entries) {
            Integer slotIndex = entry.getSlotIndex();
            if (slotIndex == null || !NormalInventoryLayout.isManagedSlot(slotIndex)) {
                continue;
            }
            int guiSlotIndex = NormalInventoryLayout.toGuiSlotIndex(slotIndex);
            if (guiSlotIndex < 0 || guiSlotIndex >= storageContents.length) {
                continue;
            }

            ItemStack itemStack = itemStackResolver.resolve(entry);
            if (itemStack == null) {
                continue;
            }

            storageContents[guiSlotIndex] = itemStack;
        }

        playerInventory.setStorageContents(storageContents);
    }

    /**
     * 通常インベントリの管理対象スロットだけをスナップショットとして保存します。
     * <p>
     * ホットバー、装備欄、オフハンドは通常インベントリの表示対象外として保存しません。
     *
     * @param astPlayer 保存対象プレイヤー
     */
    public void saveNormalInventorySnapshot(@NotNull AstPlayer astPlayer) {
        var accountId = astPlayer.getAccount().getUuid();
        var inventory = ensureInventory(accountId, InventoryType.NORMAL, NormalInventoryLayout.CAPACITY, accountId);
        var metadataJson = snapshotCodec.encode(NormalInventoryLayout.createManagedStorageSnapshot(astPlayer.getBukkit()));
        inventoryRepository.updateMetadata(inventory.getInventoryId(), metadataJson, accountId);
    }

    private boolean applyNormalInventoryFromSnapshot(Player bukkitPlayer, InventoryModel inventory) {
        if (inventory.getMetadataJson() == null || inventory.getMetadataJson().isBlank()) {
            return false;
        }

        ItemStack[] contents = snapshotCodec.decode(inventory.getMetadataJson());
        if (contents == null) {
            return false;
        }

        NormalInventoryLayout.applyManagedStorageSnapshot(bukkitPlayer, contents);
        return true;
    }

    private ItemModel resolveItemModel(String itemId) {
        ItemModel loaded = itemService.findLoadedById(itemId);
        if (loaded != null) {
            return loaded;
        }
        return itemService.loadItem(itemId);
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

    private @Nullable Integer resolveSlotCapacity(@NotNull InventoryType inventoryType) {
        return inventoryType.isSlotted() ? NormalInventoryLayout.CAPACITY : null;
    }
}

