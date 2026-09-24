package io.github.trae.spigot.framework.billboard;

import io.github.trae.di.InjectorApi;
import io.github.trae.di.annotations.method.Scheduler;
import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.utility.UtilMap;
import io.github.trae.spigot.framework.utility.UtilNms;
import io.github.trae.spigot.framework.utility.UtilTask;
import lombok.Getter;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Loads every registered {@link Billboard} and shows it to nearby players through packets.
 * <p>
 * Once the server has finished loading, each billboard's frames are built on the main thread and
 * its colours loaded off it. An asynchronous scheduler then runs every tick, advancing each playing
 * video, and every half second reconciles who is in range of each billboard.
 * <p>
 * Bandwidth is spent only where a player can see it. A player coming into range is sent the frames,
 * an image's colours the first time only, and a video's current frame in full. A player going out of
 * range, with a margin so the edge of the range does not flicker, is sent the frames' removal. A
 * video's patches go only to players in range.
 */
@Getter
@Singleton
public final class BillboardManager implements Listener {

    /**
     * How many scheduler passes pass between viewer reconciliations: every half second.
     */
    private static final int RECONCILE_INTERVAL = 10;

    /**
     * How far past the view distance, in blocks, a viewer must move before the billboard is removed,
     * so a player standing on the edge of the range is not sent it and its removal repeatedly.
     */
    private static final double DESPAWN_MARGIN = 8.0D;

    /**
     * How far behind a video may fall before it skips ahead rather than catching up frame by frame.
     */
    private static final long MAXIMUM_LAG = TimeUnit.SECONDS.toNanos(1L);

    /**
     * Every registered billboard, or {@code null} until the server has finished loading.
     */
    private List<Billboard> billboardList;

    /**
     * The number of scheduler passes run, for spacing out viewer reconciliation.
     */
    private int passCount;

    /**
     * Builds every registered billboard and loads its colours off the main thread, once, when the
     * server finishes loading. A billboard whose world is not loaded, or whose source cannot be read,
     * is skipped with a warning.
     *
     * @param event the server load event
     */
    @EventHandler
    public void onServerLoad(final ServerLoadEvent event) {
        if (this.billboardList != null) {
            return;
        }

        this.billboardList = InjectorApi.getAll(Billboard.class).stream().toList();

        for (final Billboard billboard : this.billboardList) {
            if (!billboard.build()) {
                Bukkit.getLogger().warning("Billboard %s is in a world that is not loaded".formatted(billboard.getIdentifier()));
                continue;
            }

            UtilTask.executeAsynchronous(() -> {
                try {
                    billboard.load();
                    billboard.markLoaded();
                } catch (final IOException exception) {
                    Bukkit.getLogger().log(Level.WARNING, "Failed to load billboard %s".formatted(billboard.getIdentifier()), exception);
                }
            });
        }
    }

    /**
     * Per-tick pass over every loaded billboard: advances each playing video, and every
     * {@link #RECONCILE_INTERVAL} passes reconciles every billboard's viewers.
     */
    @Scheduler(period = 50, unit = TimeUnit.MILLISECONDS, asynchronous = true)
    public void onScheduler() {
        if (this.billboardList == null) {
            return;
        }

        final long now = System.nanoTime();
        final boolean reconcile = this.passCount++ % RECONCILE_INTERVAL == 0;

        for (final Billboard billboard : this.billboardList) {
            if (!billboard.isLoaded()) {
                continue;
            }

            if (billboard instanceof final BillboardVideo video) {
                this.advance(video, now);
            }

            if (reconcile) {
                this.updateViewers(billboard);
            }
        }
    }

    /**
     * Resumes a video, or restarts it when a video that does not loop has finished.
     *
     * @param video the video to play
     */
    public void play(final BillboardVideo video) {
        video.play();
    }

    /**
     * Pauses a video on its current frame.
     *
     * @param video the video to stop
     */
    public void stop(final BillboardVideo video) {
        video.stop();
    }

    /**
     * Finds the billboard registered under the given identifier.
     *
     * @param identifier the billboard identifier
     * @return an {@link Optional} containing the billboard, or empty if none is registered under it
     */
    public Optional<Billboard> getBillboardByIdentifier(final String identifier) {
        if (this.billboardList == null) {
            return Optional.empty();
        }

        return this.billboardList.stream().filter(billboard -> billboard.getIdentifier().equals(identifier)).findFirst();
    }

    /**
     * Drops the player from every billboard's viewers, for when their client has discarded the frames,
     * so they are sent them again on the next pass. When the client has also discarded its maps, the
     * player is dropped from every image's sent set too, so its colours are sent again.
     *
     * @param player    the player to forget
     * @param clearMaps whether the client has discarded its maps as well
     */
    public void forget(final Player player, final boolean clearMaps) {
        if (this.billboardList == null) {
            return;
        }

        final UUID uuid = player.getUniqueId();

        for (final Billboard billboard : this.billboardList) {
            billboard.getViewerSet().remove(uuid);

            if (clearMaps && billboard instanceof final BillboardImage image) {
                image.getSentSet().remove(uuid);
            }
        }
    }

