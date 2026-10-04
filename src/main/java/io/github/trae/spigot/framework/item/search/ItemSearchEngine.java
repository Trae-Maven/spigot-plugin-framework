package io.github.trae.spigot.framework.item.search;

import io.github.trae.spigot.framework.item.CustomItem;
import io.github.trae.spigot.framework.utility.search.SpigotSearchEngine;
import org.bukkit.command.CommandSender;

import java.awt.Color;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Resolves a {@link CustomItem} from a search term, for commands and anywhere else a person names an
 * item rather than the framework identifying one.
 * <p>
 * Matching is on the namespace rather than the identifier, since the identifier is an opaque value
 * nobody would type. Display names are not matched either: they carry formatting and change with the
 * item's styling, where the namespace is stable and typeable.
 * <p>
 * Results are presented by display name in the item's own colour, falling back to the input colour
 * when the item has none, so a player reading them sees the item as it appears in game even though
 * they searched by namespace.
 */
public final class ItemSearchEngine extends SpigotSearchEngine<CustomItem> {

    /**
     * Creates a search engine over the items the given supplier yields.
     * <p>
     * A supplier rather than a collection because the registry it draws from populates lazily, long
     * after this is constructed. A snapshot taken at construction would be permanently empty;
     * resolving on each search means the engine always sees the current set.
     *
     * @param customItemSupplier supplies the items to search across, resolved on each search
     */
    public ItemSearchEngine(final Supplier<Collection<? extends CustomItem>> customItemSupplier) {
        super("Item Search", customItemSupplier);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the item's display name, so results read as the item appears in game even though
     * matching runs on the namespace.</p>
     */
    @Override
    protected String getTypeName(final CustomItem customItem, final CommandSender commandSender) {
        return customItem.getName();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the item's own colour, or the input colour when the item has none.</p>
     */
    @Override
    protected Color getTypeColor(final CustomItem customItem, final CommandSender commandSender) {
        return Objects.requireNonNullElse(customItem.getColor(), this.getInputColor());
    }

    /**
     * {@inheritDoc}
     *
     * <p>An exact match is a namespace equal to the term, ignoring case.</p>
     */
    @Override
    protected boolean isExact(final CustomItem customItem, final String result) {
        return customItem.getNamespace().equalsIgnoreCase(result);
    }

    /**
     * {@inheritDoc}
     *
     * <p>A partial match is a namespace containing the term, ignoring case.</p>
     */
    @Override
    protected boolean isMatching(final CustomItem customItem, final String result) {
        return customItem.getNamespace().toLowerCase(Locale.ROOT).contains(result.toLowerCase(Locale.ROOT));
    }
}