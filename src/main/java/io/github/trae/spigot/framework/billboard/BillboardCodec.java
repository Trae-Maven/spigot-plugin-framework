package io.github.trae.spigot.framework.billboard;

import io.github.trae.spigot.framework.utility.UtilMap;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Encodes a video billboard's frames into patches, and caches the result on disk.
 * <p>
 * Encoding scales each frame to the grid, matches it to map colours, and keeps only the region of
 * each tile that differs from the previous frame. That is the expensive part, so the result is
 * written deflated to {@code .cache} in the frame directory and read back on later loads. The cache
 * is keyed on every frame file's name, size and modification time along with the grid size, so
 * changing any frame or the grid re-encodes it.
 */
@UtilityClass
class BillboardCodec {

    /**
     * The name of the cache file within the frame directory.
     */
    private static final String CACHE_FILE_NAME = ".cache";

    /**
     * Marks a file as a billboard cache.
     */
    private static final int MAGIC = 0x42424430;

    /**
     * The cache format version, folded into the key so a format change invalidates every cache.
     */
    private static final int VERSION = 1;

    /**
     * Installs the video's encoded frames, from its cache when that is current, otherwise by encoding
     * them and writing a new cache. Failing to write the cache is logged and does not fail the load.
     *
     * @param video the video to load
     * @throws IOException if the frames cannot be listed or read
     */
    static void load(final BillboardVideo video) throws IOException {
        final List<File> frameFiles = video.getFrameFiles();
        final long key = getKey(video, frameFiles);
        final File cacheFile = new File(video.getDirectory(), CACHE_FILE_NAME);

        if (cacheFile.isFile() && read(video, cacheFile, key)) {
            return;
        }

        encode(video, frameFiles);

        try {
            write(video, cacheFile, key);
        } catch (final IOException exception) {
            Bukkit.getLogger().log(Level.WARNING, "Failed to write cache for billboard %s".formatted(video.getIdentifier()), exception);
        }
    }

    /**
     * Returns the cache key for the video's current frames and grid.
     *
     * @param video      the video
     * @param frameFiles the video's frame files
     * @return the cache key
     */
    private static long getKey(final BillboardVideo video, final List<File> frameFiles) {
        long key = VERSION;
        key = 31L * key + video.getColumns();
        key = 31L * key + video.getRows();

        for (final File file : frameFiles) {
            key = 31L * key + file.getName().hashCode();
            key = 31L * key + file.length();
            key = 31L * key + file.lastModified();
        }

        return key;
    }

    /**
     * Encodes every frame into patches against the frame before it, then the first frame against the
     * last, for looping, and installs the result.
     *
     * @param video      the video
     * @param frameFiles the video's frame files, in order
     * @throws IOException if a frame cannot be read
     */
    private static void encode(final BillboardVideo video, final List<File> frameFiles) throws IOException {
        final List<BillboardPatch[]> patchFrameList = new ArrayList<>(frameFiles.size());

        byte[][] first = null;
        byte[][] previous = null;

        for (final File file : frameFiles) {
            final BufferedImage image = ImageIO.read(file);
            if (image == null) {
                throw new IOException("Unsupported image format: %s".formatted(file.getName()));
            }

            final byte[][] tiles = UtilMap.toTiles(image, video.getColumns(), video.getRows());

            patchFrameList.add(previous == null ? null : diff(previous, tiles));

            if (first == null) {
                first = tiles;
            }

            previous = tiles;
        }

        patchFrameList.set(0, diff(previous, first));

        video.install(first, patchFrameList);
    }

