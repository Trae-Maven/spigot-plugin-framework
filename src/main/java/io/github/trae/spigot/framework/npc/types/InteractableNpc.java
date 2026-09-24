package io.github.trae.spigot.framework.npc.types;

import org.bukkit.entity.Player;

/**
 * Marks an {@link io.github.trae.spigot.framework.npc.Npc} that players can right-click.
 * <p>
 * Interactions are routed by {@link io.github.trae.spigot.framework.npc.listeners.NpcInteractListener}
 * after the {@link io.github.trae.spigot.framework.npc.events.NpcInteractEvent} has passed.
 */
public interface InteractableNpc {

    /**
     * Returns whether the player may interact with this NPC. The per-NPC half of the gate pattern,
     * checked after the {@link io.github.trae.spigot.framework.npc.events.NpcInteractEvent}.
     *
     * @param player the player attempting to interact
     * @return {@code true} if the interaction may proceed
     */
    boolean canInteract(final Player player);

    /**
     * Called when a player interacts with this NPC and every gate has passed.
     *
     * @param player the player interacting
     */
    void onInteract(final Player player);
}