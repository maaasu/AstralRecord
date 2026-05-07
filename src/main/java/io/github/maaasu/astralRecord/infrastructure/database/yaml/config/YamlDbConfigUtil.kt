package io.github.maaasu.astralRecord.infrastructure.database.yaml.config

import io.github.maaasu.astralRecord.infrastructure.file.FileDatabaseManager
import io.github.maaasu.astralRecord.infrastructure.logging.LogId
import io.github.maaasu.astralRecord.infrastructure.logging.Logger

import java.io.File


object YamlDbConfigUtil {

    private const val CONFIG_FILE_NAME = "config.yml"


    fun reload(): YamlDbConfig? {
        val rootDir =
            FileDatabaseManager.getInstance().rootDirectory ?: run {
                Logger.log(LogId.W_1400)
                return null
            }

        val configFile = File(rootDir, CONFIG_FILE_NAME)
        if (!configFile.exists()) {
            Logger.log(LogId.W_1401, configFile.absolutePath)
            return null
        }

        val config = YamlDbConfigLoader.loadAndCache(configFile)
        if (config != null) {
            Logger.log(LogId.I_1400)
        }
        return config
    }


    fun getConfig(): YamlDbConfig? {
        return YamlDbConfigLoader.getCachedConfig() ?: reload()
    }


    fun getDatabasePath(category: YamlDatabaseCategory): String {
        return getDatabasePath(category.value)
    }


    private fun getDatabasePath(name: String): String {
        return getConfig()?.databases?.find { it.name == name }?.path ?: name
    }


    fun getDatabaseNameByReference(referencePrefix: String): String? {
        val config = getConfig() ?: return null

        // normalized comment
        val byPrefix = config.referenceResolvers.find { it.prefix == referencePrefix }
        if (byPrefix != null) return byPrefix.database

        // normalized comment
        return config.referenceResolvers.find { it.aliases.contains(referencePrefix) }?.database
    }
}
