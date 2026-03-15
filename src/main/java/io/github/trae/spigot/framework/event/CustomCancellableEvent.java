package io.github.trae.spigot.framework.event;

import io.github.trae.spigot.framework.event.interfaces.ICustomCancellableEvent;
import lombok.Getter;
import lombok.Setter;

/**
 * Base class for custom events that support cancellation.
 *
 * <p>Extends {@link CustomEvent} with cancellation state and an optional reason.
 * Events extending this class can be cancelled by listeners via {@link #setCancelled(boolean)}
 * or {@link #setCancelledWithReason(String)}, and checked by the dispatcher using
 * {@link #isCancelled()} to determine whether to proceed.</p>
 */
@Getter
public class CustomCancellableEvent extends CustomEvent implements ICustomCancellableEvent {

    @Setter
    private boolean cancelled;

    private String cancelledReason;

    /**
     * Cancels this event and sets the reason for cancellation.
     *
     * <p>Convenience method equivalent to calling {@link #setCancelled(boolean)}
     * with {@code true} followed by setting the cancellation reason.</p>
     *
     * @param cancelledReason the human-readable reason for cancellation
     */
    @Override
    public void setCancelledWithReason(final String cancelledReason) {
        this.cancelledReason = cancelledReason;
        this.setCancelled(true);
    }
}