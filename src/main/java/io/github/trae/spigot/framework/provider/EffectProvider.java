package io.github.trae.spigot.framework.provider;

import io.github.trae.spigot.framework.utility.UtilServer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * A world effect, with the data to play it with, resolved once and replayable anywhere.
 *
 * <p>The data is validated against {@link Effect#getData()} at construction. An effect that needs data given none or
 * data of the wrong type, and an effect that takes no data given some, both yield a provider whose effect is absent,
 * rather than one that throws on every play.</p>
 *
 * <p>Every play method, including {@link #broadcast()}, is a no-op when the effect is absent. The effect is absent when
 * a {@code null} effect is passed in or its data does not match. Nothing is thrown, so a provider built straight from a
 * nullable source needs no guard and simply never plays.</p>
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class EffectProvider {

    /**
     * The effect to play, or {@code null} if none was given or its data did not match.
     */
    private final Effect effect;

    /**
     * The data every play passes, or {@code null} for an effect that takes none.
     */
    private final Object data;

    /**
     * Creates a provider for the given effect.
     *
     * @param effect the effect to play, or {@code null} for a provider that plays nothing
     * @param data   the data the effect requires, or {@code null} for an effect that takes none
     * @return the provider
     */
    public static EffectProvider of(final Effect effect, final Object data) {
        final boolean valid = effect != null && (data == null ? effect.getData() == null : effect.getData() != null && effect.getData().isInstance(data));

        return new EffectProvider(valid ? effect : null, data);
    }

    /**
     * Creates a provider for the given effect, carrying no data.
     *
     * @param effect the effect to play, or {@code null} for a provider that plays nothing
     * @return the provider
     */
    public static EffectProvider of(final Effect effect) {
        return of(effect, null);
    }

    /**
     * Plays the effect at the given location, seen and heard by every player in range.
     *
     * <p>Does nothing when the effect is absent.</p>
     *
     * @param location the location to play at
     */
    public void play(final Location location) {
        if (this.effect == null) {
            return;
        }

        location.getWorld().playEffect(location, this.effect, this.data);
    }

    /**
     * Plays the effect at the given player's location, seen and heard by that player alone.
     *
     * <p>Does nothing when the effect is absent.</p>
     *
     * @param player the player to play to
     */
    public void play(final Player player) {
        if (this.effect == null) {
            return;
        }

        player.playEffect(player.getLocation(), this.effect, this.data);
    }

    /**
     * Plays the effect to every online player, each at their own location alone.
     *
     * <p>Does nothing when the effect is absent.</p>
     */
    public void broadcast() {
        if (this.effect == null) {
            return;
        }

        UtilServer.getOnlinePlayers().forEach(this::play);
    }
}