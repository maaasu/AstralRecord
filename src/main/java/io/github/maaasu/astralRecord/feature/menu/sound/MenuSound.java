package io.github.maaasu.astralRecord.feature.menu.sound;

import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * メニュー操作で使用する効果音を共通定義します。
 */
public enum MenuSound {
    /** メニューを開くときの効果音。 */
    OPEN(Sound.BLOCK_CHEST_OPEN, 0.6f, 1.2f),
    /** メニュー項目を選択したときの効果音。 */
    SELECT(Sound.UI_BUTTON_CLICK, 0.6f, 1.4f),
    /** メニューを閉じるときの効果音。 */
    CLOSE(Sound.BLOCK_CHEST_CLOSE, 0.6f, 1.0f),
    /** 無効な操作を抑止したときの効果音。 */
    DENY(Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.7f);

    private final Sound sound;
    private final float volume;
    private final float pitch;

    MenuSound(@NotNull Sound sound, float volume, float pitch) {
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
    }

    /**
     * 指定プレイヤーへこの効果音を再生します。
     *
     * @param player 再生対象プレイヤー
     */
    public void play(@NotNull Player player) {
        player.playSound(player.getLocation(), sound, SoundCategory.PLAYERS, volume, pitch);
    }
}
