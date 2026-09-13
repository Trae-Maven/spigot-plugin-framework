package io.github.trae.spigot.framework.death.events;

import io.github.trae.spigot.framework.damage.data.CustomReason;
import io.github.trae.spigot.framework.damage.data.Reason;
import io.github.trae.spigot.framework.damage.events.damage.CustomPostDamageEvent;
import io.github.trae.spigot.framework.event.CustomEvent;
import io.github.trae.spigot.framework.utility.UtilMessage;
import io.github.trae.utilities.UtilString;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

/**
 * An entity has died, with the damage pass that killed it attached.
 *
 * <p>Fired after the vanilla death events, so drops and death handling have already run. Not
 * cancellable for that reason: by the time this reaches a listener the death is settled.</p>
 *
 * <p>The names of both sides live on the damage pass, so a plugin that renamed either during the
 * damage sees that carried through. The reason is the exception: it is resolved by the death system
 * and carried here, since the attribution can come from an earlier hit rather than the killing
 * one.</p>
 *
 * @see io.github.trae.spigot.framework.death.listeners.DeathListener
 * @see CustomDeathMessageEvent
 */
@Getter
public class CustomDeathEvent extends CustomEvent {

    /**
     * The damage pass that killed the entity.
     */
    private final CustomPostDamageEvent damageEvent;

    /**
     * The entity that died, and the entity that killed it. The killer is {@code null} for
     * environmental deaths, and is taken from the damage pass rather than resolved again.
     */
    private final LivingEntity entity;
    private final Entity killer;

    /**
     * What the death is attributed to in the message.
     *
     * <p>Resolved by the death system rather than read straight off the damage pass: an unexpired
     * {@link CustomReason} the killer set earlier wins over the killing hit's own reason, so a kill
     * landed with a sword moments after an ability still names the ability.</p>
     */
    private final Reason reason;

    public CustomDeathEvent(final CustomPostDamageEvent damageEvent, final LivingEntity entity, final Reason reason) {
        this.damageEvent = damageEvent;

        this.entity = entity;
        this.killer = damageEvent.getDamager();
        this.reason = reason;
    }

    /**
     * The reason as it should read in a sentence.
     *
     * <p>A plain reason gets an article in front of it, chosen from its plain text, so an item reads
     * as "with a Diamond Sword". A {@link CustomReason} does not, since an ability reads as "with
     * Frostbite" rather than "with a Frostbite". Either way the component is rebuilt so the hover
     * tooltip survives the round trip.</p>
     *
     * @return the formatted reason, or {@code null} when there is no reason to show
     */
    public final Component getFormattedReason() {
        final Reason reason = this.reason;
        if (reason == null) {
            return null;
        }

        final Component reasonName = reason.getName();
        if (reasonName == null) {
            return null;
        }

        final String indefiniteArticlePrefix = reason instanceof CustomReason ? "" : UtilString.getIndefiniteArticlePrefix(PlainTextComponentSerializer.plainText().serialize(reasonName));

        return UtilMessage.deserialize(indefiniteArticlePrefix + UtilMessage.serializeWithReset(reasonName));
    }
}