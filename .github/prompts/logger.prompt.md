---
description: AstralRecord向けのログ出力
---

# AstralRecord — ログ出力規約プロンプト

> このプロンプトはログ出力・`LogId` 登録・`logger.properties` 定義タスク時に参照するカスタムプロンプトです。
> `.github/copilot-instructions.md` および `code.prompt.md` の全般ルールと併用してください。

---

## ログ出力の基本方針

- ログ出力にはすべて `infrastructure/logging/Logger` クラスを使用すること。
- `System.out.println` / `println` / `java.util.logging.Logger` の直接使用は**禁止**。
- ログメッセージ文字列はハードコーディングせず、**必ず `logger.properties` に定義**し、`LogId` 経由で使用すること。

---

## 使用クラスと役割

| クラス名                 | 役割                                                                |
|:---------------------|:------------------------------------------------------------------|
| `Logger`             | ログ出力のエントリポイント。`info / warn / error / debug / log` メソッドを提供。        |
| `LogId`              | ログメッセージIDを定義する enum。`logger.properties` のキーと対応。                   |
| `LogMessageProvider` | `logger.properties` からメッセージを取得し、`String.format` でフォーマットするユーティリティ。 |

---

## ログ出力の使い方

### 基本パターン（LogId 指定）

```java
// レベルを自動判定して出力（推奨）
Logger.log(LogId.W_1000, filePath);

// レベルを明示して出力
Logger.info(LogId.I_XXXX, arg1);
Logger.warn(LogId.W_XXXX, arg1);
Logger.error(LogId.E_XXXX, exception, arg1);
Logger.debug(LogId.D_XXXX, arg1);
```

### 例外を伴うエラー出力

```java
try {
    // ...
} catch (Exception e) {
    Logger.log(LogId.E_XXXX, e, filePath);
}
```

> `Logger.log(LogId, Throwable, Object...)` は ID のプレフィックスが `E_` の場合のみ例外スタックトレースを出力します。

### 文字列直書き（LogId が不要な場合）

一時的な開発用デバッグ出力に限り、文字列直書きを許容します。ただし本番コードには残さないこと。

```java
Logger.debug("一時デバッグ: value = %s", value);
```

---

## LogId の定義ルール

### ファイル: `infrastructure/logging/LogId.java`

新しいログメッセージを追加する場合は、必ず `LogId` enum に定義すること。

#### フォーマット

```
{Type}_{Number}
```

| プレフィックス | レベル   | 対応 Logger メソッド |
|:--------|:------|:---------------|
| `I_`    | Info  | `Logger.info`  |
| `W_`    | Warn  | `Logger.warn`  |
| `E_`    | Error | `Logger.error` |
| `D_`    | Debug | `Logger.debug` |

#### 番号範囲（レイヤ別割り当て）

| 番号範囲        | 割り当てレイヤ          |
|:------------|:-----------------|
| `1000-2999` | `infrastructure` |
| `3000-4999` | `core`           |
| `5000-8999` | `feature`        |
| `9000-9999` | `temp`（暫定・開発用）   |

#### 実装例

```java
// region /util/YamlLoaderUtil.java
/**
 * 指定されたYAMLファイルが見つかりませんでした: %s
 */
W_1000(1000),
/**
 * YAMLファイルの読み込みに失敗しました: %s
 */
E_1000(1000),
/**
 * YAMLファイルが正常に読み込まれました: %s
 */
D_1000(1000),
// endregion
```

> - 同一番号でも `I_` / `W_` / `E_` / `D_` のプレフィックスが異なれば、別キーとして `logger.properties` に登録される。
> - `region` コメントには対応するソースファイルのパスを記載し、可読性を維持すること。
> - Javadoc にはメッセージ内容を記載すること。

---

## logger.properties の定義ルール

### ファイル: `src/main/resources/logger.properties`

`LogId` に追加した ID に対応するメッセージを必ずここに定義すること。

#### キー書式

```properties
{Type}_{Number}=メッセージ本文
```

フォーマット引数には Java の `String.format` 書式（`%s`, `%d` など）を使用すること。

#### セクション構成

ファイルはレイヤ・クラス単位でコメントブロックを使ってグループ化すること。

```properties
# {レイヤ名} {番号範囲}
## /{パッケージ相対パス}/{クラス名}.java {番号範囲}
{Type}_{Number}=メッセージ本文
```

#### 既存セクション定義

```properties
# infrastructure 1000-2999
## /util/YamlLoaderUtil.java 1000-1049
W_1000=指定されたYAMLファイルが見つかりませんでした: %s
E_1000=YAMLファイルの読み込みに失敗しました: %s
E_1001=ディレクトリが存在しないか、読み取り権限がありません: %s
D_1000=YAMLファイルが正常に読み込まれました: %s

# core 3000-4999

# feature 5000-8999

# temp 9000-9999
```

> `logger.properties` は **UTF-8（BOM なし）** で保存・記述すること。日本語はそのままUTF-8で直接記述し、Unicodeエスケープ（`\uXXXX`）は使用しないこと。
> エディタ（VS Code / IntelliJ など）で保存する際は必ず **「UTF-8」（BOM なし）** を選択すること。**「UTF-8 with BOM」は使用禁止**。BOM（`EF BB BF`）が付与されると `PropertyResourceBundle` の先頭キーが破損し、文字化けや読み込み失敗の原因となる。

#### 命名・運用ルール

- 同一クラス内では **50番ごと**に番号ブロックを確保すること（例: 1000-1049, 1050-1099）。
- 新規クラスを追加する際は、同一レイヤ内の末尾から50番ブロックを採番すること。
- 既存 ID の番号は**変更禁止**。廃止する場合はコメントアウトし、削除はしないこと。

---

## 実装手順チェックリスト

新しいログ出力を追加する場合、以下の順序で作業すること。

1. `logger.properties` に `{Type}_{Number}=メッセージ` を追加する
2. `LogId.java` の適切なセクションに enum 定数を追加する（Javadoc・region コメント付き）
3. ソースコード内で `Logger.log(LogId.XXXX, args...)` を呼び出す

