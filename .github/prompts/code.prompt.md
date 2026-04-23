---
description: AstralRecord向けのコード生成ルールを適用する
---

# AstralRecord — コーディング規約プロンプト

> このプロンプトはソースコードの生成・編集タスク時に参照するカスタムプロンプトです。
> `.github/copilot-instructions.md` の全般ルールと併用してください。

## 言語使い分け方針

本プロジェクトの開発者は **Java をメインとして知識を有している** ため、Java を基本とします。
ただし、Kotlin の言語仕様によりクリーンに記述できる領域は積極的に Kotlin を採用します。

| 実装領域                            | 使用言語       | 理由                                           |
|:--------------------------------|:-----------|:---------------------------------------------|
| **Model 定義**（データクラス、エンティティ）     | **Kotlin** | `data class` による簡潔な記述・不変性の担保                 |
| **Repository 定義**（DBアクセス層）      | **Kotlin** | Exposed DSL / DAO + `null safety` による安全なDB操作 |
| **GUI（View）**（インベントリUI等）        | **Kotlin** | DSL 的な記述によるレイアウトの可読性向上                       |
| **ビジネスロジック**（Service、Handler 等） | **Java**   | 開発者の主要知識に合わせ、保守性を優先                          |
| **コマンド処理**                      | **Java**   | 同上                                           |
| **イベントリスナー**                    | **Java**   | 同上                                           |
| **プラグインメインクラス**                 | **Java**   | エントリポイントは Java で統一し、初期化フローを明確化               |

> **原則**: 上記の対応表に明記されていない領域は Java で実装すること。
> Kotlin の採用が適切と判断した場合は、その旨をユーザーに提示した上で確認を取ること。

---

## コードスタイル

### 共通

- インデントは **スペース 4 つ** を使用すること（タブ不可）
- 1行の最大文字数は **120 文字** を目安とすること
- ファイル末尾には **改行を 1 つ** 入れること
- 未使用のインポートは残さないこと

### Java

- 命名規則:
  - クラス名: `UpperCamelCase`
  - メソッド名・変数名: `lowerCamelCase`
  - 定数: `UPPER_SNAKE_CASE`
  - パッケージ名: `lowercase.dot.separated`
- アクセス修飾子は必ず明示すること（`public` / `private` / `protected` の省略不可）
- フィールドには原則 `private final` を使用し、必要な場合のみ可変にすること
- Javadoc は公開メソッド・クラスに必ず付与すること

### Kotlin

- 命名規則は Java に準拠する（`UpperCamelCase` / `lowerCamelCase` / `UPPER_SNAKE_CASE`）
- `data class` のプロパティは `val` を優先し、可変が必要な場合のみ `var` を使用すること
- `!!`（non-null assertion）の使用は禁止。`?.` / `?:` / `requireNotNull` で代替すること
- `lateinit` の使用は最小限に留め、使用箇所にコメントで理由を明記すること
- KDoc は公開クラス・関数に必ず付与すること

---

## 使用禁止事項

| 禁止内容                                      | 理由                                                                             |
|:------------------------------------------|:-------------------------------------------------------------------------------|
| Bukkit API のみを使用したコード                     | 本プロジェクトは PaperAPI 準拠とする                                                        |
| `System.out.println` / `println` によるログ出力  | プラグイン付属の Logger を使用すること                                                        |
| ハードコーディングされた文字列メッセージ（プレイヤー向け表示文言）         | `config.yml` またはメッセージ定数クラスで管理すること                                              |
| `Thread.sleep` による処理停止                    | PaperAPI の非同期スケジューラを使用すること                                                     |
| `static` フィールドへのプレイヤーデータ保持                | SQL Server またはキャッシュ層で管理すること                                                    |
| Kotlin の `!!` 演算子                         | NullPointerException のリスクを排除するため                                               |
| Bukkit の `org.bukkit.entity.Player` を直接使用 | プレイヤーは必ず `AstPlayer` を使用すること。Bukkit API が必要な場合は `AstPlayer.bukkit` 経由でアクセスすること |

---

## パッケージ構成の指針

ソースコードは以下の層構造に従って配置すること。

```
io.github.astralrecord/
├── core/                     # 全体で共有する中核レイヤ
│   └── event/                # 共通イベント定義・基盤イベント
├── feature/                  # 機能単位の実装レイヤ
│   ├── model/                # 機能用データクラス・エンティティ
│   ├── repository/           # 機能用データアクセス層
│   ├── service/              # 機能用ビジネスロジック
│   ├── event/                # 機能固有イベント
│   ├── view/                 # GUI/表示ロジック
│   └── util/                 # 機能内ユーティリティ
└── infrastrucure/            # インフラストラクチャ層
    ├── config/               # 設定ロード・設定管理
    ├── database/             # DB接続・永続化実装
    ├── logging/              # ログ出力・ロギング設定
    └── util/                 # インフラ共通ユーティリティ
```
※ 新規項目を追加した際や、編集中に記載がないことを確認した場合は順次上記の構造に追加してください。
---

