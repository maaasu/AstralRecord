package io.github.maaasu.astralRecord.infrastructure.database.sqlserver;

import io.github.maaasu.astralRecord.infrastructure.config.ConfigProperties;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class SqlServerUtil {
    private SqlServerUtil() {}

    /**
     * リソースを安全にクローズします。
     */
    public static void closeQuietly(AutoCloseable... resources) {
        for (AutoCloseable r : resources) {
            if (r == null) continue;
            try {
                r.close();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * データベース接続を取得します。
     * 
     * @return Connection オブジェクト
     * @throws SQLException 接続に失敗した場合
     */
    public static Connection getConnection() throws SQLException {
        ConfigProperties config = ConfigProperties.getInstance();
        String connectionString = config.buildSqlServerConnectionString();
        return DriverManager.getConnection(connectionString);
    }

    /**
     * SELECTクエリを実行し、結果をMapのリストとして取得します。
     * 
     * @param sql 実行するSQLクエリ
     * @param params パラメータ（可変長引数）
     * @return 結果セットのリスト（各行がMap<カラム名, 値>）
     * @throws SQLException クエリ実行に失敗した場合
     */
    public static List<Map<String, Object>> executeQuery(String sql, Object... params) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            setParameters(stmt, params);
            rs = stmt.executeQuery();

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }
                results.add(row);
            }
        } finally {
            closeQuietly(rs, stmt, conn);
        }

        return results;
    }

    /**
     * SELECTクエリを実行し、各行をConsumerで処理します。
     * 大量のデータを扱う場合に、メモリ効率的に処理できます。
     * 
     * @param sql 実行するSQLクエリ
     * @param rowConsumer 各行を処理するConsumer
     * @param params パラメータ（可変長引数）
     * @throws SQLException クエリ実行に失敗した場合
     */
    public static void executeQuery(String sql, Consumer<Map<String, Object>> rowConsumer, Object... params) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            setParameters(stmt, params);
            rs = stmt.executeQuery();

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }
                rowConsumer.accept(row);
            }
        } finally {
            closeQuietly(rs, stmt, conn);
        }
    }

    /**
     * INSERT、UPDATE、DELETEなどの更新系クエリを実行します。
     * 
     * @param sql 実行するSQLクエリ
     * @param params パラメータ（可変長引数）
     * @return 影響を受けた行数
     * @throws SQLException クエリ実行に失敗した場合
     */
    public static int executeUpdate(String sql, Object... params) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            setParameters(stmt, params);
            return stmt.executeUpdate();
        } finally {
            closeQuietly(stmt, conn);
        }
    }

    /**
     * トランザクション内で複数の更新を実行します。
     * すべての処理が成功した場合のみコミットされます。
     * 
     * @param transaction トランザクション内で実行する処理
     * @throws SQLException トランザクション実行に失敗した場合
     */
    public static void executeTransaction(TransactionCallback transaction) throws SQLException {
        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);
            
            transaction.execute(conn);
            
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ignored) {
                }
            }
            closeQuietly(conn);
        }
    }

    /**
     * PreparedStatementにパラメータを設定します。
     */
    private static void setParameters(PreparedStatement stmt, Object... params) throws SQLException {
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
        }
    }

    /**
     * トランザクション処理用の関数型インターフェース
     */
    @FunctionalInterface
    public interface TransactionCallback {
        void execute(Connection conn) throws SQLException;
    }
}



