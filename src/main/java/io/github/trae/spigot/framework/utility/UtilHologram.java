package io.github.trae.spigot.framework.utility;

import io.github.trae.spigot.framework.hologram.Hologram;
import io.github.trae.spigot.framework.hologram.events.HologramDespawnEvent;
import io.github.trae.spigot.framework.hologram.events.HologramSpawnEvent;
import io.github.trae.spigot.framework.hologram.events.HologramUpdateEvent;
import lombok.experimental.UtilityClass;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Packet utility for sending {@link Hologram holograms} to individual players.
 *
 * <p>Holograms are client-side only. The {@link net.minecraft.world.entity.Display.TextDisplay}
 * backing a hologram is constructed but never added to the level, so the server has no entity to
 * tick, no chunk to persist it in, and no other plugin can see it. Everything a player observes is
 * produced by the three packets sent here.</p>
 *
 * <p>Because nothing exists server-side, the client's view is authoritative and must be kept in
 * sync by hand. A client discards its entire entity table on respawn and on a world change, so a
 * hologram it was shown is silently forgotten; the viewer set must be cleared in those cases so the
 * next tick re-sends the add packet.</p>
 *
 * <p>Each method mutates the hologram's viewer set as a side effect, so callers should treat that
 * set as the record of what a given player has actually been sent.</p>
 *
 * <p>Every method dispatches a corresponding event. {@link HologramSpawnEvent} is the cancellable
 * gate that runs before anything is sent, complementing {@link Hologram#canSee(Player)}: the event
 * lets other code veto a hologram it does not own, while {@code canSee} is the hologram's own rule.
 * {@link HologramDespawnEvent} and {@link HologramUpdateEvent} are notifications dispatched after
 * their packets have gone out.</p>
 *
 * @see Hologram
 */
@UtilityClass
public class UtilHologram {

    /**
     * Spawns a hologram for a single player and marks them as a viewer.
     *
     * <p>Dispatches {@link HologramSpawnEvent} first and returns without sending anything if it is
     * cancelled. The player is not added to the viewer set in that case, so a polling caller will
     * attempt the spawn again on its next pass; a listener that cancels must therefore keep
     * cancelling for as long as it wants the hologram hidden.</p>
     *
     * <p>Sends the add packet carrying the entity id, UUID, position and rotation, then immediately
     * follows with {@link #update(Player, Hologram)} because the add packet alone produces an
     * invisible entity with default metadata: the text, billboard, background and transformation
     * all live in the metadata packet.</p>
     *
     * <p>Position and rotation are baked into the add packet, so a hologram that has moved cannot
     * be corrected by an update. Despawn it and let the next tick spawn it afresh.</p>
     *
     * @param player   the player to spawn the hologram for
     * @param hologram the hologram to spawn
     */
    public static void spawn(final Player player, final Hologram hologram) {
        if (UtilEvent.supply(new HologramSpawnEvent(hologram, player)).isCancelled()) {
            return;
        }

        final Location location = hologram.getLocation();

        UtilNms.sendPacket(player, new ClientboundAddEntityPacket(
                hologram.getEntityId(),
                hologram.getEntityUuid(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getPitch(),
                location.getYaw(),
                EntityType.TEXT_DISPLAY,
                0,
                Vec3.ZERO,
                location.getYaw())
        );

        update(player, hologram);

        hologram.getViewerSet().add(player.getUniqueId());
    }

    /**
     * Despawns a hologram for a single player, removes them from the viewer set and dispatches
     * {@link HologramDespawnEvent}.
     *
     * <p>Safe to call for a player who is not currently viewing the hologram; the client ignores a
     * remove packet for an entity id it does not know about. The event fires regardless, so
     * listeners should not treat it as proof the player was seeing anything.</p>
     *
     * @param player   the player to despawn the hologram for
     * @param hologram the hologram to despawn
     */
    public static void despawn(final Player player, final Hologram hologram) {
        UtilNms.sendPacket(player, new ClientboundRemoveEntitiesPacket(hologram.getEntityId()));

        hologram.getViewerSet().remove(player.getUniqueId());

        UtilEvent.dispatch(new HologramDespawnEvent(hologram, player));
    }

    /**
     * Pushes the hologram's current metadata to a single player and dispatches
     * {@link HologramUpdateEvent}.
     *
     * <p>Writes this player's resolved text onto the shared {@link org.bukkit.entity.TextDisplay}
     * view, then serialises the backing entity's {@link SynchedEntityData} and sends it. Writing
     * through the Bukkit view rather than the raw data accessors is what keeps this free of
     * reflection: every setter lands in the same synched data the packet reads from.</p>
     *
     * <p>The display is shared across all viewers, so the text written here is overwritten by the
     * next call for a different player. That is harmless because the packet is built and sent
     * before this method returns, but it does mean the display's own state is never a reliable
     * record of what any particular player is seeing.</p>
     *
     * <p>{@link SynchedEntityData#getNonDefaultValues()} returns {@code null} when every value
     * still matches its default, which is the case for a hologram whose text is empty and whose
     * settings are all vanilla. Nothing is sent and no event is dispatched in that case.</p>
     *
     * <p>This runs for every viewer of every dynamic hologram on each polling pass, so listeners
     * should keep their work proportionate.</p>
     *
     * @param player   the player to send the metadata to
     * @param hologram the hologram whose metadata is sent
     */
    public static void update(final Player player, final Hologram hologram) {
        hologram.getTextDisplay().text(hologram.getComponent(player));

        final List<SynchedEntityData.DataValue<?>> dataValueList = hologram.getHandle().getEntityData().getNonDefaultValues();
        if (dataValueList == null) {
            return;
        }

        UtilNms.sendPacket(player, new ClientboundSetEntityDataPacket(hologram.getEntityId(), dataValueList));

        UtilEvent.dispatch(new HologramUpdateEvent(hologram, player));
    }
}