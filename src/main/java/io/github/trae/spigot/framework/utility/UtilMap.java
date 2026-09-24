package io.github.trae.spigot.framework.utility;

import lombok.experimental.UtilityClass;
import net.minecraft.world.level.material.MapColor;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Converts images into vanilla map colours.
 * <p>
 * The palette is built once from every vanilla {@link MapColor} at each {@link MapColor.Brightness},
 * and pixels are matched against it by red-mean distance. Matches are cached per colour quantised to
 * five bits per channel, so the cache is bounded at 32768 entries however many distinct colours an
 * image holds.
 */
@UtilityClass
public class UtilMap {

    /**
     * The width and height of a single map in pixels.
     */
    public static final int MAP_SIZE = 128;

    /**
     * The ARGB value of every drawable map colour, parallel to {@link #PALETTE_ID_ARRAY}.
     */
    private static final int[] PALETTE_ARGB_ARRAY;

    /**
     * The packed map colour ID of every drawable map colour, parallel to {@link #PALETTE_ARGB_ARRAY}.
     */
    private static final byte[] PALETTE_ID_ARRAY;

    /**
     * The matched packed colour ID for each quantised colour as an unsigned value, or {@code -1} when
     * not yet matched. Written from any thread; every write for a key stores the same value.
     */
    private static final int[] COLOR_CACHE = new int[1 << 15];

    static {
        final List<MapColor> mapColorList = new ArrayList<>();

        for (int id = 1; id < 64; id++) {
            final MapColor mapColor = MapColor.byId(id);
            if (mapColor != MapColor.NONE) {
                mapColorList.add(mapColor);
            }
        }

        final MapColor.Brightness[] brightnesses = MapColor.Brightness.values();

        PALETTE_ARGB_ARRAY = new int[mapColorList.size() * brightnesses.length];
        PALETTE_ID_ARRAY = new byte[PALETTE_ARGB_ARRAY.length];

        int index = 0;

        for (final MapColor mapColor : mapColorList) {
            for (final MapColor.Brightness brightness : brightnesses) {
                PALETTE_ARGB_ARRAY[index] = mapColor.calculateARGBColor(brightness);
                PALETTE_ID_ARRAY[index] = mapColor.getPackedId(brightness);
                index++;
            }
        }

        Arrays.fill(COLOR_CACHE, -1);
    }

    /**
     * Returns the packed map colour ID nearest to the given ARGB value, or {@code 0} (transparent) for
     * pixels under half alpha.
     *
     * @param argb the pixel's ARGB value
     * @return the packed map colour ID
     */
    public static byte matchColor(final int argb) {
        if ((argb >>> 24) < 128) {
            return 0;
        }

        final int key = ((argb >> 9) & 0x7C00) | ((argb >> 6) & 0x03E0) | ((argb >> 3) & 0x001F);

        final int cached = COLOR_CACHE[key];
        if (cached != -1) {
            return (byte) cached;
        }

        final int red = ((key >> 10) << 3) | 4;
        final int green = (((key >> 5) & 0x1F) << 3) | 4;
        final int blue = ((key & 0x1F) << 3) | 4;

        int bestIndex = 0;
        int bestDistance = Integer.MAX_VALUE;

        for (int index = 0; index < PALETTE_ARGB_ARRAY.length; index++) {
            final int paletteRed = (PALETTE_ARGB_ARRAY[index] >> 16) & 0xFF;
            final int redMean = (red + paletteRed) >> 1;
            final int deltaRed = red - paletteRed;
            final int deltaGreen = green - ((PALETTE_ARGB_ARRAY[index] >> 8) & 0xFF);
            final int deltaBlue = blue - (PALETTE_ARGB_ARRAY[index] & 0xFF);

            final int distance = (((512 + redMean) * deltaRed * deltaRed) >> 8) + (4 * deltaGreen * deltaGreen) + (((767 - redMean) * deltaBlue * deltaBlue) >> 8);

            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = index;
            }
        }

        COLOR_CACHE[key] = PALETTE_ID_ARRAY[bestIndex] & 0xFF;

        return PALETTE_ID_ARRAY[bestIndex];
    }

    /**
     * Converts one 128x128 region of the image into packed map colours, row by row.
     *
     * @param image   the image to read from
     * @param offsetX the x offset of the region
     * @param offsetY the y offset of the region
     * @return the region's packed map colours, {@code 128 * 128} long
     */
    public static byte[] toColors(final BufferedImage image, final int offsetX, final int offsetY) {
        final int[] pixels = image.getRGB(offsetX, offsetY, MAP_SIZE, MAP_SIZE, null, 0, MAP_SIZE);
        final byte[] colors = new byte[pixels.length];

        for (int index = 0; index < pixels.length; index++) {
            colors[index] = matchColor(pixels[index]);
        }

        return colors;
    }

    /**
     * Scales the image to a grid of maps and converts every map's region into packed map colours.
     * Tiles are ordered left to right, then top to bottom.
     *
     * @param image   the image to convert
     * @param columns the grid width in maps
     * @param rows    the grid height in maps
     * @return the packed map colours of every tile
     */
    public static byte[][] toTiles(final BufferedImage image, final int columns, final int rows) {
        final BufferedImage scaled = scale(image, columns * MAP_SIZE, rows * MAP_SIZE);
        final byte[][] tiles = new byte[columns * rows][];

        for (int index = 0; index < tiles.length; index++) {
            tiles[index] = toColors(scaled, (index % columns) * MAP_SIZE, (index / columns) * MAP_SIZE);
        }

        return tiles;
    }

    /**
     * Scales the image to the exact size, stretching if the aspect ratio differs. Returns the image
     * unchanged when it is already that size.
     *
     * @param image  the source image
     * @param width  the target width in pixels
     * @param height the target height in pixels
     * @return the scaled image
     */
    public static BufferedImage scale(final BufferedImage image, final int width, final int height) {
        if (image.getWidth() == width && image.getHeight() == height) {
            return image;
        }

        final BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        final Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.drawImage(image, 0, 0, width, height, null);
        graphics.dispose();

        return scaled;
    }
}