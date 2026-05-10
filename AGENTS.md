# AstralRecord Plugin — AI Agent Guide

このファイルは **AI エージェント専用** の作業手順書です。
プロジェクトルール（言語選定 / ディレクトリ構成 / コーディング規約 / JavaDoc 規約 など、人間・AI 問わず編集者全員に必要な情報）は [README.md](README.md) を参照してください。

## 必須手順

1. **コードを変更する作業を行う前に、[README.md](README.md) を必ず読む**。プロジェクトの構造・規約・データアクセス方針はそちらに記載されている。
2. 変更内容に応じて、下表の補助プロンプトのうち **該当するものだけ** を追加で読む（不要な補助プロンプトは開かない）。
3. ファイル読み取りは UTF-8 を優先する。文字化けがある場合は他のエンコーディングで再読し、文字化けのまま実装を進めない。

## 補助プロンプト（変更内容に該当するものだけ読む）

| 変更内容 | 読むファイル |
|:--|:--|
| コード追加・修正全般、配置先・言語選定の判断 | [.agents/prompts/code.md](.agents/prompts/code.md) |
| ログ出力 / `LogId` / `logger.properties` の追加・変更 | [.agents/prompts/logger.md](.agents/prompts/logger.md) |
| プレイヤー向けメッセージ / `MsgId` / `player.properties` の追加・変更 | [.agents/prompts/player_msg.md](.agents/prompts/player_msg.md) |
| DB 契約・スキーマに依存する実装、API/Database リポジトリと連動する変更 | [.agents/prompts/database.md](.agents/prompts/database.md) |

該当しない領域の補助プロンプトはトークン節約のため開かない。複数領域にまたがる変更の場合のみ、関係するファイルを必要分だけ追加で読む。

## このファイルに書かないもの

以下の情報は AI エージェント専用ではないため、すべて [README.md](README.md) または該当する補助プロンプトに置く。AGENTS.md に重複記載しない。

- 技術スタック・ディレクトリ構成・データ管理方針
- 言語選定ルール（Java / Kotlin の使い分け）
- コーディング規約（パッケージ配置、ハードコーディング禁止、`AstPlayer` 利用など）
- JavaDoc / KDoc の記載ルール
- ステータスシステム等のドメイン仕様
