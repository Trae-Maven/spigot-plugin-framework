package io.github.trae.spigot.framework.effect;

import io.github.trae.di.InjectorApi;
import io.github.trae.di.annotations.method.Scheduler;
import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.effect.events.EffectTickEvent;
import io.github.trae.spigot.framework.utility.UtilEvent;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Owns the registry of {@link Effect}s and drives their per-tick lifecycle.
 * <p>
 * Effects are collected from the dependency injector once the server has finished loading, so that
 * effects registered by any plugin are picked up regardless of enable order. From then on a tick
 * walks every effect's users, pruning those whose entity has gone away, ending those whose duration
 * has elapsed, and dispatching tick events for the rest.
 */
@Getter
@Singleton
public final class EffectManager implements Listener {

    /**
     * Every registered effect, populated once on server load.
     */
    private final List<Effect> effectList = new ArrayList<>();

    /**
     * Collects every {@link Effect} from the injector once the server has finished starting.
     * <p>
     * Ignores {@link ServerLoadEvent.LoadType#RELOAD} so a reload does not append duplicates to the
     * registry.
     *
     * @param event the server load event
     */
    @EventHandler
    public void onServerStart(final ServerLoadEvent event) {
        if (event.getType() != ServerLoadEvent.LoadType.STARTUP) {
            return;
        }

        this.effectList.addAll(InjectorApi.getAll(Effect.class));
    }

    /**
     * Advances every effect one tick, pruning its user map as it goes.
     * <p>
     * An entry is dropped outright when its entity is gone or no longer valid, unless the entity is
     * an online player. A live holder is then checked in order: a conditional removal ends the
     * effect and drops the entry, an unexpired effect dispatches {@link EffectTickEvent} and runs
     * {@link Effect#onTick}, and an elapsed one is ended through
     * {@link Effect#removeUser(LivingEntity, Effect.RemoveReason)} and dropped. Both of those
     * removal reasons leave the map entry in place precisely so it can be pruned here rather than
     * underneath this iteration.
     * <p>
     * Because the map is pruned in place, hooks invoked from here must not add or remove users of
     * the same effect directly.
     */
    @Scheduler(period = 50, unit = TimeUnit.MILLISECONDS)
    public void onScheduler() {
        for (final Effect effect : this.effectList) {
            effect.getUsers().entrySet().removeIf(entry -> {
                final Entity entity = Bukkit.getServer().getEntity(entry.getKey());

                if (!(entity instanceof Player) && (entity == null || !entity.isValid())) {
                    return true;
                }

                final EffectData effectData = entry.getValue();

                if (entity instanceof final LivingEntity livingEntity) {
                    if (!(effectData.hasExpired())) {
                        if (effect.removeOnCondition(livingEntity, effectData)) {
                            effect.removeUser(livingEntity, Effect.RemoveReason.CONDITIONAL);
                            return true;
                        }

                        UtilEvent.dispatch(new EffectTickEvent(effect, effectData, livingEntity));

                        effect.onTick(livingEntity, effectData);
                        return false;
                    }

                    effect.removeUser(livingEntity, Effect.RemoveReason.EXPIRE);
                }

                return true;
            });
        }
    }
}