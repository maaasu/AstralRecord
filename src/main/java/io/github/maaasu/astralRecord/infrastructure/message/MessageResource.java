package io.github.maaasu.astralRecord.infrastructure.message;

import io.github.maaasu.astralRecord.infrastructure.util.ColorCodeUtil;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * リソースバンドルからメッセージを取得するためのユーティリティクラス。
 */
public class MessageResource {
    private static final String BUNDLE_NAME = "messages";
    private static ResourceBundle resourceBundle;

    static {
        loadBundle(Locale.getDefault());
    }

    /**
     * 指定されたロケールでリソースバンドルをロードします。
     * @param locale ロケール
     */
    public static void loadBundle(Locale locale) {
        try {
            resourceBundle = ResourceBundle.getBundle(BUNDLE_NAME, locale);
        } catch (MissingResourceException e) {
            // デフォルトのロケールで再試行
            resourceBundle = ResourceBundle.getBundle(BUNDLE_NAME, Locale.ENGLISH);
        }
    }

    /**
     * キーに対応するメッセージを取得します。
     * カラーコード（&）は自動的に変換されます。
     *
     * @param key メッセージキー
     * @return メッセージ文字列（見つからない場合はキー自体）
     */
    public static String getMessage(String key) {
        try {
            String message = resourceBundle.getString(key);
            return ColorCodeUtil.translateAlternateColorCodes(message);
        } catch (MissingResourceException e) {
            return key;
        }
    }

    /**
     * キーに対応するメッセージを取得し、引数を適用してフォーマットします。
     *
     * @param key メッセージキー
     * @param args フォーマット引数
     * @return フォーマットされたメッセージ文字列
     */
    public static String format(String key, Object... args) {
        String message = getMessage(key);
        if (args == null || args.length == 0) {
            return message;
        }
        try {
            return String.format(message, args);
        } catch (Exception e) {
            return message + " (format-error: " + e.getMessage() + ")";
        }
    }
}
