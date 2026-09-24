package io.github.trae.spigot.framework.npc.events;

import io.github.trae.spigot.framework.event.CustomCancellableEvent;
import io.github.trae.spigot.framework.npc.Npc;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.Player;

/**
 * Fired when a player right-clicks an {@link io.github.trae.spigot.framework.npc.types.InteractableNpc}.
 * <p>
 * Dispatched by {@link io.github.trae.spigot.framework.npc.listeners.NpcInteractListener} before the
 * NPC's own {@link io.github.trae.spigot.framework.npc.types.InteractableNpc#canInteract(Player)}
 * check. Cancelling it prevents the interaction from reaching the NPC, letting unrelated code veto
 * an interaction it does not own.
 * <p>
 * Fired synchronously, as it is dispatched from the interact handler on the main thread.
 */
@AllArgsConstructor
@Getter
public final class NpcInteractEvent extends CustomCancellableEvent {

    /**
     * The NPC being interacted with.
     */
    private final Npc<?> npc;

    /**
     * The player interacting with the NPC.
     */
    private final Player player;
}