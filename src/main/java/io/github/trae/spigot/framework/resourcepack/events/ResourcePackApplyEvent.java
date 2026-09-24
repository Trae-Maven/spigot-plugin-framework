package io.github.trae.spigot.framework.resourcepack.events;

import io.github.trae.spigot.framework.event.CustomCancellableEvent;
import io.github.trae.spigot.framework.resourcepack.ResourcePack;
import io.github.trae.spigot.framework.resourcepack.ResourcePackManager;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.Player;

/**
 * Fired before a configured resource pack is sent to a player.
 * <p>
 * Dispatched once per pack via {@link io.github.trae.spigot.framework.utility.UtilEvent} by
 * {@link ResourcePackManager} once the pack's world and permission checks have passed. Cancelling
 * it prevents only that pack from being sent.
 * <p>
 * Fired synchronously, as it is dispatched from the join and world change handlers on the main thread.
 */
@AllArgsConstructor
@Getter
public final class ResourcePackApplyEvent extends CustomCancellableEvent {

    /**
     * The player the pack is about to be sent to.
     */
    private final Player player;

    /**
     * The pack about to be sent.
     */
    private final ResourcePack resourcePack;
}