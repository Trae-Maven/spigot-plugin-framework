package io.github.trae.spigot.framework.picture.listeners;

import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.picture.PictureManager;
import lombok.AllArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingBreakEvent;

/**
 * Protects picture frames from being broken, delegating the tag check to {@link PictureManager}.
 */
@AllArgsConstructor
@Singleton
public final class PictureListener implements Listener {

    /**
     * The manager frame identification is delegated to.
     */
    private final PictureManager pictureManager;

    /**
     * Cancels any break of a picture frame, whether by a player, an explosion or the wall behind it
     * being removed. Fixed frames already block item removal and rotation, but a creative player can
     * still break one.
     *
     * @param event the hanging break event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHangingBreak(final HangingBreakEvent event) {
        if (!this.pictureManager.isTagged(event.getEntity())) {
            return;
        }

        event.setCancelled(true);
    }
}