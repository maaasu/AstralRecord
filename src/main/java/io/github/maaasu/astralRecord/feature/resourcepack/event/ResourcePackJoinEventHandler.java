package io.github.maaasu.astralRecord.feature.resourcepack.event;

import io.github.maaasu.astralRecord.core.event.AbstractEventHandler;
import io.github.maaasu.astralRecord.feature.resourcepack.service.ResourcePackService;
import io.github.maaasu.astralRecord.infrastructure.logging.LogId;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Requests the resource pack when a Java Edition player joins.
 */
public class ResourcePackJoinEventHandler extends AbstractEventHandler {

    private final ResourcePackService resourcePackService;

    public ResourcePackJoinEventHandler(ResourcePackService resourcePackService) {
        this.resourcePackService = resourcePackService;
    }

    @Override
    public boolean isEnabled() {
        return resourcePackService.isEnabled();
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        runSafely(() -> resourcePackService.applyTo(event.getPlayer()), LogId.E_5550, event.getPlayer().getName());
    }
}
