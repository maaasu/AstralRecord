package io.github.maaasu.astralRecord;

import io.github.maaasu.astralRecord.core.CommandRegister;
import io.github.maaasu.astralRecord.core.event.EventManager;
import io.github.maaasu.astralRecord.feature.account.repository.AccountRepository;
import io.github.maaasu.astralRecord.feature.account.service.AccountService;
import io.github.maaasu.astralRecord.feature.item.service.ItemService;
import io.github.maaasu.astralRecord.feature.item.service.ItemStackFactory;
import io.github.maaasu.astralRecord.feature.item.view.ItemStackPacketAdapter;
import io.github.maaasu.astralRecord.feature.loot.service.LootService;
import io.github.maaasu.astralRecord.feature.player.event.PlayerJoinEventHandler;
import io.github.maaasu.astralRecord.feature.player.service.PlayerService;
import io.github.maaasu.astralRecord.feature.status.service.StatusService;
import io.github.maaasu.astralRecord.feature.user.event.UserLoginEventHandler;
import io.github.maaasu.astralRecord.feature.user.repository.UserRepository;
import io.github.maaasu.astralRecord.feature.user.service.UserService;
import io.github.maaasu.astralRecord.infrastructure.command.CommandManager;
import io.github.maaasu.astralRecord.infrastructure.config.ConfigManager;
import io.github.maaasu.astralRecord.infrastructure.api.ApiHealthChecker;
import io.github.maaasu.astralRecord.infrastructure.database.file.FileDatabaseManager;
import io.github.maaasu.astralRecord.infrastructure.database.file.yaml.config.YamlDbConfigUtil;
import io.github.maaasu.astralRecord.infrastructure.database.sqlserver.SqlServerManager;
import io.github.maaasu.astralRecord.infrastructure.logging.AuditLogger;
import io.github.maaasu.astralRecord.infrastructure.logging.AuditLoggerRegistry;
import io.github.maaasu.astralRecord.infrastructure.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;

public final class AstralRecord extends JavaPlugin {

    private static AstralRecord instance;

    // feature services
    private ItemService itemService;
    private LootService lootService;
    private ItemStackFactory itemStackFactory;
    private UserService userService;
    private PlayerService playerService;
    private EventManager eventManager;

    @Override
    public void onLoad() {
        instance = this;
        itemService = new ItemService();
        lootService = new LootService();
        itemStackFactory = new ItemStackFactory(lootService);
        // CommandManagerの初期化はPaper Lifecycle APIの制約上、onLoad()内で行う
        // コマンドをここで登録し、initialize()を呼び出す
        new CommandRegister(this, itemService, itemStackFactory);
        CommandManager.getInstance().initialize(this);
    }

    @Override
    public void onEnable() {
        // AuditLoggerの初期化
        AuditLogger.initDefault();

        // すべての LogEntry 実装クラスを自動スキャンして AuditLogger を初期化
        AuditLoggerRegistry.init("io.github.maaasu.astralRecord");

        if (!setupInfrastructure()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 3. featureの初期化
        setupFeature();

        // 4. 機能層（イベント・コマンド）の登録
        registerPluginFeatures();
    }

    @Override
    public void onDisable() {
        // AuditLoggerのシャットダウン
        AuditLogger.shutdownDefault();
        AuditLoggerRegistry.shutdownAll();
        // コマンドマネージャーのシャットダウン
        CommandManager.getInstance().shutdown();
    }

    /**
     * 起動時のメッセージとログ設定を表示
     */
    private void showStartupMessage() {
        Logger.info("AstralSaga has been enabled.");
        Logger.debug("Debug mode is enabled.");
    }

    /**
     * 設定ファイル、データベース等の基盤部分をセットアップ
     * @return 成功した場合はtrue
     */
    private boolean setupInfrastructure() {
        try {
            //設定ファイルを初期化
            ConfigManager.getInstance().initialize();


            // DB 初期化
            SqlServerManager.getInstance().initialize();

            // フォルダ型データベース初期化
            FileDatabaseManager.getInstance();

            // YamlDB設定の初期化
            YamlDbConfigUtil.INSTANCE.reload();

            // AstralRecord API 疎通確認（非同期）
            ApiHealthChecker.checkAsync();

            return true;
        }catch (Exception e) {
            Logger.error("Infrastructure setup failed.", e);
            return false;
        }
    }

    /**
     * プラグインの機能をセットアップします。
     */
    private void setupFeature() {
        // account
        AccountRepository accountRepository = new AccountRepository();
        AccountService accountService = new AccountService(accountRepository);

        // user
        UserRepository userRepository = new UserRepository();
        userService = new UserService(userRepository, accountService);

        // status
        StatusService statusService = new StatusService();

        // player
        playerService = new PlayerService(userService, accountService, statusService);

        // loot → item: loot を先にロードし、完了後に item をロード（bundle の lootTableId 解決に必要）
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            lootService.loadAll();
            itemService.loadAll();
        });

        // item: ProtocolLib パケットアダプタ（icon 差し替え）登録
        ItemStackPacketAdapter packetAdapter = new ItemStackPacketAdapter(this);
        packetAdapter.register();

        // event manager
        eventManager = new EventManager(this);
    }

    /**
     * イベントやコマンドなどの機能を登録
     */
    private void registerPluginFeatures() {
        eventManager.registerHandler(
            new UserLoginEventHandler(userService),
            getServer().getPluginManager()
        );
        eventManager.registerHandler(
            new PlayerJoinEventHandler(playerService),
            getServer().getPluginManager()
        );
    }
    /**
     * AstralSaga のインスタンスを取得します。
     * 他のクラスからインスタンスを取得する必要がある場合に使用します。
     */
    public static AstralRecord getInstance() {
        return instance;
    }

    /**
     * {@link ItemStackFactory} のインスタンスを取得します。
     *
     * @return ItemStackFactory
     */
    public ItemStackFactory getItemStackFactory() {
        return itemStackFactory;
    }
}
