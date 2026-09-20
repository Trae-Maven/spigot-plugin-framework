package io.github.trae.spigot.framework.effect.events.add;

import io.github.trae.spigot.framework.effect.Effect;
import io.github.trae.spigot.framework.effect.EffectData;
import io.github.trae.spigot.framework.effect.events.abstracts.AbstractEffectEvent;
import org.bukkit.entity.LivingEntity;

/**
 * Fired once an effect has been applied to an entity and any potion effect sent.
 */
public final class EffectPostAddEvent extends AbstractEffectEvent {

    /**
     * @param effect       the effect that was applied
     * @param data         the state it was applied with
     * @param livingEntity the entity it was applied to
     */
    public EffectPostAddEvent(final Effect effect, final EffectData data, final LivingEntity livingEntity) {
        super(effect, data, livingEntity);
    }
}