## AstralRecord API 呼び出し時の注意事項

Repository クラスで AstralRecord API のエンドポイントパスをコードに記述する場合は、以下のルールを厳守すること。

| ルール                        | 内容                                                                                                   |
|:---------------------------|:-----------------------------------------------------------------------------------------------------|
| **`docs/api/` を正式仕様とする**   | API エンドポイントのパス・リクエスト・レスポンス形式は `docs/api/*.md` を参照すること。参照前に `scripts/sync-api-docs.ps1` を実行して最新化すること。 |
| **`temp/` フォルダのファイルは参照禁止** | `temp/` 配下のファイルは作業用・一時的な参考資料であり、現行の API 仕様を保証しない。エンドポイントパスの根拠として使用してはならない。                           |
| **仕様不明時はユーザーに確認する**        | `docs/api/` にドキュメントが存在しない場合は、独断で実装せずユーザーに確認すること。                                                     |
| **既存コードのパスを優先する**          | 同一リポジトリ内に既存の API 呼び出しコードがある場合は、そのパス形式を正として参照すること。                                                    |


---

## infrastructure 共通クラスの使用方針

`infrastructure` パッケージ配下の共通クラスは、同等の機能を自前実装せず **必ず再利用**すること。
特に以下のクラスは見落としやすいため、コード生成時に積極的に参照・使用すること。

### `infrastructure/util`

| クラス名             | 用途                                                                                       |
|:-----------------|:-----------------------------------------------------------------------------------------|
| `ColorCodeUtil`  | Minecraftカラーコード（`§`記号）の定数・変換・除去。プレイヤー向けの文字列着色はすべてこのクラスを使用すること。`&` → `§` 変換も同様。           |
| `YamlLoaderUtil` | YAMLファイルの読み込み（単体・ディレクトリ一括・再帰対応）。YAMLを直接 `YamlConfiguration.loadConfiguration()` で読まないこと。 |
| `ReferenceUtil`  | YAML内の `ref` 参照（文字列またはMapの `ref` キー）を解決する際に使用すること。                                       |

### `infrastructure/database`

本プロジェクトの SQL Server アクセスには **Jetbrains Exposed** を使用する。

| 項目           | 方針                                                                             |
|:-------------|:-------------------------------------------------------------------------------|
| **DB接続**     | HikariCP の `DataSource` を `Database.connect(dataSource)` に渡して初期化すること           |
| **テーブル定義**   | `object : Table("テーブル名")` の形式で `Table` オブジェクトとして定義すること                         |
| **クエリ記述**    | Exposed の **DSL スタイル**（`select` / `insert` / `update` / `deleteWhere`）を基本とすること |
| **DAO スタイル** | シンプルな CRUD のみを行う場合は `DAO スタイル`（`Entity` / `EntityClass`）を使用しても良い               |
| **トランザクション** | DB操作は必ず `transaction { }` ブロック内で実行すること                                         |
| **非同期実行**    | `transaction { }` は PaperAPI の非同期スケジューラ上で呼び出すこと（メインスレッドで実行禁止）                  |
| **生SQL禁止**   | `exec()` による生SQL文字列の直接実行は原則禁止。型安全な DSL で記述できない場合のみ使用を許可し、コメントで理由を明記すること        |

#### テーブル定義の例

```kotlin
object AccountTable : Table("account") {
    val uuid       = uuid("uuid")
    val userId     = uuid("user_id")
    val accountName = varchar("account_name", 50)
    val slotIndex  = integer("slot_index")
    val isActive   = bool("is_active").default(false)
    val mode       = byte("mode").default(0)
    val createdAt  = datetime("created_at")
    val updatedAt  = datetime("updated_at")
    val createdBy  = uuid("created_by")
    val updatedBy  = uuid("updated_by")
    val isDeleted  = bool("is_deleted").default(false)

    override val primaryKey = PrimaryKey(uuid)
}
```

#### クエリ記述の例

```kotlin
// 取得
transaction {
    AccountTable
        .selectAll()
        .where { AccountTable.userId eq targetUuid }
        .map { it[AccountTable.accountName] }
}

// 挿入
transaction {
    AccountTable.insert {
        it[uuid]        = UUID.randomUUID()
        it[userId]      = targetUuid
        it[accountName] = "キャラクター名"
        it[slotIndex]   = 0
        it[mode]        = 0
        it[createdAt]   = LocalDateTime.now()
        it[updatedAt]   = LocalDateTime.now()
        it[createdBy]   = targetUuid
        it[updatedBy]   = targetUuid
    }
}
```



ログ出力には `Logger` クラスを使用すること。使用ルールは以下のとおり。詳細は `logger.prompt.md` を参照。

