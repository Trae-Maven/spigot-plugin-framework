package io.github.trae.spigot.framework.death.events;

import io.github.trae.spigot.framework.displayname.DisplayName;
import io.github.trae.spigot.framework.event.CustomCancellableEvent;
import io.github.trae.spigot.framework.utility.UtilMessage;
import io.github.trae.utilities.UtilString;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * A death message about to be sent to one player.
 *
 * <p>Dispatched once per recipient, so a plugin can vary the message by who is reading it, or
 * cancel to suppress it for some players and not others.</p>
 *
 * <p>Both names are seeded from the damage pass and settable here, so a recipient-specific rename,
 * such as showing a team colour, replaces only their copy.</p>
 *
 * @see CustomDeathEvent
 */
@Getter
@Setter
public class CustomDeathMessageEvent extends CustomCancellableEvent {

    /**
     * The death being described, and the player it is being described to.
     */
    private final CustomDeathEvent deathEvent;
    private final Player recipient;

    /**
     * How each side is named in this recipient's copy of the message. The killer's name is
     * {@code null} for environmental deaths.
     */
    private DisplayName entityName, killerName;

    public CustomDeathMessageEvent(final CustomDeathEvent deathEvent, final Player recipient) {
        this.deathEvent = deathEvent;
        this.recipient = recipient;

        this.entityName = deathEvent.getDamageEvent().getDamageeName();
        this.killerName = deathEvent.getDamageEvent().getDamagerName();
    }

    /**
     * The killer's name with an article in front of it, ready to drop into a sentence.
     *
     * <p>The article is chosen from the killer's raw name rather than the display name, since a
     * renamed killer should still read naturally.</p>
     *
     * @return the article-prefixed name, or {@code null} when there was no killer
     */
    public final Component getFormattedKillerName() {
        final DisplayName killerName = this.killerName;
        if (killerName == null) {
            return null;
        }

        if (killerName.getName() == null) {
            return null;
        }

        final Entity killer = this.deathEvent.getKiller();
        if (killer == null) {
            return null;
        }

        return UtilMessage.deserialize(UtilString.getIndefiniteArticlePrefix(killer.getName()) + UtilMessage.serializeWithReset(killerName.getFullName()));
    }
}