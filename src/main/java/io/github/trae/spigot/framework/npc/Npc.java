package io.github.trae.spigot.framework.npc;

import io.github.trae.spigot.framework.npc.events.NpcInitializeEvent;
import io.github.trae.spigot.framework.npc.types.DamageableNpc;
import io.github.trae.spigot.framework.utility.UtilEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;

/**
 * A non-player character backed by a real server entity.
 * <p>
 * Subclasses pass the entity class, identifier, namespace and location to the constructor and
 * override only the settings they need. Behaviour is opted into by implementing
 * {@link io.github.trae.spigot.framework.npc.types.InteractableNpc} for right-click handling and
 * {@link DamageableNpc} for damage, death and respawn handling.
 * Each NPC is registered as a {@code @Singleton} and collected by {@link NpcManager}.
 * <p>
 * The backing entity is spawned non-persistent, so it is never saved to the chunk and never
 * duplicates across restarts, and is tagged with {@link #IDENTIFIER_KEY} so any leftover can be
 * recognised and removed. When its chunk unloads the entity is discarded, and the manager respawns
 * it once the chunk is loaded again.
 *
 * @param <Entity> the type of the backing entity
 */
@RequiredArgsConstructor
@Getter
public class Npc<Entity extends LivingEntity> {

    /**
     * The persistent data key every backing entity is tagged with, holding the NPC's identifier.
     */
    public static final NamespacedKey IDENTIFIER_KEY = new NamespacedKey("custom", "npc_identifier");

    /**
     * The class of the backing entity to spawn.
     */
    private final Class<Entity> entityClass;

    /**
     * The identifier this NPC is registered and looked up under, and the namespace grouping it,
     * such as the plugin or feature that owns it.
     */
    private final String identifier, namespace;

    /**
     * Where the backing entity is spawned.
     */
    private final Location location;

    /**
     * The backing entity, or {@code null} while the NPC is not spawned.
     */
    private Entity entity;

    /**
     * Whether the backing entity died and the NPC has not spawned since. While set, the manager only
     * respawns the NPC once {@link DamageableNpc#canRespawn()} allows it. A missing entity for any
     * other reason, such as a chunk unload, respawns without that check.
     */
    private boolean dead;

    /**
     * Returns the custom name shown above the entity, or {@code null} for none. Defaults to {@code null}.
     *
     * @return the display name, or {@code null}
     */
    public Component getDisplayName() {
        return null;
    }

    /**
     * Returns whether the entity has AI, allowing it to move, path and attack. Defaults to {@code false}.
     *
     * @return {@code true} if the entity has AI
     */
    protected boolean hasAI() {
        return false;
    }

    /**
     * Returns whether the entity is invulnerable to damage. Defaults to {@code true} unless this NPC
     * is a {@link DamageableNpc}, since vanilla never fires damage events for an invulnerable entity.
     *
     * @return {@code true} if the entity is invulnerable
     */
    protected boolean isInvulnerable() {
        return !(this instanceof DamageableNpc);
    }

    /**
     * Returns whether the entity makes no sounds. Defaults to {@code true}.
     *
     * @return {@code true} if the entity is silent
     */
    protected boolean isSilent() {
        return true;
    }

    /**
     * Returns whether the entity can be pushed by, and push, other entities. Defaults to {@code false}.
     *
     * @return {@code true} if the entity is collidable
     */
    protected boolean isCollidable() {
        return false;
    }

    /**
     * Returns whether this NPC may currently spawn. The per-NPC half of the gate pattern, checked
     * after the {@link io.github.trae.spigot.framework.npc.events.NpcPreSpawnEvent}. Defaults to
     * {@code true}.
     *
     * @return {@code true} if the NPC may spawn
     */
    protected boolean canSpawn() {
        return true;
    }

    /**
     * Called once the entity has been spawned and every base setting applied, for further setup
     * such as equipment or attributes.
     *
     * @param entity the spawned entity
     */
    protected void onSpawn(final Entity entity) {
    }

    /**
     * Returns whether the NPC has a backing entity that is still valid in the world.
     *
     * @return {@code true} if the NPC is spawned
     */
    public final boolean isSpawned() {
        return this.entity != null && this.entity.isValid();
    }

    /**
     * Returns whether the NPC's location is in a loaded world and chunk, so its entity can be spawned.
     *
     * @return {@code true} if the location can be spawned into
     */
    public final boolean isSpawnable() {
        return this.location != null && this.location.getWorld() != null && location.isChunkLoaded();
    }

    /**
     * Spawns the backing entity at the NPC's location, clears the {@link #dead} flag, then calls
     * {@link #onSpawn(LivingEntity)}.
     * <p>
     * Before the entity is added to the world, it is tagged with {@link #IDENTIFIER_KEY}, every base
     * setting is applied, and a {@link NpcInitializeEvent} is dispatched so listeners can adjust it
     * before it first reaches a client.
     * <p>
     * Does not dispatch the pre-spawn event or check {@link #canSpawn()}; spawn through
     * {@link NpcManager#spawn(Npc)} so the gates apply and the entity is tracked.
     *
     * @return {@code true} if the entity was spawned, {@code false} if the location is not spawnable
     */
    public final boolean spawn() {
        if (!(this.isSpawnable())) {
            return false;
        }

        final Location location = this.location;
        final Component displayName = this.getDisplayName();

        this.entity = location.getWorld().spawn(location, this.entityClass, entity -> {
            entity.getPersistentDataContainer().set(IDENTIFIER_KEY, PersistentDataType.STRING, this.identifier);
            entity.setPersistent(false);
            entity.setRemoveWhenFarAway(false);
            entity.setCanPickupItems(false);
            entity.setAI(this.hasAI());
            entity.setInvulnerable(this.isInvulnerable());
            entity.setSilent(this.isSilent());
            entity.setCollidable(this.isCollidable());
            entity.customName(displayName);
            entity.setCustomNameVisible(displayName != null);

            UtilEvent.dispatch(new NpcInitializeEvent(this, entity));
        });

        this.dead = false;

        this.onSpawn(this.entity);

        return true;
    }

    /**
     * Removes the backing entity from the world and clears the reference. No-op if not spawned.
     * <p>
     * Does not update the manager's entity tracking; despawn through {@link NpcManager#despawn(Npc)}.
     */
    public final void despawn() {
        if (this.entity == null) {
            return;
        }

        this.entity.remove();

        this.entity = null;
    }

    /**
     * Clears the reference to the backing entity without removing it, for when the entity is already
     * gone, such as after a chunk unload. The manager respawns the NPC on its next pass.
     */
    public final void forget() {
        this.entity = null;
    }

    /**
     * Clears the reference to the backing entity and sets the {@link #dead} flag, for when the entity
     * has died. The manager respawns the NPC only once {@link DamageableNpc#canRespawn()} allows it.
     */
    public final void markDead() {
        this.entity = null;
        this.dead = true;
    }
}