package io.github.maaasu.astralRecord.infrastructure.logging;

/**
 * ログIDを定義するenum。
 * メッセージID形式: {Type}_{Number}
 * {Type}
 * - I: Info
 * - W: Warning
 * - E: Error
 * - D: Debug
 */
public enum LogId {

    // ==================== infrastructure ====================

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
    /**
     * ディレクトリが存在しないか、読み取り権限がありません: %s
     */
    E_1001(1001),
    /**
     * YAMLファイルのセーブに失敗しました: %s
     */
    E_1002(1002),
    // endregion

    // ==================== core ====================

    // region /core/event/AbstractEventHandler.java
    /**
     * イベントハンドラーが初期化されました: %s
     */
    I_3000(3000),
    /**
     * イベントハンドラーのクリーンアップを実行しました: %s
     */
    I_3001(3001),
    // endregion

    // region /core/event/EventManager.java
    /**
     * イベントマネージャーを初期化しています
     */
    I_3050(3050),
    /**
     * イベントハンドラーを登録しました。登録数: %d
     */
    I_3051(3051),
    /**
     * イベントマネージャーをシャットダウンしています
     */
    I_3052(3052),
    /**
     * イベントマネージャーのシャットダウンが完了しました
     */
    I_3053(3053),
    /**
     * イベントハンドラーが無効のためスキップしました: %s
     */
    W_3000(3000),
    /**
     * イベントハンドラーの登録に失敗しました: %s
     */
    E_3000(3000),
    /**
     * イベントハンドラーのクリーンアップに失敗しました: %s
     */
    E_3001(3001),
    /**
     * イベントハンドラーを登録しました: %s
     */
    D_3000(3000),
    // endregion

    // region /database/sqlserver/SqlServerManager.java
    /**
     * SQL Serverへの接続を初期化しています
     */
    I_1100(1100),
    /**
     * SQL Serverへの接続テストに成功しました
     */
    I_1101(1101),
    /**
     * SQL Serverが正常に初期化されました
     */
    I_1102(1102),
    /**
     * SQL Serverが正常にシャットダウンされました
     */
    I_1103(1103),
    /**
     * SQL Serverの接続テストに失敗しました: %s
     */
    E_1100(1100),
    /**
     * SQL Serverが初期化されていません
     */
    E_1101(1101),
    // endregion

    // region /database/file/yaml/YamlSnapshotRepository.kt
    /**
     * スナップショットの取得に失敗しました: %s
     */
    E_1200(1200),
    /**
     * スナップショットの保存に失敗しました: %s
     */
    E_1201(1201),
    /**
     * スナップショットの削除に失敗しました: %s
     */
    E_1202(1202),
    /**
     * 全てのスナップショットの取得に失敗しました
     */
    E_1203(1203),
    // endregion

    // region /database/file/yaml/YamlDiffService.java
    /**
     * 初回ロードのためスナップショットを作成しました: %s
     */
    D_1300(1300),
    /**
     * 前回から変更はありません: %s
     */
    D_1301(1301),
    /**
     * YAMLの差分検出を開始します: %s
     */
    I_1300(1300),
    /**
     * YAMLの差分検出が終了しました
     */
    I_1301(1301),
    /**
     * [追加] %s: %s
     */
    I_1302(1302),
    /**
     * [削除] %s: %s
     */
    I_1303(1303),
    /**
     * [変更] %s: %s -> %s
     */
    I_1304(1304),
    /**
     * ハッシュ計算に失敗しました
     */
    E_1300(1300),
    // endregion

    // region /database/file/yaml/config/YamlDbConfigUtil.kt
    /**
     * FileDatabaseのルートディレクトリの取得に失敗しました
     */
    W_1400(1400),
    /**
     * YAMLデータベースの設定ファイルが見つかりません: %s
     */
    W_1401(1401),
    /**
     * YAMLデータベースの設定がロードされました
     */
    I_1400(1400),
    /**
     * YAMLデータベースの設定のパースに失敗しました: %s
     */
    E_1400(1400),
    // endregion

    // region /infrastructure/command/CommandManager.java
    /**
     * CommandManagerはすでに初期化されています
     */
    W_1500(1500),
    /**
     * 初期化後にコマンドを登録しようとしました: %s
     */
    W_1501(1501),
    /**
     * CommandManagerを初期化しています
     */
    I_1500(1500),
    /**
     * CommandManagerが初期化されました。登録コマンド数: %d
     */
    I_1501(1501),
    /**
     * CommandManagerをシャットダウンしています
     */
    I_1502(1502),
    /**
     * CommandManagerのシャットダウンが完了しました
     */
    I_1503(1503),
    /**
     * コマンドを登録しました: %s
     */
    D_1500(1500),
    /**
     * コマンドをBrigadierに登録しました: %s
     */
    D_1501(1501),
    /**
     * コマンドの登録中にエラーが発生しました: %s
     */
    E_1500(1500),
    // endregion

    // region /infrastructure/command/ReloadCommand.java
    /**
     * リロードコマンドを実行しました。実行者: %s
     */
    I_1550(1550),
    /**
     * PlugMan を検出しました。PlugMan でリロードを実行します
     */
    I_1551(1551),
    /**
     * PlugMan が見つかりません。内部リロードを実行します
     */
    I_1552(1552),
    /**
     * 内部リロードが完了しました
     */
    I_1553(1553),
    /**
     * リロード中にエラーが発生しました: %s
     */
    E_1550(1550),
    // endregion

    // ==================== feature ====================

    // region /feature/user/event/UserLoginEventHandler.java
    /**
     * ユーザーのログイン前処理に失敗しました: %s
     */
    E_5000(5000),
    // endregion

    // region /feature/user/service/UserService.java
    /**
     * 新規ユーザーを登録しました: %s (%s)
     */
    I_5050(5050),
    // endregion

    // region /feature/player/event/PlayerJoinEventHandler.java
    /**
     * プレイヤーにOP権限を付与しました: %s (permission=%d)
     */
    I_5070(5070),
    /**
     * AstPlayerのキャッシュが見つかりませんでした。プレイヤー: %s
     */
    W_5070(5070),
    /**
     * プレイヤーログイン処理に失敗しました: %s
     */
    E_5070(5070),
    // endregion

    // region /feature/account/service/AccountService.java
    /**
     * アカウントを作成しました: %s (slot=%d, user=%s)
     */
    I_5100(5100),
    /**
     * アカウントを切り替えました: %s (user=%s)
     */
    I_5101(5101),
    // endregion

    ;
    private final String id;

    /**
     * コンストラクタ。Enum名から接頭辞（最初のアンダースコアまで）を抽出し、番号と結合します。
     * @param number メッセージIDの番号部分
     */
    LogId(int number) {
        String name = this.name();
        String prefix = name.substring(0, name.indexOf('_') + 1);
        this.id = prefix + number;
    }

    /**
     * IDを取得します。
     * @return メッセージID
     */
    public String getId() {
        return id;
    }
}
