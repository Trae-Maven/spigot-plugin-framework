package io.github.trae.spigot.framework.effect.events.update;

import io.github.trae.spigot.framework.effect.Effect;
import io.github.trae.spigot.framework.effect.EffectData;
import io.github.trae.spigot.framework.effect.events.abstracts.AbstractEffectCancellableEvent;
import org.bukkit.entity.LivingEntity;

/**
 * Fired before an entity's effect data is mutated. Cancelling means the update consumer never runs,
 * so the state is left untouched.
 */
public final class EffectPreUpdateEvent extends AbstractEffectCancellableEvent {

    /**
     * @param effect       the effect about to be updated
     * @param data         the entity's current state, as yet unmodified
     * @param livingEntity the entity whose effect would be updated
     */
    public EffectPreUpdateEvent(final Effect effect, final EffectData data, final LivingEntity livingEntity) {
        super(effect, data, livingEntity);
    }
}