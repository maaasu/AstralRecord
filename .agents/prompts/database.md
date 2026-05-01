# Plugin Database Prompt

## 読むタイミング

- Plugin 側の DB アクセスコードを追加・変更するとき
- DB 契約に依存する機能を作るとき
- DB スキーマ変更を伴う作業をするとき

## 確認対象

- repository の入出力モデル
- API 契約
- `docs/database/` がある場合はその説明資料
- `Database` リポジトリ側のテーブル定義と `sqlserver/init.sql`

## 必須ルール

- DB スキーマ前提のコードを書く前に、定義書と実装が一致しているか確認する。
- テーブルやカラムの変更を伴う場合は `Database` 側の更新漏れを疑う。
- API と Plugin の契約変更は片側だけで終わらせない。

## 非推奨

- 古い契約を前提に repository や DTO を増やすこと
- Database 側の定義を確認せずに DB 名称を決め打ちすること
