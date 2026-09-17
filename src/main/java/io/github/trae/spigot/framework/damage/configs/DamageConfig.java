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
            "When enabled, the attack cooldown is removed, and every target has its own immunity window per attacker and per environmental cause.",
            "When disabled, the attack cooldown applies, and a player hit by a player gets one immunity window shared by every player, as vanilla does.",
            "Hits that are not PvP use the per attacker and per cause windows in both modes."
    })
    private boolean oldCombatEnabled = false;

    /**
     * The attack speed given to players while old combat is enabled.
     */
    @Comment({
            "The attack speed given to players while old combat is enabled.",
            "High enough that the swing meter refills within a tick, so every hit lands at full strength.",
            "Players get the attribute's default back when old combat is disabled."
    })
    private double oldCombatAttackSpeed = 1024.0D;

    /**
     * How long a target is immune after taking damage.
     */
    @Comment("How long, in milliseconds, a target is immune after taking damage.")
    private Delay delay = new Delay();

    /**
     * What each hit of a timed damage cause deals, and when starvation stops.
     */
    @Comment("What each hit of a timed damage cause deals, and when starvation stops. The defaults match vanilla.")
    private Interval interval = new Interval();

    /**
     * How critical hits are rewarded.
     */
    @Comment("How critical hits are rewarded. The defaults match vanilla.")
    private Critical critical = new Critical();

    /**
     * How far a hit pushes its target.
     */
    @Comment("How far a hit pushes its target. The defaults match vanilla.")
    private Knockback knockback = new Knockback();

    /**
     * How a player's held item is valued in melee damage.
     */
    @Comment("How a player's held item is valued in melee damage. The defaults match vanilla.")
    private WeaponReduction weaponReduction = new WeaponReduction();

    /**
     * How worn armour and protection enchantments reduce incoming damage.
     */
    @Comment("How worn armour and protection enchantments reduce incoming damage. The defaults match vanilla.")
    private ArmourReduction armourReduction = new ArmourReduction();

    /**
     * How potion effects change the damage a hit deals.
     */
    @Comment("How potion effects change the damage a hit deals. The defaults match vanilla.")
    private PotionEffect potionEffect = new PotionEffect();

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
         *
         * <p>For poison, wither, burning, drowning, freezing and starvation, the window is the actual
         * rate, since those causes are offered a hit on every tick while they last. The defaults
         * match vanilla's own intervals.</p>
         */
        @Comment({
                "The window per damage cause, by cause name.",
                "Used in both combat modes.",
                "Poison, wither, burning, drowning, freezing and starvation hit exactly this often, down to one tick.",
                "The defaults match vanilla's own intervals."
        })
        private Map<String, Long> damageCauseValues = Map.of(
                DamageCause.ENTITY_ATTACK.name(), 500L,
                DamageCause.POISON.name(), 1250L,
                DamageCause.WITHER.name(), 2000L,
                DamageCause.FIRE_TICK.name(), 1000L,
                DamageCause.DROWNING.name(), 1000L,
                DamageCause.FREEZE.name(), 2000L,
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

    /**
     * What each hit of a timed damage cause deals, and when starvation stops, with vanilla's figures
     * as the defaults.
     *
     * <p>Poison and wither are not listed, since their hits are dealt by the effect's own code.</p>
     */
    @NoArgsConstructor
    @Getter
    @Setter
    public static class Interval {

        /**
         * The damage each burning hit deals.
         */
        @Comment("The damage each burning hit deals.")
        private float fireTickDamage = 1.0F;

        /**
         * The damage each drowning hit deals.
         */
        @Comment("The damage each drowning hit deals.")
        private float drowningDamage = 2.0F;

        /**
         * The damage each freezing hit deals.
         */
        @Comment("The damage each freezing hit deals.")
        private float freezeDamage = 1.0F;

        /**
         * The damage each freezing hit deals to mobs that suffer extra from the cold.
         */
        @Comment({
                "The damage each freezing hit deals to mobs that suffer extra from the cold.",
                "Covers the types vanilla tags as freeze_hurts_extra_types, such as strays and blazes."
        })
        private float freezeExtraDamage = 5.0F;

        /**
         * The damage each starvation hit deals.
         */
        @Comment("The damage each starvation hit deals.")
        private float starvationDamage = 1.0F;

        /**
         * The health starvation stops at on peaceful and easy.
         */
        @Comment("The health starvation stops at on peaceful and easy.")
        private double starvationEasyMinimumHealth = 10.0D;

        /**
         * The health starvation stops at on normal.
         */
        @Comment({
                "The health starvation stops at on normal.",
                "Starvation never stops on hard."
        })
        private double starvationNormalMinimumHealth = 1.0D;
    }

    /**
     * How critical hits are rewarded, with vanilla's figure as the default.
     */
    @NoArgsConstructor
    @Getter
    @Setter
    public static class Critical {

        /**
         * Whether a critical hit deals extra damage.
         */
        @Comment("Whether a critical hit deals extra damage.")
        private boolean enabled = true;

        /**
         * What a critical hit's damage is multiplied by.
         */
        @Comment("What a critical hit's damage is multiplied by.")
        private double multiplier = 1.5D;
    }

    /**
     * How far a hit pushes its target, with vanilla's figures as the defaults.
     */
    @NoArgsConstructor
    @Getter
    @Setter
    public static class Knockback {

        /**
         * The strength of the push away from the attacker.
         */
        @Comment({
                "The strength of the push away from the attacker.",
                "Reduced by the target's knockback resistance."
        })
        private double strength = 0.4D;

        /**
         * The most upward velocity a grounded target is given.
         */
        @Comment({
                "The most upward velocity a grounded target is given.",
                "An airborne target keeps its own vertical velocity."
        })
        private double verticalLimit = 0.4D;
    }

    /**
     * How a player's held item is valued in melee damage, with vanilla's figures as the defaults.
     */
    @NoArgsConstructor
    @Getter
    @Setter
    public static class WeaponReduction {

        /**
         * The damage the first level of sharpness adds.
         */
        @Comment("The damage the first level of sharpness adds.")
        private double sharpnessBase = 1.0D;

        /**
         * The damage each level of sharpness above the first adds.
         */
        @Comment("The damage each level of sharpness above the first adds.")
        private double sharpnessPerLevel = 0.5D;

        /**
         * The share of the item's damage an uncharged swing keeps.
         */
        @Comment({
                "The share of the item's damage an uncharged swing keeps.",
                "Only used when old combat is disabled."
        })
        private double chargeMinimum = 0.2D;

        /**
         * The share of the item's damage the swing's charge adds on top of the minimum, scaled by the
         * charge squared.
         */
        @Comment({
                "The share of the item's damage the swing's charge adds on top of the minimum, scaled by the charge squared.",
                "The minimum and this together should add up to 1, so a fully charged swing deals the whole damage.",
                "Sharpness is scaled by the charge alone, as in vanilla.",
                "Only used when old combat is disabled."
        })
        private double chargeRange = 0.8D;
    }

    /**
     * How worn armour and protection enchantments reduce incoming damage, with vanilla's figures as
     * the defaults.
     */
    @NoArgsConstructor
    @Getter
    @Setter
    public static class ArmourReduction {

        /**
         * The most armour points that count, however many are worn.
         */
        @Comment("The most armour points that count, however many are worn.")
        private double armourCap = 20.0D;

        /**
         * What armour points are divided by to give the share of damage prevented.
         */
        @Comment({
                "What armour points are divided by to give the share of damage prevented.",
                "With the defaults, 20 points prevents 80% of a hit."
        })
        private double armourDivisor = 25.0D;

        /**
         * What the armour total is divided by to give the least a heavy hit can wear it down to.
         */
        @Comment({
                "What the armour total is divided by to give the least a heavy hit can wear it down to.",
                "With the default, a hit never cuts through more than four fifths of the armour."
        })
        private double armourMinimumDivisor = 5.0D;

        /**
         * The base of the figure a hit's damage is divided by to give the armour points it cuts
         * through.
         */
        @Comment({
                "How much a hit wears armour down: points lost = damage / (toughnessBase + toughness / toughnessDivisor).",
                "The base of that figure, which applies even with no toughness."
        })
        private double toughnessBase = 2.0D;

        /**
         * What toughness is divided by before it is added to the base.
         */
        @Comment({
                "What toughness is divided by before it is added to toughnessBase.",
                "A smaller value makes toughness hold up better against heavy hits."
        })
        private double toughnessDivisor = 4.0D;

        /**
         * The most protection levels that count across the whole set.
         */
        @Comment("The most protection levels that count across the whole set.")
        private double protectionCap = 20.0D;

        /**
         * What protection levels are divided by to give the share of damage prevented.
         */
        @Comment({
                "What protection levels are divided by to give the share of damage prevented.",
                "With the defaults, 20 levels prevents 80% of a hit."
        })
        private double protectionDivisor = 25.0D;
    }

    /**
     * How potion effects change the damage a hit deals, with vanilla's figures as the defaults.
     */
    @NoArgsConstructor
    @Getter
    @Setter
    public static class PotionEffect {

        /**
         * The melee damage each level of strength adds to a player's hit.
         */
        @Comment("The melee damage each level of strength adds to a player's hit.")
        private double strengthPerLevel = 3.0D;

        /**
         * The melee damage each level of weakness removes from a player's hit.
         */
        @Comment({
                "The melee damage each level of weakness removes from a player's hit.",
                "Never takes the hit below what the weapon itself deals."
        })
        private double weaknessPerLevel = 4.0D;

        /**
         * The share of damage each level of resistance prevents.
         */
        @Comment({
                "The share of damage each level of resistance prevents, from 0 to 1.",
                "Damage is never reduced below zero, so enough levels grant total immunity."
        })
        private double resistancePerLevel = 0.2D;

        /**
         * The share of the strength and weakness bonus an uncharged swing keeps.
         */
        @Comment({
                "The share of the strength and weakness bonus an uncharged swing keeps.",
                "Only used when old combat is disabled."
        })
        private double chargeMinimum = 0.2D;

        /**
         * The share of the strength and weakness bonus the swing's charge adds on top of the minimum,
         * scaled by the charge squared.
         */
        @Comment({
                "The share of the strength and weakness bonus the swing's charge adds on top of the minimum, scaled by the charge squared.",
                "The minimum and this together should add up to 1, so a fully charged swing keeps the whole bonus.",
                "Only used when old combat is disabled."
        })
        private double chargeRange = 0.8D;
    }
}