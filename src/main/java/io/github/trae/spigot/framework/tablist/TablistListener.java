package io.github.trae.spigot.framework.tablist;

import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.tablist.events.TablistUpdateEvent;
import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Applies tablist state in response to {@link TablistUpdateEvent}s and player quit, delegating all
 * packet work to {@link TablistManager}.
 */
@AllArgsConstructor
@Singleton
public class TablistListener implements Listener {

    private final TablistManager tablistManager;

    /**
     * Handles a {@link TablistUpdateEvent} by resolving and applying the player's tablist.
     * <p>
     * A cancelled event clears the player's tablist. Otherwise the eligible tablist is re-resolved
     * and either sent, or cleared if none qualify.
     *
     * @param event the tablist update event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onTablistUpdate(final TablistUpdateEvent event) {
        final Player player = event.getPlayer();

        if (event.isCancelled()) {
            this.tablistManager.remove(player);
            return;
        }

        this.tablistManager.getEligibleTablist(player).ifPresentOrElse(tablist -> this.tablistManager.create(player, tablist), () -> this.tablistManager.remove(player));
    }

    /**
     * On quit, drops the player's tracking entry so the active set does not leak across player
     * sessions. No packet is sent, as the player's connection is already closing.
     *
     * @param event the player quit event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        this.tablistManager.getActiveTablistSet().remove(event.getPlayer().getUniqueId());
    }
}