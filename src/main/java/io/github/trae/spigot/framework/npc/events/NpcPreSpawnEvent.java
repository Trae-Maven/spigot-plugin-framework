package io.github.trae.spigot.framework.npc.events;

import io.github.trae.spigot.framework.event.CustomCancellableEvent;
import io.github.trae.spigot.framework.npc.Npc;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Fired before an NPC's backing entity is spawned.
 * <p>
 * Dispatched by {@link io.github.trae.spigot.framework.npc.NpcManager#spawn(Npc)} before the NPC's
 * own {@code canSpawn()} check. Cancelling it prevents the spawn, letting unrelated code veto an
 * NPC it does not own. A cancelled spawn is retried on the manager's next scheduler pass.
 * <p>
 * Fired synchronously, as spawning happens on the main thread.
 */
@AllArgsConstructor
@Getter
public final class NpcPreSpawnEvent extends CustomCancellableEvent {

    /**
     * The NPC about to be spawned.
     */
    private final Npc<?> npc;
}