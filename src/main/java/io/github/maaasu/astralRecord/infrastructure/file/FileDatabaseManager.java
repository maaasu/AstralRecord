package io.github.maaasu.astralRecord.infrastructure.file;

import io.github.maaasu.astralRecord.AstralRecord;
import io.github.maaasu.astralRecord.infrastructure.config.ConfigProperties;
import io.github.maaasu.astralRecord.infrastructure.logging.LogId;
import io.github.maaasu.astralRecord.infrastructure.logging.Logger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

/**
 * ファイルベースのデータベースを管理するクラス。
 * 設定されたルートディレクトリ配下のファイルを読み書きします。
 */
public class FileDatabaseManager {

    private static FileDatabaseManager instance;
    private File rootDirectory;

    private FileDatabaseManager() {
        initialize();
    }

    public static synchronized FileDatabaseManager getInstance() {
        if (instance == null) {
            instance = new FileDatabaseManager();
        }
        return instance;
    }

    /**
     * 初期化を行い、ルートディレクトリを確定します。
     */
    private void initialize() {
        String rootPath = ConfigProperties.getInstance().getFileDatabaseRootPath();
        if (rootPath == null || rootPath.isBlank() || rootPath.equals("database.path")) {
            //Logger.log(LogId.FILE_DB_ROOT_PATH_FAILED);
            return;
        }

        // 絶対パスか相対パスかを判定
        File pathFile = new File(rootPath);
        if (pathFile.isAbsolute()) {
            // 絶対パスの場合はそのまま使用
            this.rootDirectory = pathFile;
        } else {
            // 相対パスの場合はプラグインのデータフォルダ配下に配置
            File dataFolder = AstralRecord.getInstance().getDataFolder();
            this.rootDirectory = new File(dataFolder, rootPath);
        }

        if (!rootDirectory.exists()) {
            if (rootDirectory.mkdirs()) {
                //Logger.log(LogId.FILE_DB_ROOT_CREATED, rootDirectory.getPath());
            } else {
                //Logger.log(LogId.FILE_DB_ROOT_CREATE_FAILED, rootDirectory.getPath());
            }
        } else {
            //Logger.log(LogId.FILE_DB_INITIALIZED, rootDirectory.getPath());
        }
    }

    /**
     * 指定された相対パスのYAML設定を取得します。
     *
     * @param relativePath ルートディレクトリからの相対パス
     * @return FileConfiguration
     */
    public FileConfiguration getConfig(String relativePath) {
        File file = new File(rootDirectory, relativePath);
        return YamlConfiguration.loadConfiguration(file);
    }

    /**
     * 指定されたパスセグメントからYAML設定を取得します。
     * 例: getConfig("ITEM", "木の剣") -> ルートディレクトリ\ITEM\木の剣.yml
     *
     * @param pathSegments パスセグメント（可変長引数）
     * @return FileConfiguration
     */
    public FileConfiguration getConfig(String... pathSegments) {
        String relativePath = buildPath(pathSegments);
        return getConfig(relativePath);
    }

    /**
     * 指定された相対パスへファイルを保存します。
     *
     * @param config 保存する設定
     * @param relativePath ルートディレクトリからの相対パス
     */
    public void saveConfig(FileConfiguration config, String relativePath) {
        File file = new File(rootDirectory, relativePath);

        // 親ディレクトリを作成
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs()) {
                //Logger.log(LogId.FILE_DB_ROOT_CREATE_FAILED, parent.getPath());
                return;
            }
        }

        try {
            config.save(file);
        } catch (IOException e) {
            //Logger.log(LogId.FILE_DB_SAVE_ERROR, e, file.getPath());
        }
    }

    /**
     * 指定されたパスセグメントからファイルを保存します。
     * 例: saveConfig(config, "ITEM", "木の剣") -> ルートディレクトリ\ITEM\木の剣.yml
     *
     * @param config 保存する設定
     * @param pathSegments パスセグメント（可変長引数）
     */
    public void saveConfig(FileConfiguration config, String... pathSegments) {
        String relativePath = buildPath(pathSegments);
        saveConfig(config, relativePath);
    }

    /**
     * ファイルが存在するかを確認します。
     *
     * @param relativePath ルートディレクトリからの相対パス
     * @return 存在すれば true
     */
    public boolean exists(String relativePath) {
        return new File(rootDirectory, relativePath).exists();
    }

    /**
     * 指定されたパスセグメントからファイルの存在を確認します。
     * 例: exists("ITEM", "木の剣") -> ルートディレクトリ\ITEM\木の剣.yml
     *
     * @param pathSegments パスセグメント（可変長引数）
     * @return 存在すれば true
     */
    public boolean exists(String... pathSegments) {
        String relativePath = buildPath(pathSegments);
        return exists(relativePath);
    }

    /**
     * パスセグメントから相対パスを構築します。
     * 最後のセグメントに .yml 拡張子を付与します。
     *
     * @param pathSegments パスセグメント
     * @return 構築された相対パス
     */
    private String buildPath(String... pathSegments) {
        if (pathSegments == null || pathSegments.length == 0) {
            throw new IllegalArgumentException("Path segments cannot be null or empty");
        }

        StringBuilder pathBuilder = new StringBuilder();
        for (int i = 0; i < pathSegments.length; i++) {
            if (i > 0) {
                pathBuilder.append(File.separator);
            }
            pathBuilder.append(pathSegments[i]);
        }

        // 最後のセグメントに .yml 拡張子を自動付与
        String path = pathBuilder.toString();
        var yml = ".yml";
        if (!path.endsWith(yml)) {
            path += yml;
        }

        return path;
    }

    /**
     * ルートディレクトリを返します。
     *
     * @return ルートディレクトリ
     */
    public File getRootDirectory() {
        return rootDirectory;
    }

    /**
     * システムをリロード（再初期化）します。
     */
    public void reload() {
        initialize();
    }
}
