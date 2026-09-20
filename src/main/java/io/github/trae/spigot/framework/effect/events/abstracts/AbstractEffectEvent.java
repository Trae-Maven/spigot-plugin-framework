package io.github.trae.spigot.framework.effect.events.abstracts;

import io.github.trae.spigot.framework.effect.Effect;
import io.github.trae.spigot.framework.effect.EffectData;
import io.github.trae.spigot.framework.event.CustomEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.LivingEntity;

/**
 * Base for the effect events that report a transition already applied, carrying the effect, the
 * holder, and the state involved.
 *
 * @see AbstractEffectCancellableEvent for the pre-transition counterpart
 */
@AllArgsConstructor
@Getter
public class AbstractEffectEvent extends CustomEvent {

    /**
     * The effect the transition concerns.
     */
    private final Effect effect;

    /**
     * The state involved in the transition. Since this is the live instance rather than a copy,
     * mutating it here changes what the effect holds.
     */
    private final EffectData data;

    /**
     * The entity holding the effect.
     */
    private final LivingEntity livingEntity;
}