package io.github.trae.spigot.framework.utility;

import io.github.trae.spigot.framework.SpigotPlugin;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;

/**
 * Utility class for executing tasks across different threading contexts in the Spigot environment.
 *
 * <p>Provides methods for immediate, synchronous (main server thread), asynchronous,
 * and scheduled execution. Time-based methods convert {@link ChronoUnit} durations
 * to Bukkit ticks (1 tick = 50ms). Convenience overloads delegate to
 * {@link UtilPlugin#getInstance()} for the plugin reference.</p>
 */
@UtilityClass
public class UtilTask {

    /**
     * Executes a {@link Runnable} immediately on the calling thread.
     *
     * @param runnable the task to execute
     * @throws IllegalArgumentException if {@code runnable} is {@code null}
     */
    public static void execute(final Runnable runnable) {
        if (runnable == null) {
            throw new IllegalArgumentException("Runnable cannot be null.");
        }

        runnable.run();
    }

    /**
     * Submits a {@link Runnable} to the main server thread via the Bukkit scheduler.
     *
     * @param spigotPlugin the plugin owning the task
     * @param runnable     the task to execute on the main thread
     * @throws IllegalArgumentException if {@code spigotPlugin} or {@code runnable} is {@code null}
     */
    public static void executeSynchronous(final SpigotPlugin spigotPlugin, final Runnable runnable) {
        if (spigotPlugin == null) {
            throw new IllegalArgumentException("Spigot Plugin cannot be null.");
        }

        if (runnable == null) {
            throw new IllegalArgumentException("Runnable cannot be null.");
        }

        Bukkit.getServer().getScheduler().runTask(spigotPlugin, runnable);
    }

    /**
     * Submits a {@link Runnable} to the main server thread using the default plugin instance.
     *
     * @param runnable the task to execute on the main thread
     * @throws IllegalArgumentException if {@code runnable} is {@code null}
     * @see #executeSynchronous(SpigotPlugin, Runnable)
     */
    public static void executeSynchronous(final Runnable runnable) {
        executeSynchronous(UtilPlugin.getInstance(), runnable);
    }

    /**
     * Executes a {@link Runnable} asynchronously via the Bukkit scheduler's async thread pool.
     *
     * <p>The task will run on a separate thread managed by Bukkit. Callers must
     * ensure no Bukkit API calls are made from within the runnable unless
     * explicitly documented as thread-safe.</p>
     *
     * @param spigotPlugin the plugin owning the task
     * @param runnable     the task to execute asynchronously
     * @throws IllegalArgumentException if {@code spigotPlugin} or {@code runnable} is {@code null}
     */
    public static void executeAsynchronous(final SpigotPlugin spigotPlugin, final Runnable runnable) {
        if (spigotPlugin == null) {
            throw new IllegalArgumentException("Spigot Plugin cannot be null.");
        }

        if (runnable == null) {
            throw new IllegalArgumentException("Runnable cannot be null.");
        }

        Bukkit.getServer().getScheduler().runTaskAsynchronously(spigotPlugin, runnable);
    }

    /**
     * Executes a {@link Runnable} asynchronously using the default plugin instance.
     *
     * @param runnable the task to execute asynchronously
     * @throws IllegalArgumentException if {@code runnable} is {@code null}
     * @see #executeAsynchronous(SpigotPlugin, Runnable)
     */
    public static void executeAsynchronous(final Runnable runnable) {
        executeAsynchronous(UtilPlugin.getInstance(), runnable);
    }

