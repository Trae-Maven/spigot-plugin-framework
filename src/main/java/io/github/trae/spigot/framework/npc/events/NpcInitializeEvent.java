package io.github.trae.spigot.framework.npc.events;

import io.github.trae.spigot.framework.event.CustomEvent;
import io.github.trae.spigot.framework.npc.Npc;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.LivingEntity;

/**
 * Fired while an NPC's backing entity is being spawned, after every base setting has been applied
 * but before the entity is added to the world.
 * <p>
 * Dispatched from inside the spawn consumer of {@link Npc#spawn()}, so any change a listener makes
 * to the entity, such as equipment, attributes or metadata, is already in place when the entity
 * first reaches a client. The entity is not yet in the world, so listeners must not teleport it,
 * mount it, or do anything else that needs it to be added.
 * <p>
 * Not cancellable, as the spawn is already underway. Use {@link NpcPreSpawnEvent} to prevent a spawn.
 * <p>
 * Fired synchronously, as spawning happens on the main thread.
 */
@AllArgsConstructor
@Getter
public final class NpcInitializeEvent extends CustomEvent {

    /**
     * The NPC whose backing entity is being initialised.
     */
    private final Npc<?> npc;

    /**
     * The backing entity being initialised, not yet added to the world.
     */
    private final LivingEntity entity;
}