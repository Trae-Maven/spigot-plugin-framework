package io.github.trae.spigot.framework.damage.events.damage.abstracts;

import io.github.trae.spigot.framework.damage.data.Reason;
import io.github.trae.spigot.framework.damage.modifier.DamageModifier;
import io.github.trae.spigot.framework.displayname.DisplayName;
import io.github.trae.spigot.framework.event.CustomCancellableEvent;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Optional;

/**
 * Shared state for every stage of a damage pass.
 *
 * <p>One instance exists per stage, but the modifier maps are passed by reference between them, so
 * a contribution written at the pre stage is still present at post. The damage figure a consumer
 * should act on is {@link #getFinalDamage()}, never {@code getDamage()} alone, since the latter is
 * only the base before modifiers.</p>
 *
 * <h2>Stages</h2>
 * <p>The pre stage gates the damage and establishes the base. The damage stage is where consuming
 * plugins set ability damage. The post stage applies reductions and side effects. Cancelling at any
 * stage stops the chain, so a later stage never runs against damage that was refused.</p>
 *
 * @see io.github.trae.spigot.framework.damage.events.damage.CustomPreDamageEvent
 * @see io.github.trae.spigot.framework.damage.events.damage.CustomDamageEvent
 * @see io.github.trae.spigot.framework.damage.events.damage.CustomPostDamageEvent
 */
@Getter
@Setter
public abstract class AbstractCustomDamageEvent extends CustomCancellableEvent {

    /**
     * When this damage pass began, in milliseconds.
     *
     * <p>Stamped once at the pre stage and carried through, so every stage and every listener
     * shares one reference point rather than each reading the clock independently.</p>
     */
    private final long systemTime;

    /**
     * Flat contributions, applied before any multiplier.
     */
    private final Map<DamageModifier, Double> additiveMap;

    /**
     * Proportional contributions, applied after every additive.
     */
    private final Map<DamageModifier, Double> multiplierMap;

    /**
     * The entity taking the damage, and the entity dealing it. The damager is {@code null} for
     * environmental causes, and is resolved to the shooter rather than the projectile when the
     * damage came from one.
     */
    private final Entity damagee, damager;

    /**
     * The projectile that carried the damage, or {@code null} when the attack was direct.
     */
    private final Projectile projectile;

    /**
     * The vanilla damage source, carried through so the applied damage is attributed correctly in
     * death messages and combat tracking.
     */
    private final DamageSource source;

    /**
     * What caused the damage. Drives which reductions apply and what delay is enforced.
     */
    private final DamageCause cause;

    /**
     * The damager's main hand item at the time of the attack, or {@code null} when there was no
     * damager or nothing held.
     */
    private final ItemStack itemStack;

    /**
     * The damagee's armour at the time of the attack. May be {@code null} for a non-living damagee,
     * and individual slots are {@code null} when unequipped.
     */
    private final ItemStack[] armourContents;

    /**
     * What vanilla would have dealt before the framework took over. Kept for reference and logging;
     * it is not the base the pipeline builds on for attacks, since the weapon contribution is
     * resolved from the item instead.
     */
    private final double originalDamage;

    /**
     * Whether the attack met the conditions for a critical hit, resolved once at the pre stage.
     */
    private final boolean critical;

    /**
     * The base damage before modifiers. Set this to replace the base outright, which is what an
     * ability with a flat damage figure does.
     */
    private double damage;

    /**
     * How long, in milliseconds, this damagee is immune to the same source afterwards. Zero leaves
     * the delay listener to pick a default from the cause.
     */
    private long delay;

    /**
     * How each side is named in messages. Seeded from the entities, and settable, so a plugin with
     * display names, ranks or nicknames writes them once here rather than everywhere a message is
     * built. The damager's name is {@code null} for environmental causes.
     */
    private DisplayName damageeName, damagerName;

    /**
     * How the cause reads in a message, cleaned from the enum constant so it is ordinary text rather
     * than a constant name.
     */
    private Component causeName;

    /**
     * What the damage is attributed to in messages. Seeded from the damager's held item, complete
     * with its hover tooltip, and {@code null} when there was no item or no damager.
     *
     * <p>Set a plain {@link Reason} to attribute this hit to something else. Set a
     * {@link io.github.trae.spigot.framework.damage.data.CustomReason} to have the attribution
     * outlive the hit, which is what an ability whose effect lingers wants.</p>
     */
    private Reason reason;

