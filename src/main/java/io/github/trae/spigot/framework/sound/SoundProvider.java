package io.github.trae.spigot.framework.sound;

import io.github.trae.spigot.framework.utility.UtilServer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * A sound and the volume and pitch to play it at, resolved once and replayable anywhere.
 * <p>
 * The sound is held as its key rather than as a {@link Sound}, so a provider can name a sound that
 * exists only in a resource pack as easily as a vanilla one, and so the value survives being written
 * to configuration and read back. A vanilla sound is converted through {@link Registry#SOUNDS} at
 * construction, since a {@link Sound} is a registry entry rather than an enum constant.
 * <p>
 * Every play method is a no-op when the key is absent, which happens only when a {@link Sound} with
 * no registered key is passed in. Nothing is thrown, so a sound that cannot be named simply never
 * plays rather than breaking whatever was about to play it.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class SoundProvider {

    /**
     * The namespaced key of the sound to play, or {@code null} if it could not be resolved.
     */
    private final String key;

    /**
     * The volume and pitch every play uses. Volume above one increases the distance the sound carries
     * rather than its loudness, and pitch is clamped by the client to between half and double speed.
     */
    private final float volume, pitch;

    /**
     * Creates a provider for the sound with the given key.
     *
     * @param key    the namespaced key of the sound, vanilla or from a resource pack
     * @param volume the volume to play at
     * @param pitch  the pitch to play at
     * @return the provider
     */
    public static SoundProvider of(final String key, final float volume, final float pitch) {
        return new SoundProvider(key, volume, pitch);
    }

    /**
     * Creates a provider for the sound with the given key, at full volume and normal pitch.
     *
     * @param key the namespaced key of the sound, vanilla or from a resource pack
     * @return the provider
     */
    public static SoundProvider of(final String key) {
        return of(key, 1.0F, 1.0F);
    }

    /**
     * Creates a provider for a vanilla sound, resolving its key through the registry.
     * <p>
     * A sound carrying no key yields a provider that plays nothing, rather than failing here, so a
     * provider can be declared in a static field without guarding the lookup.
     *
     * @param sound  the sound to play
     * @param volume the volume to play at
     * @param pitch  the pitch to play at
     * @return the provider
     */
    public static SoundProvider of(final Sound sound, final float volume, final float pitch) {
        final NamespacedKey namespacedKey = Registry.SOUNDS.getKey(sound);

        return new SoundProvider(namespacedKey != null ? namespacedKey.asString() : null, volume, pitch);
    }

    /**
     * Creates a provider for a vanilla sound, at full volume and normal pitch.
     *
     * @param sound the sound to play
     * @return the provider
     */
    public static SoundProvider of(final Sound sound) {
        return of(sound, 1.0F, 1.0F);
    }

    /**
     * Plays the sound at the given location, heard by every player in range.
     *
     * @param location the location to play at
     */
    public final void play(final Location location) {
        if (this.key == null) {
            return;
        }

        location.getWorld().playSound(location, this.key, this.volume, this.pitch);
    }

    /**
     * Plays the sound to the given player alone, emitted from the player themselves so it follows them
     * rather than staying where they stood.
     *
     * @param player the player to play to
     */
    public final void play(final Player player) {
        if (this.key == null) {
            return;
        }

        player.playSound(player, this.key, this.volume, this.pitch);
    }

    /**
     * Plays the sound to every online player, each hearing it from their own position, so distance
     * and direction play no part.
     */
    public final void broadcast() {
        if (this.key == null) {
            return;
        }

        UtilServer.getOnlinePlayers().forEach(this::play);
    }
}