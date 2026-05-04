package io.github.maaasu.astralRecord.feature.menu.repository;

import io.github.maaasu.astralRecord.AstralRecord;
import io.github.maaasu.astralRecord.feature.menu.model.MenuShortcutAction;
import io.github.maaasu.astralRecord.feature.menu.model.MenuShortcutSettings;
import io.github.maaasu.astralRecord.infrastructure.util.YamlLoaderUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.UUID;

/**
 * メニューショートカット設定をプラグインデータフォルダへ保存します。
 */
public class MenuShortcutRepository {
    private static final String FILE_NAME = "menu-shortcuts.yml";
    private static final String ROOT_KEY = "accounts";

    private final File file;
    private YamlConfiguration yaml;

    /**
     * リポジトリを生成し、既存のショートカット設定を読み込みます。
     *
     * @param plugin プラグインインスタンス
     */
    public MenuShortcutRepository(@NotNull AstralRecord plugin) {
        this.file = new File(plugin.getDataFolder(), FILE_NAME);
        reload();
    }

    /**
     * 保存済み YAML を再読み込みします。
     */
    public final void reload() {
        if (!file.exists()) {
            this.yaml = new YamlConfiguration();
            return;
        }
        YamlConfiguration loaded = YamlLoaderUtil.load(file);
        this.yaml = loaded != null ? loaded : new YamlConfiguration();
    }

    /**
     * 指定アカウントのショートカット設定を取得します。
     *
     * @param accountId アカウントID
     * @return ショートカット設定
     */
    public @NotNull MenuShortcutSettings findByAccountId(@NotNull UUID accountId) {
        MenuShortcutAction[] actions = new MenuShortcutAction[MenuShortcutSettings.SLOT_COUNT];
        String accountPath = ROOT_KEY + "." + accountId;
        for (int slot = 0; slot < MenuShortcutSettings.SLOT_COUNT; slot++) {
            actions[slot] = MenuShortcutAction.fromCode(yaml.getString(accountPath + ".slots." + slot));
            if (actions[slot] == MenuShortcutAction.NONE && !yaml.contains(accountPath + ".slots." + slot)) {
                actions[slot] = MenuShortcutAction.defaultForSlot(slot);
            }
        }
        return new MenuShortcutSettings(actions);
    }

    /**
     * 指定アカウントの1スロット分のショートカット設定を保存します。
     *
     * @param accountId アカウントID
     * @param slotIndex 0始まりのショートカットスロット番号
     * @param action 保存する項目
     */
    public void updateSlot(
        @NotNull UUID accountId,
        int slotIndex,
        @NotNull MenuShortcutAction action
    ) {
        if (slotIndex < 0 || slotIndex >= MenuShortcutSettings.SLOT_COUNT) {
            return;
        }
        String accountPath = ROOT_KEY + "." + accountId;
        yaml.set(accountPath + ".slots." + slotIndex, action.getCode());
        YamlLoaderUtil.save(yaml, file);
    }
}
