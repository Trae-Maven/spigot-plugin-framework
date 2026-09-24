package io.github.trae.spigot.framework.npc.events;

import io.github.trae.spigot.framework.event.CustomEvent;
import io.github.trae.spigot.framework.npc.Npc;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Fired after an NPC's backing entity has been spawned into the world and is being tracked.
 * <p>
 * The spawned entity is available through the NPC's {@code getEntity()}. Not cancellable, as the
 * entity already exists. Use {@link NpcPreSpawnEvent} to prevent a spawn.
 * <p>
 * Fired synchronously, as spawning happens on the main thread.
 */
@AllArgsConstructor
@Getter
public final class NpcPostSpawnEvent extends CustomEvent {

    /**
     * The NPC that was spawned.
     */
    private final Npc<?> npc;
}