    protected AbstractCustomDamageEvent(final long systemTime, final Map<DamageModifier, Double> additiveMap, final Map<DamageModifier, Double> multiplierMap, final Entity damagee, final Entity damager, final Projectile projectile, final DamageSource source, final DamageCause cause, final ItemStack itemStack, final ItemStack[] armourContents, final double originalDamage, final boolean critical, final double damage, final long delay, final DisplayName damageeName, final DisplayName damagerName, final Component causeName, final Reason reason) {
        this.systemTime = systemTime;
        this.additiveMap = additiveMap;
        this.multiplierMap = multiplierMap;
        this.damagee = damagee;
        this.damager = damager;
        this.projectile = projectile;
        this.source = source;
        this.cause = cause;
        this.itemStack = itemStack;
        this.armourContents = armourContents;
        this.originalDamage = originalDamage;
        this.critical = critical;
        this.damage = damage;
        this.delay = delay;
        this.damageeName = damageeName;
        this.damagerName = damagerName;
        this.causeName = causeName;
        this.reason = reason;
    }

    /**
     * The flat contribution filed under the given key, or zero if none.
     *
     * @param damageModifier the key to read
     * @return the additive value
     */
    public final double getModifier(final DamageModifier damageModifier) {
        return this.additiveMap.getOrDefault(damageModifier, 0.0D);
    }

    /**
     * Replaces the flat contribution under the given key.
     *
     * <p>Use this where the key holds one thing, such as a weapon's own damage, so re-running the
     * calculation does not double it.</p>
     *
     * @param damageModifier the key to write
     * @param value          the additive value, negative to reduce
     */
    public final void setModifier(final DamageModifier damageModifier, final double value) {
        this.additiveMap.put(damageModifier, value);
    }

    /**
     * Adds to the flat contribution under the given key.
     *
     * <p>Use this where contributions genuinely stack, such as several armour pieces each reducing
     * a share of the damage.</p>
     *
     * @param damageModifier the key to write
     * @param value          the additive value, negative to reduce
     */
    public final void addModifier(final DamageModifier damageModifier, final double value) {
        this.additiveMap.merge(damageModifier, value, Double::sum);
    }

    /**
     * The proportional contribution filed under the given key, or one if none.
     *
     * @param damageModifier the key to read
     * @return the multiplier
     */
    public final double getMultiplier(final DamageModifier damageModifier) {
        return this.multiplierMap.getOrDefault(damageModifier, 1.0D);
    }

    /**
     * Replaces the proportional contribution under the given key.
     *
     * <p>Multipliers compose across keys, so 1.5 under one key and 0.5 under another produce 0.75
     * overall. Two writes to the same key do not compose; the second wins.</p>
     *
     * @param damageModifier the key to write
     * @param value          the multiplier, below one to reduce
     */
    public final void setMultiplier(final DamageModifier damageModifier, final double value) {
        this.multiplierMap.put(damageModifier, value);
    }

    /**
     * Drops both the additive and the multiplier under the given key.
     *
     * <p>This is how an ability with a flat damage figure discards a contribution it does not want,
     * such as the weapon's own damage.</p>
     *
     * @param damageModifier the key to clear
     */
    public final void removeModifier(final DamageModifier damageModifier) {
        this.additiveMap.remove(damageModifier);
        this.multiplierMap.remove(damageModifier);
    }

    /**
     * The damager's held item, if there was one.
     *
     * @return the item, or empty
     */
    public final Optional<ItemStack> getItemStack() {
        return Optional.ofNullable(this.itemStack);
    }

    /**
     * Whether the base damage is above zero, before modifiers.
     *
     * @return whether there is base damage
     */
    public final boolean hasDamage() {
        return this.damage > 0.0D;
    }

    /**
     * The damage that will actually be dealt.
     *
     * <p>Every additive is applied to the base first, then every multiplier, and the result is
     * floored at zero. That ordering is why a reduction written as a negative additive is still
     * scaled by a multiplier written elsewhere.</p>
     *
     * @return the resolved damage
     */
    public final double getFinalDamage() {
        double damage = this.damage;

        for (final Double value : this.additiveMap.values()) {
            damage += value;
        }

        for (final Double value : this.multiplierMap.values()) {
            damage *= value;
        }

        return Math.max(0.0D, damage);
    }

    /**
     * Whether anything will actually be dealt once modifiers are resolved.
     *
     * @return whether the resolved damage is above zero
     */
    public final boolean hasFinalDamage() {
        return this.getFinalDamage() > 0.0D;
    }

    /**
     * Whether a delay was set explicitly.
     *
     * @return whether the delay is above zero
     */
    public final boolean hasDelay() {
        return this.delay > 0L;
    }
}