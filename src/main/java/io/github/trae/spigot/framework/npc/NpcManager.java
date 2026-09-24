package io.github.trae.spigot.framework.npc;

import io.github.trae.di.InjectorApi;
import io.github.trae.di.annotations.method.Scheduler;
import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.npc.events.NpcPostSpawnEvent;
import io.github.trae.spigot.framework.npc.events.NpcPreSpawnEvent;
import io.github.trae.spigot.framework.npc.types.DamageableNpc;
import io.github.trae.spigot.framework.utility.UtilEvent;
import lombok.Getter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Tracks every registered {@link Npc} and keeps its backing entity spawned.
 * <p>
 * NPCs are collected from the injector once the server has finished starting. A scheduler then
 * forgets any entity that is no longer valid, such as after a chunk unload, and spawns every NPC
 * without an entity whose location is loaded. An NPC whose entity died is only respawned once
 * {@link DamageableNpc#canRespawn()} allows it. Backing entities are indexed by UUID so entity
 * lookups from listeners are constant time.
 */
@Getter
@Singleton
public final class NpcManager implements Listener {

    /**
     * The NPC owning each tracked backing entity, keyed by the entity's UUID.
     */
    private final ConcurrentHashMap<UUID, Npc<?>> entityMap = new ConcurrentHashMap<>();

    /**
     * Every registered NPC, keyed by identifier. {@code null} until the server has finished starting.
     */
    private Map<String, Npc<?>> npcMap;

    /**
     * Collects every registered NPC from the injector once, when the server finishes starting.
     *
     * @param event the server load event
     */
    @EventHandler
    public void onServerLoad(final ServerLoadEvent event) {
        if (event.getType() != ServerLoadEvent.LoadType.STARTUP) {
            return;
        }

        if (this.npcMap == null) {
            this.npcMap = InjectorApi.getAll(Npc.class).stream().<Npc<?>>map(npc -> npc).collect(Collectors.toMap(Npc::getIdentifier, npc -> npc));
        }
    }

    /**
     * Periodic pass over every NPC: forgets and stops tracking an entity that is no longer valid,
     * and spawns an NPC with no entity whose location is loaded. An NPC flagged as dead is only
     * spawned once it is a {@link DamageableNpc} whose {@link DamageableNpc#canRespawn()} allows it.
     */
    @Scheduler(period = 1, unit = TimeUnit.SECONDS)
    public void onScheduler() {
        if (this.npcMap == null) {
            return;
        }

        for (final Npc<?> npc : this.npcMap.values()) {
            final LivingEntity entity = npc.getEntity();

            if (entity != null && !entity.isValid()) {
                this.entityMap.remove(entity.getUniqueId());
                npc.forget();
            }

            if (entity == null && npc.isSpawnable() && (!npc.isDead() || (npc instanceof final DamageableNpc damageableNpc && damageableNpc.canRespawn()))) {
                this.spawn(npc);
            }
        }
    }

    /**
     * Finds the NPC whose backing entity is the given entity, among tracked entities only.
     *
     * @param entity the entity to look up, possibly {@code null}
     * @return an {@link Optional} containing the NPC, or empty if the entity is {@code null} or not a
     * tracked NPC
     */
    public Optional<Npc<?>> getNpcByEntity(final Entity entity) {
        if (entity == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(this.entityMap.get(entity.getUniqueId()));
    }

    /**
     * Finds the NPC the entity is tagged as through {@link Npc#IDENTIFIER_KEY}, whether or not the
     * entity is still tracked. Use this where the entity may already have been untracked, such as
     * after a death.
     *
     * @param entity the entity to look up, possibly {@code null}
     * @return an {@link Optional} containing the NPC, or empty if the entity is {@code null}, untagged,
     * or tagged with an identifier no NPC is registered under
     */
    public Optional<Npc<?>> getNpcByTag(final Entity entity) {
        if (entity == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(entity.getPersistentDataContainer().get(Npc.IDENTIFIER_KEY, PersistentDataType.STRING)).flatMap(this::getNpcByIdentifier);
    }

    /**
     * Finds the NPC registered under the given identifier.
     *
     * @param identifier the NPC identifier
     * @return an {@link Optional} containing the NPC, or empty if none is registered under it
     */
    public Optional<Npc<?>> getNpcByIdentifier(final String identifier) {
        if (this.npcMap == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(this.npcMap.get(identifier));
    }

    /**
     * Spawns the NPC's backing entity and starts tracking it.
     * <p>
     * No-op if already spawned. Follows the gate pattern: a cancellable {@link NpcPreSpawnEvent} is
     * dispatched first, then the NPC's own {@link Npc#canSpawn()} check. Once spawned and tracked, a
     * {@link NpcPostSpawnEvent} is dispatched.
     *
     * @param npc the NPC to spawn
     */
    public void spawn(final Npc<?> npc) {
        if (npc.isSpawned()) {
            return;
        }

        if (UtilEvent.supply(new NpcPreSpawnEvent(npc)).isCancelled()) {
            return;
        }

        if (!npc.canSpawn()) {
            return;
        }

        if (npc.spawn()) {
            this.entityMap.put(npc.getEntity().getUniqueId(), npc);

            UtilEvent.dispatch(new NpcPostSpawnEvent(npc));
        }
    }

    /**
     * Stops tracking and removes the NPC's backing entity. No-op if not spawned.
     *
     * @param npc the NPC to despawn
     */
    public void despawn(final Npc<?> npc) {
        if (npc.getEntity() == null) {
            return;
        }

        this.entityMap.remove(npc.getEntity().getUniqueId());

        npc.despawn();
    }

    /**
     * Returns whether the entity carries the NPC tag {@link Npc#IDENTIFIER_KEY}.
     *
     * @param entity the entity to check
     * @return {@code true} if the entity is tagged as an NPC
     */
    public boolean isTagged(final Entity entity) {
        return entity.getPersistentDataContainer().has(Npc.IDENTIFIER_KEY);
    }

    /**
     * Removes the entity if it carries the NPC tag, stopping tracking and forgetting it on its NPC
     * if tracked, so the NPC respawns on the next scheduler pass. No-op for untagged entities.
     *
     * @param entity the entity to check and remove
     */
    public void removeStaleEntity(final Entity entity) {
        if (!this.isTagged(entity)) {
            return;
        }

        Optional.ofNullable(this.entityMap.remove(entity.getUniqueId())).ifPresent(Npc::forget);

        entity.remove();
    }
}