package io.github.trae.spigot.framework.utility;

import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;

import java.util.concurrent.CompletableFuture;

/**
 * Utility class for dispatching Bukkit events with thread-safety enforcement.
 *
 * <p>Provides synchronous and asynchronous dispatch methods with strict validation
 * that synchronous events are only fired from the main thread context, and asynchronous
 * events are only fired from off-thread contexts. Each method has a fire-and-forget
 * variant and a supply variant that returns the event instance for inspection after
 * all handlers have been invoked.</p>
 *
 * <p>Asynchronous dispatch uses Bukkit's scheduler to ensure execution occurs on
 * a Bukkit-managed async thread, maintaining compatibility across Bukkit, Spigot,
 * and Paper implementations.</p>
 */
@UtilityClass
public class UtilEvent {

    /**
     * Dispatches the given event synchronously on the calling thread.
     *
     * @param event the event to fire
     * @param <T>   the event type
     * @throws IllegalArgumentException if the event is null
     * @throws IllegalStateException    if the event is asynchronous
     */
    public static <T extends Event> void dispatch(final T event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null.");
        }

        if (event.isAsynchronous()) {
            throw new IllegalStateException("Cannot dispatch asynchronous event synchronously.");
        }

        Bukkit.getServer().getPluginManager().callEvent(event);

    }

    /**
     * Dispatches the given event asynchronously on a Bukkit-managed thread.
     *
     * <p>The event is submitted to Bukkit's async scheduler and fired on a
     * separate thread. This method returns immediately; use
     * {@link #supplyAsynchronous(Event)} if the result is needed.</p>
     *
     * @param event the event to fire
     * @param <T>   the event type
     * @throws IllegalArgumentException if the event is null
     * @throws IllegalStateException    if the event is not asynchronous
     */
    public static <T extends Event> void dispatchAsynchronous(final T event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null.");
        }

        if (!(event.isAsynchronous())) {
            throw new IllegalStateException("Cannot dispatch synchronous event asynchronously.");
        }

        Bukkit.getScheduler().runTaskAsynchronously(UtilPlugin.getInstance(), () -> Bukkit.getServer().getPluginManager().callEvent(event));
    }

    /**
     * Dispatches the given event synchronously and returns the event instance.
     *
     * <p>Useful for cancellable events where the caller needs to inspect the
     * event state after all handlers have been invoked.</p>
     *
     * @param event the event to fire
     * @param <R>   the event type
     * @return the same event instance after all handlers have been invoked
     * @throws IllegalArgumentException if the event is null
     * @throws IllegalStateException    if the event is asynchronous
     */
    public static <R extends Event> R supply(final R event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null.");
        }

        if (event.isAsynchronous()) {
            throw new IllegalStateException("Cannot dispatch asynchronous event synchronously.");
        }

        dispatch(event);
        return event;
    }

    /**
     * Dispatches the given event asynchronously and returns a future containing the event instance.
     *
     * <p>The event is fired on a Bukkit-managed async thread. The returned
     * {@link CompletableFuture} completes with the event after all handlers
     * have been invoked, or completes exceptionally if a handler throws.</p>
     *
     * @param event the event to fire
     * @param <R>   the event type
     * @return a {@link CompletableFuture} that completes with the event after all handlers have been invoked
     * @throws IllegalArgumentException if the event is null
     * @throws IllegalStateException    if the event is not asynchronous
     */
    public static <R extends Event> CompletableFuture<R> supplyAsynchronous(final R event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null.");
        }

        if (!(event.isAsynchronous())) {
            throw new IllegalStateException("Cannot dispatch synchronous event asynchronously.");
        }

        final CompletableFuture<R> completableFuture = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(UtilPlugin.getInstance(), () -> {
            try {
                Bukkit.getServer().getPluginManager().callEvent(event);
                completableFuture.complete(event);
            } catch (final Exception e) {
                completableFuture.completeExceptionally(e);
            }
        });

        return completableFuture;
    }
}