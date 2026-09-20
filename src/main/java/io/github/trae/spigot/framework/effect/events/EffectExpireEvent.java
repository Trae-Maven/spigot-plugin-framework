package io.github.trae.spigot.framework.effect.events;

import io.github.trae.spigot.framework.effect.Effect;
import io.github.trae.spigot.framework.effect.EffectData;
import io.github.trae.spigot.framework.effect.events.abstracts.AbstractEffectEvent;
import org.bukkit.entity.LivingEntity;

/**
 * Fired once an effect's duration has elapsed and its potion effect been cleared, in place of
 * {@link io.github.trae.spigot.framework.effect.events.remove.EffectPostRemoveEvent}. Expiry is not vetoable, so there is no cancellable counterpart.
 */
public final class EffectExpireEvent extends AbstractEffectEvent {

    /**
     * @param effect       the effect that expired
     * @param data         the state it held at expiry
     * @param livingEntity the entity that held it
     */
    public EffectExpireEvent(final Effect effect, final EffectData data, final LivingEntity livingEntity) {
        super(effect, data, livingEntity);
    }
}