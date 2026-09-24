package io.github.trae.spigot.framework.billboard;

import io.github.trae.spigot.framework.utility.UtilMap;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A billboard showing a still image.
 * <p>
 * Subclasses pass an identifier, the top-left frame location, the facing direction and the grid
 * size, and supply the image through {@link #loadImage()}. PNG, JPG and anything else
 * {@link javax.imageio.ImageIO} reads all work.
 * <p>
 * The image's colours are sent to a player once, the first time they come into range, and never
 * again while the client still holds them. Coming back into range only re-sends the frames, which
 * are a few bytes each. The colours are re-sent only after the client has discarded its maps, on a
 * world change or a rejoin.
 */
@Getter
public abstract non-sealed class BillboardImage extends Billboard {

    /**
     * The players whose client already holds this image's colours, by UUID.
     */
    private final Set<UUID> sentSet = ConcurrentHashMap.newKeySet();

    /**
     * The packed map colours of every tile, or {@code null} until loaded.
     */
    private byte[][] tiles;

    /**
     * Creates an image billboard.
     *
     * @param identifier the identifier the billboard is registered under
     * @param location   the location of the top-left frame
     * @param facing     the direction the frames face
     * @param columns    the grid width in maps
     * @param rows       the grid height in maps
     */
    public BillboardImage(final String identifier, final Location location, final BlockFace facing, final int columns, final int rows) {
        super(identifier, location, facing, columns, rows);
    }

    /**
     * Loads the source image. Called once, off the main thread. The image is scaled to the grid size,
     * stretching if the aspect ratio differs, so it should be exported at {@code columns * 128} by
     * {@code rows * 128} for the best result.
     *
     * @return the source image
     * @throws IOException if the image cannot be read
     */
    protected abstract BufferedImage loadImage() throws IOException;

    /**
     * Loads the image and converts it into the colours of every tile.
     *
     * @throws IOException if the image cannot be read or is in an unsupported format
     */
    @Override
    final void load() throws IOException {
        final BufferedImage image = this.loadImage();
        if (image == null) {
            throw new IOException("Unsupported image format");
        }

        this.tiles = UtilMap.toTiles(image, this.getColumns(), this.getRows());
    }
}