package io.github.trae.spigot.framework.hologram.events;

import io.github.trae.spigot.framework.event.CustomEvent;
import io.github.trae.spigot.framework.hologram.Hologram;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.Player;

/**
 * Fired after a hologram's metadata packet has been sent to a player.
 *
 * <p>A notification, not a gate: the text and settings are already on their way. To change what a
 * player sees, override the hologram's own line resolution instead.</p>
 *
 * <p>This is the hottest event in the subsystem. It fires once per viewer per scheduler pass for
 * every {@link Hologram#isDynamic() dynamic} hologram, so listeners should stay proportionate to
 * that.</p>
 *
 * @see io.github.trae.spigot.framework.utility.UtilHologram#update(Player, Hologram)
 */
@AllArgsConstructor
@Getter
public class HologramUpdateEvent extends CustomEvent {

    private final Hologram hologram;
    private final Player player;
}