package io.github.trae.spigot.framework.hologram.listeners;

import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.hologram.HologramManager;
import lombok.AllArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Keeps hologram viewer sets honest when a client discards what it knows.
 *
 * <p>Holograms exist only as packets, so the viewer set is the sole record of what a client has been
 * told about. A client wipes its entire entity table on respawn and on a world change without
 * telling the server, which would leave the viewer set claiming a hologram is still on screen when
 * it has silently vanished, and the polling pass would never re-send it because nothing about
 * visibility changed. Clearing the player from every viewer set makes the next pass treat them as
 * never having seen anything and spawn everything again.</p>
 *
 * <p>Quit is cleanup rather than correctness: the client is gone, but the sets would otherwise keep
 * its UUID forever.</p>
 *
 * <p>All three run at {@link EventPriority#MONITOR} because this only observes; nothing here changes
 * the outcome of the event.</p>
 *
 * @see HologramManager#forget(org.bukkit.entity.Player)
 */
@AllArgsConstructor
@Singleton
public class HologramListener implements Listener {

    private final HologramManager hologramManager;

    /**
     * Drops the departing player from every viewer set.
     *
     * @param event the quit event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public final void onPlayerQuit(final PlayerQuitEvent event) {
        this.hologramManager.forget(event.getPlayer());
    }

    /**
     * Forgets the player so holograms in their new world are sent fresh.
     *
     * @param event the world change event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public final void onPlayerChangedWorld(final PlayerChangedWorldEvent event) {
        this.hologramManager.forget(event.getPlayer());
    }

    /**
     * Forgets the player so holograms are re-sent after their client resets.
     *
     * @param event the respawn event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public final void onPlayerRespawn(final PlayerRespawnEvent event) {
        this.hologramManager.forget(event.getPlayer());
    }
}