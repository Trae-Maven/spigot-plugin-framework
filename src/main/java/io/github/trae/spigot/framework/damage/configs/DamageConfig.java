package io.github.trae.spigot.framework.damage.configs;

import io.github.trae.di.configuration.annotations.Comment;
import io.github.trae.di.configuration.annotations.Configuration;
import io.github.trae.di.configuration.enums.ConfigType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.Map;

/**
 * Settings for the damage pipeline.
 */
@NoArgsConstructor
@Getter
@Setter
@Configuration(value = "Damage", type = ConfigType.JSON)
public class DamageConfig {

    /**
     * Whether combat follows pre-1.9 rules rather than vanilla's.
     */
    @Comment({
            "Whether combat follows pre-1.9 rules.",
            "When enabled, the attack cooldown is removed, and each attacker and environmental cause has its own immunity window on a target.",
            "When disabled, combat matches vanilla: the attack cooldown applies, and each target has one immunity window shared by every source."
    })
    private boolean oldCombatEnabled = false;

    /**
     * How long a target is immune after taking damage.
     */
    @Comment("How long, in milliseconds, a target is immune after taking damage.")
    private Delay delay = new Delay();

    /**
     * Immunity window lengths, in milliseconds.
     */
    @NoArgsConstructor
    @Getter
    @Setter
    public static class Delay {

        /**
         * The window used for any cause without its own value.
         */
        @Comment("The window used for any cause without its own value.")
        private long defaultValue = 500L;

        /**
         * The window per damage cause, by cause name, used in both combat modes.
         */
        @Comment({
                "The window per damage cause, by cause name.",
                "Used in both combat modes.",
                "A value only matters when it is longer than the gap between the cause's own hits."
        })
        private Map<String, Long> damageCauseValues = Map.of(
                DamageCause.ENTITY_ATTACK.name(), 500L,
                DamageCause.DROWNING.name(), 1000L,
                DamageCause.FIRE_TICK.name(), 1000L,
                DamageCause.POISON.name(), 1000L,
                DamageCause.WITHER.name(), 1000L,
                DamageCause.STARVATION.name(), 4000L
        );

        /**
         * The window for the given cause, falling back to the default when the cause has none.
         *
         * @param damageCause the cause to look up
         * @return the window in milliseconds
         */
        public final long getValueByCause(final DamageCause damageCause) {
            return this.damageCauseValues.getOrDefault(damageCause.name(), this.defaultValue);
        }
    }
}