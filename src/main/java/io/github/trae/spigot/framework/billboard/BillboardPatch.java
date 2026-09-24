package io.github.trae.spigot.framework.billboard;

import io.github.trae.spigot.framework.utility.UtilMap;

/**
 * The changed region of one tile between two video frames: the smallest rectangle holding every
 * pixel that differs, with the new frame's colours for all of it.
 * <p>
 * The colours are absolute rather than a difference, so applying a patch to a tile that already
 * holds the new frame changes nothing.
 *
 * @param tile   the tile index
 * @param x      the region's x offset within the tile
 * @param y      the region's y offset within the tile
 * @param width  the region's width
 * @param height the region's height
 * @param colors the region's packed map colours, row by row
 */
public record BillboardPatch(int tile, int x, int y, int width, int height, byte[] colors) {

    /**
     * Writes the patch's colours into the tile's region of the canvas.
     *
     * @param canvas the colours of every tile
     */
    public void apply(final byte[][] canvas) {
        for (int row = 0; row < this.height; row++) {
            System.arraycopy(this.colors, row * this.width, canvas[this.tile], (this.y + row) * UtilMap.MAP_SIZE + this.x, this.width);
        }
    }
}