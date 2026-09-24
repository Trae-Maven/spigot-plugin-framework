package io.github.trae.spigot.framework.billboard.listeners;

import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.billboard.BillboardManager;
import lombok.AllArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Tells {@link BillboardManager} when a player's client has discarded what it was sent, so it is sent
 * again rather than assumed to still be there.
 */
@AllArgsConstructor
@Singleton
public final class BillboardListener implements Listener {

    /**
     * The manager viewer tracking is delegated to.
     */
    private final BillboardManager billboardManager;

    /**
     * Forgets the player's frames and maps, since the client discards both with its level on a world
     * change.
     *
     * @param event the player changed world event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(final PlayerChangedWorldEvent event) {
        this.billboardManager.forget(event.getPlayer(), true);
    }

    /**
     * Forgets the player's frames, since the server re-sends every entity on a respawn and the fake
     * frames are not among them. Maps survive a respawn in the same world, so an image is not re-sent
     * its colours; a respawn in another world is covered by the world change.
     *
     * @param event the player respawn event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(final PlayerRespawnEvent event) {
        this.billboardManager.forget(event.getPlayer(), false);
    }

    /**
     * Forgets everything sent to the player, since a rejoin starts a new client session.
     *
     * @param event the player quit event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        this.billboardManager.forget(event.getPlayer(), true);
    }
}