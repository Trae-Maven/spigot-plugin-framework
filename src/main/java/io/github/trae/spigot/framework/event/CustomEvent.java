package io.github.trae.spigot.framework.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for all custom events within the framework.
 *
 * <p>Provides a shared {@link HandlerList} and supports both synchronous and
 * asynchronous dispatch. Subclasses should extend this for non-cancellable events,
 * or extend {@link CustomCancellableEvent} for events that can be cancelled.</p>
 */
public class CustomEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    /**
     * Creates a synchronous custom event.
     */
    public CustomEvent() {
    }

    /**
     * Creates a custom event with the specified async mode.
     *
     * @param isAsync true if this event should be fired asynchronously
     */
    public CustomEvent(final boolean isAsync) {
        super(isAsync);
    }

    /**
     * Returns the static handler list for all custom events.
     *
     * @return the handler list
     */
    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}