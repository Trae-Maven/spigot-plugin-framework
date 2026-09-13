package io.github.trae.spigot.framework.displayname;

import io.github.trae.spigot.framework.utility.UtilAdventure;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.kyori.adventure.text.Component;

/**
 * A name in three parts, with whatever sits before and after it.
 *
 * <p>Keeping the parts separate rather than pre-joined means a consumer can take just the name where
 * a prefix would be noise, such as a compact scoreboard line, and the full thing where it would
 * not.</p>
 *
 * <p>Either side may be absent, which is the normal case for anything unranked or untagged.</p>
 */
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class DisplayName {

    /**
     * What sits before the name, the name itself, and what sits after it. Prefix and suffix are
     * {@code null} when there is nothing to show.
     */
    private final Component prefix, name, suffix;

    /**
     * Builds a name with a prefix and suffix.
     *
     * @param prefix what sits before the name, or {@code null}
     * @param name   the name itself
     * @param suffix what sits after the name, or {@code null}
     * @return the display name
     */
    public static DisplayName of(final Component prefix, final Component name, final Component suffix) {
        return new DisplayName(prefix, name, suffix);
    }

    /**
     * Builds a name with nothing either side of it.
     *
     * @param name the name itself
     * @return the display name
     */
    public static DisplayName of(final Component name) {
        return new DisplayName(null, name, null);
    }

    /**
     * The three parts joined into one component, ready to render.
     *
     * <p>A single space separates the parts that are present, and absent ones produce no stray
     * spacing, so a name with no prefix does not start with one.</p>
     *
     * @return the joined name
     */
    public final Component getFullName() {
        return UtilAdventure.join(this.prefix, this.name, this.suffix);
    }
}