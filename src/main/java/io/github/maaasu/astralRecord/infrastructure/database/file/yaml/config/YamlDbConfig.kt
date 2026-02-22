package io.github.maaasu.astralRecord.infrastructure.database.file.yaml.config

/**
 * YamlDBのconfig.ymlの設定を保持するデータクラス
 */
data class YamlDbConfig(
    val schemaVersion: Int,
    val databases: List<DatabaseEntry>,
    val referenceResolvers: List<ReferenceResolverEntry>,
    val rules: RulesConfig
)

/**
 * データベースエントリ
 * @param name データベース名
 * @param path データベースのパス
 */
data class DatabaseEntry(
    val name: String,
    val path: String
)

/**
 * 参照リゾルバエントリ
 * @param prefix プレフィックス
 * @param database 対象データベース名
 * @param aliases エイリアスリスト
 */
data class ReferenceResolverEntry(
    val prefix: String,
    val database: String,
    val aliases: List<String>
)

/**
 * ルール設定
 * @param idRegex IDの正規表現パターン
 * @param fileNameFormat ファイル名フォーマット
 */
data class RulesConfig(
    val idRegex: String,
    val fileNameFormat: String
)
