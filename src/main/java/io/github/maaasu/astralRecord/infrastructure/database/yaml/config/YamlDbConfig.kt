package io.github.maaasu.astralRecord.infrastructure.database.yaml.config


data class YamlDbConfig(
    val schemaVersion: Int,
    val databases: List<DatabaseEntry>,
    val referenceResolvers: List<ReferenceResolverEntry>,
    val rules: RulesConfig
)


data class DatabaseEntry(
    val name: String,
    val path: String
)


data class ReferenceResolverEntry(
    val prefix: String,
    val database: String,
    val aliases: List<String>
)


data class RulesConfig(
    val idRegex: String,
    val fileNameFormat: String
)
