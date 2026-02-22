package io.github.maaasu.astralRecord.infrastructure.logging;

import io.github.maaasu.astralRecord.AstralRecord;
import io.github.maaasu.astralRecord.infrastructure.config.ConfigProperties;
import io.github.maaasu.astralRecord.infrastructure.message.Messages;

import java.util.logging.Level;

public final class Logger {
    // ANSIエスケープコード（色設定用）
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_CYAN = "\u001B[96m";
    private static final String ANSI_GRAY = "\u001B[90m";

    // Prefix
    private static final String INFO_PREFIX = "[INFO] ";
    private static final String WARN_PREFIX = "[WARN] ";
    private static final String ERROR_PREFIX = "[ERROR] ";
    private static final String DEBUG_PREFIX = "[DEBUG] ";

    private Logger() {
        // utility
    }

    public static void info(String message, Object... args) {
        log(Level.INFO, INFO_PREFIX + message, null, args);
    }

    public static void info(Messages message, Object... args) {
        info(message.format(args));
    }

    public static void warn(String message, Object... args) {
        log(Level.WARNING, WARN_PREFIX + message, null, args);
    }

    public static void warn(Messages message, Object... args) {
        warn(message.format(args));
    }

    public static void error(String message, Throwable t, Object... args) {
        log(Level.SEVERE, ERROR_PREFIX + message, t, args);
    }

    public static void error(Messages message, Throwable t, Object... args) {
        error(message.format(args), t);
    }

    public static void debug(String message, Object... args) {
        if (ConfigProperties.getInstance().isPluginDebugMode()) {
            log(Level.INFO, DEBUG_PREFIX + message, null, args);
        }
    }

    public static void debug(Messages message, Object... args) {
        if (ConfigProperties.getInstance().isPluginDebugMode()) {
            debug(message.format(args));
        }
    }

    private static void log(Level level, String message, Throwable t, Object... args) {
        java.util.logging.Logger logger = AstralRecord.getInstance().getLogger();
        String formatted = safeFormat(message, args);
        String coloredMessage = addColor(level, formatted);

        if (t == null) {
            logger.log(level, coloredMessage);
        } else {
            logger.log(level, coloredMessage, t);
        }
    }

    private static String addColor(Level level, String message) {
        String colorCode;

        if (level == Level.SEVERE) {
            colorCode = ANSI_RED;
        } else if (level == Level.WARNING) {
            colorCode = ANSI_YELLOW;
        } else if (level == Level.FINE || level == Level.FINER || level == Level.FINEST) {
            colorCode = ANSI_GRAY;
        } else if (message.startsWith(ANSI_GRAY) || message.startsWith(DEBUG_PREFIX)) {
            // デバッグメッセージの場合はグレー
            colorCode = ANSI_GRAY;
        } else {
            colorCode = ANSI_CYAN;
        }

        return colorCode + message + ANSI_RESET;
    }

    private static String safeFormat(String pattern, Object... args) {
        try {
            return (args == null || args.length == 0) ? pattern : String.format(pattern, args);
        } catch (Exception e) {
            // フォーマット失敗時は生メッセージと例外情報を返す
            return pattern + " (format-error: " + e.getMessage() + ")";
        }
    }
}
