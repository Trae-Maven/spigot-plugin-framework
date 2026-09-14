package io.github.trae.spigot.framework.hologram;

import io.github.trae.spigot.framework.utility.UtilMessage;
import io.github.trae.utilities.UtilJava;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay.TextAlignment;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A packet-only floating text display.
 *
 * <p>Backed by an NMS {@link net.minecraft.world.entity.Display.TextDisplay} that is constructed
 * but never added to a level. The server never ticks it, no chunk persists it, and no other plugin
 * can see it; everything a player observes is produced by the packets in
 * {@link io.github.trae.spigot.framework.utility.UtilHologram}. Settings are applied by driving the
 * entity's {@link org.bukkit.entity.TextDisplay} view, which writes into the same synched data the
 * metadata packet is built from, so no reflection is needed.</p>
 *
 * <p>Subclasses pass a name and location to the constructor and supply {@link #getLines(Player)};
 * everything else has a sensible default and is overridden only when needed. Each hologram is
 * registered as a {@code @Singleton} and collected by {@link HologramManager} through list
 * injection, so a subclass never registers itself.</p>
 *
 * <h2>Lifecycle</h2>
 * <p>The handle is created in {@link #build()}, not the constructor, because constructor injection
 * means a subclass's injected fields are still unassigned while {@code super(...)} runs, and the
 * settings overrides read during a build may depend on them. {@link HologramManager} builds each
 * hologram on the first scheduler pass that finds it unbuilt, and retries any that is still not
 * ready.</p>
 *
 * <h2>Cost</h2>
 * <p>{@link #canSee(Player)} is called once per player per hologram per pass, and
 * {@link #getLines(Player)} once per viewer per pass for a {@link #isDynamic() dynamic} hologram.
 * Both should be cheap and allocation-light: caching text that does not actually vary per player,
 * and deciding visibility without lookups that hit storage.</p>
 *
 * @see HologramManager
 * @see io.github.trae.spigot.framework.utility.UtilHologram
 */
@RequiredArgsConstructor
@Getter
public abstract class Hologram {

    /**
     * The players currently sent this hologram, by UUID.
     *
     * <p>This is the record of what each client has actually been told about, not of what should be
     * visible. It is mutated by the spawn and despawn packet methods, and cleared for a player by
     * {@link HologramManager#forget(Player)} when their client discards its entity table.</p>
     */
    private final Set<UUID> viewerSet = ConcurrentHashMap.newKeySet();

    /**
     * The unique name this hologram is registered and looked up under. Matched case-insensitively.
     */
    private final String name;

    /**
     * Where the hologram sits, with the yaw and pitch it faces when the billboard mode is
     * {@link Billboard#FIXED}.
     *
     * <p>Read on every build and every visibility test. A subclass that needs the position to move
     * overrides the getter instead, and must despawn before the world changes, since a rebuild for a
     * different world assigns a new entity id.</p>
     */
    private final Location location;

    /**
     * The backing NMS entity, or {@code null} until {@link #build()} has run successfully.
     */
    private net.minecraft.world.entity.Display.TextDisplay handle;

    /**
     * The Bukkit view of {@link #handle}, used to apply settings without touching data accessors
     * directly. Shared across all viewers.
     */
    private org.bukkit.entity.TextDisplay textDisplay;

    /**
     * Whether {@link #build()} has produced a usable handle.
     *
     * <p>False until the first scheduler pass builds it, and for any hologram whose world was not
     * loaded when the build was attempted. Nothing may be spawned or sent while this is false,
     * since {@link #getEntityId()} would dereference a null handle.</p>
     *
     * @return whether this hologram has a backing entity
     */
    public final boolean isBuilt() {
        return this.handle != null;
    }

    /**
     * The entity id every viewer's client knows this hologram by.
     *
     * <p>Shared across all viewers, and regenerated whenever {@link #build()} has to recreate the
     * handle for a different world. A hologram must be despawned before that happens, or viewers
     * keep a stale id that no remove packet will ever match.</p>
     *
     * @return the entity id
     */
    public final int getEntityId() {
        return this.handle.getId();
    }

    /**
     * The entity UUID sent in the add packet.
     *
     * @return the entity UUID
     */
    public final UUID getEntityUuid() {
        return this.handle.getUUID();
    }

    /**
     * Resolves this hologram's text for a single player.
     *
     * <p>Deserialises each line returned by {@link #getLines(Player)} as MiniMessage and joins them
     * with newlines, which is how a single text display renders multiple lines.</p>
     *
     * @param player the player to resolve the text for
     * @return the joined component
     */
    public final Component getComponent(final Player player) {
        return Component.join(JoinConfiguration.newlines(), this.getLines(player).stream().map(UtilMessage::deserialize).toList());
    }

    /**
     * The lines of text shown to a given player, as MiniMessage strings.
     *
     * <p>Called once per viewer on every scheduler pass when {@link #isDynamic()} is true, and once
     * per spawn otherwise.</p>
     *
     * @param player the player the text is being resolved for
     * @return the lines, top to bottom
     */
    protected abstract List<String> getLines(final Player player);

    /**
     * This hologram's own visibility rule, checked after the world and distance tests pass.
     *
     * <p>The per-hologram half of the gate pattern: this is the hologram's own decision, while
     * {@link io.github.trae.spigot.framework.hologram.events.HologramSpawnEvent} lets unrelated code
     * veto a hologram it does not own. Called once per player per pass, so keep it cheap.</p>
     *
     * @param player the player to test
     * @return whether the player may see this hologram
     */
    public boolean canSee(final Player player) {
        return true;
    }

    /**
     * How the display rotates to face viewers.
     *
     * <p>{@link Billboard#FIXED} does not rotate at all and faces whatever direction the location's
     * yaw and pitch specify. {@link Billboard#VERTICAL} turns to face the player but stays upright.
     * {@link Billboard#CENTER}, the default, pivots on both axes.</p>
     *
     * @return the billboard mode
     */
    protected Billboard getBillboard() {
        return Billboard.CENTER;
    }

    /**
     * How multiple lines are aligned relative to each other.
     *
     * @return the text alignment
     */
    protected TextAlignment getAlignment() {
        return TextAlignment.CENTER;
    }

    /**
     * The background colour behind the text, as ARGB.
     *
     * <p>Defaults to fully transparent. The vanilla grey box is disabled in {@link #build()}
     * regardless, so this is the only background a viewer sees. Only ever a rectangle: a shaped
     * background needs a bitmap font glyph drawn behind the text instead.</p>
     *
     * @return the background colour
     */
    protected Color getBackgroundColor() {
        return Color.fromARGB(0, 0, 0, 0);
    }

    /**
     * The uniform scale applied to the display.
     *
     * @return the scale factor
     */
    protected float getScale() {
        return 1.0F;
    }

    /**
     * The text opacity, where {@code -1} means fully opaque.
     *
     * @return the opacity byte
     */
    protected byte getTextOpacity() {
        return -1;
    }

    /**
     * The width in pixels at which text wraps.
     *
     * @return the line width
     */
    protected int getLineWidth() {
        return 200;
    }

    /**
     * Whether the text renders through blocks.
     *
     * @return whether the display is visible through terrain
     */
    protected boolean isSeeThrough() {
        return false;
    }

    /**
     * Whether the text is drawn with a shadow.
     *
     * @return whether the text is shadowed
     */
    protected boolean isShadowed() {
        return false;
    }

    /**
     * How far away, in blocks, a player may be and still be sent this hologram.
     *
     * <p>Also drives the display's own view range in {@link #build()}, so the client culls at the
     * same distance the server stops sending at rather than at the vanilla default.</p>
     *
     * @return the view distance in blocks
     */
    public double getViewDistance() {
        return 48.0D;
    }

    /**
     * Whether this hologram's text is re-sent to every viewer on each scheduler pass.
     *
     * <p>Leave this false unless the text actually changes on its own. A dynamic hologram costs one
     * {@link #getLines(Player)} call, one MiniMessage deserialisation per line, one metadata packet
     * and one event dispatch per viewer per pass. A static one costs that only at spawn.</p>
     *
     * @return whether the text is refreshed continuously
     */
    public boolean isDynamic() {
        return false;
    }

    /**
     * Whether this hologram should currently be sent to a player, against an already-resolved
     * location.
     *
     * <p>Tests world, then distance, then {@link #canSee(Player)}, cheapest first. The distance is
     * computed from the player's raw coordinates rather than {@link Player#getLocation()}, which
     * would allocate a {@link Location} per call.</p>
     *
     * @param player   the player to test
     * @param location this hologram's location, possibly {@code null}
     * @return whether the player should be seeing this hologram
     */
    public final boolean isVisible(final Player player, final Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }

        if (!location.getWorld().equals(player.getWorld())) {
            return false;
        }

        final double x = location.getX() - player.getX();
        final double y = location.getY() - player.getY();
        final double z = location.getZ() - player.getZ();

        if ((x * x) + (y * y) + (z * z) > this.getViewDistance() * this.getViewDistance()) {
            return false;
        }

        return this.canSee(player);
    }

    /**
     * Whether this hologram should currently be sent to a player, resolving the location itself.
     *
     * <p>Prefer {@link #isVisible(Player, Location)} in a loop over many players, so a subclass that
     * computes its location resolves it once rather than per player.</p>
     *
     * @param player the player to test
     * @return whether the player should be seeing this hologram
     */
    public final boolean isVisible(final Player player) {
        return this.isVisible(player, this.getLocation());
    }

    /**
     * Creates the backing entity if needed and applies every current setting to it.
     *
     * <p>Does nothing when the location is {@code null} or names an unloaded world, leaving
     * {@link #isBuilt()} false so the caller can retry later. The handle is recreated when the
     * location resolves to a different world than the one it was built against, which assigns a new
     * entity id; callers must despawn before that happens.</p>
     *
     * <p>Applying settings only writes them into the entity's synched data. Nothing reaches a client
     * until a metadata packet is sent, so a settings change on a static hologram needs an explicit
     * update or a despawn and respawn.</p>
     */
    public final void build() {
        final Location location = this.getLocation();
        if (location == null || location.getWorld() == null) {
            return;
        }

        final ServerLevel serverLevel = UtilJava.cast(CraftWorld.class, location.getWorld()).getHandle();

        if (this.handle == null || this.textDisplay == null || !this.handle.level().equals(serverLevel)) {
            this.handle = new net.minecraft.world.entity.Display.TextDisplay(EntityType.TEXT_DISPLAY, serverLevel);
            this.textDisplay = UtilJava.cast(org.bukkit.entity.TextDisplay.class, this.handle.getBukkitEntity());
        }

        this.handle.setPos(location.getX(), location.getY(), location.getZ());
        this.handle.setYRot(location.getYaw());
        this.handle.setXRot(location.getPitch());

        this.textDisplay.setBillboard(this.getBillboard());
        this.textDisplay.setAlignment(this.getAlignment());
        this.textDisplay.setBackgroundColor(this.getBackgroundColor());
        this.textDisplay.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(this.getScale(), this.getScale(), this.getScale()), new Quaternionf()));
        this.textDisplay.setTextOpacity(this.getTextOpacity());
        this.textDisplay.setLineWidth(this.getLineWidth());
        this.textDisplay.setSeeThrough(this.isSeeThrough());
        this.textDisplay.setShadowed(this.isShadowed());
        this.textDisplay.setDefaultBackground(false);
        this.textDisplay.setViewRange((float) (this.getViewDistance() / 64.0D));
    }
}