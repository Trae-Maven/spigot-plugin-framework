package io.github.trae.spigot.framework.hologram;

import io.github.trae.di.InjectorApi;
import io.github.trae.di.annotations.method.Scheduler;
import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.utility.UtilHologram;
import io.github.trae.spigot.framework.utility.UtilServer;
import io.github.trae.utilities.UtilJava;
import io.github.trae.utilities.UtilString;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Owns every registered {@link Hologram} and keeps each player's client in sync with them.
 *
 * <p>Holograms are resolved from the container on first use, so any {@code @Singleton} extending
 * {@link Hologram} is picked up with no registration call. Each is built on the first scheduler
 * pass that finds it unbuilt, and from then on a pass decides, for each player and hologram,
 * whether to spawn, despawn or refresh.</p>
 *
 * <h2>Why polling</h2>
 * <p>Visibility depends on the player's position, the hologram's position and
 * {@link Hologram#canSee(Player)}, and only the first of those produces an event. Polling covers all
 * three uniformly. {@code PlayerMoveEvent} would fire several times per tick per player, including
 * on head rotation alone, and would still miss the other two.</p>
 *
 * <h2>Cost</h2>
 * <p>The pass is holograms outer, players inner, so {@link Hologram#getLocation()} and the other
 * per-hologram settings are resolved once per hologram rather than once per player per hologram, and
 * players in other worlds are rejected by a single world comparison. What remains per player is a
 * set lookup, three subtractions and {@link Hologram#canSee(Player)}.</p>
 *
 * @see Hologram
 * @see io.github.trae.spigot.framework.utility.UtilHologram
 */
@Singleton
public class HologramManager {

    /**
     * Every registered hologram, keyed by lowercased name.
     *
     * <p>Resolved on first use rather than at construction, since the container is still wiring
     * when this is built. Never mutated afterwards.</p>
     */
    private Map<String, Hologram> hologramMap;

    private Map<String, Hologram> getHologramMap() {
        if (this.hologramMap == null) {
            this.hologramMap = UtilJava.createMap(new HashMap<>(), map -> {
                for (final Hologram hologram : InjectorApi.getAll(Hologram.class)) {
                    map.put(hologram.getName().toLowerCase(Locale.ROOT), hologram);
                }
            });
        }

        return this.hologramMap;
    }

    /**
     * Reconciles every player's client with every hologram.
     *
     * <p>Spawns holograms that have come into view, despawns those that have left it, and re-sends
     * the text of {@link Hologram#isDynamic() dynamic} holograms to their current viewers. Also
     * builds any hologram that is not yet built, which covers both the initial build and a hologram
     * whose world loads late, so neither needs a separate startup hook.</p>
     *
     * <p>Runs on the main thread, which is required: packet sends and whatever a subclass touches
     * when resolving its lines both assume it.</p>
     */
    @Scheduler(period = 500, unit = TimeUnit.MILLISECONDS)
    public final void onScheduler() {
        final List<Player> playerList = UtilServer.getOnlinePlayers();
        if (playerList.isEmpty()) {
            return;
        }

        for (final Hologram hologram : this.getHologramMap().values()) {
            if (!hologram.isBuilt()) {
                hologram.build();

                if (!hologram.isBuilt()) {
                    continue;
                }
            }

            final Location location = hologram.getLocation();
            final Set<UUID> viewerSet = hologram.getViewerSet();
            final boolean dynamic = hologram.isDynamic();

            for (final Player player : playerList) {
                final boolean viewing = viewerSet.contains(player.getUniqueId());
                final boolean visible = hologram.isVisible(player, location);

                if (visible && !viewing) {
                    UtilHologram.spawn(player, hologram);
                    continue;
                }

                if (!visible && viewing) {
                    UtilHologram.despawn(player, hologram);
                    continue;
                }

                if (visible && dynamic) {
                    UtilHologram.update(player, hologram);
                }
            }
        }
    }

    /**
     * Looks a hologram up by name, case-insensitively.
     *
     * @param name the registered name
     * @return the hologram, or empty if the name is blank or unknown
     */
    public final Optional<Hologram> getHologramByName(final String name) {
        if (UtilString.isEmpty(name)) {
            return Optional.empty();
        }

        return Optional.ofNullable(this.getHologramMap().get(name.toLowerCase(Locale.ROOT)));
    }

    /**
     * Applies a hologram's new position.
     *
     * <p>Position and rotation live in the add packet, and a move across worlds assigns a new entity
     * id, so neither can be pushed as a metadata update. Every viewer is despawned first, then the
     * hologram is rebuilt; the next scheduler pass spawns it afresh wherever it now belongs.</p>
     *
     * <p>Call this after the subclass's {@link Hologram#getLocation()} already returns the new
     * location, not before.</p>
     *
     * @param hologram the hologram that has moved
     */
    public final void relocate(final Hologram hologram) {
        this.despawn(hologram);

        hologram.build();
    }

    /**
     * Pushes a hologram's current settings to everyone currently viewing it.
     *
     * <p>Only needed for a hologram that is not {@link Hologram#isDynamic() dynamic}, where nothing
     * re-sends after the initial spawn. Does not cover a position change; use
     * {@link #relocate(Hologram)} for that.</p>
     *
     * @param hologram the hologram to refresh
     */
    public final void refresh(final Hologram hologram) {
        hologram.build();

        if (!hologram.isBuilt()) {
            return;
        }

        for (final Player player : UtilServer.getOnlinePlayers()) {
            if (!hologram.getViewerSet().contains(player.getUniqueId())) {
                continue;
            }

            UtilHologram.update(player, hologram);
        }
    }

    /**
     * Removes a hologram from every player currently viewing it.
     *
     * <p>The scheduler will spawn it again on its next pass if it is still visible, so this is a way
     * to force a clean re-send, not a way to hide a hologram. Use
     * {@link Hologram#canSee(Player)} or a cancelled
     * {@link io.github.trae.spigot.framework.hologram.events.HologramSpawnEvent} to hide one.</p>
     *
     * @param hologram the hologram to despawn
     */
    public final void despawn(final Hologram hologram) {
        if (!hologram.isBuilt()) {
            return;
        }

        for (final Player player : UtilServer.getOnlinePlayers()) {
            if (!hologram.getViewerSet().contains(player.getUniqueId())) {
                continue;
            }

            UtilHologram.despawn(player, hologram);
        }
    }

    /**
     * Drops a player from every hologram's viewer set without sending anything.
     *
     * <p>Used when the client has already discarded its entity table on its own, which happens on
     * respawn and on a world change, and when the player has left. Sending a remove packet in those
     * cases would be pointless; what matters is that the viewer sets stop claiming the client knows
     * about holograms it has forgotten, so the next pass re-sends them.</p>
     *
     * @param player the player to forget
     */
    public final void forget(final Player player) {
        this.getHologramMap().values().forEach(hologram -> hologram.getViewerSet().remove(player.getUniqueId()));
    }
}