> **⚠️ ログ出力の必須ルール（`logger.prompt.md` 要約）**
>
> | ルール                              | 内容                                                                                                          |
> |:---------------------------------|:------------------------------------------------------------------------------------------------------------|
> | **LogId 経由を必須とする**               | `Logger.info(...)` 等にメッセージ文字列を直接渡すことは**禁止**。必ず `LogId` enum の定数を経由すること（例: `Logger.log(LogId.I_XXXX, arg)`）  |
> | **メッセージは logger.properties に定義** | ログ文言はソースコードにハードコーディングせず、`logger.properties` に `{Type}_{Number}=メッセージ` 形式で定義すること                             |
> | **例外ありエラーの引数順序**                 | `Logger.error` / `Logger.log` で例外を渡す場合は `(LogId, Throwable, Object...)` の順。`(String, Throwable)` の形式は使用不可   |
> | **実装手順**                         | ① `logger.properties` にメッセージ追加 → ② `LogId.java` に enum 定数追加 → ③ コードで `Logger.log(LogId.XXXX, args...)` 呼び出し |

### `feature/player`

プレイヤーを扱う処理では、Bukkit の `org.bukkit.entity.Player` を直接使用せず、**必ず `AstPlayer`（`feature/player/model/AstPlayer.kt`）を使用すること**。

| ルール                                  | 内容                                                                                                                         |
|:-------------------------------------|:---------------------------------------------------------------------------------------------------------------------------|
| **`AstPlayer` を基本とする**               | コマンド・イベント・サービス等でプレイヤーを受け渡す際は `AstPlayer` を引数・戻り値の型として使用すること                                                                |
| **Bukkit `Player` の直接使用禁止**          | `org.bukkit.entity.Player` を直接フィールド・引数・ローカル変数として保持・受け渡しすることは禁止。Bukkit API の呼び出しが必要な場合は `AstPlayer.bukkit` プロパティ経由でアクセスすること |
| **メッセージ送信は `AstPlayer.sendMessage`** | プレイヤーへのメッセージ送信は `AstPlayer.sendMessage(PlayerMsgId, vararg args)` を使用すること。`bukkit.sendMessage(...)` を直接呼び出すことは禁止           |

プレイヤー向けメッセージには `PlayerMsgResource` クラスを使用すること。使用ルールは以下のとおり。詳細は `player_msg.prompt.md` を参照。

> **⚠️ プレイヤーメッセージの必須ルール（`player_msg.prompt.md` 要約）**
>
> | ルール                              | 内容                                                                                                                               |
> |:---------------------------------|:---------------------------------------------------------------------------------------------------------------------------------|
> | **MsgId 経由を必須とする**               | `player.sendMessage(...)` にメッセージ文字列を直接渡すことは**禁止**。必ず `MsgId` の定数を経由すること（例: `PlayerMsgResource.getMessage(MsgId.P_XXXX)`）         |
> | **メッセージは player.properties に定義** | メッセージ文言はソースコードにハードコーディングせず、`player.properties` に `{Prefix}_{Number}=メッセージ` 形式で定義すること                                             |
> | **カラーコードは `&` 記号で記述**            | カラーコードは `player.properties` 内に `&` 記号で記述すること（`PlayerMsgResource` が `§` へ自動変換する）                                                  |
> | **フォーマット引数は `{0}`, `{1}` 形式**    | プレースホルダーは `MessageFormat` 準拠の `{0}`, `{1}` を使用し、`PlayerMsgResource.format(MsgId.P_XXXX, args...)` で展開すること                        |
> | **実装手順**                         | ① `player.properties` にメッセージ追加 → ② `MsgId.java` に定数追加 → ③ `PlayerMsgResource.format(...)` 呼び出し → ④ `player.sendMessage(...)` で送信 |

### `infrastructure/config`

| クラス名               | 用途                                                                   |
|:-------------------|:---------------------------------------------------------------------|
| `ConfigManager`    | `config.yml` の読み込み・再読み込み管理。                                          |
| `ConfigProperties` | `config.yml` の値をタイプセーフに取得するアクセサ。デバッグモード判定（`isPluginDebugMode()`）も含む。 |
| `ConfigKeys`       | `config.yml` のキー文字列定数。キーをハードコーディングしないこと。                             |

> **禁止**: 上記と同等の機能をローカルで再実装すること。必ず `infrastructure` のクラスを import して使用すること。

---

## Java ↔ Kotlin 相互運用時の注意事項

- Kotlin の `data class` を Java から参照する場合、`component1()` 等の destructuring 関数は Java から直接呼ばず、getter を使用すること
- Kotlin の `object`（シングルトン）を Java から参照する場合は `INSTANCE` フィールド経由でアクセスすること
- Kotlin の `internal` 修飾子は Java からは `public` として見えるため、意図しない外部参照に注意すること
- `@JvmStatic` / `@JvmField` / `@JvmOverloads` は Java との相互運用が必要な箇所に適切に付与すること


## 
