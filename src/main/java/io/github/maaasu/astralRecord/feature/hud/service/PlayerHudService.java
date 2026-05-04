package io.github.maaasu.astralRecord.feature.hud.service;

import io.github.maaasu.astralRecord.AstralRecord;
import io.github.maaasu.astralRecord.feature.player.AstPlayerCache;
import io.github.maaasu.astralRecord.feature.status.model.StatusSnapshot;
import io.github.maaasu.astralRecord.feature.status.model.StatusType;
import io.github.maaasu.astralRecord.feature.status.service.StatusService;
import io.github.maaasu.astralRecord.infrastructure.util.ColorCodeUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public class PlayerHudService {
    private static final String OBJECTIVE_NAME = "astral_info";

    private final StatusService statusService;
    private BukkitTask task;

    public PlayerHudService(StatusService statusService) {
        this.statusService = statusService;
    }

    public void start(AstralRecord plugin) {
        if (task != null) {
            return;
        }
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::updateAll, 20L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void updateAll() {
        for (var astPlayer : AstPlayerCache.getAll()) {
            Player player = astPlayer.getBukkit();
            if (!player.isOnline()) {
                continue;
            }

            StatusSnapshot snapshot = statusService.getStatus(astPlayer);
            updateActionBar(player, snapshot);
            updateBars(player, snapshot);
            updateSidebar(player, astPlayer.getAccount().getMode().name(), astPlayer.getUser().getPermission());
        }
    }

    private void updateActionBar(Player player, StatusSnapshot snapshot) {
        double maxHp = snapshot.getMaxValue(StatusType.MAX_HEALTH);
        double maxMp = snapshot.getMaxValue(StatusType.MAX_MANA);
        double maxEnergy = snapshot.getMaxValue(StatusType.MAX_ENERGY);
        player.sendActionBar(Component.empty()
            .append(statText("HP", snapshot.getCurrentHp(), maxHp, NamedTextColor.RED))
            .append(Component.text("  ", NamedTextColor.DARK_GRAY))
            .append(statText("MP", snapshot.getCurrentMp(), maxMp, NamedTextColor.AQUA))
            .append(Component.text("  ", NamedTextColor.DARK_GRAY))
            .append(statText("ENG", snapshot.getCurrentEnergy(), maxEnergy, NamedTextColor.YELLOW)));
    }

    private void updateBars(Player player, StatusSnapshot snapshot) {
        setHealthBar(player, ratio(snapshot.getCurrentHp(), snapshot.getMaxValue(StatusType.MAX_HEALTH)));
        player.setFoodLevel((int) Math.round(ratio(snapshot.getCurrentMp(), snapshot.getMaxValue(StatusType.MAX_MANA)) * 20.0D));
        player.setSaturation(0.0F);
        player.setExp((float) ratio(snapshot.getCurrentEnergy(), snapshot.getMaxValue(StatusType.MAX_ENERGY)));
    }

    private void setHealthBar(Player player, double ratio) {
        var maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null && maxHealth.getBaseValue() != 20.0D) {
            maxHealth.setBaseValue(20.0D);
        }
        double health = Math.clamp(ratio * 20.0D, 0.5D, 20.0D);
        if (player.getHealth() != health) {
            player.setHealth(health);
        }
    }

    private void updateSidebar(Player player, String mode, int permission) {
        Scoreboard scoreboard = player.getScoreboard();
        if (scoreboard == Bukkit.getScoreboardManager().getMainScoreboard()) {
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(scoreboard);
        }

        Objective objective = scoreboard.getObjective(OBJECTIVE_NAME);
        if (objective == null) {
            objective = scoreboard.registerNewObjective(
                OBJECTIVE_NAME,
                Criteria.DUMMY,
                Component.text("ASTRAL RECORD", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)
            );
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        clearSidebar(scoreboard);
        objective.getScore(ColorCodeUtil.DARK_GRAY + "----------------").setScore(5);
        objective.getScore(ColorCodeUtil.GOLD + "Server " + ColorCodeUtil.WHITE + Bukkit.getServer().getName()).setScore(4);
        objective.getScore(ColorCodeUtil.AQUA + "Online " + ColorCodeUtil.WHITE + Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers()).setScore(3);
        objective.getScore(ColorCodeUtil.BLACK + " ").setScore(2);
        objective.getScore(ColorCodeUtil.BLUE + "Mode " + ColorCodeUtil.WHITE + mode).setScore(1);
        objective.getScore(ColorCodeUtil.GREEN + "Perm " + ColorCodeUtil.WHITE + permission).setScore(0);
    }

    private void clearSidebar(Scoreboard scoreboard) {
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }
    }

    private double ratio(double current, double max) {
        if (max <= 0.0D) {
            return 0.0D;
        }
        return Math.clamp(current / max, 0.0D, 1.0D);
    }

    /**
     * アクションバー用の色付きステータステキストを作成します。
     *
     * @param label 表示ラベル
     * @param current 現在値
     * @param max 最大値
     * @param color メインカラー
     * @return 表示用コンポーネント
     */
    private Component statText(String label, double current, double max, NamedTextColor color) {
        return Component.text(label + " ", color, TextDecoration.BOLD)
            .append(Component.text(String.format("%.0f", current), NamedTextColor.WHITE))
            .append(Component.text("/", NamedTextColor.DARK_GRAY))
            .append(Component.text(String.format("%.0f", max), NamedTextColor.GRAY));
    }
}
