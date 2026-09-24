package io.github.trae.spigot.framework.npc.listeners;

import io.github.trae.di.InjectorApi;
import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.death.DeathManager;
import io.github.trae.spigot.framework.death.events.CustomDeathEvent;
import io.github.trae.spigot.framework.death.events.CustomDeathMessageEvent;
import io.github.trae.spigot.framework.death.events.VanillaDeathEvent;
import io.github.trae.spigot.framework.displayname.DisplayName;
import io.github.trae.spigot.framework.npc.Npc;
import io.github.trae.spigot.framework.npc.NpcManager;
import io.github.trae.spigot.framework.npc.types.DamageableNpc;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.function.Consumer;

/**
 * Routes the death of a {@link DamageableNpc}'s backing entity to the NPC, through whichever death
 * pipeline is active, and names NPCs in death messages.
 * <p>
 * Exactly one handler applies at a time, decided by which framework subsystems are registered:
 * <ul>
 *     <li>No {@link DeathManager}: the vanilla {@link EntityDeathEvent} is used.</li>
 *     <li>A {@link DeathManager} without the damage manager: {@link VanillaDeathEvent} is used.</li>
 *     <li>A {@link DeathManager} with the damage manager: {@link CustomDeathEvent} is used.</li>
 * </ul>
 * Deaths of NPCs that are not {@link DamageableNpc} are ignored, so such an NPC is respawned like
 * any other missing entity.
 */
@Singleton
public final class NpcDeathListener implements Listener {

    /**
     * The manager NPC lookups and entity tracking are delegated to.
     */
    private final NpcManager npcManager;

    /**
     * The death subsystem's manager, or {@code null} when the death subsystem is not registered.
     */
    private final DeathManager deathManager;

    /**
     * Creates the listener, resolving the optional {@link DeathManager} from the injector.
     *
     * @param npcManager the manager NPC lookups and entity tracking are delegated to
     */
    public NpcDeathListener(final NpcManager npcManager) {
        this.npcManager = npcManager;
        this.deathManager = InjectorApi.get(DeathManager.class);
    }

    /**
     * Resolves the NPC backing the entity and, if it is a {@link DamageableNpc}, stops tracking the
     * entity, passes the NPC to the consumer, and marks it dead so it only respawns once
     * {@link DamageableNpc#canRespawn()} allows it.
     *
     * @param entity   the entity that died
     * @param consumer the callback that forwards the death event to the NPC
     */
    private void handleDeath(final Entity entity, final Consumer<DamageableNpc> consumer) {
        final Npc<?> npc = this.npcManager.getNpcByEntity(entity).orElse(null);
        if (npc == null) {
            return;
        }

        if (!(npc instanceof final DamageableNpc damageableNpc)) {
            return;
        }

        this.npcManager.getEntityMap().remove(entity.getUniqueId());

        consumer.accept(damageableNpc);

        npc.markDead();
    }

    /**
     * Handles the vanilla death event when the death subsystem is not registered.
     *
     * @param event the entity death event
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(final EntityDeathEvent event) {
        if (this.deathManager != null) {
            return;
        }

        if (event.isCancelled()) {
            return;
        }

        this.handleDeath(event.getEntity(), damageableNpc -> damageableNpc.onEntityDeathEvent(event));
    }

    /**
     * Handles the death subsystem's vanilla death event when the damage manager is not registered.
     *
     * @param event the vanilla death event
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onVanillaDeathEvent(final VanillaDeathEvent event) {
        if (this.deathManager == null || this.deathManager.isDamageManagerRegistered()) {
            return;
        }

        this.handleDeath(event.getEntity(), damageableNpc -> damageableNpc.onVanillaDeathEvent(event));
    }

    /**
     * Handles the death subsystem's custom death event when the damage manager is registered. The
     * death message is broadcast for every NPC death, before the NPC's own callback runs so it can
     * still turn the broadcast off.
     *
     * @param event the custom death event
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onCustomDeath(final CustomDeathEvent event) {
        if (this.deathManager == null || !this.deathManager.isDamageManagerRegistered()) {
            return;
        }

        this.handleDeath(event.getEntity(), damageableNpc -> {
            event.setBroadcastMessage(true);

            damageableNpc.onCustomDeathEvent(event);
        });
    }

    /**
     * Replaces the victim and killer names in a death message with the NPC's display name, for
     * whichever of them is an NPC with one set.
     * <p>
     * Both are resolved through their NPC tag rather than entity tracking, since the victim has
     * already been untracked by the time the message is built.
     *
     * @param event the custom death message event
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onCustomDeathMessage(final CustomDeathMessageEvent event) {
        if (this.deathManager == null) {
            return;
        }

        if (event.isCancelled()) {
            return;
        }

        this.npcManager.getNpcByTag(event.getDeathEvent().getEntity()).map(Npc::getDisplayName).ifPresent(displayName -> event.setEntityName(DisplayName.of(displayName)));
        this.npcManager.getNpcByTag(event.getDeathEvent().getKiller()).map(Npc::getDisplayName).ifPresent(displayName -> event.setKillerName(DisplayName.of(displayName)));
    }
}