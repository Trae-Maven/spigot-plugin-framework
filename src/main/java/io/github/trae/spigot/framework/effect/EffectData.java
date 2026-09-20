package io.github.trae.spigot.framework.effect;

import io.github.trae.utilities.mixins.DurationMixin;
import io.github.trae.utilities.mixins.ExpiredMixin;
import io.github.trae.utilities.mixins.RemainingMixin;
import io.github.trae.utilities.mixins.SystemTimeMixin;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * The mutable per-entity state of an {@link Effect}: its strength, when it was applied, and how
 * long it runs for.
 * <p>
 * The mixins derive the rest from those three values, so elapsed time, remaining time, and expiry
 * are all computed against {@code systemTime} rather than stored. Both the amplifier and the
 * duration are writable, which is how {@link Effect#updateUser} lets callers extend, weaken, or end
 * an effect in place.
 */
@AllArgsConstructor
@Getter
@Setter
public class EffectData implements SystemTimeMixin, DurationMixin, RemainingMixin, ExpiredMixin {

    /**
     * The one-based strength of the effect, where {@code 1} is level I. A value of {@code 0} or
     * less means no potion effect is applied, and {@link Effect#updateUser} drops the entry on
     * sight of it.
     */
    private int amplifier;

    /**
     * When the effect was applied, in epoch milliseconds, and how long it runs for from that point,
     * in milliseconds. Extending the effect means raising the duration, not moving the start.
     */
    private long systemTime, duration;
}