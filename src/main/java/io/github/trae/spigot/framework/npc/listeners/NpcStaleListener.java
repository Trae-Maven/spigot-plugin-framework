package io.github.trae.spigot.framework.npc.listeners;

import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.npc.NpcManager;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.event.world.EntitiesLoadEvent;

import java.util.List;

/**
 * Removes NPC-tagged entities left behind in the world, such as ones that survived a crash or a
 * reload, so they are never mistaken for or duplicated alongside the live NPCs.
 */
@AllArgsConstructor
@Singleton
public final class NpcStaleListener implements Listener {

    /**
     * The manager stale entity removal is delegated to.
     */
    private final NpcManager npcManager;

    /**
     * Passes every entity to {@link NpcManager#removeStaleEntity(Entity)}, which removes it only if
     * it carries the NPC tag.
     *
     * @param entities the entities to check
     */
    private void handleEntities(final List<Entity> entities) {
        for (final Entity entity : entities) {
            this.npcManager.removeStaleEntity(entity);
        }
    }

    /**
     * Sweeps every loaded world for tagged entities once the server has finished loading.
     *
     * @param event the server load event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onServerLoad(final ServerLoadEvent event) {
        for (final World world : Bukkit.getServer().getWorlds()) {
            this.handleEntities(world.getEntities());
        }
    }

    /**
     * Sweeps entities loaded from disk with a chunk for tagged entities.
     *
     * @param event the entities load event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitiesLoad(final EntitiesLoadEvent event) {
        this.handleEntities(event.getEntities());
    }
}