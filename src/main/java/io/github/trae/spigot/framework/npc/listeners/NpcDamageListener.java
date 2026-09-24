package io.github.trae.spigot.framework.npc.listeners;

import io.github.trae.di.InjectorApi;
import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.damage.DamageManager;
import io.github.trae.spigot.framework.damage.events.damage.CustomDamageEvent;
import io.github.trae.spigot.framework.npc.Npc;
import io.github.trae.spigot.framework.npc.NpcManager;
import io.github.trae.spigot.framework.npc.types.DamageableNpc;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.function.Consumer;

/**
 * Routes damage to a {@link DamageableNpc}'s backing entity to the NPC, through whichever damage
 * pipeline is active, and blocks damage to every other NPC.
 * <p>
 * Exactly one handler applies at a time, decided by whether the damage subsystem is registered:
 * <ul>
 *     <li>No {@link DamageManager}: the vanilla {@link EntityDamageByEntityEvent} is used.</li>
 *     <li>A {@link DamageManager}: {@link CustomDamageEvent} is used.</li>
 * </ul>
 * Damage to an NPC that is not a {@link DamageableNpc} is cancelled, so an NPC stays unhurt even when
 * the active pipeline does not respect the entity's invulnerability flag.
 */
@Singleton
public final class NpcDamageListener implements Listener {

    /**
     * The manager NPC lookups are delegated to.
     */
    private final NpcManager npcManager;

    /**
     * The damage subsystem's manager, or {@code null} when the damage subsystem is not registered.
     */
    private final DamageManager damageManager;

    /**
     * Creates the listener, resolving the optional {@link DamageManager} from the injector.
     *
     * @param npcManager the manager NPC lookups are delegated to
     */
    public NpcDamageListener(final NpcManager npcManager) {
        this.npcManager = npcManager;
        this.damageManager = InjectorApi.get(DamageManager.class);
    }

    /**
     * Resolves the NPC backing the entity. A {@link DamageableNpc} is passed to the consumer, while
     * any other NPC has the damage cancelled. No-op for entities that are not an NPC.
     *
     * @param entity   the entity that was damaged
     * @param cancel   the callback that cancels the damage event
     * @param consumer the callback that forwards the damage event to the NPC
     */
    private void handleDamage(final Entity entity, final Runnable cancel, final Consumer<DamageableNpc> consumer) {
        final Npc<?> npc = this.npcManager.getNpcByEntity(entity).orElse(null);
        if (npc == null) {
            return;
        }

        if (!(npc instanceof final DamageableNpc damageableNpc)) {
            cancel.run();
            return;
        }

        consumer.accept(damageableNpc);
    }

    /**
     * Handles the vanilla damage event when the damage subsystem is not registered.
     *
     * @param event the entity damage by entity event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent event) {
        if (this.damageManager != null) {
            return;
        }

        if (event.isCancelled()) {
            return;
        }

        this.handleDamage(event.getEntity(), () -> event.setCancelled(true), damageableNpc -> damageableNpc.onEntityDamageByEntityEvent(event));
    }

    /**
     * Handles the damage subsystem's custom damage event when the damage subsystem is registered.
     *
     * @param event the custom damage event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onCustomDamage(final CustomDamageEvent event) {
        if (this.damageManager == null) {
            return;
        }

        if (event.isCancelled()) {
            return;
        }

        this.handleDamage(event.getDamagee(), () -> event.setCancelled(true), damageableNpc -> damageableNpc.onCustomDamageEvent(event));
    }
}