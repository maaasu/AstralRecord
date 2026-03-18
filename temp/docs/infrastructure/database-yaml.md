# YAML ファイルDB — 構造図

アイテム定義などの静的マスタデータを YAML ファイルで管理する層の構造です。  
SQL Server と連携した差分検出（スナップショット）機能も含みます。

---

## クラス構成図

```mermaid
classDiagram
    class FileDatabaseManager {
        <<Singleton>>
        -instance: FileDatabaseManager$
        +rootDirectory: File
        +getInstance() FileDatabaseManager$
        +getConfig(relativePath) FileConfiguration
        +getConfig(pathSegments) FileConfiguration
        +saveConfig(config, relativePath) void
    }

    class YamlDbConfigUtil {
        <<object / Singleton>>
        +reload() YamlDbConfig?
        +getConfig() YamlDbConfig?
        +getDatabasePath(category) String
        +getDatabasePath(name) String
    }

    class YamlDbConfigLoader {
        <<object>>
        -cachedConfig: YamlDbConfig?
        +loadAndCache(file) YamlDbConfig?
        +getCachedConfig() YamlDbConfig?
    }

    class YamlDbConfig {
        <<data class>>
        +schemaVersion: Int
        +databases: List~DatabaseEntry~
        +referenceResolvers: List~ReferenceResolverEntry~
        +rules: RulesConfig
    }

    class DatabaseEntry {
        <<data class>>
        +name: String
        +path: String
    }

    class ReferenceResolverEntry {
        <<data class>>
        +prefix: String
        +database: String
        +aliases: List~String~
    }

    class RulesConfig {
        <<data class>>
        +idRegex: String
        +fileNameFormat: String
    }

    class YamlDbConfigKey {
        <<object>>
        SCHEMA_VERSION: String$
        DATABASES: String$
        REFERENCE_RESOLVERS: String$
        RULES: String$
    }

    class YamlDatabaseCategory {
        <<enum>>
        ITEM
        BUFF
        LOOT
        +value: String
        +fromString(value) YamlDatabaseCategory?$
    }

    class YamlDiffService {
        -snapshotRepository: YamlSnapshotRepository
        -gson: Gson
        +checkAndLogDiff(file, yaml) CompletableFuture~Boolean~
        -logDifferences(filePath, previous, current) void
        -calculateHash(content) String
        -yamlToMap(yaml) Map
    }

    class YamlSnapshotRepository {
        -sqlServerManager: SqlServerManager
        +findByFilePath(filePath) CompletableFuture~YamlSnapshot?~
        +save(snapshot) CompletableFuture~Boolean~
        +deleteByFilePath(filePath) CompletableFuture~Boolean~
    }

    class YamlSnapshot {
        <<data class>>
        +snapshotId: UUID
        +filePath: String
        +fileHash: String
        +contentJson: String
        +createdAt: LocalDateTime
        +updatedAt: LocalDateTime
        +create(filePath, fileHash, contentJson) YamlSnapshot$
    }

    FileDatabaseManager --> YamlDbConfigUtil : rootDirectory 提供
    YamlDbConfigUtil --> FileDatabaseManager : rootDirectory 取得
    YamlDbConfigUtil --> YamlDbConfigLoader : loadAndCache()
    YamlDbConfigUtil --> YamlDatabaseCategory : カテゴリ名解決
    YamlDbConfigLoader --> YamlDbConfig : 生成・キャッシュ
    YamlDbConfigLoader --> YamlDbConfigKey : キー定数参照
    YamlDbConfig *-- DatabaseEntry
    YamlDbConfig *-- ReferenceResolverEntry
    YamlDbConfig *-- RulesConfig

    YamlDiffService --> YamlSnapshotRepository : スナップショット取得・保存
    YamlDiffService --> YamlSnapshot : 生成・比較
    YamlSnapshotRepository --> YamlSnapshot : CRUD
```

---

## YamlDB 設定ファイルの構造 (`database/file/config.yml`)

```mermaid
graph TD
    CFG["config.yml\n(YamlDbConfig)"]

    CFG --> DB["databases[]"]
    CFG --> RR["referenceResolvers[]"]
    CFG --> RULES["rules"]

    DB --> DB1["name: item\npath: 10.features.item/"]
    DB --> DB2["name: buff\npath: 70.shared.buff/"]
    DB --> DB3["name: loot\npath: 80.shared.loot/"]

    RR --> RR1["prefix: @item\ndatabase: item"]
    RR --> RR2["prefix: @buff\ndatabase: buff"]

    RULES --> R1["idRegex: パターン"]
    RULES --> R2["fileNameFormat: フォーマット"]
```

---

## YAML 読み込みフロー（通常）

```mermaid
sequenceDiagram
    participant Feature as feature 層
    participant FDM as FileDatabaseManager
    participant YDCU as YamlDbConfigUtil
    participant FS as ファイルシステム

    Feature->>YDCU: getDatabasePath(YamlDatabaseCategory.ITEM)
    YDCU->>YDCU: getConfig() でキャッシュ確認
    YDCU-->>Feature: "10.features.item/" (相対パス)

    Feature->>FDM: getConfig("10.features.item/", "iron_sword")
    FDM->>FS: rootDirectory + "10.features.item/iron_sword.yml" を読み込み
    FS-->>FDM: YamlConfiguration
    FDM-->>Feature: FileConfiguration
```

---

## YAML 差分検出フロー（YamlDiffService）

YAMLファイルの変更を検出し、前回ロード時との差分をデバッグログに出力します。  
スナップショットは SQL Server の `yaml_snapshot` テーブルに保存されます。

```mermaid
sequenceDiagram
    participant Caller as 呼び出し元
    participant YDS as YamlDiffService
    participant YSR as YamlSnapshotRepository
    participant DB as SQL Server (yaml_snapshot)

    Caller->>YDS: checkAndLogDiff(file, yaml)
    YDS->>YDS: calculateHash(yaml.saveToString())
    YDS->>YDS: yamlToMap(yaml) → JSON 変換

    YDS->>YSR: findByFilePath(filePath)
    YSR->>DB: SELECT WHERE file_path = ?
    DB-->>YSR: ResultSet
    YSR-->>YDS: YamlSnapshot? (null = 初回)

    alt 初回ロード（スナップショットなし）
        YDS->>YDS: Logger.log(D_1300) ← 初回ロードログ
        YDS->>YSR: save(newSnapshot)
        YSR->>DB: MERGE INTO yaml_snapshot
        YDS-->>Caller: false (差分なし)

    else ハッシュ一致（変更なし）
        YDS->>YDS: Logger.log(D_1301) ← 変更なしログ
        YDS-->>Caller: false

    else ハッシュ不一致（変更あり）
        YDS->>YDS: logDifferences(前回JSON, 現在JSON)
        Note over YDS: 追加・削除・変更キーをデバッグログ出力
        YDS->>YSR: save(updatedSnapshot)
        YSR->>DB: MERGE INTO yaml_snapshot (UPDATE)
        YDS-->>Caller: true (差分あり)
    end
```

---

## ディレクトリ構成とカテゴリの対応

| `YamlDatabaseCategory` | `config.yml` の `name` | 実際のパス例                        |
|:-----------------------|:----------------------|:--------------------------------|
| `ITEM`                 | `item`                | `database/file/10.features.item/` |
| `BUFF`                 | `buff`                | `database/file/70.shared.buff/`   |
| `LOOT`                 | `loot`                | `database/file/80.shared.loot/`   |

