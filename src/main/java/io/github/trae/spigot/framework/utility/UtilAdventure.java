package io.github.trae.spigot.framework.utility;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.JoinConfiguration;

import java.util.function.Predicate;

/**
 * Utility helpers for working with Adventure {@link Component}s.
 */
@UtilityClass
public class UtilAdventure {

    /**
     * Joins the given components into a single {@link Component}, optionally inserting a separator
     * between them and filtering which components are included.
     * <p>
     * A {@code null} separator means no separator is inserted; a {@code null} predicate means every
     * component is included. Filtered-out components produce no surrounding separator.
     *
     * @param separator  the separator to place between components, or {@code null} for none
     * @param predicate  the filter deciding which components to include, or {@code null} to include all
     * @param components the components to join
     * @return the joined component
     */
    public static Component join(final ComponentLike separator, final Predicate<ComponentLike> predicate, final Component... components) {
        final JoinConfiguration.Builder builder = JoinConfiguration.builder();

        if (separator != null) {
            builder.separator(separator);
        }

        if (predicate != null) {
            builder.predicate(predicate);
        }

        return Component.join(builder.build(), components);
    }

    /**
     * Joins the given components with a single space between them, skipping empty components so no
     * stray spacing is produced.
     *
     * @param components the components to join
     * @return the joined component
     */
    public static Component join(final Component... components) {
        return join(Component.space(), componentLike -> componentLike != Component.empty(), components);
    }
}