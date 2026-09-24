package io.github.trae.spigot.framework.picture;

import io.github.trae.di.InjectorApi;
import io.github.trae.di.annotations.method.Scheduler;
import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.utilities.UtilJava;
import lombok.Getter;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Entity;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.persistence.PersistentDataType;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Renders every registered {@link Picture} into locked maps and keeps its frames spawned.
 * <p>
 * Once the server has finished loading, leftover tagged frames are removed and every picture is
 * rendered. Map IDs are reused from the picture's stored state, so a restart creates no new maps,
 * and pixels are only re-rendered when the stored hash no longer matches. A scheduler then spawns
 * any missing frame whose chunk is loaded.
 * <p>
 * Pixels are matched against a palette built from every vanilla {@link MapColor} at each
 * {@link MapColor.Brightness}, using red-mean colour distance.
 */
@Getter
@Singleton
public final class PictureManager implements Listener {

    /**
     * The ARGB value of every drawable map colour, parallel to {@link #PALETTE_ID_ARRAY}.
     */
    private static final int[] PALETTE_ARGB_ARRAY;

    /**
     * The packed map colour ID of every drawable map colour, parallel to {@link #PALETTE_ARGB_ARRAY}.
     */
    private static final byte[] PALETTE_ID_ARRAY;

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
    }

    /**
     * Every registered picture, or {@code null} until the server has finished loading.
     */
    private List<Picture> pictureList;

    /**
     * Removes leftover tagged frames and renders every registered picture, once, when the server
     * finishes loading. A picture whose image cannot be read is skipped with a warning.
     *
     * @param event the server load event
     */
    @EventHandler
    public void onServerLoad(final ServerLoadEvent event) {
        for (final World world : Bukkit.getServer().getWorlds()) {
            world.getEntitiesByClass(ItemFrame.class).stream().filter(this::isTagged).forEach(Entity::remove);
        }

        if (this.pictureList == null) {
            this.pictureList = InjectorApi.getAll(Picture.class).stream().toList();
        }

        for (final Picture picture : this.pictureList) {
            try {
                this.render(picture);
            } catch (final IOException exception) {
                Bukkit.getLogger().log(Level.WARNING, "Failed to load picture %s".formatted(picture.getIdentifier()), exception);
            }
        }
    }

    /**
     * Periodic pass over every rendered picture, spawning the frame for any tile whose frame is
     * missing or no longer valid, such as after a chunk unload, once the tile's chunk is loaded.
     */
    @Scheduler(period = 1, unit = TimeUnit.SECONDS)
    public void onScheduler() {
        if (this.pictureList == null) {
            return;
        }

        for (final Picture picture : this.pictureList) {
            if (picture.getMapViewList() == null) {
                continue;
            }

            for (int index = 0; index < picture.getTileCount(); index++) {
                final ItemFrame itemFrame = picture.getFrameMap().get(index);
                if (itemFrame != null && itemFrame.isValid()) {
                    continue;
                }

                final Block block = picture.getBlock(index);
                if (!block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)) {
                    picture.getFrameMap().remove(index);
                    continue;
                }

                picture.getFrameMap().put(index, this.spawnFrame(picture, index, block));
            }
        }
    }

    /**
     * Returns whether the entity carries the picture tag {@link Picture#IDENTIFIER_KEY}.
     *
     * @param entity the entity to check
     * @return {@code true} if the entity is a picture frame
     */
    public boolean isTagged(final Entity entity) {
        return entity.getPersistentDataContainer().has(Picture.IDENTIFIER_KEY);
    }

    /**
     * Resolves a map for every tile of the picture, reusing the map IDs from its stored state where
     * they still exist and creating maps only for the rest, then draws the image into any map that is
     * new or whose stored hash is stale, and saves the updated state.
     *
     * @param picture the picture to render
     * @throws IOException if the image cannot be read
     */
    private void render(final Picture picture) throws IOException {
        final BufferedImage source = picture.loadImage();
        if (source == null) {
            throw new IOException("Unsupported image format");
        }

        final int columns = picture.getColumns();
        final int width = columns * Picture.MAP_SIZE;
        final int height = picture.getRows() * Picture.MAP_SIZE;

        final BufferedImage image = this.scale(source, width, height);
        final int hash = Arrays.hashCode(image.getRGB(0, 0, width, height, null, 0, width));

        final World world = picture.getLocation().getWorld();
        final int[] state = world.getPersistentDataContainer().get(picture.getStateKey(), PersistentDataType.INTEGER_ARRAY);
        final boolean current = state != null && state[0] == hash;

        final int tileCount = picture.getTileCount();
        final int[] newState = new int[tileCount + 1];
        newState[0] = hash;

        final List<MapView> mapViewList = new ArrayList<>(tileCount);
        final Map<Integer, Byte> colorCache = new HashMap<>();

        for (int index = 0; index < tileCount; index++) {
            final MapView existing = state != null && index + 1 < state.length ? Bukkit.getMap(state[index + 1]) : null;
            final MapView mapView = existing != null ? existing : Bukkit.createMap(world);

            if (!current || existing == null) {
                this.draw(world, mapView, image, (index % columns) * Picture.MAP_SIZE, (index / columns) * Picture.MAP_SIZE, colorCache);
            }

            mapViewList.add(mapView);
            newState[index + 1] = mapView.getId();
        }

        world.getPersistentDataContainer().set(picture.getStateKey(), PersistentDataType.INTEGER_ARRAY, newState);

        picture.setMapViewList(mapViewList);
    }

    /**
     * Locks the map and writes one 128x128 region of the bufferedImage into its saved colours, so vanilla
     * persists the pixels with the map and no renderer is needed. No-op if the map's saved data
     * cannot be resolved.
     *
     * @param world         the world the map belongs to
     * @param mapView       the map to draw into
     * @param bufferedImage the full scaled bufferedImage
     * @param offsetX       the x offset of the tile's region in the bufferedImage
     * @param offsetY       the y offset of the tile's region in the bufferedImage
     * @param colorCache    the ARGB to map colour cache shared across a picture's tiles
     */
    private void draw(final World world, final MapView mapView, final BufferedImage bufferedImage, final int offsetX, final int offsetY, final Map<Integer, Byte> colorCache) {
        mapView.setLocked(true);
        mapView.setTrackingPosition(false);
        mapView.setUnlimitedTracking(false);

        final MapItemSavedData mapItemSavedData = UtilJava.cast(CraftWorld.class, world).getHandle().getMapData(new MapId(mapView.getId()));
        if (mapItemSavedData == null) {
            return;
        }

        for (int y = 0; y < Picture.MAP_SIZE; y++) {
            for (int x = 0; x < Picture.MAP_SIZE; x++) {
                mapItemSavedData.colors[x + y * Picture.MAP_SIZE] = colorCache.computeIfAbsent(bufferedImage.getRGB(offsetX + x, offsetY + y), this::matchColor);
            }
        }

        mapItemSavedData.setDirty();
    }

    /**
     * Returns the packed map colour ID nearest to the given ARGB value by red-mean distance, or
     * {@code 0} (transparent) for pixels under half alpha.
     *
     * @param argb the pixel's ARGB value
     * @return the packed map colour ID
     */
    private byte matchColor(final int argb) {
        if ((argb >>> 24) < 128) {
            return 0;
        }

        final int red = (argb >> 16) & 0xFF;
        final int green = (argb >> 8) & 0xFF;
        final int blue = argb & 0xFF;

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

        return PALETTE_ID_ARRAY[bestIndex];
    }

    /**
     * Spawns an invisible, fixed, invulnerable and non-persistent frame holding the tile's map,
     * tagged with the picture's identifier.
     *
     * @param picture the picture the frame belongs to
     * @param index   the tile index
     * @param block   the block the frame occupies
     * @return the spawned frame
     */
    private ItemFrame spawnFrame(final Picture picture, final int index, final Block block) {
        final ItemStack itemStack = new ItemStack(Material.FILLED_MAP);
        itemStack.editMeta(MapMeta.class, mapMeta -> mapMeta.setMapView(picture.getMapViewList().get(index)));

        final Class<? extends ItemFrame> frameClass = picture.isGlowing() ? GlowItemFrame.class : ItemFrame.class;

        return block.getWorld().spawn(block.getLocation(), frameClass, itemFrame -> {
            itemFrame.getPersistentDataContainer().set(Picture.IDENTIFIER_KEY, PersistentDataType.STRING, picture.getIdentifier());
            itemFrame.setPersistent(false);
            itemFrame.setFacingDirection(picture.getFacing(), true);
            itemFrame.setItem(itemStack, false);
            itemFrame.setVisible(false);
            itemFrame.setFixed(true);
            itemFrame.setInvulnerable(true);
        });
    }

    /**
     * Scales the image to the exact grid size, stretching if the aspect ratio differs. Returns the
     * image unchanged when it is already that size.
     *
     * @param bufferedImage the source image
     * @param width         the target width in pixels
     * @param height        the target height in pixels
     * @return the scaled image
     */
    private BufferedImage scale(final BufferedImage bufferedImage, final int width, final int height) {
        if (bufferedImage.getWidth() == width && bufferedImage.getHeight() == height) {
            return bufferedImage;
        }

        final BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        final Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.drawImage(bufferedImage, 0, 0, width, height, null);
        graphics.dispose();

        return scaled;
    }
}