package io.github.trae.spigot.framework.utility.search;

import io.github.trae.spigot.framework.utility.UtilColor;
import io.github.trae.spigot.framework.utility.UtilMessage;
import io.github.trae.spigot.framework.utility.enums.ChatColor;
import io.github.trae.utilities.search.AbstractSearchEngine;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

import java.awt.Color;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * Spigot-flavoured {@link AbstractSearchEngine} that reports to a {@link CommandSender}.
 *
 * <p>Replaces the logger-backed default messaging with {@link UtilMessage}, so search feedback lands
 * in the sender's chat, and supplies the colour conventions used across the framework: the input and
 * result count are highlighted in yellow by default, and matches are separated by a grey comma.</p>
 *
 * <p>Every untrusted value, meaning the search input and each candidate's raw name, has its
 * MiniMessage tags escaped before colouring, so player-controlled text always renders literally.</p>
 *
 * <p>Subclasses supply the matching rules, each candidate's raw name through
 * {@link AbstractSearchEngine#getTypeName(Object, Object)} and its colour through
 * {@link #getTypeColor(Object, CommandSender)}.</p>
 *
 * @param <Type> the type being searched for
 */
public abstract class SpigotSearchEngine<Type> extends AbstractSearchEngine<Type, CommandSender> {

    /**
     * Creates a search engine over the given candidate supplier.
     *
     * @param name               the prefix applied to informational messages, may be null or empty
     * @param collectionSupplier supplies the candidates to search, evaluated on every search
     */
    public SpigotSearchEngine(final String name, final Supplier<Collection<? extends Type>> collectionSupplier) {
        super(name, collectionSupplier);
    }

    /**
     * Sends the message to the command sender through {@link UtilMessage}, prefixed with the engine
     * name.
     *
     * @param commandSender the sender to message
     * @param prefix        the engine name, may be null or empty
     * @param message       the message body
     */
    @Override
    protected final void message(final CommandSender commandSender, final String prefix, final String message) {
        UtilMessage.message(commandSender, prefix, message);
    }

    /**
     * Escapes the value's MiniMessage tags and serializes it in the colour from
     * {@link #getInputColor()}.
     *
     * @param string the value to format, such as the search input or result count
     * @return the escaped, coloured value
     */
    @Override
    protected final String getInputFormat(final String string) {
        return UtilColor.serialize(this.getInputColor(), this.escapeString(string));
    }

    /**
     * Escapes the candidate's raw name and serializes it in the colour from
     * {@link #getTypeColor(Object, CommandSender)}.
     *
     * @param type          the candidate to format
     * @param name          the candidate's raw display name
     * @param commandSender the sender the message is being built for
     * @return the escaped, coloured name
     */
    @Override
    protected final String getTypeFormat(final Type type, final String name, final CommandSender commandSender) {
        return UtilColor.serialize(this.getTypeColor(type, commandSender), this.escapeString(name));
    }

    /**
     * Separator placed between formatted matches in an ambiguous-result message.
     *
     * @return a grey comma separator
     */
    @Override
    protected final String getMatchSeparator() {
        return UtilColor.serialize(ChatColor.GRAY.getColor(), ", ");
    }

    /**
     * Supplies the colour the search input and result count are displayed in.
     *
     * @return the input colour, yellow by default
     */
    protected Color getInputColor() {
        return ChatColor.YELLOW.getColor();
    }

    /**
     * Supplies the colour a candidate's name is displayed in within an ambiguous-result message.
     *
     * @param type          the candidate to colour
     * @param commandSender the sender the message is being built for, allowing per-sender colouring
     * @return the colour of the candidate's name
     */
    protected abstract Color getTypeColor(final Type type, final CommandSender commandSender);

    /**
     * Escapes MiniMessage tags in an untrusted value so it renders literally.
     *
     * @param string the value to escape
     * @return the escaped value
     */
    private String escapeString(final String string) {
        return MiniMessage.miniMessage().escapeTags(string);
    }
}