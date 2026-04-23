---
title: Database Reference Prompt
description: AstralRecord のデータベース定義（テーブル・カラム・インデックスなど）を参照する際の手順と制約を定義します。
---

# Database 仕様参照プロンプト

## 概要

SQL Server テーブル定義・カラム仕様・インデックス・制約など、データベースに関する情報が必要な場合は、
**必ず以下の手順に従って `docs/database/` を参照すること。**

---

## 参照手順

1. `scripts/sync-database-docs.ps1` を実行して `docs/database/` を最新化する

   ```powershell
   powershell -ExecutionPolicy Bypass -File "scripts/sync-database-docs.ps1"
   ```

2. `docs/database/` 内のファイルを参照してテーブル定義・仕様を確認する

3. `docs/database/` にドキュメントが存在しない場合はユーザーに確認すること

---

## 制約・禁止事項

- `temp/` フォルダ内のファイルをデータベース仕様の根拠として使用しないこと。
- スクリプト未実行のまま `docs/database/` が空・または存在しない状態でテーブル定義を推測・捏造しないこと。
- テーブル定義やカラム名は `docs/database/` の内容を正とし、ソースコードからの逆推測で補完しないこと。

---

## 利用シーン例

- SQL Server テーブル定義の確認・新規テーブル追加
- Entity クラス・Repository クラスの生成
- YAML マスタデータと DB テーブルのマッピング確認
- マイグレーションスクリプトの作成

