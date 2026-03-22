# AstralRecord — 開発ガイドライン

Minecraft MMO RPG プラグイン「AstralRecord」の開発に関する方針・構成をまとめたドキュメントです。
GitHub Copilot・JetBrains AI Assistant・Junie 向けのプロンプト設定もこのディレクトリで管理します。

---

## プロジェクト概要

本プロジェクトは Minecraft の MMO RPG「**AstralRecord**」のサーバープラグインです。

---

## 技術スタック

| 分類             | 内容                                                                    |
|:---------------|:----------------------------------------------------------------------|
| **言語**         | Java / Kotlin                                                         |
| **API**        | PaperAPI およびその関連ライブラリ                                                 |
| **データベース**     | SQL Server (T-SQL)                                                    |
| **ORM**        | Jetbrains Exposed                                                     |
| **外部 Web API** | AstralRecord API（[GitHub](https://github.com/maaasu/AstralRecordApi)） |

---

## データ管理方針

データの性質に応じて保存先を厳格に区別し、適切なパスを参照すること。
ただし、**DB への直接接続・直接アクセスは原則禁止**とし、後述の「AstralRecord API」経由でデータを操作することを基本方針とする。

| データ種別     | 内容例                        | 管理手法           | ディレクトリパス             |
|:----------|:---------------------------|:---------------|:---------------------|
| **動的データ** | プレイヤーレベル、経験値、所持アイテム、座標など   | **SQL Server** | `database/sqlserver` |
| **静的データ** | アイテムの基本設定（名前、説明、武器ステータス）など | **YAMLファイル**   | `database/file`      |

- **SQL Server**: 頻繁に更新・書き込みが発生するプレイヤー固有のデータ。
- **YAML**: ソースコードから読み取られるマスタデータ（アイテム定義など）。

テーブル定義・YAML スキーマ定義など、データベースの構造を確認する場合は以下のリポジトリの README.md を参照すること（ブランチ: `master`）。

- **リポジトリ**: [https://github.com/maaasu/Database](https://github.com/maaasu/Database)

---

## AstralRecord API

本プロジェクトでは、DB（静的データ・動的データを含む）へのアクセスを **AstralRecord API** 経由で行うことを基本方針とします。
DB に直接接続してデータを操作するのではなく、API を呼び出してデータを取得・操作する形を原則とすること。

- **リポジトリ**: [https://github.com/maaasu/AstralRecordApi](https://github.com/maaasu/AstralRecordApi)（ブランチ: `master`）
- **API ドキュメント**: リポジトリの `docs/api/` 配下を参照
- **ランタイム**: .NET 10 / ASP.NET Core Web API

### エンドポイント

最新のエンドポイント一覧・仕様は必ずリポジトリの以下を参照すること。エンドポイントをソースコードに記述する前に確認すること。

- [README.md](https://github.com/maaasu/AstralRecordApi/blob/master/README.md) — エンドポイント一覧
- `docs/api/` — 各エンドポイントの詳細仕様

### データアクセス原則

- **動的データ**（プレイヤー情報等）および**静的データ**（アイテム定義等）のいずれも、AstralRecord API 経由で取得・操作すること。
- Jetbrains Exposed や YAML ファイルへの直接アクセスは、API では提供できない内部処理（プラグイン固有の初期化・内部キャッシュ更新など）に限定する。
- API の仕様変更・エンドポイント追加が発生した場合は、必ずリポジトリの `README.md` および `docs/api/` を確認してから実装すること。

---

## プロジェクト構造と適用プロンプト

各ディレクトリの役割と、変更を行う際に参照すべきカスタムプロンプトを以下に示す。
**変更・生成作業を行う前に、対象ディレクトリに対応するプロンプトファイルを必ず参照すること。**

| ディレクトリ               | 役割                                      | 参照プロンプト                                |
|:---------------------|:----------------------------------------|:---------------------------------------|
| `database/file`      | 静的データ（YAML）                             | `.github/prompts/file.prompt.md`       |
| `database/sqlserver` | 動的データ（SQL）                              | `.github/prompts/sql.prompt.md`        |
| `src`                | ソースコード（Java / Kotlin）                   | `.github/prompts/code.prompt.md`       |
| `src`（ログ出力）          | ログ出力・LogId・logger.properties 定義         | `.github/prompts/logger.prompt.md`     |
| `src`（プレイヤーメッセージ）    | プレイヤー向けメッセージ・MsgId・player.properties 定義 | `.github/prompts/player_msg.prompt.md` |

---

