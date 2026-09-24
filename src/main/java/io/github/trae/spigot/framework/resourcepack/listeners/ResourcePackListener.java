package io.github.trae.spigot.framework.resourcepack.listeners;

import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.resourcepack.ResourcePack;
import io.github.trae.spigot.framework.resourcepack.ResourcePackManager;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent.Status;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Sends the configured resource packs on join, swaps them on world change, and enforces required
 * packs, delegating all pack work to {@link ResourcePackManager}.
 */
@AllArgsConstructor
@Singleton
public final class ResourcePackListener implements Listener {

    /**
     * The statuses that mean a pack will not be active on the client.
     */
    private static final Set<Status> FAILED_STATUS_SET = EnumSet.of(Status.DECLINED, Status.FAILED_DOWNLOAD, Status.INVALID_URL, Status.FAILED_RELOAD);

    /**
     * The manager every pack send and lookup is delegated to.
     */
    private final ResourcePackManager resourcePackManager;

    /**
     * Sends every eligible pack to a player when they join.
     *
     * @param event the player join event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        this.resourcePackManager.apply(event.getPlayer());
    }

    /**
     * Removes packs the player was eligible for in the world they left but not the one they entered,
     * and sends packs that only the new world makes them eligible for. Packs eligible in both are
     * left loaded.
     *
     * @param event the player changed world event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(final PlayerChangedWorldEvent event) {
        final Player player = event.getPlayer();

        final List<ResourcePack> fromList = this.resourcePackManager.getEligible(player, event.getFrom());
        final List<ResourcePack> toList = this.resourcePackManager.getEligible(player, player.getWorld());

        this.resourcePackManager.remove(player, fromList.stream().filter(resourcePack -> !(toList.contains(resourcePack))).toList());
        this.resourcePackManager.apply(player, toList.stream().filter(resourcePack -> !(fromList.contains(resourcePack))).toList());
    }

    /**
     * Kicks a player whose required pack was declined or failed to load. Statuses for packs not in
     * the config, or not required, are ignored.
     *
     * @param event the resource pack status event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onResourcePackStatus(final PlayerResourcePackStatusEvent event) {
        if (!(FAILED_STATUS_SET.contains(event.getStatus())) || !(this.resourcePackManager.getResourcePack(event.getID()).map(ResourcePack::isRequired).orElse(false))) {
            return;
        }

        event.getPlayer().kick(MiniMessage.miniMessage().deserialize(this.resourcePackManager.getResourcePackConfig().getKickMessage()));
    }
}