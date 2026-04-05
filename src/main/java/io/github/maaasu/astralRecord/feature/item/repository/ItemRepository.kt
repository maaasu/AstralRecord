package io.github.maaasu.astralRecord.feature.item.repository

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.maaasu.astralRecord.feature.item.model.ItemBundle
import io.github.maaasu.astralRecord.feature.item.model.ItemBundleOnUse
import io.github.maaasu.astralRecord.feature.item.model.ItemConsumable
import io.github.maaasu.astralRecord.feature.item.model.ItemConsumableEffect
import io.github.maaasu.astralRecord.feature.item.model.ItemConsumableEffectType
import io.github.maaasu.astralRecord.feature.item.model.ItemConsumableOnUse
import io.github.maaasu.astralRecord.feature.item.model.ItemCurrency
import io.github.maaasu.astralRecord.feature.item.model.ItemEquipment
import io.github.maaasu.astralRecord.feature.item.model.ItemEquipmentDurability
import io.github.maaasu.astralRecord.feature.item.model.ItemEquipmentHandType
import io.github.maaasu.astralRecord.feature.item.model.ItemEquipmentOnUse
import io.github.maaasu.astralRecord.feature.item.model.ItemEquipmentSlot
import io.github.maaasu.astralRecord.feature.item.model.ItemEquipmentStat
import io.github.maaasu.astralRecord.feature.item.model.ItemEquipmentStatType
import io.github.maaasu.astralRecord.feature.item.model.ItemModel
import io.github.maaasu.astralRecord.feature.item.model.ItemSummary
import io.github.maaasu.astralRecord.infrastructure.logging.LogId
import io.github.maaasu.astralRecord.infrastructure.logging.Logger
import io.github.maaasu.astralRecord.infrastructure.util.ApiRequestUtil
import java.io.IOException
import java.net.URLEncoder
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

/**
 * AstralRecord API を通じてアイテム定義を取得するリポジトリ。
 */
class ItemRepository {