    /**
     * Advances a playing video by every frame now due, applying each frame's patches to the canvas and
     * sending them to its viewers. Landing on the first frame calls {@link BillboardVideo#onLoop(List)},
     * and a video that does not loop stops once it lands on its last frame.
     *
     * @param video the video to advance
     * @param now   the current time in nanoseconds
     */
    private void advance(final BillboardVideo video, final long now) {
        if (!video.isPlaying()) {
            return;
        }

        if (now - video.getNextFrameTime() > MAXIMUM_LAG) {
            video.setNextFrameTime(now);
        }

        final long interval = TimeUnit.SECONDS.toNanos(1L) / video.getFrameRate();
        final int lastIndex = video.getPatchFrameList().size() - 1;

        while (video.isPlaying() && now >= video.getNextFrameTime()) {
            final int frameIndex = video.getFrameIndex() == lastIndex ? 0 : video.getFrameIndex() + 1;
            final List<Player> viewerList = this.getViewers(video);

            final List<ClientboundMapItemDataPacket> packetList = new ArrayList<>();

            for (final BillboardPatch patch : video.getPatchFrameList().get(frameIndex)) {
                patch.apply(video.getCanvas());

                if (!viewerList.isEmpty()) {
                    packetList.add(this.createPacket(video.getMapIds()[patch.tile()], patch.x(), patch.y(), patch.width(), patch.height(), patch.colors()));
                }
            }

            for (final Player player : viewerList) {
                packetList.forEach(packet -> UtilNms.sendPacket(player, packet));
            }

            video.setFrameIndex(frameIndex);
            video.setNextFrameTime(video.getNextFrameTime() + interval);

            if (frameIndex == 0) {
                video.onLoop(viewerList);
            }

            if (frameIndex == lastIndex && !video.isLooping()) {
                video.stop();
            }
        }
    }

    /**
     * Reconciles the billboard's viewers: a player in its world within the view distance of its centre
     * is shown it, and a viewer past the view distance plus the margin, or no longer in its world, is
     * sent its removal.
     *
     * @param billboard the billboard to reconcile
     */
    private void updateViewers(final Billboard billboard) {
        final Location center = billboard.getCenter();
        final double showDistanceSquared = billboard.getViewDistance() * billboard.getViewDistance();
        final double hideDistanceSquared = (billboard.getViewDistance() + DESPAWN_MARGIN) * (billboard.getViewDistance() + DESPAWN_MARGIN);

        for (final Player player : Bukkit.getServer().getOnlinePlayers()) {
            final boolean viewing = billboard.getViewerSet().contains(player.getUniqueId());

            if (!player.getWorld().equals(center.getWorld())) {
                if (viewing) {
                    this.hide(billboard, player);
                }
                continue;
            }

            final double x = center.getX() - player.getX();
            final double y = center.getY() - player.getY();
            final double z = center.getZ() - player.getZ();
            final double distanceSquared = (x * x) + (y * y) + (z * z);

            if (!viewing && distanceSquared <= showDistanceSquared) {
                this.show(billboard, player);
            } else if (viewing && distanceSquared > hideDistanceSquared) {
                this.hide(billboard, player);
            }
        }
    }

    /**
     * Shows the billboard to the player: its colours first, so no blank map is ever drawn, then its
     * frames. An image's colours are sent only if the player's client does not already hold them. A
     * video's live canvas is always sent, copied per tile since the packet is encoded later on the
     * network thread while playback keeps mutating it.
     *
     * @param billboard the billboard to show
     * @param player    the player to show it to
     */
    private void show(final Billboard billboard, final Player player) {
        final UUID uuid = player.getUniqueId();

        switch (billboard) {
            case final BillboardImage image -> {
                if (image.getSentSet().add(uuid)) {
                    for (int tile = 0; tile < image.getTileCount(); tile++) {
                        UtilNms.sendPacket(player, this.createPacket(image.getMapIds()[tile], 0, 0, UtilMap.MAP_SIZE, UtilMap.MAP_SIZE, image.getTiles()[tile]));
                    }
                }
            }
            case final BillboardVideo video -> {
                for (int tile = 0; tile < video.getTileCount(); tile++) {
                    UtilNms.sendPacket(player, this.createPacket(video.getMapIds()[tile], 0, 0, UtilMap.MAP_SIZE, UtilMap.MAP_SIZE, video.getCanvas()[tile].clone()));
                }
            }
        }

        billboard.getSpawnPacketList().forEach(packet -> UtilNms.sendPacket(player, packet));

        billboard.getViewerSet().add(uuid);
    }

    /**
     * Sends the player the removal of the billboard's frames and drops them from its viewers. The
     * client keeps the maps' colours, so an image is not re-sent them when shown again.
     *
     * @param billboard the billboard to hide
     * @param player    the player to hide it from
     */
    private void hide(final Billboard billboard, final Player player) {
        UtilNms.sendPacket(player, billboard.getRemovePacket());

        billboard.getViewerSet().remove(player.getUniqueId());
    }

    /**
     * Resolves the billboard's current viewers to online players.
     *
     * @param billboard the billboard
     * @return the online viewers
     */
    private List<Player> getViewers(final Billboard billboard) {
        return billboard.getViewerSet().stream().map(Bukkit::getPlayer).filter(Objects::nonNull).toList();
    }

    /**
     * Creates a map data packet updating one region of a map, leaving its decorations untouched.
     *
     * @param mapId  the map's ID
     * @param x      the region's x offset
     * @param y      the region's y offset
     * @param width  the region's width
     * @param height the region's height
     * @param colors the region's packed map colours, row by row
     * @return the packet
     */
    private ClientboundMapItemDataPacket createPacket(final int mapId, final int x, final int y, final int width, final int height, final byte[] colors) {
        return new ClientboundMapItemDataPacket(new MapId(mapId), (byte) 0, true, Optional.empty(), Optional.of(new MapItemSavedData.MapPatch(x, y, width, height, colors)));
    }
}