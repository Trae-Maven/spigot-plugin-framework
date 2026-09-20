package io.github.trae.spigot.framework.death.events;

import io.github.trae.spigot.framework.damage.data.CustomReason;
import io.github.trae.spigot.framework.damage.data.Reason;
import io.github.trae.spigot.framework.damage.events.damage.CustomPostDamageEvent;
import io.github.trae.spigot.framework.event.CustomEvent;
import io.github.trae.spigot.framework.sound.SoundProvider;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * An entity has died, with the damage pass that killed it attached.
 *
 * <p>Fired from inside the vanilla death event at its last priority, so the death itself is settled
 * and this event is not cancellable. Drops, experience and the death sound are still open at that
 * point: all three are carried here, and whatever listeners leave them as is written back to the
 * vanilla death.</p>
 *
 * <p>The names of both sides live on the damage pass, so a plugin that renamed either during the
 * damage sees that carried through. The reason is the exception: it is resolved by the death system
 * and carried here, since the attribution can come from an earlier hit rather than the killing
 * one.</p>
 *
 * @see io.github.trae.spigot.framework.death.listeners.DeathListener
 * @see CustomDeathMessageEvent
 */
@Getter
@Setter
public final class CustomDeathEvent extends CustomEvent implements DeathEvent {

    /**
     * The damage pass that killed the entity, still holding the item, the names and the cause.
     */
    private final CustomPostDamageEvent damageEvent;

    /**
     * The entity that died.
     */
    private final LivingEntity entity;

    /**
     * The entity that killed it, or {@code null} for an environmental death.
     *
     * <p>Taken from the damage pass rather than resolved again, so it always agrees with the pass
     * attached to this event.</p>
     */
    private final Entity killer;

    /**
     * What the death is attributed to in the message.
     *
     * <p>Resolved by the death system rather than read straight off the damage pass: an unexpired
     * {@link CustomReason} the killer set earlier wins over the killing hit's own reason, so a kill
     * landed with a sword moments after an ability still names the ability.</p>
     */
    private final Reason reason;

    /**
     * The items that will be dropped as part of the death.
     *
     * <p>The list remains mutable so listeners can add, remove or replace drops before they are
     * ultimately handled.</p>
     */
    private final List<ItemStack> drops;

    /**
     * The amount of experience that will be dropped as part of the death.
     *
     * <p>Mutable so listeners can change or suppress the experience awarded by the death.</p>
     */
    private int dropExp;

    /**
     * The sound played for the death, or {@code null} for none.
     *
     * <p>Mutable so listeners can replace the sound, or clear it to keep the death silent.</p>
     */
    private SoundProvider soundProvider;

    /**
     * Takes the reason already resolved rather than resolving it here, since the lookup needs the
     * damage manager's retained state and this event is meant to be readable without it.
     *
     * @param damageEvent   the damage pass that killed the entity
     * @param entity        the entity that died
     * @param reason        what the death is attributed to, or {@code null} when there is nothing to
     *                      name
     * @param drops         the items that will be dropped as part of the death
     * @param dropExp       the amount of experience that will be dropped as part of the death
     * @param soundProvider the sound played for the death, or {@code null} for none
     */
    public CustomDeathEvent(final CustomPostDamageEvent damageEvent, final LivingEntity entity, final Reason reason, final List<ItemStack> drops, final int dropExp, final SoundProvider soundProvider) {
        this.damageEvent = damageEvent;
        this.entity = entity;
        this.killer = damageEvent.getDamager();
        this.reason = reason;
        this.drops = drops;
        this.dropExp = dropExp;
        this.soundProvider = soundProvider;
    }

    /**
     * The cause recorded by the killing damage pass.
     *
     * @return the damage cause behind the death
     */
    @Override
    public EntityDamageEvent.DamageCause getCause() {
        return this.damageEvent.getCause();
    }
}