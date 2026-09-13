package io.github.trae.spigot.framework.damage.modifier;

/**
 * The keys a damage contribution is filed under on a damage event.
 *
 * <p>Each key holds at most one additive and one multiplier value, so two listeners writing to the
 * same key overwrite rather than stack. Splitting contributions across keys is what lets them
 * compose: a critical multiplier and an armour reduction both apply, where two multipliers on one
 * key would not.</p>
 *
 * @see io.github.trae.spigot.framework.damage.events.damage.abstracts.AbstractCustomDamageEvent
 */
public enum DamageModifier {

    /**
     * The attacking item's own damage contribution.
     */
    WEAPON,

    /**
     * The critical hit multiplier.
     */
    CRITICAL,

    /**
     * Contributions from potion effects on the attacker, such as strength and weakness.
     */
    POTION,

    /**
     * The reduction from the damagee's worn armour.
     */
    ARMOUR,

    /**
     * The reduction from protection enchantments on the damagee's armour.
     */
    PROTECTION,

    /**
     * The reduction from the damagee's resistance potion effect.
     */
    RESISTANCE,

    /**
     * Anything a consuming plugin contributes that does not belong under another key.
     */
    CUSTOM
}