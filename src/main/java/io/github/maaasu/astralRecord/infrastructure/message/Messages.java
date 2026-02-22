package io.github.maaasu.astralRecord.infrastructure.message;

/**
 * アプリケーション全体で使用するメッセージを一元管理するenum。
 * - ログメッセージ
 * - エラーメッセージ
 * - ユーザー向けメッセージ
 * <p>
 * - I Infoメッセージ
 * - W Warningメッセージ
 * - E Errorメッセージ
 * <p>
 * 使用例:
 * <pre>
 * // パラメータなし
 * LoggerUtil.info(Messages.I_1000.getMessage());
 *
 * // パラメータあり
 * LoggerUtil.info(Messages.I_1000.format(folderPath));
 * </pre>
 */
public enum Messages {

    // ==================== システムメッセージ ====================
    I_1000(1000);

    private final int id;

    Messages(int id) {
        this.id = id;
    }

    /**
     * IDを取得します。
     * @return メッセージID
     */
    public int getId() {
        return id;
    }

    /**
     * メッセージを取得します。
     *
     * @return メッセージ文字列
     */
    public String getMessage() {
        return MessageResource.getMessage(String.valueOf(id));
    }

    /**
     * パラメータを適用してフォーマットされたメッセージを取得します。
     *
     * @param args フォーマット引数
     * @return フォーマットされたメッセージ
     */
    public String format(Object... args) {
        return MessageResource.format(String.valueOf(id), args);
    }

    /**
     * メッセージを文字列として取得します（toString のオーバーライド）
     *
     * @return メッセージ文字列
     */
    @Override
    public String toString() {
        return getMessage();
    }
}
