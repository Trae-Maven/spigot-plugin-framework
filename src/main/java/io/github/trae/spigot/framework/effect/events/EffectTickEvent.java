package io.github.trae.spigot.framework.effect.events;

import io.github.trae.spigot.framework.effect.Effect;
import io.github.trae.spigot.framework.effect.EffectData;
import io.github.trae.spigot.framework.effect.events.abstracts.AbstractEffectEvent;
import org.bukkit.entity.LivingEntity;

/**
 * Fired every tick for each live, unexpired holder of an effect, before the effect's own tick
 * hook. Given the tick rate, handlers should stay cheap.
 */
public final class EffectTickEvent extends AbstractEffectEvent {

    /**
     * @param effect       the effect being ticked
     * @param data         the entity's current state for that effect
     * @param livingEntity the entity holding the effect
     */
    public EffectTickEvent(final Effect effect, final EffectData data, final LivingEntity livingEntity) {
        super(effect, data, livingEntity);
    }
}