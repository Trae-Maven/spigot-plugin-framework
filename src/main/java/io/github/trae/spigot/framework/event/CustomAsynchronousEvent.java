package io.github.trae.spigot.framework.event;

/**
 * Base class for custom events that are fired asynchronously (off the main server thread).
 *
 * <p>Extends {@link CustomEvent}, passing {@code true} to mark the event as asynchronous.
 * Listeners handling this event must not touch thread-unsafe Bukkit API directly and should
 * schedule any main-thread work accordingly.</p>
 */
public class CustomAsynchronousEvent extends CustomEvent {

    /**
     * Constructs an asynchronous custom event.
     */
    public CustomAsynchronousEvent() {
        super(true);
    }
}