    /**
     * Schedules a {@link Runnable} to execute at a fixed rate on the main server thread
     * with an optional cancellation supplier.
     *
     * <p>The {@code initialDelay} and {@code period} are converted from the given
     * {@link ChronoUnit} to Bukkit ticks (1 tick = 50ms). If a {@code cancelSupplier}
     * is provided, it is checked before each invocation — if it returns {@code true},
     * the task is cancelled and the runnable will not execute.</p>
     *
     * @param spigotPlugin   the plugin owning the task
     * @param runnable       the task to execute
     * @param initialDelay   the time to delay first execution
     * @param period         the period between successive executions
     * @param chronoUnit     the time unit of the {@code initialDelay} and {@code period} parameters
     * @param cancelSupplier a supplier that returns {@code true} to cancel the scheduled task (may be {@code null})
     * @throws IllegalArgumentException if {@code spigotPlugin}, {@code runnable}, or {@code chronoUnit} is {@code null},
     *                                  or if {@code initialDelay} or {@code period} is negative
     */
    public static void schedule(final SpigotPlugin spigotPlugin, final Runnable runnable, final int initialDelay, final int period, final ChronoUnit chronoUnit, final Supplier<Boolean> cancelSupplier) {
        if (spigotPlugin == null) {
            throw new IllegalArgumentException("Spigot Plugin cannot be null.");
        }

        if (runnable == null) {
            throw new IllegalArgumentException("Runnable cannot be null.");
        }

        if (initialDelay < 0 || period < 0) {
            throw new IllegalArgumentException("Initial delay and Period must be >= 0.");
        }

        if (chronoUnit == null) {
            throw new IllegalArgumentException("Chrono Unit cannot be null.");
        }

        final long initialDelayTicks = Duration.of(initialDelay, chronoUnit).toMillis() / 50L;
        final long periodTicks = Duration.of(period, chronoUnit).toMillis() / 50L;

        Bukkit.getServer().getScheduler().runTaskTimer(spigotPlugin, bukkitTask -> {
            if (cancelSupplier != null && cancelSupplier.get()) {
                bukkitTask.cancel();
                return;
            }

            runnable.run();
        }, initialDelayTicks, periodTicks);
    }

    /**
     * Schedules a {@link Runnable} to execute at a fixed rate on the main server thread.
     *
     * @param spigotPlugin the plugin owning the task
     * @param runnable     the task to execute
     * @param initialDelay the time to delay first execution
     * @param period       the period between successive executions
     * @param chronoUnit   the time unit of the {@code initialDelay} and {@code period} parameters
     * @see #schedule(SpigotPlugin, Runnable, int, int, ChronoUnit, Supplier)
     */
    public static void schedule(final SpigotPlugin spigotPlugin, final Runnable runnable, final int initialDelay, final int period, final ChronoUnit chronoUnit) {
        schedule(spigotPlugin, runnable, initialDelay, period, chronoUnit, null);
    }

    /**
     * Schedules a {@link Runnable} to execute at a fixed rate on the main server thread
     * using the default plugin instance, with an optional cancellation supplier.
     *
     * @param runnable       the task to execute
     * @param initialDelay   the time to delay first execution
     * @param period         the period between successive executions
     * @param chronoUnit     the time unit of the {@code initialDelay} and {@code period} parameters
     * @param cancelSupplier a supplier that returns {@code true} to cancel the scheduled task (may be {@code null})
     * @see #schedule(SpigotPlugin, Runnable, int, int, ChronoUnit, Supplier)
     */
    public static void schedule(final Runnable runnable, final int initialDelay, final int period, final ChronoUnit chronoUnit, final Supplier<Boolean> cancelSupplier) {
        schedule(UtilPlugin.getInstance(), runnable, initialDelay, period, chronoUnit, cancelSupplier);
    }

    /**
     * Schedules a {@link Runnable} to execute at a fixed rate on the main server thread
     * using the default plugin instance.
     *
     * @param runnable     the task to execute
     * @param initialDelay the time to delay first execution
     * @param period       the period between successive executions
     * @param chronoUnit   the time unit of the {@code initialDelay} and {@code period} parameters
     * @see #schedule(SpigotPlugin, Runnable, int, int, ChronoUnit, Supplier)
     */
    public static void schedule(final Runnable runnable, final int initialDelay, final int period, final ChronoUnit chronoUnit) {
        schedule(UtilPlugin.getInstance(), runnable, initialDelay, period, chronoUnit, null);
    }

