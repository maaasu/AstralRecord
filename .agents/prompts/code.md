# Plugin Code Prompt

## 読むタイミング

- Plugin のコードを追加・修正するとき
- どのパッケージに置くべきか迷うとき
- Java と Kotlin の使い分けを判断するとき

## 必須ルール

- `core/` は登録起点だけを持つ。
- 機能実装は `feature/<feature>/` に置く。
- 横断処理は `infrastructure/` に置く。
- ゲームロジックを `infrastructure/` に入れない。

## 言語選定

- 基本は Java。
- 次の用途では Kotlin を選んでよい。
  - `data class` を使う Model
  - Repository
  - GUI / View
- 既存コードの流儀に逆らって言語を混在させない。

## 実装チェック

1. 責務に合ったパッケージへ配置しているか。
2. Bukkit/Paper API のスレッド制約を守っているか。
3. プレイヤー表現に `AstPlayer` を使えているか。
4. ログや設定値の扱いが既存ルールに沿っているか。
5. DB アクセスが repository に閉じているか。

## 非推奨

- `System.out.println` の使用
- `org.bukkit.entity.Player` の広範囲な直接利用
- 新機能を既存の無関係パッケージへ混ぜ込むこと
