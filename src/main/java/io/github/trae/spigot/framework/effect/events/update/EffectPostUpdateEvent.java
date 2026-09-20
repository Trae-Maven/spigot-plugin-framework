package io.github.trae.spigot.framework.effect.events.update;

import io.github.trae.spigot.framework.effect.Effect;
import io.github.trae.spigot.framework.effect.EffectData;
import io.github.trae.spigot.framework.effect.events.abstracts.AbstractEffectEvent;
import org.bukkit.entity.LivingEntity;

/**
 * Fired once an entity's effect data has been mutated and its potion effect reconciled. Fires even
 * when the update ended the effect, so the state carried may already be spent.
 */
public final class EffectPostUpdateEvent extends AbstractEffectEvent {

    /**
     * @param effect       the effect that was updated
     * @param data         the mutated state
     * @param livingEntity the entity whose effect was updated
     */
    public EffectPostUpdateEvent(final Effect effect, final EffectData data, final LivingEntity livingEntity) {
        super(effect, data, livingEntity);
    }
}