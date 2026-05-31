package io.github.trae.spigot.framework.utility;

import io.github.trae.utilities.UtilCollection;
import lombok.experimental.UtilityClass;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Minecraft-specific search utility that delegates to {@link UtilCollection#search}
 * with message output routed through {@link UtilMessage}.
 */
@UtilityClass
public class UtilSearch {

    /**
     * Search a collection for a matching element, sending any result or
     * ambiguity messages to the given {@link CommandSender}.
     *
     * @param collection        the collection to search
     * @param equalsPredicate   predicate for exact matching, or {@code null} to skip
     * @param containsPredicate predicate for partial matching, or {@code null} to skip
     * @param listConsumer      receives the list of partial matches, or {@code null} to skip
     * @param colorFunction     applies color formatting to highlighted segments of the result message
     * @param resultFunction    maps each matched element to its display name for the result message
     * @param prefix            the message prefix passed to {@link UtilMessage#message}
     * @param commandSender     the receiver to send result messages to
     * @param input             the raw input string shown in the "no matches" message
     * @param inform            whether to send a result message to the receiver
     * @param <Type>            the element type
     * @return the matched element, or {@link Optional#empty()} if zero or multiple matches were found
     */
    public static <Type> Optional<Type> search(final Collection<? extends Type> collection, final Predicate<Type> typePredicate, final Predicate<Type> equalsPredicate, final Predicate<Type> containsPredicate, final Consumer<List<Type>> listConsumer, final Function<String, String> colorFunction, final Function<Type, String> resultFunction, final String prefix, final CommandSender commandSender, final String input, final boolean inform) {
        final Consumer<String> messageConsumer = message -> UtilMessage.message(commandSender, prefix, message);

        return UtilCollection.search(collection, typePredicate, equalsPredicate, containsPredicate, listConsumer, messageConsumer, colorFunction, resultFunction, input, inform);
    }
}