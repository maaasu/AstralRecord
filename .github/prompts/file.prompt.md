---
title: 静的データ（YAML）管理プロンプト
description: database/file ディレクトリ配下のYAMLファイル管理に関する指示
version: 1.0.0
lastUpdated: 2026-03-08
---

# 静的データ（YAML）管理プロンプト

> このプロンプトは `database/file/` ディレクトリ配下でのYAMLファイル管理タスク時に参照するカスタムプロンプトです。
> `.github/copilot-instructions.md` の全般ルールと併用してください。

## 対象範囲

| 項目        | 内容               |
|:----------|:-----------------|
| **パス**    | `database/file/` |
| **データ種別** | 静的データ情報          |
| **管理方式**  | YAMLファイル         |

## 概要

本プロンプトは、AstralRecord プロジェクト内で **静的マスタデータ** を YAML 形式で管理・編集する際の指示書です。
プレイヤーに固有ではないアイテムの基本設定、装備の仕様、消費アイテムのパラメータなど、
ゲームデザイン上で定義された不変的なデータを扱います。

---

## 適用対象

YAMLスキーマに基づいた以下のディレクトリ構造下でのファイル操作：

- `database/file/10.features.item/bundle/` — バンドルアイテム定義
- `database/file/10.features.item/consumable/` — 消費アイテム定義
- `database/file/10.features.item/cosmetic/` — コスメティックアイテム定義
- `database/file/10.features.item/currency/` — 通貨定義
- `database/file/10.features.item/equipment-temp/` — 装備アイテム定義
- `database/file/10.features.item/extras/` — 特殊アイテム定義
- `database/file/10.features.item/material/` — 素材定義
- `database/file/10.features.item/quest/` — クエストアイテム定義
- `database/file/70.shared.buff/` — バフ効果定義
- `database/file/80.shared.loot/` — ドロップテーブル定義

---

## 実装ルール

### 1. YAMLスキーマ遵守

- 各カテゴリのスキーマ定義ファイル（`.YAMLスキーマ定義.md`）を必ず確認し、その仕様に従うこと
- スキーマに定義されていないフィールドを追加しないこと
- 必須フィールドは必ず記述すること
- フィールドの順序・構造を定義に合わせること

### 2. ファイル命名規則

- ファイル名は英数字のスネークケース（`snake_case`）を使用すること
- 例: `healing_potion.yml`, `iron_sword.yml`, `mage_kit.yml`

### 3. YAML構文

- インデント: **スペース 2 文字** を使用（タブは不可）
- エンコーディング: UTF-8
- ダブルクォーテーション: 特殊文字を含む文字列のみ囲む

### 4. 値の定義

- データ型を厳密に守ること（数値、文字列、ブール値など）
- 日本語テキストは記述可能だが、コードで参照する識別子は英字のみとすること
- 前後の空白は削除すること

### 5. コメント

- YAMLコメント（`#`）は必要に応じて使用可能
- スキーマ仕様を超える意図や背景を説明する場合は記述すること

---

## 変更時の確認項目

1. **スキーマ確認**: 対応するスキーマ定義ファイルを確認
2. **構文検証**: YAML構文に誤りがないか確認
3. **必須項目**: すべての必須フィールドが記述されているか確認
4. **値の妥当性**: データ型と値の範囲が適切か確認

---

## 変更フロー

1. 対応するスキーマを確認
2. YAMLファイルを修正・作成
3. 変更内容を検証
4. 差分を明示し、修正内容を表示

---

*本プロンプトは `.github/copilot-instructions.md` から参照されます。*
