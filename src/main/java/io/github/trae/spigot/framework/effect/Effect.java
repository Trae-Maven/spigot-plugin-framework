package io.github.trae.spigot.framework.effect;

import io.github.trae.spigot.framework.effect.events.EffectExpireEvent;
import io.github.trae.spigot.framework.effect.events.add.EffectPostAddEvent;
import io.github.trae.spigot.framework.effect.events.add.EffectPreAddEvent;
import io.github.trae.spigot.framework.effect.events.remove.EffectPostRemoveEvent;
import io.github.trae.spigot.framework.effect.events.remove.EffectPreRemoveEvent;
import io.github.trae.spigot.framework.effect.events.update.EffectPostUpdateEvent;
import io.github.trae.spigot.framework.effect.events.update.EffectPreUpdateEvent;
import io.github.trae.spigot.framework.utility.UtilEvent;
import io.github.trae.spigot.framework.utility.UtilPotionEffect;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A timed effect that can be applied to any {@link LivingEntity}, tracking its own users and
 * optionally mirroring itself onto a vanilla {@link PotionEffectType}.
 * <p>
 * Subclasses are discovered by {@link EffectManager} via the dependency injector and override the
 * {@code can*} guards to veto transitions, the {@code on*} hooks to react to them, and
 * {@link #getPotionEffectType()} to bind a vanilla potion effect that is kept in sync with the
 * stored {@link EffectData}.
 * <p>
 * Every transition routes through this class, including expiry, which {@link EffectManager}
 * delegates here once a user's data has elapsed. Only the deliberate removal path is vetoable: the
 * reasons that report something that already happened, such as death or a disconnect, are applied
 * unconditionally.
 */
@AllArgsConstructor
@Getter
public abstract class Effect {

    /**
     * The active users of this effect and their state, keyed by entity identifier. Entries are
     * expired and pruned by {@link EffectManager}'s tick, so the map is mutated during iteration
     * there.
     */
    private final Map<UUID, EffectData> users = new HashMap<>();

    /**
     * The display name of this effect.
     */
    private final String name;

    /**
     * Applies this effect to the entity at the given amplifier and duration, replacing any state it
     * already had.
     * <p>
     * No-op if {@link EffectPreAddEvent} is cancelled or {@link #canAdd(LivingEntity, EffectData)}
     * rejects it. The bound potion effect is only sent when the amplifier and duration are both
     * positive, so a listener that zeroes either one leaves the effect stored with no vanilla
     * counterpart. On success {@link EffectPostAddEvent} is dispatched and
     * {@link #onAdd(LivingEntity, EffectData)} runs.
     *
     * @param livingEntity the entity to apply the effect to
     * @param amplifier    the one-based amplifier ({@code 1} for level I)
     * @param duration     the duration in milliseconds, measured from now
     */
    public final void addUser(final LivingEntity livingEntity, final int amplifier, final long duration) {
        final EffectData effectData = new EffectData(amplifier, System.currentTimeMillis(), duration);

        if (UtilEvent.supply(new EffectPreAddEvent(this, effectData, livingEntity)).isCancelled() || !this.canAdd(livingEntity, effectData)) {
            return;
        }

        this.users.put(livingEntity.getUniqueId(), effectData);

        if (this.getPotionEffectType() != null && effectData.getAmplifier() > 0 && effectData.getDuration() > 0L) {
            UtilPotionEffect.add(livingEntity, this.getPotionEffectType(), effectData.getAmplifier(), effectData.getDuration());
        }

        UtilEvent.dispatch(new EffectPostAddEvent(this, effectData, livingEntity));

        this.onAdd(livingEntity, effectData);
    }

    /**
     * Applies this effect to the entity at amplifier {@code 0}, which leaves it logical only: no
     * potion effect is sent, and the first {@link #updateUser} drops the entry unless the consumer
     * raises the amplifier.
     *
     * @param livingEntity the entity to apply the effect to
     * @param duration     the duration in milliseconds, measured from now
     */
    public final void addUser(final LivingEntity livingEntity, final long duration) {
        this.addUser(livingEntity, 0, duration);
    }

    /**
     * Ends this effect for the entity, for the given reason. No-op if the entity is not a user.
     * <p>
     * Only {@link RemoveReason#NORMAL} consults {@link EffectPreRemoveEvent} and
     * {@link #canRemove(LivingEntity, EffectData)}, since it is the one path where the removal is
     * still a proposal; the rest report something that has already happened and are applied
     * unconditionally. The map entry is kept for {@link RemoveReason#EXPIRE} and
     * {@link RemoveReason#CONDITIONAL}, both of which are driven from {@link EffectManager}'s
     * iteration and pruned there instead.
     * <p>
     * The bound potion effect is always cleared. Expiry then reports through
     * {@link EffectExpireEvent} and {@link #onExpire(LivingEntity, EffectData)}, every other reason
     * through {@link EffectPostRemoveEvent} and
     * {@link #onRemove(LivingEntity, EffectData, RemoveReason)}, and both end with
     * {@link #onRemoveOrExpire(LivingEntity, EffectData, RemoveReason)}.
     *
     * @param livingEntity the entity to remove the effect from
     * @param removeReason why the effect is ending
     */
    public final void removeUser(final LivingEntity livingEntity, final RemoveReason removeReason) {
        final EffectData effectData = this.users.get(livingEntity.getUniqueId());
        if (effectData == null) {
            return;
        }

        if (removeReason == RemoveReason.NORMAL) {
            if (UtilEvent.supply(new EffectPreRemoveEvent(this, effectData, livingEntity, removeReason)).isCancelled() || !this.canRemove(livingEntity, effectData)) {
                return;
            }
        }

        if (removeReason != RemoveReason.EXPIRE && removeReason != RemoveReason.CONDITIONAL) {
            this.users.remove(livingEntity.getUniqueId());
        }

        if (this.getPotionEffectType() != null) {
            UtilPotionEffect.remove(livingEntity, this.getPotionEffectType());
        }

        if (removeReason != RemoveReason.EXPIRE) {
            UtilEvent.dispatch(new EffectPostRemoveEvent(this, effectData, livingEntity, removeReason));

            this.onRemove(livingEntity, effectData, removeReason);
            this.onRemoveOrExpire(livingEntity, effectData, removeReason);
        } else {
            UtilEvent.dispatch(new EffectExpireEvent(this, effectData, livingEntity));

            this.onExpire(livingEntity, effectData);
            this.onRemoveOrExpire(livingEntity, effectData, removeReason);
        }
    }

    /**
     * Removes this effect from the entity deliberately, the one removal path that
     * {@link EffectPreRemoveEvent} and {@link #canRemove(LivingEntity, EffectData)} can veto.
     *
     * @param livingEntity the entity to remove the effect from
     */
    public final void removeUser(final LivingEntity livingEntity) {
        this.removeUser(livingEntity, RemoveReason.NORMAL);
    }

    /**
     * Mutates the entity's existing {@link EffectData} through the consumer and reconciles the
     * bound potion effect with the result.
     * <p>
     * No-op if the entity is not a user, if {@link EffectPreUpdateEvent} is cancelled, or if
     * {@link #canUpdate(LivingEntity, EffectData)} rejects it. After the consumer runs, the outcome
     * depends on what it left behind: a positive amplifier and duration re-apply the potion effect
     * if either value changed, a positive amplifier with a non-positive duration clears the potion
     * effect but keeps the entry, and a non-positive amplifier clears both the potion effect and
     * the entry.
     * <p>
     * When {@code carryOver} is set and the duration grew, the time already elapsed is credited: the
     * potion effect is re-applied for the time remaining plus the increase, rather than for the full
     * new duration. This stops a refresh from discarding progress or granting the whole duration
     * again.
     * <p>
     * {@link EffectPostUpdateEvent} and {@link #onUpdate(LivingEntity, EffectData)} run on every
     * outcome, including the one that dropped the entry. Since this path ends the effect without
     * going through {@link #removeUser}, neither the removal events nor
     * {@link #onRemoveOrExpire(LivingEntity, EffectData, RemoveReason)} fire for it.
     *
     * @param livingEntity the entity whose effect to update
     * @param carryOver    whether to keep the remaining time when the duration is extended
     * @param consumer     the mutation to apply to the entity's effect data
     */
    public final void updateUser(final LivingEntity livingEntity, final boolean carryOver, final Consumer<EffectData> consumer) {
        final EffectData effectData = this.users.get(livingEntity.getUniqueId());
        if (effectData == null) {
            return;
        }

        if (UtilEvent.supply(new EffectPreUpdateEvent(this, effectData, livingEntity)).isCancelled() || !this.canUpdate(livingEntity, effectData)) {
            return;
        }

        final int previousAmplifier = effectData.getAmplifier();
        final long previousDuration = effectData.getDuration();
        final long previousRemaining = effectData.getRemaining();

        consumer.accept(effectData);

        final int newAmplifier = effectData.getAmplifier();
        final long newDuration = effectData.getDuration();

        if (newAmplifier > 0 && newDuration > 0L) {
            if (newAmplifier != previousAmplifier || newDuration != previousDuration) {
                if (this.getPotionEffectType() != null) {
                    final long finalNewDuration = carryOver && newDuration > previousDuration ? previousRemaining + (newDuration - previousDuration) : newDuration;

                    UtilPotionEffect.add(livingEntity, this.getPotionEffectType(), newAmplifier, finalNewDuration);
                }
            }
        } else if (newAmplifier > 0) {
            if (this.getPotionEffectType() != null) {
                UtilPotionEffect.remove(livingEntity, this.getPotionEffectType());
            }
        } else {
            this.users.remove(livingEntity.getUniqueId());

            if (this.getPotionEffectType() != null) {
                UtilPotionEffect.remove(livingEntity, this.getPotionEffectType());
            }
        }

        UtilEvent.dispatch(new EffectPostUpdateEvent(this, effectData, livingEntity));

        this.onUpdate(livingEntity, effectData);
    }

    /**
     * Returns the users whose entity is currently resolvable, keyed by the entity itself.
     * <p>
     * This is an immutable snapshot rebuilt per call, and it silently omits any user whose entity is
     * offline or unloaded even though their entry still exists. The user map remains the source of
     * truth for membership, counts, and removal.
     *
     * @return the resolvable users and their effect data
     */
    public final Map<LivingEntity, EffectData> getActiveUsers() {
        return this.users.entrySet().stream()
                .map(entry -> new AbstractMap.SimpleEntry<>(Bukkit.getServer().getEntity(entry.getKey()), entry.getValue()))
                .filter(entry -> entry.getKey() instanceof LivingEntity)
                .collect(Collectors.toUnmodifiableMap(entry -> (LivingEntity) entry.getKey(), Map.Entry::getValue));
    }

    /**
     * Returns the effect data stored against the given entity identifier.
     *
     * @param id the entity identifier
     * @return an {@link Optional} containing the effect data, or empty if the entity is not a user
     */
    public final Optional<EffectData> getUserById(final UUID id) {
        return Optional.ofNullable(this.users.get(id));
    }

    /**
     * Returns the effect data stored against the given entity.
     *
     * @param livingEntity the entity to look up
     * @return an {@link Optional} containing the effect data, or empty if the entity is not a user
     */
    public final Optional<EffectData> getUserByLivingEntity(final LivingEntity livingEntity) {
        return this.getUserById(livingEntity.getUniqueId());
    }

    /**
     * Returns whether the effect may be applied to the entity with the given data. Runs alongside
     * {@link EffectPreAddEvent}, before any state is written. Defaults to {@code true}.
     *
     * @param livingEntity the entity the effect would be applied to
     * @param effectData   the data the effect would be applied with
     * @return {@code true} if the effect may be applied
     */
    protected boolean canAdd(final LivingEntity livingEntity, final EffectData effectData) {
        return true;
    }

    /**
     * Returns whether the effect may be removed from the entity. Consulted only on the deliberate
     * {@link RemoveReason#NORMAL} path, alongside {@link EffectPreRemoveEvent}. Defaults to
     * {@code true}.
     *
     * @param livingEntity the entity the effect would be removed from
     * @param effectData   the entity's current effect data
     * @return {@code true} if the effect may be removed
     */
    protected boolean canRemove(final LivingEntity livingEntity, final EffectData effectData) {
        return true;
    }

    /**
     * Returns whether the entity's effect data may be updated. Runs alongside
     * {@link EffectPreUpdateEvent}, before the consumer is applied. Defaults to {@code true}.
     *
     * @param livingEntity the entity whose effect would be updated
     * @param effectData   the entity's current effect data, as yet unmodified
     * @return {@code true} if the update may proceed
     */
    protected boolean canUpdate(final LivingEntity livingEntity, final EffectData effectData) {
        return true;
    }

    /**
     * Called after the effect has been applied to the entity and any potion effect sent.
     *
     * @param livingEntity the entity the effect was applied to
     * @param effectData   the newly stored effect data
     */
    protected void onAdd(final LivingEntity livingEntity, final EffectData effectData) {
    }

    /**
     * Called after the effect has been removed from the entity. Not called on expiry, which routes
     * through {@link #onExpire(LivingEntity, EffectData)} instead.
     *
     * @param livingEntity the entity the effect was removed from
     * @param effectData   the effect data as it stood at removal
     * @param removeReason why the effect was removed
     */
    protected void onRemove(final LivingEntity livingEntity, final EffectData effectData, final RemoveReason removeReason) {
    }

    /**
     * Called after the entity's effect data has been mutated and the potion effect reconciled.
     *
     * @param livingEntity the entity whose effect was updated
     * @param effectData   the mutated effect data
     */
    protected void onUpdate(final LivingEntity livingEntity, final EffectData effectData) {
    }

    /**
     * Called once per tick by {@link EffectManager} for each user that has not yet expired and is
     * not being removed conditionally.
     *
     * @param livingEntity the entity holding the effect
     * @param effectData   the entity's current effect data
     */
    protected void onTick(final LivingEntity livingEntity, final EffectData effectData) {
    }

    /**
     * Called once the entity's effect data has elapsed and its potion effect been cleared,
     * immediately before {@link EffectManager} prunes the entry.
     *
     * @param livingEntity the entity whose effect expired
     * @param effectData   the expired effect data
     */
    protected void onExpire(final LivingEntity livingEntity, final EffectData effectData) {
    }

    /**
     * Called on every path through {@link #removeUser}, whether removal or expiry, after the path's
     * own hook has run. Use this for teardown that must happen regardless of how the effect ended.
     *
     * @param livingEntity the entity whose effect ended
     * @param effectData   the effect data as it stood at the end
     * @param removeReason why the effect ended, {@link RemoveReason#EXPIRE} when it elapsed
     */
    protected void onRemoveOrExpire(final LivingEntity livingEntity, final EffectData effectData, final RemoveReason removeReason) {
    }

    /**
     * Returns the vanilla potion effect this effect mirrors onto its users, kept in sync with the
     * stored amplifier and duration across add, update, and remove. Defaults to {@code null}, which
     * leaves the effect purely logical with no vanilla counterpart.
     *
     * @return the bound potion effect type, or {@code null} for none
     */
    protected PotionEffectType getPotionEffectType() {
        return null;
    }

    /**
     * Returns whether the effect should end this tick for a reason of the subclass's own choosing,
     * checked by {@link EffectManager} before the tick hook runs. Defaults to {@code false}.
     *
     * @param livingEntity the entity holding the effect
     * @param effectData   the entity's current effect data
     * @return {@code true} to remove the effect with {@link RemoveReason#CONDITIONAL}
     */
    protected boolean removeOnCondition(final LivingEntity livingEntity, final EffectData effectData) {
        return false;
    }

    /**
     * Returns whether this effect is cleared when its holder dies. Defaults to {@code false}, which
     * leaves the effect in place across death.
     *
     * @return {@code true} to remove the effect on death
     */
    public boolean removeOnDeath() {
        return false;
    }

    /**
     * Returns whether this effect is cleared when its holder disconnects. Defaults to {@code false},
     * which leaves the effect in place across a reconnect.
     *
     * @return {@code true} to remove the effect on quit
     */
    public boolean removeOnQuit() {
        return false;
    }

    /**
     * Why an effect ended, carried on the removal events and hooks.
     */
    public enum RemoveReason {

        /**
         * Removed deliberately through {@link #removeUser(LivingEntity)}. The only reason that can
         * be vetoed, since the removal is still a proposal at that point.
         */
        NORMAL,
        /**
         * The effect's duration elapsed. Reported through {@link EffectExpireEvent} rather than the
         * removal events, with the map entry left for {@link EffectManager}'s tick to prune.
         */
        EXPIRE,
        /**
         * {@link #removeOnCondition(LivingEntity, EffectData)} returned {@code true} during the
         * tick. The map entry is left for the tick's own iteration to prune.
         */
        CONDITIONAL,
        /**
         * The holder died and the effect opted into {@link #removeOnDeath()}.
         */
        DEATH,
        /**
         * The holder disconnected and the effect opted into {@link #removeOnQuit()}.
         */
        QUIT
    }
}