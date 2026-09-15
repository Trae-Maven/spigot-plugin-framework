package io.github.trae.spigot.framework.death.listeners;

import io.github.trae.di.InjectorApi;
import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.damage.DamageManager;
import io.github.trae.spigot.framework.damage.data.CustomReason;
import io.github.trae.spigot.framework.damage.data.Reason;
import io.github.trae.spigot.framework.damage.events.damage.CustomPostDamageEvent;
import io.github.trae.spigot.framework.death.DeathManager;
import io.github.trae.spigot.framework.death.events.CustomDeathEvent;
import io.github.trae.spigot.framework.death.events.VanillaDeathEvent;
import io.github.trae.spigot.framework.utility.UtilEvent;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Turns a vanilla death into the framework's own, enriched with what killed the entity when the
 * damage pipeline can say.
 *
 * <p>Listens on the vanilla event rather than firing from the damage manager, so deaths that never
 * went through the pipeline are still seen. What the pipeline provides is the context: the vanilla
 * event knows an entity died, the retained damage pass knows who dealt the blow, with what item and
 * for what reason. Without it the death is reported from what the entity itself still remembers.</p>
 *
 * <p>Attribution is resolved here rather than taken from the damage pass, because a reason can
 * outlive the hit that set it. A killer with an unexpired {@link CustomReason} on this entity is
 * credited with that instead of with whatever the killing hit happened to be, so a kill landed with
 * a sword moments after an ability still names the ability.</p>
 *
 * <p>Drops and experience are passed through the framework event and then written back to the
 * vanilla death, allowing listeners to change what the death ultimately drops.</p>
 *
 * @see CustomDeathEvent
 * @see VanillaDeathEvent
 * @see CustomReason
 */
@RequiredArgsConstructor
@Singleton
public class DeathListener implements Listener {

    private final DeathManager deathManager;

    /**
     * Looked up on first use rather than injected, since the damage manager may not be registered at
     * all and asking for it up front would tie the death system to it.
     */
    private DamageManager damageManager;

    /**
     * Picks which death event a death gets, and dispatches it.
     *
     * <p>Runs at {@code MONITOR} so drops and vanilla death handling have already settled, and skips
     * a cancelled death outright since nothing actually died.</p>
     *
     * @param event the vanilla death event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public final void onEntityDeath(final EntityDeathEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (this.deathManager.isDamageManagerRegistered()) {
            if (this.damageManager == null) {
                this.damageManager = InjectorApi.get(DamageManager.class);
            }

            this.handleCustomDeathEvent(event);
        } else {
            this.handleVanillaDeathEvent(event);
        }
    }

    /**
     * Dispatches the thin death event, built from what the entity and the vanilla damage source
     * still hold.
     *
     * <p>The cause falls back to {@code CUSTOM} when the entity kept no last damage cause, since a
     * death always has to be attributed to something.</p>
     *
     * <p>Any changes listeners make to the drops or dropped experience are copied back to the vanilla
     * death event after dispatch.</p>
     *
     * @param event the vanilla death event
     */
    private void handleVanillaDeathEvent(final EntityDeathEvent event) {
        final LivingEntity entity = event.getEntity();

        final Entity killer = event.getDamageSource().getCausingEntity();
        final EntityDamageEvent.DamageCause damageCause = entity.getLastDamageCause() != null ? entity.getLastDamageCause().getCause() : EntityDamageEvent.DamageCause.CUSTOM;

        final VanillaDeathEvent vanillaDeathEvent = UtilEvent.supply(new VanillaDeathEvent(entity, killer, damageCause, event.getDrops(), event.getDroppedExp()));

        event.getDrops().clear();
        event.getDrops().addAll(vanillaDeathEvent.getDrops());

        event.setDroppedExp(vanillaDeathEvent.getDropExp());
    }

    /**
     * Dispatches the enriched death event for a death the pipeline has a record of.
     *
     * <p>Nothing is dispatched without that record, since a death with no damage behind it has
     * nothing to report beyond the entity itself.</p>
     *
     * <p>Any changes listeners make to the drops or dropped experience are copied back to the vanilla
     * death event after dispatch.</p>
     *
     * <p>Both retained records are dropped only after the dispatch, so a listener reading either for
     * the same entity still finds them.</p>
     *
     * @param event the vanilla death event
     */
    private void handleCustomDeathEvent(final EntityDeathEvent event) {
        final LivingEntity entity = event.getEntity();

        final CustomPostDamageEvent customPostDamageEvent = this.damageManager.getLastDamageMap().get(entity.getUniqueId());
        if (customPostDamageEvent == null) {
            return;
        }

        Reason reason = customPostDamageEvent.getReason();

        if (customPostDamageEvent.getDamager() != null) {
            final ConcurrentHashMap<UUID, CustomReason> map = this.damageManager.getLastCustomReasonMap().get(entity.getUniqueId());
            if (map != null) {
                final CustomReason customReason = map.get(customPostDamageEvent.getDamager().getUniqueId());
                if (customReason != null && !customReason.hasExpired()) {
                    reason = customReason;
                }
            }
        }

        final CustomDeathEvent customDeathEvent = UtilEvent.supply(new CustomDeathEvent(customPostDamageEvent, entity, reason, event.getDrops(), event.getDroppedExp()));

        event.getDrops().clear();
        event.getDrops().addAll(customDeathEvent.getDrops());

        event.setDroppedExp(customDeathEvent.getDropExp());

        this.damageManager.getLastDamageMap().remove(entity.getUniqueId());
        this.damageManager.getLastCustomReasonMap().remove(entity.getUniqueId());
    }
}