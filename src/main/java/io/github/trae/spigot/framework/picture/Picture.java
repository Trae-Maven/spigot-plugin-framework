package io.github.trae.spigot.framework.picture;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemFrame;
import org.bukkit.map.MapView;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * An image shown on a wall as a grid of locked maps in invisible item frames.
 * <p>
 * Subclasses pass an identifier, the top-left frame location, the facing direction and the grid
 * size to the constructor, and supply the image through {@link #loadImage()}. Each picture is
 * registered as a {@code @Singleton} and collected by {@link PictureManager}.
 * <p>
 * The image is rendered into one map per 128x128 tile. The map IDs and a hash of the rendered pixels
 * are stored in the world's persistent data under {@link #getStateKey()}, so the same maps are
 * reused across restarts and are only re-rendered when the image or grid changes. The frames
 * themselves are spawned non-persistent and tagged with {@link #IDENTIFIER_KEY}, so they are never
 * saved to the chunk, and are respawned whenever their chunk is loaded.
 */
@RequiredArgsConstructor
@Getter
public abstract class Picture {

    /**
     * The persistent data key every frame is tagged with, holding the picture's identifier.
     */
    public static final NamespacedKey IDENTIFIER_KEY = new NamespacedKey("custom", "picture_identifier");

    /**
     * The width and height of a single map in pixels.
     */
    public static final int MAP_SIZE = 128;

    /**
     * The spawned frame for each tile, keyed by tile index. A missing or invalid entry is respawned by
     * the manager once its chunk is loaded.
     */
    private final Map<Integer, ItemFrame> frameMap = new HashMap<>();

    /**
     * The identifier this picture is registered under, also used to build its state key. Must only
     * contain characters valid in a {@link NamespacedKey} key once lowercased.
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
     * The map for each tile, in tile index order, or {@code null} until the manager has rendered it.
     */
    private List<MapView> mapViewList;

    /**
     * Loads the source image. Called once at startup; the image is scaled to the grid size, stretching
     * if the aspect ratio differs, so it should be exported at {@code columns * 128} by
     * {@code rows * 128} for the best result.
     *
     * @return the source image
     * @throws IOException if the image cannot be read
     */
    protected abstract BufferedImage loadImage() throws IOException;

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
     * Returns the number of tiles in the grid.
     *
     * @return {@code columns * rows}
     */
    public final int getTileCount() {
        return this.columns * this.rows;
    }

    /**
     * Returns the world persistent data key this picture's rendered state is stored under.
     *
     * @return the state key
     */
    public final NamespacedKey getStateKey() {
        return new NamespacedKey("custom", "picture_state_%s".formatted(this.identifier.toLowerCase(Locale.ROOT)));
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
     * Sets the rendered map for each tile. Only called by {@link PictureManager}.
     *
     * @param mapViewList the map for each tile, in tile index order
     */
    public final void setMapViewList(final List<MapView> mapViewList) {
        this.mapViewList = mapViewList;
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