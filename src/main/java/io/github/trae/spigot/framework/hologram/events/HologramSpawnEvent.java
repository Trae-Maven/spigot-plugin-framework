package io.github.trae.spigot.framework.hologram.events;

import io.github.trae.spigot.framework.event.CustomCancellableEvent;
import io.github.trae.spigot.framework.hologram.Hologram;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.Player;

/**
 * Fired before a hologram is sent to a player, and cancellable to stop it.
 *
 * <p>The system-level half of the gate pattern: this lets code veto a hologram it does not own,
 * while {@link Hologram#canSee(Player)} is the hologram's own rule. Both must pass.</p>
 *
 * <p>Cancelling leaves the player out of the viewer set, so the polling pass tries again roughly
 * twice a second. A listener that wants a hologram hidden must keep cancelling for as long as that
 * holds, rather than cancelling once.</p>
 *
 * @see io.github.trae.spigot.framework.utility.UtilHologram#spawn(Player, Hologram)
 */
@AllArgsConstructor
@Getter
public final class HologramSpawnEvent extends CustomCancellableEvent {

    private final Hologram hologram;
    private final Player player;
}