    /**
     * Schedules a {@link Runnable} to execute at a fixed rate on an async thread
     * via the Bukkit scheduler, with an optional cancellation supplier.
     *
     * <p>The {@code initialDelay} and {@code period} are converted from the given
     * {@link ChronoUnit} to Bukkit ticks (1 tick = 50ms). The task runs on a Bukkit
     * async thread, preventing long-running work from blocking the main server thread.
     * If a {@code cancelSupplier} is provided, it is checked before each invocation —
     * if it returns {@code true}, the task is cancelled and the runnable will not execute.</p>
     *
     * <p>Callers must ensure no Bukkit API calls are made from within the runnable
     * unless explicitly documented as thread-safe.</p>
     *
     * @param spigotPlugin   the plugin owning the task
     * @param runnable       the task to execute asynchronously on each tick
     * @param initialDelay   the time to delay first execution
     * @param period         the period between successive executions
     * @param chronoUnit     the time unit of the {@code initialDelay} and {@code period} parameters
     * @param cancelSupplier a supplier that returns {@code true} to cancel the scheduled task (may be {@code null})
     * @throws IllegalArgumentException if {@code spigotPlugin}, {@code runnable}, or {@code chronoUnit} is {@code null},
     *                                  or if {@code initialDelay} or {@code period} is negative
     */
    public static void scheduleAsynchronous(final SpigotPlugin spigotPlugin, final Runnable runnable, final int initialDelay, final int period, final ChronoUnit chronoUnit, final Supplier<Boolean> cancelSupplier) {
        if (spigotPlugin == null) {
            throw new IllegalArgumentException("Spigot Plugin cannot be null.");
        }

        if (runnable == null) {
            throw new IllegalArgumentException("Runnable cannot be null.");
        }

        if (initialDelay < 0 || period < 0) {
            throw new IllegalArgumentException("Initial delay and Period must be >= 0.");
        }

        if (chronoUnit == null) {
            throw new IllegalArgumentException("Chrono Unit cannot be null.");
        }

        final long initialDelayTicks = Duration.of(initialDelay, chronoUnit).toMillis() / 50L;
        final long periodTicks = Duration.of(period, chronoUnit).toMillis() / 50L;

        Bukkit.getServer().getScheduler().runTaskTimerAsynchronously(spigotPlugin, bukkitTask -> {
            if (cancelSupplier != null && cancelSupplier.get()) {
                bukkitTask.cancel();
                return;
            }

            runnable.run();
        }, initialDelayTicks, periodTicks);
    }

    /**
     * Schedules a {@link Runnable} to execute at a fixed rate on an async thread
     * via the Bukkit scheduler.
     *
     * @param spigotPlugin the plugin owning the task
     * @param runnable     the task to execute asynchronously on each tick
     * @param initialDelay the time to delay first execution
     * @param period       the period between successive executions
     * @param chronoUnit   the time unit of the {@code initialDelay} and {@code period} parameters
     * @see #scheduleAsynchronous(SpigotPlugin, Runnable, int, int, ChronoUnit, Supplier)
     */
    public static void scheduleAsynchronous(final SpigotPlugin spigotPlugin, final Runnable runnable, final int initialDelay, final int period, final ChronoUnit chronoUnit) {
        scheduleAsynchronous(spigotPlugin, runnable, initialDelay, period, chronoUnit, null);
    }

    /**
     * Schedules a {@link Runnable} to execute at a fixed rate on an async thread
     * using the default plugin instance, with an optional cancellation supplier.
     *
     * @param runnable       the task to execute asynchronously on each tick
     * @param initialDelay   the time to delay first execution
     * @param period         the period between successive executions
     * @param chronoUnit     the time unit of the {@code initialDelay} and {@code period} parameters
     * @param cancelSupplier a supplier that returns {@code true} to cancel the scheduled task (may be {@code null})
     * @see #scheduleAsynchronous(SpigotPlugin, Runnable, int, int, ChronoUnit, Supplier)
     */
    public static void scheduleAsynchronous(final Runnable runnable, final int initialDelay, final int period, final ChronoUnit chronoUnit, final Supplier<Boolean> cancelSupplier) {
        scheduleAsynchronous(UtilPlugin.getInstance(), runnable, initialDelay, period, chronoUnit, cancelSupplier);
    }

    /**
     * Schedules a {@link Runnable} to execute at a fixed rate on an async thread
     * using the default plugin instance.
     *
     * @param runnable     the task to execute asynchronously on each tick
     * @param initialDelay the time to delay first execution
     * @param period       the period between successive executions
     * @param chronoUnit   the time unit of the {@code initialDelay} and {@code period} parameters
     * @see #scheduleAsynchronous(SpigotPlugin, Runnable, int, int, ChronoUnit, Supplier)
     */
    public static void scheduleAsynchronous(final Runnable runnable, final int initialDelay, final int period, final ChronoUnit chronoUnit) {
        scheduleAsynchronous(UtilPlugin.getInstance(), runnable, initialDelay, period, chronoUnit, null);
    }
}