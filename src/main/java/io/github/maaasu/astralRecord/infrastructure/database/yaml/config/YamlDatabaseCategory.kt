package io.github.maaasu.astralRecord.infrastructure.database.yaml.config


enum class YamlDatabaseCategory(val value: String) {
    Null("null");

    companion object {

        fun fromString(value: String): YamlDatabaseCategory? {
            return entries.find { it.value.equals(value, ignoreCase = true) }
        }
    }
}
