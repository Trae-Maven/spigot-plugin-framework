package io.github.trae.spigot.framework.hologram.events;

import io.github.trae.spigot.framework.event.CustomEvent;
import io.github.trae.spigot.framework.hologram.Hologram;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.Player;

/**
 * Fired after a hologram's remove packet has been sent to a player.
 *
 * <p>A notification, not a gate: the packet is already gone by the time listeners run. Fires
 * whenever a despawn is attempted, including for a player who was not actually viewing the hologram,
 * so it is not proof anything was on screen.</p>
 *
 * <p>Note this does not fire when a client discards a hologram on its own, which is what happens on
 * respawn and world change; see
 * {@link io.github.trae.spigot.framework.hologram.listeners.HologramListener}.</p>
 *
 * @see io.github.trae.spigot.framework.utility.UtilHologram#despawn(Player, Hologram)
 */
@AllArgsConstructor
@Getter
public final class HologramDespawnEvent extends CustomEvent {

    private final Hologram hologram;
    private final Player player;
}