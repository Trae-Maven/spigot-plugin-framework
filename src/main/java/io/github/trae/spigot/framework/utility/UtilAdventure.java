package io.github.trae.spigot.framework.utility;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.JoinConfiguration;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Utility helpers for joining Adventure {@link Component}s.
 *
 * <p>Wraps the {@link Component#join} and {@link JoinConfiguration} pairing in shorter helpers, so
 * callers can supply a separator and an inclusion filter without building a configuration by
 * hand.</p>
 */
@UtilityClass
public class UtilAdventure {

    /**
     * Joins the given components into a single {@link Component}, optionally inserting a separator
     * between them and filtering which components are included.
     * <p>
     * A {@code null} separator means no separator is inserted; a {@code null} predicate means every
     * component is included. Filtered-out components produce no surrounding separator.
     * <p>
     * Null components are dropped before the join rather than left to the predicate, since
     * {@link Component#join} rejects a null element in the array before any predicate is consulted.
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

        return Component.join(builder.build(), Arrays.stream(components).filter(Objects::nonNull).toArray(Component[]::new));
    }

    /**
     * Joins the given components with a single space between them, skipping empty components so no
     * stray spacing is produced. Nulls are dropped by the underlying join.
     * <p>
     * Named separately from {@link #join(ComponentLike, Predicate, Component...)} because a varargs
     * overload of the same name would be ambiguous at every call site that passes a separator.
     * <p>
     * Emptiness is tested by identity against {@link Component#empty()}, so only that shared instance
     * is skipped, not any other component that happens to render as nothing.
     *
     * @param components the components to join
     * @return the joined component
     */
    public static Component joinWithSpace(final Component... components) {
        return join(Component.space(), componentLike -> componentLike != Component.empty(), components);
    }

    /**
     * Joins the given components back to back, with nothing between them.
     * <p>
     * Any spacing belongs in the components themselves. Null components are dropped, so an absent
     * part leaves no trace in the result.
     *
     * @param components the components to join
     * @return the joined component
     */
    public static Component joinWithEmpty(final Component... components) {
        return join(Component.empty(), null, components);
    }
}