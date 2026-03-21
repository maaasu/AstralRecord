---
description: AstralRecord向けのプレイヤーメッセージ定義・出力規約
---

# AstralRecord — プレイヤーメッセージ規約プロンプト

> このプロンプトはプレイヤー向けメッセージの定義・`MsgId` 登録・`player.properties` 定義タスク時に参照するカスタムプロンプトです。
> `.github/copilot-instructions.md` および `code.prompt.md` の全般ルールと併用してください。

---

## プレイヤーメッセージ出力の基本方針

- プレイヤー向けのメッセージ出力にはすべて `PlayerMsgResource` クラスを使用すること。
- メッセージ文字列はソースコードにハードコーディングせず、**必ず `player.properties` に定義**すること。
- カラーコード（`&` 記号）は `player.properties` 内に記述し、`PlayerMsgResource` が自動的に `§` へ変換する。
- プレイヤーへの送信には `player.sendMessage(...)` を使用すること。

---

## 使用クラスと役割

| クラス名                | 役割                                                                                               |
|:--------------------|:-------------------------------------------------------------------------------------------------|
| `PlayerMsgResource` | `player.properties` からメッセージを取得するユーティリティ。`getMessage` / `format` メソッドを提供。                         |
| `MsgId`             | メッセージIDを定義する定数クラス（または enum）。`player.properties` のキーと対応。                                          |
| `ColorCodeUtil`     | Minecraftカラーコード（`&` → `§`）の変換ユーティリティ。`PlayerMsgResource` の内部で自動的に使用される。                          |
| `MessageFormatUtil` | `{0}`, `{1}` 形式のプレースホルダーを `MessageFormat` でフォーマットするユーティリティ。`PlayerMsgResource.format` の内部で使用される。 |

---

## プレイヤーメッセージ出力の使い方

### 基本パターン（MsgId 指定）

```java
// 引数なし
player.sendMessage(PlayerMsgResource.getMessage(MsgId.P_XXXX));

// 引数あり（{0}, {1} ... のプレースホルダーをフォーマット）
player.sendMessage(PlayerMsgResource.format(MsgId.P_XXXX, arg1, arg2));
```

### 実装例

```java
// ウェルカムメッセージ（player.properties: P_5000=&aAstralRecord&r へようこそ、&e{0}&r さん！）
player.sendMessage(PlayerMsgResource.format(MsgId.P_5000, player.getName()));
```

> `PlayerMsgResource.format` は `getMessage` でカラーコードを変換した後に `MessageFormatUtil.format` でフォーマットします。
> プレースホルダーは `{0}`, `{1}`, `{2}` ... の形式（`MessageFormat` 準拠）を使用すること。

---

## MsgId の定義ルール

### ファイル: `feature/player/MsgId.java`

新しいプレイヤーメッセージを追加する場合は、必ず `MsgId` クラスに定数として定義すること。

#### フォーマット

```
{Prefix}_{Number}
```

| プレフィックス | 対象                     |
|:--------|:-----------------------|
| `P_`    | プレイヤー向け汎用メッセージ（Player） |

#### 番号範囲（レイヤ別割り当て）

| 番号範囲        | 割り当てレイヤ        |
|:------------|:---------------|
| `5000-8999` | `feature`      |
| `9000-9999` | `temp`（暫定・開発用） |

> 番号は `feature` レイヤ（`5000-8999`）を使用すること。機能ごとに50番ブロック単位で採番する。

#### ブロック採番ルール

- 同一クラス（機能）内では **50番ごと**にブロックを確保すること（例: `5000-5049`, `5050-5099`）。
- 新規クラス（機能）を追加する際は、同一レイヤ内の末尾から50番ブロックを採番すること。
- 既存 ID の番号は**変更禁止**。廃止する場合はコメントアウトし、削除はしないこと。

#### 実装例

```java
public final class MsgId {
    private MsgId() {}

    // region /feature/player/AstPlayer.kt 5000-5049
    /** AstralRecord へようこそ、{0} さん！ */
    public static final String P_5000 = "P_5000";
    // endregion
}
```

> - `region` コメントには対応するソースファイルのパスを記載し、可読性を維持すること。
> - Javadoc（または行コメント）にはメッセージの内容を記載すること。

---

## player.properties の定義ルール

### ファイル: `src/main/resources/player.properties`

`MsgId` に追加した ID に対応するメッセージを必ずここに定義すること。

#### キー書式

```properties
{Prefix}_{Number}=メッセージ本文
```

フォーマット引数には `MessageFormat` 書式（`{0}`, `{1}` など）を使用すること。

#### カラーコードの記述

カラーコードは `&` 記号で記述すること（`PlayerMsgResource.getMessage` が `§` へ自動変換する）。

```properties
P_5000=&aAstralRecord&r へようこそ、&e{0}&r さん！
```

#### セクション構成

ファイルはレイヤ・クラス単位でコメントブロックを使ってグループ化すること。

```properties
# {レイヤ名} {番号範囲}
## /{パッケージ相対パス}/{クラス名}.kt {番号範囲}
{Prefix}_{Number}=メッセージ本文
```

#### 既存セクション定義

```properties
# feature 5000-8999
## /feature/player/AstPlayer.kt 5000-5049
P_5000=&aAstralRecord&r へようこそ、&e{0}&r さん！
```

> `player.properties` は **UTF-8（BOM なし）** で保存・記述すること。日本語はそのままUTF-8で直接記述し、Unicodeエスケープ（`\uXXXX`）は使用しないこと。
> エディタ（VS Code / IntelliJ など）で保存する際は必ず **「UTF-8」（BOM なし）** を選択すること。**「UTF-8 with BOM」は使用禁止**。BOM（`EF BB BF`）が付与されると `PropertyResourceBundle` の先頭キーが破損し、文字化けや読み込み失敗の原因となる。

---

## 実装手順チェックリスト

新しいプレイヤーメッセージを追加する場合、以下の順序で作業すること。

1. `player.properties` に `{Prefix}_{Number}=メッセージ` を追加する
2. `MsgId.java` の適切なセクションに定数を追加する（Javadoc・region コメント付き）
3. ソースコード内で `PlayerMsgResource.getMessage(MsgId.P_XXXX)` または `PlayerMsgResource.format(MsgId.P_XXXX, args...)` を呼び出す
4. `player.sendMessage(...)` でプレイヤーに送信する

