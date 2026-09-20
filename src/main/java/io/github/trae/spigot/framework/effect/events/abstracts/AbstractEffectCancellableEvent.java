package io.github.trae.spigot.framework.effect.events.abstracts;

import io.github.trae.spigot.framework.effect.Effect;
import io.github.trae.spigot.framework.effect.EffectData;
import io.github.trae.spigot.framework.event.CustomCancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.LivingEntity;

/**
 * Base for the effect events fired before a transition is applied, where cancelling stops the
 * transition outright.
 *
 * @see AbstractEffectEvent for the post-transition counterpart
 */
@AllArgsConstructor
@Getter
public class AbstractEffectCancellableEvent extends CustomCancellableEvent {

    /**
     * The effect the transition concerns.
     */
    private final Effect effect;

    /**
     * The state the transition would be applied with. Since this is the live instance rather than a
     * copy, mutating it here changes what the effect goes on to store.
     */
    private final EffectData data;

    /**
     * The entity holding the effect.
     */
    private final LivingEntity livingEntity;
}