    /**
     * Returns a patch for every tile that differs between the two frames, covering the smallest
     * rectangle holding every changed pixel. Tiles that are identical produce no patch.
     *
     * @param previous the colours of every tile in the previous frame
     * @param current  the colours of every tile in the current frame
     * @return the patches taking the previous frame to the current one
     */
    private static BillboardPatch[] diff(final byte[][] previous, final byte[][] current) {
        final List<BillboardPatch> patchList = new ArrayList<>();

        for (int tile = 0; tile < current.length; tile++) {
            int minX = UtilMap.MAP_SIZE, minY = UtilMap.MAP_SIZE, maxX = -1, maxY = -1;

            for (int y = 0; y < UtilMap.MAP_SIZE; y++) {
                for (int x = 0; x < UtilMap.MAP_SIZE; x++) {
                    final int index = y * UtilMap.MAP_SIZE + x;
                    if (previous[tile][index] != current[tile][index]) {
                        minX = Math.min(minX, x);
                        minY = Math.min(minY, y);
                        maxX = Math.max(maxX, x);
                        maxY = Math.max(maxY, y);
                    }
                }
            }

            if (maxX < 0) {
                continue;
            }

            final int patchWidth = maxX - minX + 1;
            final int patchHeight = maxY - minY + 1;
            final byte[] colors = new byte[patchWidth * patchHeight];

            for (int row = 0; row < patchHeight; row++) {
                System.arraycopy(current[tile], (minY + row) * UtilMap.MAP_SIZE + minX, colors, row * patchWidth, patchWidth);
            }

            patchList.add(new BillboardPatch(tile, minX, minY, patchWidth, patchHeight, colors));
        }

        return patchList.toArray(new BillboardPatch[0]);
    }

    /**
     * Writes the video's installed frames to the cache file, deflated.
     *
     * @param video the video
     * @param file  the cache file
     * @param key   the cache key
     * @throws IOException if the file cannot be written
     */
    private static void write(final BillboardVideo video, final File file, final long key) throws IOException {
        try (final DataOutputStream output = new DataOutputStream(new BufferedOutputStream(new DeflaterOutputStream(new FileOutputStream(file))))) {
            output.writeInt(MAGIC);
            output.writeLong(key);
            output.writeInt(video.getCanvas().length);

            for (final byte[] tile : video.getCanvas()) {
                output.write(tile);
            }

            output.writeInt(video.getPatchFrameList().size());

            for (final BillboardPatch[] patches : video.getPatchFrameList()) {
                output.writeInt(patches.length);

                for (final BillboardPatch patch : patches) {
                    output.writeShort(patch.tile());
                    output.writeByte(patch.x());
                    output.writeByte(patch.y());
                    output.writeByte(patch.width());
                    output.writeByte(patch.height());
                    output.write(patch.colors());
                }
            }
        }
    }

    /**
     * Reads the encoded frames from the cache file and installs them. The cache is written after
     * installing, so the canvas it holds is the first frame.
     *
     * @param video the video
     * @param file  the cache file
     * @param key   the expected cache key
     * @return {@code true} if installed, {@code false} if the cache is stale, from another format, or
     * cannot be read
     */
    private static boolean read(final BillboardVideo video, final File file, final long key) {
        try (final DataInputStream input = new DataInputStream(new BufferedInputStream(new InflaterInputStream(new FileInputStream(file))))) {
            if (input.readInt() != MAGIC || input.readLong() != key || input.readInt() != video.getTileCount()) {
                return false;
            }

            final byte[][] canvas = new byte[video.getTileCount()][UtilMap.MAP_SIZE * UtilMap.MAP_SIZE];

            for (final byte[] tile : canvas) {
                input.readFully(tile);
            }

            final int frameCount = input.readInt();
            final List<BillboardPatch[]> patchFrameList = new ArrayList<>(frameCount);

            for (int frame = 0; frame < frameCount; frame++) {
                final BillboardPatch[] patches = new BillboardPatch[input.readInt()];

                for (int index = 0; index < patches.length; index++) {
                    final int tile = input.readUnsignedShort();
                    final int x = input.readUnsignedByte();
                    final int y = input.readUnsignedByte();
                    final int width = input.readUnsignedByte();
                    final int height = input.readUnsignedByte();

                    final byte[] colors = new byte[width * height];
                    input.readFully(colors);

                    patches[index] = new BillboardPatch(tile, x, y, width, height, colors);
                }

                patchFrameList.add(patches);
            }

            video.install(canvas, patchFrameList);

            return true;
        } catch (final IOException exception) {
            return false;
        }
    }
}