package io.github.trae.spigot.framework.death.events;

import io.github.trae.spigot.framework.displayname.DisplayName;
import io.github.trae.spigot.framework.event.CustomCancellableEvent;
import io.github.trae.spigot.framework.utility.UtilColor;
import io.github.trae.spigot.framework.utility.UtilMessage;
import io.github.trae.spigot.framework.utility.enums.ChatColor;
import io.github.trae.utilities.UtilString;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * A death message about to be sent to one player.
 *
 * <p>Dispatched once per recipient, so a plugin can vary the message by who is reading it, or cancel
 * to suppress it for some players and not others.</p>
 *
 * <p>Both names are seeded on construction and settable here, so a recipient-specific rename, such
 * as showing a team colour, replaces only their copy.</p>
 *
 * @see CustomDeathEvent
 * @see VanillaDeathEvent
 */
@Getter
@Setter
public final class CustomDeathMessageEvent extends CustomCancellableEvent {

    /**
     * The death being described, and the player it is being described to. Held as the shared
     * interface, since a message reads the same either way.
     */
    private final DeathEvent deathEvent;
    private final Player recipient;

    /**
     * How each side is named in this recipient's copy of the message. The killer's name is
     * {@code null} for environmental deaths.
     */
    private DisplayName entityName, killerName;

    /**
     * Seeds both names off the damage pass, so anything a plugin renamed during the damage is
     * already in place before a listener gets its chance to change them again.
     *
     * @param deathEvent the death being described
     * @param recipient  the player the message is for
     */
    public CustomDeathMessageEvent(final CustomDeathEvent deathEvent, final Player recipient) {
        this.deathEvent = deathEvent;
        this.recipient = recipient;

        this.entityName = deathEvent.getDamageEvent().getDamageeName();
        this.killerName = deathEvent.getDamageEvent().getDamagerName();
    }

    /**
     * Seeds both names off the entities themselves, since a vanilla death carries no renamed sides
     * to inherit.
     *
     * @param deathEvent the death being described
     * @param recipient  the player the message is for
     */
    public CustomDeathMessageEvent(final VanillaDeathEvent deathEvent, final Player recipient) {
        this.deathEvent = deathEvent;
        this.recipient = recipient;

        this.entityName = DisplayName.of(Component.text(deathEvent.getEntity().getName()).color(UtilColor.toTextColor(ChatColor.YELLOW.getColor())));
        this.killerName = deathEvent.getKiller() == null ? null : DisplayName.of(Component.text(deathEvent.getKiller().getName()).color(UtilColor.toTextColor(ChatColor.YELLOW.getColor())));
    }

    /**
     * The killer's name as it goes in a sentence.
     *
     * <p>A player is named bare, since a player name is already definite. Anything else gets an
     * article, chosen from the killer's raw name rather than the display name, so a renamed killer
     * still reads naturally.</p>
     *
     * @return the name ready to drop into a sentence, or {@code null} when there was no killer
     */
    public Component getFormattedKillerName() {
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

        String name = UtilMessage.serializeWithReset(killerName.getFullName());
        if (!(killer instanceof Player)) {
            name = UtilString.getIndefiniteArticlePrefix(killer.getName()) + name;
        }

        return UtilMessage.deserialize(name);
    }
}