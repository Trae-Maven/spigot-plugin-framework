package io.github.trae.spigot.framework.damage.listeners;

import io.github.trae.di.annotations.method.Scheduler;
import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.damage.DamageManager;
import io.github.trae.spigot.framework.damage.events.damage.CustomPostDamageEvent;
import io.github.trae.spigot.framework.damage.events.damage.CustomPreDamageEvent;
import lombok.AllArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Enforces how soon an entity can be damaged again, under the configured combat rules.
 *
 * <p>Cancelling vanilla's damage handling also discards its invulnerability window, so this is the
 * only thing standing between a target and being hit every tick.</p>
 *
 * <p>How long a window lasts comes from the cause in either mode. What the combat rules decide is
 * who the window applies to.</p>
 *
 * <p>With old combat enabled, the window is tracked per attacker, so two players can hit the same
 * target simultaneously, and per environmental cause, so burning does not protect against drowning.
 * Both are deliberate departures from vanilla, and they are what make team fights work.</p>
 *
 * <p>With old combat disabled, the window matches vanilla: one per target, shared across every
 * source, so a hit from anything protects against everything for as long as that hit's cause
 * allows.</p>
 */
@AllArgsConstructor
@Singleton
public class DamageDelayListener implements Listener {

    private final DamageManager damageManager;

    /**
     * Expiry per damagee, shared across every source, under vanilla combat.
     */
    private final Map<UUID, Long> delayMap = new HashMap<>();

    /**
     * Expiry per damagee, per environmental cause, under old combat.
     */
    private final Map<UUID, Map<DamageCause, Long>> delayByCauseMap = new HashMap<>();

    /**
     * Expiry per damagee, per attacker, under old combat.
     */
    private final Map<UUID, Map<UUID, Long>> delayByEntityMap = new HashMap<>();

    /**
     * Drops expired entries and the empty maps left behind.
     *
     * <p>Nothing else clears these, since an entity that is never hit again leaves its entry behind,
     * and mobs die or unload without notice. A stale entry is harmless because the check reads the
     * expiry directly, including entries left in the other mode's maps after the setting changes;
     * this is purely to stop the maps growing.</p>
     */
    @Scheduler(period = 10, unit = TimeUnit.SECONDS)
    public final void onScheduler() {
        final long now = System.currentTimeMillis();

        this.delayMap.values().removeIf(expiry -> now >= expiry);

        this.delayByCauseMap.values().forEach(map -> map.values().removeIf(expiry -> now >= expiry));
        this.delayByCauseMap.values().removeIf(Map::isEmpty);

        this.delayByEntityMap.values().forEach(map -> map.values().removeIf(expiry -> now >= expiry));
        this.delayByEntityMap.values().removeIf(Map::isEmpty);
    }

    /**
     * Refuses damage that arrives inside an active delay.
     *
     * <p>Runs first at the pre stage so nothing downstream does work for damage that will not
     * land.</p>
     *
     * @param event the pre stage
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public final void onCustomPreDamage(final CustomPreDamageEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (this.canAttack(event)) {
            return;
        }

        event.setCancelled(true);
    }

    /**
     * Records the delay for damage that landed.
     *
     * <p>A delay set explicitly on the event wins; otherwise one is picked from the cause, falling
     * back to the configured default for a cause with no value of its own.</p>
     *
     * @param event the completed post stage
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public final void onCustomPostDamage(final CustomPostDamageEvent event) {
        if (event.isCancelled()) {
            return;
        }

        final long delay = event.hasDelay() ? event.getDelay() : this.damageManager.getDamageConfig().getDelay().getValueByCause(event.getCause());
        if (delay <= 0L) {
            return;
        }

        this.onAttack(event, delay);
    }

    /**
     * Whether the delay for this damagee and source has passed.
     *
     * <p>Reads the event's own timestamp rather than the clock, so the check and the record written
     * at the post stage share one reference point.</p>
     *
     * @param event the pre stage
     * @return whether the damage may proceed
     */
    private boolean canAttack(final CustomPreDamageEvent event) {
        final long systemTime = event.getSystemTime();

        final UUID damageeId = event.getDamagee().getUniqueId();

        if (!this.damageManager.getDamageConfig().isOldCombatEnabled()) {
            // Shared
            return this.isExpired(systemTime, this.delayMap.get(damageeId));
        }

        if (event.getDamager() != null) {
            // Damager
            return this.isExpired(systemTime, this.delayByEntityMap.getOrDefault(damageeId, Collections.emptyMap()).get(event.getDamager().getUniqueId()));
        } else {
            // Cause
            return this.isExpired(systemTime, this.delayByCauseMap.getOrDefault(damageeId, Collections.emptyMap()).get(event.getCause()));
        }
    }

    /**
     * Stores when this damagee may next be hit, by any source under vanilla combat, or by the same
     * source under old combat.
     *
     * @param event the completed post stage
     * @param delay the delay to enforce, in milliseconds
     */
    private void onAttack(final CustomPostDamageEvent event, final long delay) {
        final long expiry = event.getSystemTime() + delay;

        final UUID damageeId = event.getDamagee().getUniqueId();

        if (!this.damageManager.getDamageConfig().isOldCombatEnabled()) {
            // Shared
            this.delayMap.put(damageeId, expiry);
            return;
        }

        if (event.getDamager() != null) {
            // Damager
            this.delayByEntityMap.computeIfAbsent(damageeId, __ -> new HashMap<>()).put(event.getDamager().getUniqueId(), expiry);
        } else {
            // Cause
            this.delayByCauseMap.computeIfAbsent(damageeId, __ -> new EnumMap<>(DamageCause.class)).put(event.getCause(), expiry);
        }
    }

    /**
     * Whether a recorded expiry has passed. A missing record counts as expired.
     *
     * @param systemTime the moment being tested
     * @param expiry     the recorded expiry, or {@code null}
     * @return whether the delay has passed
     */
    private boolean isExpired(final long systemTime, final Long expiry) {
        return expiry == null || systemTime >= expiry;
    }
}