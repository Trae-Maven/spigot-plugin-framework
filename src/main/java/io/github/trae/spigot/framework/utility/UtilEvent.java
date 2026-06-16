package io.github.trae.spigot.framework.utility;

import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;

import java.util.concurrent.CompletableFuture;

/**
 * Utility class for dispatching Bukkit events.
 *
 * <p>Provides several dispatch strategies:</p>
 * <ul>
 *   <li><b>Current-thread</b> — {@link #dispatch(Event)} and {@link #supply(Event)} fire the event
 *       immediately on the calling thread, with no thread-context validation.</li>
 *   <li><b>Synchronous</b> — {@link #dispatchSynchronously(Event)} and {@link #supplySynchronous(Event)}
 *       marshal the event onto the main server thread, rejecting asynchronous events.</li>
 *   <li><b>Asynchronous</b> — {@link #dispatchAsynchronous(Event)} and {@link #supplyAsynchronous(Event)}
 *       fire the event on a Bukkit-managed async thread, rejecting synchronous events.</li>
 * </ul>
 *
 * <p>Each strategy has a fire-and-forget variant and a supply variant that returns the event
 * (or a {@link CompletableFuture} of it) for inspection after all handlers have run.</p>
 */
@UtilityClass
public class UtilEvent {

    /**
     * Fires the given event immediately on the calling thread, without any thread-context validation.
     *
     * @param event the event to fire
     * @param <T>   the event type
     * @throws IllegalArgumentException if the event is null
     */
    public static <T extends Event> void dispatch(final T event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null.");
        }

        Bukkit.getServer().getPluginManager().callEvent(event);
    }

    /**
     * Fires the given event on the main server thread, marshalling onto it if the caller is off-thread.
     *
     * @param event the event to fire
     * @param <T>   the event type
     * @throws IllegalArgumentException if the event is null
     * @throws IllegalStateException    if the event is asynchronous
     */
    public static <T extends Event> void dispatchSynchronously(final T event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null.");
        }

        if (event.isAsynchronous()) {
            throw new IllegalStateException("Cannot dispatch asynchronous event synchronously.");
        }

        UtilTask.executeSynchronous(() -> Bukkit.getServer().getPluginManager().callEvent(event));
    }

    /**
     * Fires the given event on a Bukkit-managed async thread.
     *
     * <p>This method returns immediately; use {@link #supplyAsynchronous(Event)} if the result
     * is needed.</p>
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
     * Fires the given event immediately on the calling thread and returns it.
     *
     * <p>Useful for cancellable events where the caller needs to inspect the event state after all
     * handlers have run. No thread-context validation is performed.</p>
     *
     * @param event the event to fire
     * @param <R>   the event type
     * @return the same event instance after all handlers have been invoked
     * @throws IllegalArgumentException if the event is null
     */
    public static <R extends Event> R supply(final R event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null.");
        }

        dispatch(event);

        return event;
    }

    /**
     * Fires the given event on the main server thread and returns a future containing it.
     *
     * <p>The returned {@link CompletableFuture} completes with the event after all handlers have
     * run, or completes exceptionally if a handler throws.</p>
     *
     * @param event the event to fire
     * @param <R>   the event type
     * @return a {@link CompletableFuture} that completes with the event after all handlers have run
     * @throws IllegalArgumentException if the event is null
     * @throws IllegalStateException    if the event is asynchronous
     */
    public static <R extends Event> CompletableFuture<R> supplySynchronous(final R event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null.");
        }

        if (event.isAsynchronous()) {
            throw new IllegalStateException("Cannot supply asynchronous event synchronously.");
        }

        final CompletableFuture<R> completableFuture = new CompletableFuture<>();

        UtilTask.executeSynchronous(() -> {
            try {
                Bukkit.getServer().getPluginManager().callEvent(event);
                completableFuture.complete(event);
            } catch (final Exception e) {
                completableFuture.completeExceptionally(e);
            }
        });

        return completableFuture;
    }

    /**
     * Fires the given event on a Bukkit-managed async thread and returns a future containing it.
     *
     * <p>The returned {@link CompletableFuture} completes with the event after all handlers have
     * run, or completes exceptionally if a handler throws.</p>
     *
     * @param event the event to fire
     * @param <R>   the event type
     * @return a {@link CompletableFuture} that completes with the event after all handlers have run
     * @throws IllegalArgumentException if the event is null
     * @throws IllegalStateException    if the event is not asynchronous
     */
    public static <R extends Event> CompletableFuture<R> supplyAsynchronous(final R event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null.");
        }

        if (!(event.isAsynchronous())) {
            throw new IllegalStateException("Cannot supply synchronous event asynchronously.");
        }

        final CompletableFuture<R> completableFuture = new CompletableFuture<>();

        UtilTask.executeAsynchronous(() -> {
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