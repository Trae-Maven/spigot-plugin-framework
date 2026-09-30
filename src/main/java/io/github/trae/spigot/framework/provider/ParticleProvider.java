package io.github.trae.spigot.framework.provider;

import io.github.trae.spigot.framework.utility.UtilServer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * A particle, with the count, spread, extra value and data to spawn it with, resolved once and replayable anywhere.
 *
 * <p>The particle is held as its key rather than as a {@link Particle}, so the value survives being written to
 * configuration and read back, and resolves through {@link Registry#PARTICLE_TYPE} on every spawn. A vanilla particle
 * is converted to its key at construction.</p>
 *
 * <p>Every spawn method, including {@link #broadcast()}, is a no-op when the key is absent or unregistered, or when the
 * data does not match {@link Particle#getDataType()}: a particle that needs data, such as {@link Particle#DUST} with its
 * {@link Particle.DustOptions}, given none or data of the wrong type, and a particle that takes no data given some.
 * Nothing is thrown, so a provider built straight from a nullable source needs no guard and simply never spawns.</p>
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class ParticleProvider {

    /**
     * The namespaced key of the particle to spawn, or {@code null} if it could not be resolved.
     */
    private final String key;

    /**
     * The number of particles every spawn creates.
     */
    private final int count;

    /**
     * The random spread on each axis and the extra value every spawn uses.
     *
     * <p>The extra value is the speed for most particles. With a count of zero, the offsets are instead read as a
     * direction or colour by particles that support it.</p>
     */
    private final double offsetX, offsetY, offsetZ, extra;

    /**
     * The data every spawn passes, or {@code null} for a particle that takes none.
     */
    private final Object data;

    /**
     * Creates a provider for the particle with the given key.
     *
     * @param key     the namespaced key of the particle, or {@code null} for a provider that spawns nothing
     * @param count   the number of particles to spawn
     * @param offsetX the random spread on the x axis
     * @param offsetY the random spread on the y axis
     * @param offsetZ the random spread on the z axis
     * @param extra   the extra value, usually the speed
     * @param data    the data the particle requires, or {@code null} for a particle that takes none
     * @return the provider
     */
    public static ParticleProvider of(final String key, final int count, final double offsetX, final double offsetY, final double offsetZ, final double extra, final Object data) {
        return new ParticleProvider(key, count, offsetX, offsetY, offsetZ, extra, data);
    }

    /**
     * Creates a provider for the particle with the given key, carrying no data.
     *
     * @param key     the namespaced key of the particle, or {@code null} for a provider that spawns nothing
     * @param count   the number of particles to spawn
     * @param offsetX the random spread on the x axis
     * @param offsetY the random spread on the y axis
     * @param offsetZ the random spread on the z axis
     * @param extra   the extra value, usually the speed
     * @return the provider
     */
    public static ParticleProvider of(final String key, final int count, final double offsetX, final double offsetY, final double offsetZ, final double extra) {
        return of(key, count, offsetX, offsetY, offsetZ, extra, null);
    }

    /**
     * Creates a provider for the particle with the given key, with no spread, no extra value and no data.
     *
     * @param key   the namespaced key of the particle, or {@code null} for a provider that spawns nothing
     * @param count the number of particles to spawn
     * @return the provider
     */
    public static ParticleProvider of(final String key, final int count) {
        return of(key, count, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    /**
     * Creates a provider for a vanilla particle, resolving its key through the registry.
     *
     * <p>A {@code null} particle, or one carrying no key, yields a provider that spawns nothing rather than failing
     * here, so a provider can be built from a nullable source or declared in a static field without guarding the
     * lookup.</p>
     *
     * @param particle the particle to spawn, or {@code null} for a provider that spawns nothing
     * @param count    the number of particles to spawn
     * @param offsetX  the random spread on the x axis
     * @param offsetY  the random spread on the y axis
     * @param offsetZ  the random spread on the z axis
     * @param extra    the extra value, usually the speed
     * @param data     the data the particle requires, or {@code null} for a particle that takes none
     * @return the provider
     */
    public static ParticleProvider of(final Particle particle, final int count, final double offsetX, final double offsetY, final double offsetZ, final double extra, final Object data) {
        final NamespacedKey namespacedKey = particle != null ? Registry.PARTICLE_TYPE.getKey(particle) : null;

        return new ParticleProvider(namespacedKey != null ? namespacedKey.asString() : null, count, offsetX, offsetY, offsetZ, extra, data);
    }

    /**
     * Creates a provider for a vanilla particle, carrying no data.
     *
     * @param particle the particle to spawn, or {@code null} for a provider that spawns nothing
     * @param count    the number of particles to spawn
     * @param offsetX  the random spread on the x axis
     * @param offsetY  the random spread on the y axis
     * @param offsetZ  the random spread on the z axis
     * @param extra    the extra value, usually the speed
     * @return the provider
     */
    public static ParticleProvider of(final Particle particle, final int count, final double offsetX, final double offsetY, final double offsetZ, final double extra) {
        return of(particle, count, offsetX, offsetY, offsetZ, extra, null);
    }

    /**
     * Creates a provider for a vanilla particle, with no spread, no extra value and no data.
     *
     * @param particle the particle to spawn, or {@code null} for a provider that spawns nothing
     * @param count    the number of particles to spawn
     * @return the provider
     */
    public static ParticleProvider of(final Particle particle, final int count) {
        return of(particle, count, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    /**
     * Resolves the key back to a particle through the registry.
     *
     * <p>A missing, malformed or unregistered key is empty rather than throwing.</p>
     *
     * @return the particle, or empty when the key is absent, malformed or not registered
     */
    public Optional<Particle> getParticle() {
        return Optional.ofNullable(this.key)
                .map(NamespacedKey::fromString)
                .map(Registry.PARTICLE_TYPE::get);
    }

    /**
     * Spawns the particle at the given location, seen by every player in range.
     *
     * <p>Does nothing when the key is absent or unregistered, or the data does not match.</p>
     *
     * @param location the location to spawn at
     */
    public void play(final Location location) {
        this.getSpawnableParticle().ifPresent(particle -> location.getWorld().spawnParticle(particle, location, this.count, this.offsetX, this.offsetY, this.offsetZ, this.extra, this.data));
    }

    /**
     * Spawns the particle at the given player's location, seen by that player alone.
     *
     * <p>Does nothing when the key is absent or unregistered, or the data does not match.</p>
     *
     * @param player the player to show to
     */
    public void play(final Player player) {
        this.getSpawnableParticle().ifPresent(particle -> player.spawnParticle(particle, player.getLocation(), this.count, this.offsetX, this.offsetY, this.offsetZ, this.extra, this.data));
    }

    /**
     * Spawns the particle for every online player, each seeing it at their own location alone.
     *
     * <p>Does nothing when the key is absent or unregistered, or the data does not match.</p>
     */
    public void broadcast() {
        if (this.getSpawnableParticle().isEmpty()) {
            return;
        }

        UtilServer.getOnlinePlayers().forEach(this::play);
    }

    private Optional<Particle> getSpawnableParticle() {
        return this.getParticle().filter(particle -> this.data == null ? particle.getDataType() == Void.class : particle.getDataType().isInstance(this.data));
    }
}