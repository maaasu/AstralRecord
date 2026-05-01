package io.github.maaasu.astralRecord.feature.resourcepack.event;

import io.github.maaasu.astralRecord.core.event.AbstractEventHandler;
import io.github.maaasu.astralRecord.feature.resourcepack.service.ResourcePackService;
import io.github.maaasu.astralRecord.infrastructure.logging.LogId;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

/**
 * Tracks the client result for the managed resource pack request.
 */
public class ResourcePackStatusEventHandler extends AbstractEventHandler {

    private final ResourcePackService resourcePackService;

    public ResourcePackStatusEventHandler(ResourcePackService resourcePackService) {
        this.resourcePackService = resourcePackService;
    }

    @Override
    public boolean isEnabled() {
        return resourcePackService.isEnabled();
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onStatus(PlayerResourcePackStatusEvent event) {
        if (!resourcePackService.isManagedPack(event.getID())) {
            return;
        }

        runSafely(
                () -> resourcePackService.handleStatus(event.getPlayer(), event.getStatus()),
                LogId.E_5550,
                event.getPlayer().getName()
        );
    }
}
