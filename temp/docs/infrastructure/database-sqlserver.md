# SQL Server データベース — 構造図

プレイヤー固有の動的データを管理する SQL Server 接続層の構造です。

---

## クラス構成図

```mermaid
classDiagram
    class SqlServerManager {
        <<Singleton>>
        -instance: SqlServerManager$
        -pool: ConnectionPool
        -initialized: boolean
        +getInstance() SqlServerManager$
        +initialize() void
        +getConnection() Connection
        +shutdown() void
    }

    class SqlServerConfig {
        -host: String
        -port: int
        -database: String
        -username: String
        -password: String
        +getJdbcUrl() String
    }

    class ConnectionPool {
        -dataSource: HikariDataSource
        +ConnectionPool(config)
        +getConnection() Connection
        +close() void
    }

    class SqlServerUtil {
        <<final>>
        +executeQuery(conn, sql, params)$
        +executeUpdate(conn, sql, params)$
    }

    class YamlSnapshotRepository {
        -sqlServerManager: SqlServerManager
        +findByFilePath(filePath) CompletableFuture~YamlSnapshot?~
        +save(snapshot) CompletableFuture~Boolean~
        +deleteByFilePath(filePath) CompletableFuture~Boolean~
    }

    SqlServerManager --> SqlServerConfig : 設定読み込み
    SqlServerManager --> ConnectionPool : 生成・保持
    ConnectionPool --> SqlServerConfig : 接続情報参照
    YamlSnapshotRepository --> SqlServerManager : getConnection()
```

---

## 接続初期化フロー

```mermaid
sequenceDiagram
    participant AR as AstralRecord
    participant SSM as SqlServerManager
    participant SSC as SqlServerConfig
    participant POOL as ConnectionPool
    participant DB as SQL Server

    AR->>SSM: getInstance().initialize()
    SSM->>SSC: new SqlServerConfig()
    SSC->>SSC: ConfigProperties から接続情報読み込み
    SSM->>POOL: new ConnectionPool(config)
    POOL->>POOL: HikariDataSource 設定
    SSM->>POOL: getConnection() ※接続テスト
    POOL->>DB: TCP 接続確立
    alt 接続成功
        DB-->>POOL: Connection
        POOL-->>SSM: Connection
        SSM->>SSM: initialized = true
    else 接続失敗
        DB-->>POOL: SQLException
        POOL-->>SSM: 例外スロー
        SSM->>SSM: Logger.log(E_1100, e)
    end
    SSM-->>AR: 初期化完了
```

---

## リポジトリパターン

リポジトリクラスは `SqlServerManager.getConnection()` を通じて接続を取得し、  
非同期（`CompletableFuture`）でクエリを実行します。

```mermaid
sequenceDiagram
    participant Service as 呼び出し元
    participant Repo as YamlSnapshotRepository
    participant SSM as SqlServerManager
    participant DB as SQL Server

    Service->>Repo: findByFilePath(filePath)
    Repo->>Repo: CompletableFuture.supplyAsync { ... }

    Note over Repo,DB: 非同期スレッドで実行
    Repo->>SSM: getConnection()
    SSM->>DB: コネクションプールから払い出し
    DB-->>SSM: Connection
    SSM-->>Repo: Connection

    Repo->>DB: prepareStatement + executeQuery
    DB-->>Repo: ResultSet
    Repo->>Repo: mapResultSetToSnapshot(rs)
    Repo-->>Service: CompletableFuture~YamlSnapshot?~
```

---

## テーブル定義（参照）

| テーブル名           | 定義ファイル                                         | 概要                     |
|:----------------|:-----------------------------------------------|:-----------------------|
| `yaml_snapshot` | `database/sqlserver/dbo.yaml_snapshot/yaml_snapshot.md` | YAML スナップショット保存テーブル    |
| `account`       | `database/sqlserver/dbo.account/account.md`    | アカウント情報                |
| `user`          | `database/sqlserver/dbo.user/user.md`          | ユーザー（プレイヤー）情報          |

