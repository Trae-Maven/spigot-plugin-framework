package io.github.trae.spigot.framework.effect.events.add;

import io.github.trae.spigot.framework.effect.Effect;
import io.github.trae.spigot.framework.effect.EffectData;
import io.github.trae.spigot.framework.effect.events.abstracts.AbstractEffectCancellableEvent;
import org.bukkit.entity.LivingEntity;

/**
 * Fired before an effect is applied to an entity. Cancelling prevents the effect from being stored
 * and the potion effect from being sent.
 */
public final class EffectPreAddEvent extends AbstractEffectCancellableEvent {

    /**
     * @param effect       the effect about to be applied
     * @param data         the state it would be applied with
     * @param livingEntity the entity it would be applied to
     */
    public EffectPreAddEvent(final Effect effect, final EffectData data, final LivingEntity livingEntity) {
        super(effect, data, livingEntity);
    }
}