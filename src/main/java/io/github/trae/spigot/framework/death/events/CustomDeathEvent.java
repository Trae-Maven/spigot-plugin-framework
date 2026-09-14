package io.github.trae.spigot.framework.death.events;

import io.github.trae.spigot.framework.damage.data.CustomReason;
import io.github.trae.spigot.framework.damage.data.Reason;
import io.github.trae.spigot.framework.damage.events.damage.CustomPostDamageEvent;
import io.github.trae.spigot.framework.event.CustomEvent;
import lombok.Getter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * An entity has died, with the damage pass that killed it attached.
 *
 * <p>Fired after the vanilla death events, so drops and death handling have already run. Not
 * cancellable for that reason: by the time this reaches a listener the death is settled.</p>
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
public class CustomDeathEvent extends CustomEvent implements DeathEvent {

    /**
     * The damage pass that killed the entity, still holding the item, the names and the cause.
     */
    private final CustomPostDamageEvent damageEvent;

    /**
     * The entity that died, and the entity that killed it. The killer is {@code null} for
     * environmental deaths, and is taken from the damage pass rather than resolved again.
     */
    private final LivingEntity entity;
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
     * Takes the reason already resolved rather than resolving it here, since the lookup needs the
     * damage manager's retained state and this event is meant to be readable without it.
     *
     * @param damageEvent the damage pass that killed the entity
     * @param entity      the entity that died
     * @param reason      what the death is attributed to, or {@code null} when there is nothing to
     *                    name
     */
    public CustomDeathEvent(final CustomPostDamageEvent damageEvent, final LivingEntity entity, final Reason reason) {
        this.damageEvent = damageEvent;

        this.entity = entity;
        this.killer = damageEvent.getDamager();
        this.reason = reason;
    }

    /**
     * The cause recorded by the killing damage pass.
     *
     * @return the damage cause behind the death
     */
    @Override
    public final EntityDamageEvent.DamageCause getCause() {
        return this.damageEvent.getCause();
    }
}