# AstralRecord Plugin Guide

対象: `AstralRecord/src/main/java/io/github/maaasu/astralRecord/`

## 役割

- Minecraft サーバープラグイン本体を管理する。
- ゲームロジック、プレイヤー体験、Plugin 内部の機能連携を担当する。

## ディレクトリ方針

- `core/`
  - コマンド・イベント登録の起点だけを置く。
- `feature/<feature>/`
  - 機能単位の実装を置く。
- `infrastructure/`
  - 横断的な技術要素だけを置く。ゲームロジックは持たせない。
- `src/main/resources/`
  - `plugin.yml`、`config.yml`、properties 類などのリソースを置く。

## 実装方針

- ビジネスロジックは Java を優先する。
- Kotlin は次の用途で積極的に使ってよい。
  - `data class` を使う Model
  - Repository 実装
  - GUI / View
- 新規追加時は、既存ファイルの言語と責務分離を崩さない。
- Bukkit/Paper API のスレッド制約を守る。
- プレイヤー表現は原則 `AstPlayer` を使う。
- `org.bukkit.entity.Player` を直接引き回すのは最小限にする。

## 変更時の必須確認

- 機能追加は `feature/` 配下に閉じているか。
- DB アクセスが repository 層に閉じているか。
- ログ、プレイヤーメッセージ、設定値の扱いが既存ルールに合っているか。
- API や DB 契約が変わるなら関連プロジェクトの更新も必要か。

## 補助プロンプト

- `.agents/prompts/code.md`
  - Plugin 全般のコード規約、言語選定、構造ルールを扱う。
- `.agents/prompts/logger.md`
  - `LogId` や `logger.properties` に関わる変更で読む。
- `.agents/prompts/player_msg.md`
  - `MsgId` や `player.properties` に関わる変更で読む。
- `.agents/prompts/database.md`
  - DB 契約や DB ドキュメントに関わる変更で読む。

## AI 作業手順（必須）

- 作業開始時に必ず `AGENTS.md`（本ファイル）を最初に読む。
- 変更内容に応じて、対応する `.agents/prompts/*.md` を作業前に読む。
  - ログ変更は `.agents/prompts/logger.md` を必ず読む。
- ファイル読み取り時は UTF-8 を優先し、文字化けがある場合はエンコーディングを切り替えて再読する。
- 文字化けした状態のまま実装を進めない。
