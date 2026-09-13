package io.github.trae.spigot.framework.damage.data;

import io.github.trae.utilities.mixins.DurationMixin;
import io.github.trae.utilities.mixins.ExpiredMixin;
import io.github.trae.utilities.mixins.RemainingMixin;
import io.github.trae.utilities.mixins.SystemTimeMixin;
import lombok.Getter;
import net.kyori.adventure.text.Component;

/**
 * A reason that outlives the damage pass it was set on.
 *
 * <p>A plain {@link Reason} describes only the hit it belongs to, which is wrong for anything whose
 * effect lingers. An ability that sets someone on fire should be named in the death message even
 * though the killing blow was the fire itself, and a poison should be credited to whoever applied
 * it rather than to the tick that finished the job.</p>
 *
 * <p>The manager retains one of these per damagee and attacker pair for as long as its duration
 * lasts, so a later death from any source is attributed to it instead of to whatever landed last.
 * Once expired it is dropped and the pass's own reason takes over again.</p>
 *
 * <p>Unlike a plain reason, the name is used verbatim in a message with no article in front of it,
 * since an ability reads as "killed by Bob with Frostbite" rather than "with a Frostbite".</p>
 *
 * @see Reason
 * @see io.github.trae.spigot.framework.damage.DamageManager
 */
@Getter
public class CustomReason extends Reason implements SystemTimeMixin, DurationMixin, RemainingMixin, ExpiredMixin {

    /**
     * When the reason was set, and how long it stays authoritative for, both in milliseconds.
     */
    private final long systemTime, duration;

    protected CustomReason(final Component name, final long systemTime, final long duration) {
        super(name);

        this.systemTime = systemTime;
        this.duration = duration;
    }

    /**
     * Builds a reason that started at a given moment.
     *
     * <p>Use this when the reason should age from something other than now, such as an effect that
     * was applied earlier in the same pass.</p>
     *
     * @param name       how it reads in a message
     * @param systemTime when it started, in milliseconds
     * @param duration   how long it stays authoritative, in milliseconds
     * @return the reason
     */
    public static CustomReason of(final Component name, final long systemTime, final long duration) {
        return new CustomReason(name, systemTime, duration);
    }

    /**
     * Builds a reason starting now.
     *
     * @param name     how it reads in a message
     * @param duration how long it stays authoritative, in milliseconds
     * @return the reason
     */
    public static CustomReason of(final Component name, final long duration) {
        return new CustomReason(name, System.currentTimeMillis(), duration);
    }
}