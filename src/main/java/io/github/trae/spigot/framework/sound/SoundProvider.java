package io.github.trae.spigot.framework.sound;

import io.github.trae.spigot.framework.utility.UtilServer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * A sound, with the category, volume and pitch to play it at, resolved once and replayable anywhere.
 *
 * <p>The sound is held as its key rather than as a {@link Sound}, so a provider can name a sound that
 * exists only in a resource pack as easily as a vanilla one, and so the value survives being written
 * to configuration and read back. A vanilla sound is converted through {@link Registry#SOUNDS} at
 * construction, since a {@link Sound} is a registry entry rather than an enum constant.</p>
 *
 * <p>Every play method, including {@link #broadcast()}, is a no-op when the key or the category is
 * absent. The key is absent when a {@code null} or unregistered {@link Sound} is passed in, or a
 * {@code null} key is given directly. Nothing is thrown, so a provider built straight from a nullable
 * source, such as the death sound of an entity that has none, needs no guard and simply never
 * plays.</p>
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class SoundProvider {

    /**
     * The namespaced key of the sound to play, or {@code null} if it could not be resolved.
     */
    private final String key;

    /**
     * The category every play uses, which decides the client volume slider that controls the sound,
     * or {@code null} if none was given.
     */
    private final SoundCategory category;

    /**
     * The volume and pitch every play uses.
     *
     * <p>Volume above one increases the distance the sound carries rather than its loudness, and
     * pitch is clamped by the client to between half and double speed.</p>
     */
    private final float volume, pitch;

    /**
     * Creates a provider for the sound with the given key.
     *
     * @param key      the namespaced key of the sound, vanilla or from a resource pack, or
     *                 {@code null} for a provider that plays nothing
     * @param category the category to play under
     * @param volume   the volume to play at
     * @param pitch    the pitch to play at
     * @return the provider
     */
    public static SoundProvider of(final String key, final SoundCategory category, final float volume, final float pitch) {
        return new SoundProvider(key, category, volume, pitch);
    }

    /**
     * Creates a provider for the sound with the given key, at full volume and normal pitch.
     *
     * @param key      the namespaced key of the sound, vanilla or from a resource pack, or
     *                 {@code null} for a provider that plays nothing
     * @param category the category to play under
     * @return the provider
     */
    public static SoundProvider of(final String key, final SoundCategory category) {
        return of(key, category, 1.0F, 1.0F);
    }

    /**
     * Creates a provider for a vanilla sound, resolving its key through the registry.
     *
     * <p>A {@code null} sound, or one carrying no key, yields a provider that plays nothing rather
     * than failing here, so a provider can be built from a nullable source or declared in a static
     * field without guarding the lookup.</p>
     *
     * @param sound    the sound to play, or {@code null} for a provider that plays nothing
     * @param category the category to play under
     * @param volume   the volume to play at
     * @param pitch    the pitch to play at
     * @return the provider
     */
    public static SoundProvider of(final Sound sound, final SoundCategory category, final float volume, final float pitch) {
        final NamespacedKey namespacedKey = sound != null ? Registry.SOUNDS.getKey(sound) : null;

        return new SoundProvider(namespacedKey != null ? namespacedKey.asString() : null, category, volume, pitch);
    }

    /**
     * Creates a provider for a vanilla sound, at full volume and normal pitch.
     *
     * @param sound    the sound to play, or {@code null} for a provider that plays nothing
     * @param category the category to play under
     * @return the provider
     */
    public static SoundProvider of(final Sound sound, final SoundCategory category) {
        return of(sound, category, 1.0F, 1.0F);
    }

    /**
     * Resolves the key back to a vanilla sound through the registry.
     *
     * <p>Only sounds the server knows about resolve, so a key that names a sound from a resource pack
     * yields an empty result even though it still plays. A missing or malformed key is empty too,
     * rather than throwing.</p>
     *
     * @return the sound, or empty when the key is absent, malformed or not registered
     */
    public Optional<Sound> getSound() {
        return Optional.ofNullable(this.key)
                .map(NamespacedKey::fromString)
                .map(Registry.SOUNDS::get);
    }

    /**
     * Plays the sound at the given location, heard by every player in range.
     *
     * <p>Does nothing when the key or the category is absent.</p>
     *
     * @param location the location to play at
     */
    public void play(final Location location) {
        if (this.key == null || this.category == null) {
            return;
        }

        location.getWorld().playSound(location, this.key, this.category, this.volume, this.pitch);
    }

    /**
     * Plays the sound to the given player alone, emitted from the player themselves so it follows them
     * rather than staying where they stood.
     *
     * <p>Does nothing when the key or the category is absent.</p>
     *
     * @param player the player to play to
     */
    public void play(final Player player) {
        if (this.key == null || this.category == null) {
            return;
        }

        player.playSound(player, this.key, this.category, this.volume, this.pitch);
    }

    /**
     * Plays the sound to every online player, each hearing it from their own position, so distance
     * and direction play no part.
     *
     * <p>Does nothing when the key or the category is absent.</p>
     */
    public void broadcast() {
        if (this.key == null || this.category == null) {
            return;
        }

        UtilServer.getOnlinePlayers().forEach(this::play);
    }
}