package io.github.trae.spigot.framework.effect.events.remove;

import io.github.trae.spigot.framework.effect.Effect;
import io.github.trae.spigot.framework.effect.EffectData;
import io.github.trae.spigot.framework.effect.events.abstracts.AbstractEffectCancellableEvent;
import lombok.Getter;
import org.bukkit.entity.LivingEntity;

/**
 * Fired before an effect is deliberately removed from an entity. Cancelling leaves the effect in
 * place.
 * <p>
 * Only the {@link Effect.RemoveReason#NORMAL} path fires this: the reasons that report something
 * that already happened, such as expiry, death or a disconnect, are not vetoable.
 */
@Getter
public final class EffectPreRemoveEvent extends AbstractEffectCancellableEvent {

    /**
     * Why the effect is being removed. Always {@link Effect.RemoveReason#NORMAL}, since no other
     * path fires this event.
     */
    private final Effect.RemoveReason removeReason;

    /**
     * @param effect       the effect about to be removed
     * @param data         the entity's current state for that effect
     * @param livingEntity the entity it would be removed from
     * @param removeReason why it is being removed
     */
    public EffectPreRemoveEvent(final Effect effect, final EffectData data, final LivingEntity livingEntity, final Effect.RemoveReason removeReason) {
        super(effect, data, livingEntity);

        this.removeReason = removeReason;
    }
}