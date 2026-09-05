package io.github.trae.spigot.framework.sidebar;

import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.sidebar.events.SidebarUpdateEvent;
import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Optional;

/**
 * Applies sidebar state in response to player lifecycle and {@link SidebarUpdateEvent}s, delegating
 * all packet work to {@link SidebarManager}.
 */
@AllArgsConstructor
@Singleton
public class SidebarListener implements Listener {

    private final SidebarManager sidebarManager;

    /**
     * Handles a {@link SidebarUpdateEvent}.
     * <p>
     * A cancelled event clears the player's sidebar. If the event is scoped to an identifier that
     * does not match the player's active sidebar, it is ignored. Otherwise the eligible sidebar is
     * re-resolved: if none qualify the sidebar is cleared; if the same sidebar remains active its
     * title and lines are diffed and updated; if a different sidebar wins the old one is cleared and
     * the new one created.
     *
     * @param event the sidebar update event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onSidebarUpdate(final SidebarUpdateEvent event) {
        final Player player = event.getPlayer();

        final Sidebar activeSidebar = this.sidebarManager.getActiveSidebarMap().get(player.getUniqueId());

        if (event.isCancelled()) {
            if (activeSidebar != null) {
                this.sidebarManager.clear(player);
            }
            return;
        }

        if (event.getIdentifier() != null && (activeSidebar == null || !activeSidebar.getIdentifier().equals(event.getIdentifier()))) {
            return;
        }

        final Optional<Sidebar> eligibleSidebarOptional = this.sidebarManager.getEligibleSidebar(player);
        if (eligibleSidebarOptional.isEmpty()) {
            this.sidebarManager.clear(player);
            return;
        }

        final Sidebar eligibleSidebar = eligibleSidebarOptional.get();

        if (activeSidebar != null && activeSidebar.getIdentifier().equals(eligibleSidebar.getIdentifier())) {
            this.sidebarManager.refreshTitle(player, eligibleSidebar);
            this.sidebarManager.updateLines(player, eligibleSidebar);
        } else {
            this.sidebarManager.clear(player);
            this.sidebarManager.create(player, eligibleSidebar);
        }

        this.sidebarManager.getActiveSidebarMap().put(player.getUniqueId(), eligibleSidebar);
    }

    /**
     * Creates the eligible sidebar for a player when they join, if any qualifies.
     *
     * @param event the player join event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        this.sidebarManager.getEligibleSidebar(player).ifPresent(sidebar -> {
            this.sidebarManager.create(player, sidebar);
            this.sidebarManager.getActiveSidebarMap().put(player.getUniqueId(), sidebar);
        });
    }

    /**
     * Clears all sidebar state for a player when they quit.
     *
     * @param event the player quit event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        this.sidebarManager.clear(event.getPlayer());
    }
}