package io.github.trae.spigot.framework.npc.types;

import io.github.trae.spigot.framework.damage.events.damage.CustomDamageEvent;
import io.github.trae.spigot.framework.death.events.CustomDeathEvent;
import io.github.trae.spigot.framework.death.events.VanillaDeathEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Marks an {@link io.github.trae.spigot.framework.npc.Npc} whose backing entity can be damaged, die
 * and respawn. Any NPC that does not implement this has all damage to it cancelled.
 * <p>
 * Damage and deaths are each routed through exactly one of their callbacks, depending on which
 * damage and death pipelines are active. Damage is routed by
 * {@link io.github.trae.spigot.framework.npc.listeners.NpcDamageListener} and deaths by
 * {@link io.github.trae.spigot.framework.npc.listeners.NpcDeathListener}. After a death the entity
 * is forgotten, and {@link io.github.trae.spigot.framework.npc.NpcManager} respawns the NPC once
 * {@link #canRespawn()} returns {@code true}.
 */
public interface DamageableNpc {

    /**
     * Returns whether this NPC may respawn now. Checked on every scheduler pass while the NPC has no
     * entity, so a respawn delay is implemented by tracking the death time and comparing against it.
     * Defaults to {@code false}, so the NPC stays gone after dying.
     *
     * @return {@code true} if the NPC may respawn
     */
    default boolean canRespawn() {
        return false;
    }

    /**
     * Called when the backing entity is damaged by another entity and the damage subsystem is not
     * registered.
     *
     * @param event the vanilla entity damage by entity event
     */
    default void onEntityDamageByEntityEvent(final EntityDamageByEntityEvent event) {
    }

    /**
     * Called when the backing entity is damaged and the damage subsystem is registered.
     *
     * @param event the damage subsystem's custom damage event
     */
    default void onCustomDamageEvent(final CustomDamageEvent event) {
    }

    /**
     * Called when the backing entity dies and the death subsystem is not registered.
     *
     * @param event the vanilla entity death event
     */
    default void onEntityDeathEvent(final EntityDeathEvent event) {
    }

    /**
     * Called when the backing entity dies and the death subsystem is registered without the damage
     * manager.
     *
     * @param event the death subsystem's vanilla death event
     */
    default void onVanillaDeathEvent(final VanillaDeathEvent event) {
    }

    /**
     * Called when the backing entity dies and the death subsystem is registered with the damage
     * manager.
     *
     * @param event the death subsystem's custom death event
     */
    default void onCustomDeathEvent(final CustomDeathEvent event) {
    }
}