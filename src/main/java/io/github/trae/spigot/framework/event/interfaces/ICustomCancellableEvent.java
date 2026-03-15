package io.github.trae.spigot.framework.event.interfaces;

import org.bukkit.event.Cancellable;

public interface ICustomCancellableEvent extends Cancellable {

    void setCancelledWithReason(final String cancelledReason);
}