    /**
     * 全カテゴリのアイテム一覧（id/category）を取得します。
     * GET /api/item
     */
    fun findAll(): List<ItemSummary> {
        val path = "/api/item"
        try {
            ApiRequestUtil.buildClient().use { client ->
                val request = ApiRequestUtil.buildRequestBuilder(path).GET().build()
                val response = client.send(request, HttpResponse.BodyHandlers.ofString())
                return when (response.statusCode()) {
                    200 -> parseSummaryList(response.body())
                    else -> {
                        val message = "HTTP ${response.statusCode()} for GET $path"
                        Logger.log(LogId.E_5201, message)
                        throw IOException("Unexpected status ${response.statusCode()} for GET $path")
                    }
                }
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            Logger.log(LogId.E_5201, e, e.message ?: "Interrupted while GET $path")
            throw RuntimeException(e)
        }
    }

    /**
     * 指定カテゴリの全アイテムを取得します。
     * 一覧API（/api/item）でカテゴリを抽出し、詳細APIで展開します。
     */
    fun findAllByCategory(category: String): List<ItemModel> {
        val normalizedCategory = category.trim()
        if (normalizedCategory.isBlank()) {
            return emptyList()
        }

        val summaries = findAll().filter { it.category.equals(normalizedCategory, ignoreCase = true) }
        val items = mutableListOf<ItemModel>()
        for (summary in summaries) {
            val item = findById(summary.category, summary.id)
            if (item != null) {
                items += item
            }
        }

        Logger.log(LogId.D_5202, normalizedCategory, items.size)
        return items
    }

    fun findById(category: String, itemId: String): ItemModel? {
        val normalizedCategory = category.trim()
        val encodedCategory = URLEncoder.encode(normalizedCategory, StandardCharsets.UTF_8).replace("+", "%20")
        val encodedItemId = URLEncoder.encode(itemId, StandardCharsets.UTF_8).replace("+", "%20")
        val path = "/api/item/$encodedCategory/$encodedItemId"

        try {
            ApiRequestUtil.buildClient().use { client ->
                val request = ApiRequestUtil.buildRequestBuilder(path).GET().build()
                val response = client.send(request, HttpResponse.BodyHandlers.ofString())
                return when (response.statusCode()) {
                    200 -> {
                        val item = parseItem(response.body())
                        Logger.log(LogId.D_5200, normalizedCategory, itemId)
                        item
                    }
                    404 -> {
                        Logger.log(LogId.W_5200, normalizedCategory, itemId)
                        null
                    }
                    else -> {
                        Logger.log(LogId.E_5200, "HTTP ${response.statusCode()} for GET $path")
                        throw IOException("Unexpected status ${response.statusCode()} for GET $path")
                    }
                }
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            Logger.log(LogId.E_5200, e)
            throw RuntimeException(e)
        } catch (e: IOException) {
            Logger.log(LogId.E_5200, e)
            throw e
        }
    }

    private fun parseItem(json: String): ItemModel {
        val obj = JsonParser.parseString(json).asJsonObject
        return parseItem(obj)
    }

    private fun parseSummaryList(json: String): List<ItemSummary> {
        val array = JsonParser.parseString(json).asJsonArray
        return array.mapNotNull { element ->
            if (!element.isJsonObject) {
                return@mapNotNull null
            }
            val obj = element.asJsonObject
            val id = obj.get("id")?.takeIf { !it.isJsonNull }?.asString ?: return@mapNotNull null
            val category = obj.get("category")?.takeIf { !it.isJsonNull }?.asString ?: return@mapNotNull null
            ItemSummary(id = id, category = category)
        }
    }

    private fun parseItemList(json: String): List<ItemModel> {
        val array = JsonParser.parseString(json).asJsonArray
        return array.map { parseItem(it.asJsonObject) }
    }

    private fun parseItem(obj: JsonObject): ItemModel {
        val category = obj.get("category")?.asString ?: "unknown"
        return ItemModel(
            schemaVersion = obj.get("schemaVersion")?.asInt ?: 1,
            id = obj.get("id").asString,
            category = category,
            name = obj.get("name").asString,
            icon = obj.get("icon").asString,
            rarity = obj.get("rarity").asString,
            maxStack = obj.get("maxStack")?.asInt ?: 64,
            saleValue = obj.get("saleValue")?.asInt ?: 0,
            customModelData = if (obj.has("customModelData") && !obj.get("customModelData").isJsonNull) {
                obj.get("customModelData").asInt
            } else {
                null
            },
            lore = parseLore(obj.getAsJsonArray("lore")),
            unTradeable = obj.get("unTradeable")?.asBoolean ?: false,
            unSellable = obj.get("unSellable")?.asBoolean ?: false,
            bundle = parseBundle(obj),
            currency = parseCurrency(obj),
            equipment = parseEquipment(obj),
            consumable = parseConsumable(obj),
        )
    }

    private fun parseBundle(obj: JsonObject): ItemBundle? {
        val bundleObj = parseObjectOrNull(obj, "bundle") ?: return null
        val onUseObj = parseObjectOrNull(bundleObj, "onUse")

        return ItemBundle(
            lootTableId = parseStringOrNull(bundleObj, "lootTableId"),
            onUse = if (onUseObj != null) {
                ItemBundleOnUse(
                    sound = parseStringOrNull(onUseObj, "sound"),
                    effect = parseStringOrNull(onUseObj, "effect"),
                    particle = parseStringOrNull(onUseObj, "particle"),
                )
            } else {
                null
            },
        )
    }

    private fun parseCurrency(obj: JsonObject): ItemCurrency? {
        val currencyObj = parseObjectOrNull(obj, "currency") ?: return null
        return ItemCurrency(
            type = parseStringOrNull(currencyObj, "type"),
            group = parseStringOrNull(currencyObj, "group"),
            expiresAt = parseStringOrNull(currencyObj, "expiresAt"),
        )
    }

    private fun parseEquipment(obj: JsonObject): ItemEquipment? {
        val equipmentObj = parseObjectOrNull(obj, "equipment") ?: return null

        val statsArray = equipmentObj.getAsJsonArray("stats") ?: JsonArray()
        val stats = statsArray.mapNotNull { element ->
            if (!element.isJsonObject) {
                return@mapNotNull null
            }

            val statObj = element.asJsonObject
            val status = parseStringOrNull(statObj, "status") ?: return@mapNotNull null
            val value = parseStringOrNull(statObj, "value") ?: return@mapNotNull null
            ItemEquipmentStat(
                status = status,
                type = ItemEquipmentStatType.fromApiValue(parseStringOrNull(statObj, "type")),
                value = value,
            )
        }

        val durabilityObj = parseObjectOrNull(equipmentObj, "durability")
        val durability = if (durabilityObj != null && durabilityObj.has("max") && !durabilityObj.get("max").isJsonNull) {
            ItemEquipmentDurability(
                max = durabilityObj.get("max").asInt,
                consume = durabilityObj.get("consume")?.asInt ?: 1,
            )
        } else {
            null
        }

        val onUseObj = parseObjectOrNull(equipmentObj, "onUse")
        val onUse = if (onUseObj != null) {
            ItemEquipmentOnUse(
                leftClickCooldownTicks = parseIntOrNull(onUseObj, "leftClickCooldownTicks"),
                leftClickSkillId = parseStringOrNull(onUseObj, "leftClickSkillId"),
                rightClickCooldownTicks = parseIntOrNull(onUseObj, "rightClickCooldownTicks")
                    ?: parseIntOrNull(onUseObj, "RightClickCooldownTicks"),
                rightClickSkillId = parseStringOrNull(onUseObj, "rightClickSkillId")
                    ?: parseStringOrNull(onUseObj, "RightClickSkillId"),
            )
        } else {
            null
        }

        return ItemEquipment(
            slot = ItemEquipmentSlot.fromApiValue(parseStringOrNull(equipmentObj, "slot")),
            handType = ItemEquipmentHandType.fromApiValue(parseStringOrNull(equipmentObj, "handType")),
            requiredLevel = equipmentObj.get("requiredLevel")?.asInt ?: 0,
            requiredClasses = parseStringList(equipmentObj.getAsJsonArray("requiredClasses")),
            setId = parseStringOrNull(equipmentObj, "setId"),
            stats = stats,
            durability = durability,
            onUse = onUse,
            skills = parseStringList(equipmentObj.getAsJsonArray("skills")),
        )
    }

    private fun parseLore(array: JsonArray?): List<String> {
        if (array == null) {
            return emptyList()
        }

        return array.mapNotNull {
            if (it.isJsonPrimitive) it.asString else null
        }
    }

    private fun parseConsumable(obj: JsonObject): ItemConsumable? {
        val consumableObj = parseObjectOrNull(obj, "consumable") ?: return null

        val onUseObj = parseObjectOrNull(consumableObj, "onUse")
        val onUse = if (onUseObj != null) {
            ItemConsumableOnUse(
                sound = onUseObj.get("sound")?.takeIf { !it.isJsonNull }?.asString,
                effect = onUseObj.get("effect")?.takeIf { !it.isJsonNull }?.asString,
                amount = onUseObj.get("amount")?.asInt ?: 1,
            )
        } else {
            null
        }

        val effectsArray = consumableObj.getAsJsonArray("effects") ?: JsonArray()
        val effects = effectsArray.mapNotNull { element ->
            if (!element.isJsonObject) {
                return@mapNotNull null
            }

            val effectObj = element.asJsonObject
            ItemConsumableEffect(
                type = ItemConsumableEffectType.fromApiValue(effectObj.get("type")?.asString),
                rate = effectObj.get("rate")?.asDouble ?: 100.0,
                value = effectObj.get("value")?.takeIf { !it.isJsonNull }?.asDouble,
                status = effectObj.get("status")?.takeIf { !it.isJsonNull }?.asString,
                isPercent = effectObj.get("isPercent")?.asBoolean ?: false,
                buffId = effectObj.get("buffId")?.takeIf { !it.isJsonNull }?.asString,
            )
        }

        return ItemConsumable(
            onUse = onUse,
            effects = effects,
        )
    }

    private fun parseStringList(array: JsonArray?): List<String> {
        if (array == null) {
            return emptyList()
        }

        return array.mapNotNull { element ->
            if (element.isJsonPrimitive) element.asString else null
        }
    }

    private fun parseStringOrNull(obj: JsonObject, key: String): String? {
        if (!obj.has(key)) {
            return null
        }
        val element = obj.get(key)
        if (element == null || element.isJsonNull) {
            return null
        }
        return element.asString
    }

    private fun parseObjectOrNull(obj: JsonObject, key: String): JsonObject? {
        if (!obj.has(key)) {
            return null
        }

        val element = obj.get(key)
        if (element == null || element.isJsonNull || !element.isJsonObject) {
            return null
        }

        return element.asJsonObject
    }

    private fun parseIntOrNull(obj: JsonObject, key: String): Int? {
        if (!obj.has(key)) {
            return null
        }
        val element = obj.get(key)
        if (element == null || element.isJsonNull) {
            return null
        }
        return element.asInt
    }
}

