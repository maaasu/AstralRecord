package io.github.maaasu.astralRecord.infrastructure.config;

public class ConfigProperties {

    private static ConfigProperties instance;

    // Plugin 関連
    private boolean pluginDebugMode;

    // SQL Server 関連
    private String sqlserverIpAddress;
    private int sqlserverPort;
    private String sqlserverDatabaseName;
    private boolean sqlserverEncrypt;
    private boolean sqlserverTrustServerCertificate;
    private String sqlserverUser;
    private String sqlserverPassword;

    // フォルダ型データベース関連
    private String fileDatabaseRootPath;

    // データベース接続プール関連
    private int databasePoolMaxPoolSize;
    private int databasePoolMinIdle;
    private long databasePoolConnectionTimeout;

    private ConfigProperties() {
        // private constructor for singleton
    }

    /**
     * ConfigPropertiesのシングルトンインスタンスを取得します。
     */
    public static synchronized ConfigProperties getInstance() {
        if (instance == null) {
            instance = new ConfigProperties();
        }
        return instance;
    }

    /**
     * ConfigManagerから設定値を読み込んでフィールドに設定します。
     * プラグインフォルダのconfig.ymlに書かれた実際の値を読み取ります。
     */
    public void initialize() {
        ConfigManager configManager = ConfigManager.getInstance();

        // Plugin 関連
        this.pluginDebugMode = configManager.getConfig().getBoolean(ConfigKeys.PLUGIN_DEBUG_MODE);

        // SQL Server 関連
        this.sqlserverIpAddress = configManager.getConfig().getString(ConfigKeys.SQLSERVER_IP_ADDRESS);
        this.sqlserverPort = configManager.getConfig().getInt(ConfigKeys.SQLSERVER_PORT);
        this.sqlserverDatabaseName = configManager.getConfig().getString(ConfigKeys.SQLSERVER_DATABASE);
        this.sqlserverEncrypt = configManager.getConfig().getBoolean(ConfigKeys.SQLSERVER_ENCRYPT);
        this.sqlserverTrustServerCertificate = configManager.getConfig().getBoolean(ConfigKeys.SQLSERVER_TRUST_SERVER_CERTIFICATE);
        this.sqlserverUser = configManager.getConfig().getString(ConfigKeys.SQLSERVER_USER);
        this.sqlserverPassword = configManager.getConfig().getString(ConfigKeys.SQLSERVER_PASSWORD);

        // フォルダ型データベース関連
        this.fileDatabaseRootPath = configManager.getConfig().getString(ConfigKeys.FILE_DATABASE_ROOT_PATH, "database.path");

        // データベース接続プール関連
        this.databasePoolMaxPoolSize = configManager.getConfig().getInt(ConfigKeys.DATABASE_POOL_MAX_POOL_SIZE);
        this.databasePoolMinIdle = configManager.getConfig().getInt(ConfigKeys.DATABASE_POOL_MIN_IDLE);
        this.databasePoolConnectionTimeout = configManager.getConfig().getLong(ConfigKeys.DATABASE_POOL_CONNECTION_TIMEOUT);
    }

    /**
     * 設定をリロードします。
     */
    public void reload() {
        ConfigManager.getInstance().reload();
        initialize();
    }

    // Plugin 関連のゲッター
    public boolean isPluginDebugMode() {
        return pluginDebugMode;
    }

    // SQL Server 関連のゲッター
    public String getSqlserverIpAddress() {
        return sqlserverIpAddress;
    }

    public int getSqlserverPort() {
        return sqlserverPort;
    }

    public String getSqlserverDatabaseName() {
        return sqlserverDatabaseName;
    }

    public boolean isSqlserverEncrypt() {
        return sqlserverEncrypt;
    }

    public boolean isSqlserverTrustServerCertificate() {
        return sqlserverTrustServerCertificate;
    }

    public String getSqlserverUser() {
        return sqlserverUser;
    }

    public String getSqlserverPassword() {
        return sqlserverPassword;
    }

    // フォルダ型データベース関連のゲッター
    public String getFileDatabaseRootPath() {
        return fileDatabaseRootPath;
    }

    // データベース接続プール関連のゲッター
    public int getDatabasePoolMaxPoolSize() {
        return databasePoolMaxPoolSize;
    }

    public int getDatabasePoolMinIdle() {
        return databasePoolMinIdle;
    }

    public long getDatabasePoolConnectionTimeout() {
        return databasePoolConnectionTimeout;
    }

    /**
     * SQL Server接続文字列を構築して返します。
     */
    public String buildSqlServerConnectionString() {
        return String.format(
                "jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=%s;trustServerCertificate=%s;user=%s;password=%s",
                sqlserverIpAddress,
                sqlserverPort,
                sqlserverDatabaseName,
                sqlserverEncrypt,
                sqlserverTrustServerCertificate,
                sqlserverUser,
                sqlserverPassword
        );
    }
}
