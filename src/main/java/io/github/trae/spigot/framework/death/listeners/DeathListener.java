package io.github.trae.spigot.framework.death.listeners;

import io.github.trae.di.annotations.type.DependsOn;
import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.damage.DamageManager;
import io.github.trae.spigot.framework.damage.data.CustomReason;
import io.github.trae.spigot.framework.damage.data.Reason;
import io.github.trae.spigot.framework.damage.events.damage.CustomPostDamageEvent;
import io.github.trae.spigot.framework.death.events.CustomDeathEvent;
import io.github.trae.spigot.framework.utility.UtilEvent;
import lombok.AllArgsConstructor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Turns a vanilla death into the framework's own, enriched with what killed the entity.
 *
 * <p>Listens on the vanilla event rather than firing from the damage manager, so deaths that never
 * went through the pipeline are still seen. What the pipeline provides is the context: the vanilla
 * event knows an entity died, the retained damage pass knows who dealt the blow, with what item and
 * for what reason.</p>
 *
 * <p>Attribution is resolved here rather than taken from the damage pass, because a reason can
 * outlive the hit that set it. A killer with an unexpired {@link CustomReason} on this entity is
 * credited with that instead of with whatever the killing hit happened to be, so a kill landed with
 * a sword moments after an ability still names the ability.</p>
 *
 * @see CustomDeathEvent
 * @see CustomReason
 */
@DependsOn(values = DamageManager.class)
@AllArgsConstructor
@Singleton
public class DeathListener implements Listener {

    private final DamageManager damageManager;

    /**
     * Dispatches the framework's death event for a death the pipeline has a record of.
     *
     * <p>Nothing is dispatched without that record, since a death with no damage behind it has
     * nothing to report beyond the entity itself.</p>
     *
     * <p>Both retained records are dropped only after the dispatch, so a listener reading either for
     * the same entity still finds them.</p>
     *
     * @param event the vanilla death event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public final void onEntityDeath(final EntityDeathEvent event) {
        if (event.isCancelled()) {
            return;
        }

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

        UtilEvent.dispatch(new CustomDeathEvent(customPostDamageEvent, entity, reason));

        this.damageManager.getLastDamageMap().remove(entity.getUniqueId());
        this.damageManager.getLastCustomReasonMap().remove(entity.getUniqueId());
    }
}