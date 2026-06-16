package io.github.trae.spigot.framework.event;

/**
 * Base class for custom cancellable events that are fired asynchronously (off the main server thread).
 *
 * <p>Extends {@link CustomCancellableEvent}, passing {@code true} to mark the event as asynchronous.
 * Combines cancellation support with asynchronous dispatch: listeners may cancel the event, and
 * must not touch thread-unsafe Bukkit API directly, scheduling any main-thread work accordingly.</p>
 */
public class CustomAsynchronousCancellableEvent extends CustomCancellableEvent {

    /**
     * Constructs an asynchronous cancellable custom event.
     */
    public CustomAsynchronousCancellableEvent() {
        super(true);
    }
}