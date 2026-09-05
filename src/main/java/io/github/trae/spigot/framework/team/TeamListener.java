package io.github.trae.spigot.framework.team;

import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.team.events.TeamUpdateEvent;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Applies team state in response to player lifecycle and {@link TeamUpdateEvent}s, delegating all
 * packet work to {@link TeamManager}.
 */
@AllArgsConstructor
@Singleton
public class TeamListener implements Listener {

    private final TeamManager teamManager;

    /**
     * Handles a {@link TeamUpdateEvent} by re-resolving the player's team for every online viewer.
     * <p>
     * An unscoped event re-resolves each pair and creates or removes the team accordingly. A scoped
     * event creates the team only for pairs whose eligible team matches the event identifier, and
     * removes it from all others. A pair that resolves to no eligible team is always removed, so
     * suppression handled inside the team display checks tears the pair down on the next update.
     *
     * @param event the team update event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onTeamUpdate(final TeamUpdateEvent event) {
        final Player player = event.getPlayer();

        for (final Player viewer : Bukkit.getServer().getOnlinePlayers()) {
            if (event.getIdentifier() == null) {
                this.teamManager.refresh(player, viewer);
                continue;
            }

            this.teamManager.getEligibleTeam(player, viewer)
                    .filter(team -> team.getIdentifier().equals(event.getIdentifier()))
                    .ifPresentOrElse(team -> this.teamManager.create(player, viewer, team), () -> this.teamManager.remove(player, viewer));
        }
    }

    /**
     * On join, sends the joining player's team to every viewer and, reciprocally, sends every other
     * online player's team to the joining player.
     *
     * @param event the player join event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        for (final Player viewer : Bukkit.getServer().getOnlinePlayers()) {
            this.teamManager.refresh(player, viewer);

            if (!player.equals(viewer)) {
                this.teamManager.refresh(viewer, player);
            }
        }
    }

    /**
     * On quit, removes the quitting player's team in both directions for every other online player:
     * the quitting player's team is removed from each viewer, and each viewer's team is removed from
     * the quitting player. This clears every tracked pair involving the quitting player, leaving no
     * stale entries.
     *
     * @param event the player quit event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();

        for (final Player viewer : Bukkit.getServer().getOnlinePlayers()) {
            this.teamManager.remove(player, viewer);

            if (!player.equals(viewer)) {
                this.teamManager.remove(viewer, player);
            }
        }
    }
}