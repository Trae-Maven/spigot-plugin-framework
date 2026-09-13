package io.github.trae.spigot.framework.damage.data;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.kyori.adventure.text.Component;

/**
 * What a damage pass is attributed to in messages.
 *
 * <p>A plain reason names the thing that dealt the damage and nothing more, which is what the
 * pipeline seeds from the attacker's held item. Death messages read it to say what someone was
 * killed with.</p>
 *
 * <p>The name carries its own formatting, so an item's reason keeps the item's display name and
 * hover tooltip rather than being flattened to text.</p>
 *
 * @see CustomReason
 */
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Reason {

    /**
     * How the reason reads in a message, with formatting and any hover intact.
     */
    private final Component name;

    /**
     * Builds a reason from a name.
     *
     * @param name how it reads in a message
     * @return the reason
     */
    public static Reason of(final Component name) {
        return new Reason(name);
    }
}