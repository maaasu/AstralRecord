package io.github.maaasu.astralRecord.feature.player;

/**
 * プレイヤー向けメッセージIDを定義する enum。
 * メッセージID形式: {Type}_{Number}
 * <p>
 * {Type}
 * - P: Player（プレイヤー向け汎用メッセージ）
 * <p>
 * メッセージ文言は {@code player.properties} で管理します。
 * <p>
 * 使用例:
 * <pre>
 * // パラメータあり
 * AstPlayer.sendMessage(PlayerMsgId.P_5000, player.getName());
 * </pre>
 */
public enum PlayerMsgId {

    // ==================== feature ====================

    // region /feature/player/model/AstPlayer.kt 5000-5049
    /**
     * AstralRecord へようこそ、%s さん！
     */
    P_5000(5000),
    // endregion

    // region /feature/player/command/TestCommand.java 5050-5059
    /**
     * [TEST] プレイヤー情報: 名前={0}, UUID={1}
     */
    P_5050(5050),
    /**
     * [TEST] ユーザー権限レベル: {0}
     */
    P_5051(5051),
    // endregion

    // region /feature/player/command/TempCommand.java 5080-5089
    /**
     * [TEMP] テンポラリコマンドを実行しました。実行者: {0}
     */
    P_5080(5080),
    // endregion

    // region /feature/player/event/PlayerJoinEventHandler.java 5070-5079
    /**
     * あなたはpermissionレベル{0}の管理者です。OP権限が付与されました。
     */
    P_5070(5070),
    // endregion

    // region /infrastructure/command/AstCommand.java 5060-5069
    /**
     * このコマンドはプレイヤーのみ使用できます。
     */
    P_5060(5060),
    /**
     * このコマンドを実行する権限がありません。
     */
    P_5061(5061),
    /**
     * コマンドの実行中にエラーが発生しました: {0}
     */
    P_5062(5062),
    /**
     * 引数が不足しています。
     */
    P_5063(5063),
    /**
     * 使用方法: {0}
     */
    P_5064(5064),
    // endregion

    // region /infrastructure/command/ReloadCommand.java 5090-5099
    /**
     * リロードを開始します...
     */
    P_5090(5090),
    /**
     * PlugMan を使用してリロードします
     */
    P_5091(5091),
    /**
     * PlugMan が見つかりません。内部リロードを実行します
     */
    P_5092(5092),
    /**
     * 内部リロードが完了しました（設定・YAMLデータ）
     */
    P_5093(5093),
    /**
     * リロード中にエラーが発生しました: {0}
     */
    P_5094(5094),
    // endregion

    ;

    private final String id;

    /**
     * コンストラクタ。Enum名から接頭辞（最初のアンダースコアまで）を抽出し、番号と結合します。
     *
     * @param number メッセージIDの番号部分
     */
    PlayerMsgId(int number) {
        String name = this.name();
        String prefix = name.substring(0, name.indexOf('_') + 1);
        this.id = prefix + number;
    }

    /**
     * IDを取得します。
     *
     * @return メッセージID
     */
    public String getId() {
        return id;
    }
}

