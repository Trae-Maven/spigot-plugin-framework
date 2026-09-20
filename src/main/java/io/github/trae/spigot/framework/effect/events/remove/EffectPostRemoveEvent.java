package io.github.trae.spigot.framework.effect.events.remove;

import io.github.trae.spigot.framework.effect.Effect;
import io.github.trae.spigot.framework.effect.EffectData;
import io.github.trae.spigot.framework.effect.events.abstracts.AbstractEffectEvent;
import lombok.Getter;
import org.bukkit.entity.LivingEntity;

/**
 * Fired once an effect has been removed from an entity and its potion effect cleared. Not fired on
 * expiry, which reports through {@link io.github.trae.spigot.framework.effect.events.EffectExpireEvent} instead.
 */
@Getter
public final class EffectPostRemoveEvent extends AbstractEffectEvent {

    /**
     * Why the effect was removed. Never {@link Effect.RemoveReason#EXPIRE}.
     */
    private final Effect.RemoveReason removeReason;

    /**
     * @param effect       the effect that was removed
     * @param data         the state it held at removal
     * @param livingEntity the entity it was removed from
     * @param removeReason why it was removed
     */
    public EffectPostRemoveEvent(final Effect effect, final EffectData data, final LivingEntity livingEntity, final Effect.RemoveReason removeReason) {
        super(effect, data, livingEntity);

        this.removeReason = removeReason;
    }
}