package io.github.maaasu.astralRecord.infrastructure.database.sqlserver;

import java.sql.Connection;
import java.sql.SQLException;

public class SqlServerManager {
    private static SqlServerManager instance;
    private ConnectionPool pool;
    private boolean initialized = false;

    private SqlServerManager() {}

    public static synchronized SqlServerManager getInstance() {
        if (instance == null) {
            instance = new SqlServerManager();
        }
        return instance;
    }

    public synchronized void initialize() {
        if (initialized) return;

        SqlServerConfig config = new SqlServerConfig();
        this.pool = new ConnectionPool(config);

        // simple connection test
        //LoggerUtil.info(Messages.DB_INITIALIZING.getMessage());
        try (Connection ignored = pool.getConnection()) {
            //LoggerUtil.info(Messages.DB_CONNECTION_TEST_SUCCESS.getMessage());
        } catch (SQLException e) {
            //throw new IllegalStateException(Messages.DB_CONNECTION_TEST_FAILED.getMessage(), e);
        }

        initialized = true;
        //LoggerUtil.info(Messages.DB_INITIALIZED.getMessage());
    }

    public Connection getConnection() throws SQLException {
        if (!initialized || pool == null) {
            //throw new IllegalStateException(Messages.DB_NOT_INITIALIZED.getMessage());
        }
        return pool.getConnection();
    }

    public synchronized void shutdown() {
        if (pool != null) {
            pool.close();
            pool = null;
        }
        initialized = false;
        //LoggerUtil.info(Messages.DB_SHUTDOWN_COMPLETE.getMessage());
    }
}



