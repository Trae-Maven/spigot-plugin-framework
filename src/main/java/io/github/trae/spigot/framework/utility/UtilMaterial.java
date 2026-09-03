package io.github.trae.spigot.framework.utility;

import io.github.trae.spigot.framework.utility.search.types.MaterialSearchEngine;
import lombok.experimental.UtilityClass;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Utility helpers for resolving {@link Material} values from user-supplied input.
 *
 * <p>Wraps a shared {@link MaterialSearchEngine} so commands can turn a partial or full material
 * name into a {@link Material} without handling the match and feedback logic themselves.</p>
 */
@UtilityClass
public class UtilMaterial {

    /**
     * Search engine backing the {@code searchMaterial} helpers.
     */
    private static final MaterialSearchEngine MATERIAL_SEARCH_ENGINE = new MaterialSearchEngine();

    /**
     * Searches the material registry for the material identified by the given input.
     *
     * <p>An exact name match wins immediately, otherwise a single partial match is returned. An empty
     * or ambiguous search yields an empty result and, when informing is enabled, messages the sender
     * with the outcome.</p>
     *
     * @param sender    the sender to inform of the search outcome
     * @param input     the search input
     * @param inform    whether to message the sender when the search fails to resolve
     * @param predicate an optional filter applied before matching, or null to consider every material
     * @return the resolved material, or {@link Optional#empty()} if the search was empty or ambiguous
     */
    public static Optional<Material> searchMaterial(final CommandSender sender, final String input, final boolean inform, final Predicate<Material> predicate) {
        return MATERIAL_SEARCH_ENGINE.find(
                sender,
                input,
                inform,
                predicate
        );
    }

    /**
     * Searches the material registry for the material identified by the given input, without
     * filtering.
     *
     * @param sender the sender to inform of the search outcome
     * @param input  the search input
     * @param inform whether to message the sender when the search fails to resolve
     * @return the resolved material, or {@link Optional#empty()} if the search was empty or ambiguous
     * @see #searchMaterial(CommandSender, String, boolean, Predicate)
     */
    public static Optional<Material> searchMaterial(final CommandSender sender, final String input, final boolean inform) {
        return searchMaterial(sender, input, inform, null);
    }
}