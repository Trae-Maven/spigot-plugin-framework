package io.github.trae.spigot.framework.damage.listeners;

import io.github.trae.di.annotations.method.Scheduler;
import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.damage.events.damage.CustomPostDamageEvent;
import io.github.trae.spigot.framework.damage.events.damage.CustomPreDamageEvent;
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
 * Enforces how soon an entity can be damaged again by the same source.
 *
 * <p>Cancelling vanilla's damage handling also discards its invulnerability window, so this is the
 * only thing standing between a target and being hit every tick.</p>
 *
 * <p>Tracked per attacker rather than shared across all sources, so two players can hit the same
 * target simultaneously. That is a deliberate departure from vanilla, where one hit briefly protects
 * against every other, and it is what makes team fights work.</p>
 *
 * <p>Environmental causes are tracked per cause instead, so burning does not protect against
 * drowning.</p>
 */
@Singleton
public class DamageDelayListener implements Listener {

    private static final long DEFAULT_DELAY = 500L;
    private static final long DEFAULT_ENTITY_ATTACK_DELAY = 500L;

    /**
     * Expiry per damagee, per environmental cause.
     */
    private final Map<UUID, Map<DamageCause, Long>> delayByCauseMap = new HashMap<>();

    /**
     * Expiry per damagee, per attacker.
     */
    private final Map<UUID, Map<UUID, Long>> delayByEntityMap = new HashMap<>();

    /**
     * Drops expired entries and the empty maps left behind.
     *
     * <p>Nothing else clears these, since an entity that is never hit again leaves its entry behind,
     * and mobs die or unload without notice. A stale entry is harmless because the check reads the
     * expiry directly; this is purely to stop the maps growing.</p>
     */
    @Scheduler(period = 10, unit = TimeUnit.SECONDS)
    public final void onScheduler() {
        final long now = System.currentTimeMillis();

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
     * <p>A delay set explicitly on the event wins; otherwise one is picked from the cause.</p>
     *
     * @param event the completed post stage
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public final void onCustomPostDamage(final CustomPostDamageEvent event) {
        if (event.isCancelled()) {
            return;
        }

        long delay = event.getDelay();

        if (delay <= 0L) {
            delay = this.getDefaultDelay(event);
        }

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

        if (event.getDamager() != null) {
            // Damager
            return this.isExpired(systemTime, this.delayByEntityMap.getOrDefault(damageeId, Collections.emptyMap()).get(event.getDamager().getUniqueId()));
        } else {
            // Cause
            return this.isExpired(systemTime, this.delayByCauseMap.getOrDefault(damageeId, Collections.emptyMap()).get(event.getCause()));
        }
    }

    /**
     * Stores when this damagee may next be hit by the same source.
     *
     * @param event the completed post stage
     * @param delay the delay to enforce, in milliseconds
     */
    private void onAttack(final CustomPostDamageEvent event, final long delay) {
        final long expiry = event.getSystemTime() + delay;

        final UUID damageeId = event.getDamagee().getUniqueId();

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

    /**
     * The delay to enforce when the event did not set one.
     *
     * <p>Attacks get ten ticks, the pre-1.9 immunity window. Causes that tick on their own schedule
     * get a delay matching that schedule, which is redundant while the source paces itself but holds
     * if anything ever fires them faster.</p>
     *
     * @param event the completed post stage
     * @return the delay in milliseconds
     */
    private long getDefaultDelay(final CustomPostDamageEvent event) {
        return switch (event.getCause()) {
            case ENTITY_ATTACK -> DEFAULT_ENTITY_ATTACK_DELAY;
            case DROWNING, FIRE_TICK, POISON, WITHER -> 1000L;
            case STARVATION -> 4000L;
            default -> DEFAULT_DELAY;
        };
    }
}