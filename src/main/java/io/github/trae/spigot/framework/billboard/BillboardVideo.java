package io.github.trae.spigot.framework.billboard;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * A billboard playing a sequence of frames.
 * <p>
 * Subclasses pass the same arguments as an image billboard and supply the directory holding the
 * frames through {@link #getDirectory()}. Frames are every PNG and JPG in that directory, played in
 * file name order, so the numbered output of a tool such as ffmpeg can be used as it is.
 * <p>
 * The frames are encoded once into patches holding only the regions that change between frames, and
 * cached on disk. Playback runs on one timeline shared by every player: a player coming into range is
 * sent the current frame in full, then only the patches for as long as they stay in range. Nothing
 * is sent to a player out of range.
 */
@Getter
public abstract non-sealed class BillboardVideo extends Billboard {

    /**
     * The file extensions read as frames.
     */
    private static final List<String> FRAME_EXTENSION_LIST = List.of(".png", ".jpg", ".jpeg");

    /**
     * The live colours of every tile, as last played. Patches are applied to it as playback advances,
     * and a player coming into range is sent it in full.
     */
    private byte[][] canvas;

    /**
     * The patches for every frame, each taking the previous frame to this one. The first entry takes
     * the last frame back to the first, for looping and restarting.
     */
    private List<BillboardPatch[]> patchFrameList;

    /**
     * The index of the frame last played.
     */
    private int frameIndex;

    /**
     * The time in nanoseconds the next frame is due.
     */
    private long nextFrameTime;

    /**
     * Whether playback is advancing.
     */
    private volatile boolean playing;

    /**
     * Creates a video billboard.
     *
     * @param identifier the identifier the billboard is registered under
     * @param location   the location of the top-left frame
     * @param facing     the direction the frames face
     * @param columns    the grid width in maps
     * @param rows       the grid height in maps
     */
    public BillboardVideo(final String identifier, final Location location, final BlockFace facing, final int columns, final int rows) {
        super(identifier, location, facing, columns, rows);
    }

    /**
     * Returns the directory holding the frames. The encoded cache is written into it as
     * {@code .cache}.
     *
     * @return the frame directory
     */
    protected abstract File getDirectory();

    /**
     * Returns how many frames play per second. Defaults to {@code 10}, which halves the bandwidth of
     * twenty while still reading as smooth motion.
     *
     * @return the frame rate
     */
    public int getFrameRate() {
        return 10;
    }

    /**
     * Returns whether playback returns to the first frame after the last. Defaults to {@code true}.
     * A video that does not loop holds its last frame until played again.
     *
     * @return {@code true} if the video loops
     */
    public boolean isLooping() {
        return true;
    }

    /**
     * Returns whether playback starts on its own once loaded. Defaults to {@code true}.
     *
     * @return {@code true} if the video plays automatically
     */
    public boolean isAutoplay() {
        return true;
    }

    /**
     * Called whenever playback reaches the first frame, including the first time it starts, for
     * something that should happen once per loop, such as playing a soundtrack. Runs on the manager's
     * asynchronous scheduler.
     *
     * @param viewerList the players currently being sent the video
     */
    protected void onLoop(final List<Player> viewerList) {
    }

    /**
     * Returns the frame files in the directory, in file name order.
     *
     * @return the frame files
     * @throws IOException if the directory holds no frames
     */
    public final List<File> getFrameFiles() throws IOException {
        final File[] files = this.getDirectory().listFiles(file -> file.isFile() && FRAME_EXTENSION_LIST.stream().anyMatch(file.getName().toLowerCase(Locale.ROOT)::endsWith));
        if (files == null || files.length == 0) {
            throw new IOException("No frames in %s".formatted(this.getDirectory()));
        }

        return Arrays.stream(files).sorted(Comparator.comparing(File::getName)).toList();
    }

    /**
     * Encodes the frames, or reads them from cache, and installs them.
     *
     * @throws IOException if the frames cannot be listed or read
     */
    @Override
    final void load() throws IOException {
        BillboardCodec.load(this);
    }

    /**
     * Installs the encoded frames and readies playback, starting it if {@link #isAutoplay()}. The
     * frame index starts on the last frame with the canvas holding the first, so the first advance
     * lands on the first frame and fires {@link #onLoop(List)}. Only called by {@link BillboardCodec}.
     *
     * @param canvas         the colours of every tile at the first frame
     * @param patchFrameList the patches for every frame
     */
    final void install(final byte[][] canvas, final List<BillboardPatch[]> patchFrameList) {
        this.canvas = canvas;
        this.patchFrameList = patchFrameList;
        this.frameIndex = patchFrameList.size() - 1;
        this.nextFrameTime = System.nanoTime();
        this.playing = this.isAutoplay();
    }

    /**
     * Resumes playback from the current frame, or restarts from the first frame when a video that does
     * not loop has finished. No-op until loaded. Only called by {@link BillboardManager}.
     */
    final void play() {
        if (!this.isLoaded()) {
            return;
        }

        this.nextFrameTime = System.nanoTime();
        this.playing = true;
    }

    /**
     * Pauses playback on the current frame. Only called by {@link BillboardManager}.
     */
    final void stop() {
        this.playing = false;
    }

    /**
     * Sets the index of the frame last played. Only called by {@link BillboardManager}.
     *
     * @param frameIndex the frame index
     */
    final void setFrameIndex(final int frameIndex) {
        this.frameIndex = frameIndex;
    }

    /**
     * Sets the time in nanoseconds the next frame is due. Only called by {@link BillboardManager}.
     *
     * @param nextFrameTime the time the next frame is due
     */
    final void setNextFrameTime(final long nextFrameTime) {
        this.nextFrameTime = nextFrameTime;
    }
}