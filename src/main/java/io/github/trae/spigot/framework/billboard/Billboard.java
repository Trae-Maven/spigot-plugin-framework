package io.github.trae.spigot.framework.billboard;

import io.github.trae.utilities.UtilJava;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.maps.MapId;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.CraftWorld;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A grid of maps in invisible item frames on a wall, shown to each player through packets alone.
 * <p>
 * Sealed to its two forms: plugins extend {@link BillboardImage} for a still image or
 * {@link BillboardVideo} for a sequence of frames, never this class. Both are registered as a
 * {@code @Singleton} and collected by {@link BillboardManager}.
 * <p>
 * Nothing exists server-side. The frames are NMS entities constructed but never added to a level,
 * and the maps use IDs counted down from {@link Integer#MAX_VALUE}, so they never collide with a
 * real map and nothing is written to the world. A player within range is sent the frames' spawn
 * packets, and one out of range is sent their removal.
 */
@RequiredArgsConstructor
@Getter
public abstract sealed class Billboard permits BillboardImage, BillboardVideo {

    /**
     * The next map ID to assign, counting down so it never meets the IDs vanilla counts up from zero.
     */
    private static final AtomicInteger MAP_ID_COUNTER = new AtomicInteger(Integer.MAX_VALUE);

    /**
     * The players currently sent this billboard's frames, by UUID.
     */
    private final Set<UUID> viewerSet = ConcurrentHashMap.newKeySet();

    /**
     * The identifier this billboard is registered and looked up under.
     */
    private final String identifier;

    /**
     * The location of the top-left frame: the air block in front of the wall it hangs on.
     */
    private final Location location;

    /**
     * The direction the frames face, out of the wall.
     */
    private final BlockFace facing;

    /**
     * The grid width and height in maps.
     */
    private final int columns, rows;

    /**
     * The map ID of each tile, in tile index order, or {@code null} until built.
     */
    private int[] mapIds;

    /**
     * The add and metadata packets that spawn every frame, or {@code null} until built.
     */
    private List<Packet<?>> spawnPacketList;

    /**
     * The packet that removes every frame, or {@code null} until built.
     */
    private ClientboundRemoveEntitiesPacket removePacket;

    /**
     * The approximate centre of the grid, which view distance is measured from.
     */
    private Location center;

    /**
     * Whether the billboard's colours have been loaded and it can be shown. Set last, so a reader that
     * sees it also sees everything loaded before it.
     */
    private volatile boolean loaded;

    /**
     * Returns whether the frames are glow item frames, which render at full brightness regardless of
     * light level. Defaults to {@code true}.
     *
     * @return {@code true} to use glow item frames
     */
    protected boolean isGlowing() {
        return true;
    }

    /**
     * Returns how far from the centre of the grid, in blocks, a player may be and still be sent the
     * billboard. Defaults to {@code 48.0D}.
     *
     * @return the view distance in blocks
     */
    public double getViewDistance() {
        return 48.0D;
    }

    /**
     * Returns the number of tiles in the grid.
     *
     * @return {@code columns * rows}
     */
    public final int getTileCount() {
        return this.columns * this.rows;
    }

    /**
     * Returns the block the frame for the given tile occupies. Tiles are indexed left to right, then
     * top to bottom, as seen by a player facing the wall.
     *
     * @param index the tile index
     * @return the tile's block
     */
    public final Block getBlock(final int index) {
        return this.location.getBlock().getRelative(this.getRight(), index % this.columns).getRelative(BlockFace.DOWN, index / this.columns);
    }

    /**
     * Loads the billboard's colours. Called once off the main thread by {@link BillboardManager}.
     *
     * @throws IOException if the source cannot be read
     */
    abstract void load() throws IOException;

    /**
     * Assigns a map ID to every tile and builds the packets that spawn and remove its frames, each
     * holding a map with that ID. Only called by {@link BillboardManager}.
     *
     * @return {@code true} if built, {@code false} if the location's world is not loaded
     * @throws IllegalArgumentException if the facing is not horizontal
     */
    final boolean build() {
        final World world = this.location.getWorld();
        if (world == null) {
            return false;
        }

        final ServerLevel serverLevel = UtilJava.cast(CraftWorld.class, world).getHandle();
        final Direction direction = Direction.valueOf(this.facing.name());

        final int tileCount = this.getTileCount();
        final int[] mapIds = new int[tileCount];
        final int[] entityIds = new int[tileCount];
        final List<Packet<?>> spawnPacketList = new ArrayList<>(tileCount * 2);

        for (int index = 0; index < tileCount; index++) {
            final Block block = this.getBlock(index);
            final BlockPos blockPos = new BlockPos(block.getX(), block.getY(), block.getZ());

            final ItemStack itemStack = new ItemStack(Items.FILLED_MAP);
            mapIds[index] = MAP_ID_COUNTER.getAndDecrement();
            itemStack.set(DataComponents.MAP_ID, new MapId(mapIds[index]));

            final ItemFrame itemFrame = this.isGlowing() ? new GlowItemFrame(serverLevel, blockPos, direction) : new ItemFrame(serverLevel, blockPos, direction);
            itemFrame.setItem(itemStack, false, false);
            itemFrame.setInvisible(true);

            spawnPacketList.add(new ClientboundAddEntityPacket(itemFrame, direction.get3DDataValue(), blockPos));
            spawnPacketList.add(new ClientboundSetEntityDataPacket(itemFrame.getId(), itemFrame.getEntityData().getNonDefaultValues()));

            entityIds[index] = itemFrame.getId();
        }

        this.mapIds = mapIds;
        this.spawnPacketList = spawnPacketList;
        this.removePacket = new ClientboundRemoveEntitiesPacket(entityIds);
        this.center = this.getBlock((this.rows / 2) * this.columns + this.columns / 2).getLocation().add(0.5D, 0.5D, 0.5D);

        return true;
    }

    /**
     * Marks the billboard as loaded, so the manager starts showing it. Only called by
     * {@link BillboardManager}.
     */
    final void markLoaded() {
        this.loaded = true;
    }

    /**
     * Returns the direction that is right-hand for a player looking at the frames.
     *
     * @return the right-hand direction
     * @throws IllegalArgumentException if the facing is not horizontal
     */
    private BlockFace getRight() {
        return switch (this.facing) {
            case NORTH -> BlockFace.WEST;
            case EAST -> BlockFace.NORTH;
            case SOUTH -> BlockFace.EAST;
            case WEST -> BlockFace.SOUTH;
            default -> throw new IllegalArgumentException("Unsupported facing: %s".formatted(this.facing));
        };
    }
}