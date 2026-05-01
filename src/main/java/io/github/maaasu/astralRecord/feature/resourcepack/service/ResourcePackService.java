package io.github.maaasu.astralRecord.feature.resourcepack.service;

import io.github.maaasu.astralRecord.feature.player.AstPlayerCache;
import io.github.maaasu.astralRecord.feature.player.PlayerMsgId;
import io.github.maaasu.astralRecord.feature.player.PlayerMsgResource;
import io.github.maaasu.astralRecord.feature.player.model.AstPlayer;
import io.github.maaasu.astralRecord.infrastructure.config.ConfigProperties;
import io.github.maaasu.astralRecord.infrastructure.logging.LogId;
import io.github.maaasu.astralRecord.infrastructure.logging.Logger;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Sends the Java Edition resource pack request and handles client status updates.
 */
public class ResourcePackService {

    private static final int SHA1_LENGTH = 40;

    private final ConfigProperties configProperties;

    public ResourcePackService(ConfigProperties configProperties) {
        this.configProperties = configProperties;
    }

    public boolean isEnabled() {
        return configProperties.isResourcePackEnabled() && !getPackUrl().isBlank();
    }

    public void applyTo(Player player) {
        if (!isEnabled()) {
            return;
        }

        if (shouldSkipBedrock(player)) {
            Logger.log(LogId.I_5551, player.getName());
            return;
        }

        UUID packId = getPackId();
        String prompt = blankToNull(configProperties.getResourcePackPrompt());
        String sha1 = blankToNull(configProperties.getResourcePackSha1());

        if (sha1 != null && !isValidSha1(sha1)) {
            Logger.log(LogId.W_5550, getPackUrl(), sha1);
            sha1 = null;
        }

        player.setResourcePack(
                packId,
                getPackUrl(),
                sha1,
                prompt == null ? null : Component.text(prompt),
                configProperties.isResourcePackForce()
        );

        Logger.log(
                LogId.I_5550,
                player.getName(),
                getPackUrl(),
                configProperties.isResourcePackForce()
        );
    }

    public boolean isManagedPack(UUID packId) {
        return getPackId().equals(packId);
    }

    public void handleStatus(Player player, PlayerResourcePackStatusEvent.Status status) {
        switch (status) {
            case ACCEPTED -> Logger.log(LogId.I_5552, player.getName());
            case DOWNLOADED -> Logger.log(LogId.I_5553, player.getName());
            case SUCCESSFULLY_LOADED -> Logger.log(LogId.I_5554, player.getName());
            case DECLINED -> handleDeclined(player);
            case FAILED_DOWNLOAD -> sendPlayerMessage(player, PlayerMsgId.P_5550);
            case INVALID_URL -> sendPlayerMessage(player, PlayerMsgId.P_5551);
            case FAILED_RELOAD -> sendPlayerMessage(player, PlayerMsgId.P_5552);
            case DISCARDED -> Logger.log(LogId.W_5551, player.getName());
            default -> Logger.log(LogId.D_5550, player.getName(), status);
        }
    }

    private void handleDeclined(Player player) {
        sendPlayerMessage(player, PlayerMsgId.P_5553);
        Logger.log(LogId.W_5552, player.getName());
    }

    private String getPackUrl() {
        return configProperties.getResourcePackUrl().trim();
    }

    private UUID getPackId() {
        return UUID.nameUUIDFromBytes(getPackUrl().getBytes(StandardCharsets.UTF_8));
    }

    private boolean shouldSkipBedrock(Player player) {
        if (!configProperties.isResourcePackSkipBedrock()) {
            return false;
        }

        var p = AstPlayerCache.get(player);
        if (p == null) {return false;}
        return p.isBedrock();
    }

    private static boolean isValidSha1(String value) {
        if (value.length() != SHA1_LENGTH) {
            return false;
        }

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean hex = (c >= '0' && c <= '9')
                    || (c >= 'a' && c <= 'f')
                    || (c >= 'A' && c <= 'F');
            if (!hex) {
                return false;
            }
        }
        return true;
    }

    private static @Nullable String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static void sendPlayerMessage(Player player, PlayerMsgId msgId) {
        player.sendMessage(PlayerMsgResource.getMessage(msgId.getId()));
    }
}
