package io.github.trae.spigot.framework.resourcepack;

import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.resourcepack.configs.ResourcePackConfig;
import io.github.trae.spigot.framework.resourcepack.events.ResourcePackApplyEvent;
import io.github.trae.spigot.framework.resourcepack.listeners.ResourcePackListener;
import io.github.trae.spigot.framework.utility.UtilEvent;
import io.github.trae.spigot.framework.utility.UtilPermission;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiPredicate;

/**
 * Sends and removes the configured resource packs using the Adventure multi-pack API.
 * <p>
 * Every pack is addressed by its own identifier with {@code replace} disabled, so applying or
 * removing one never disturbs any other the client has loaded. Eligibility follows the gate
 * pattern: each pack's world and permission checks via {@link #isEligible(Player, World, ResourcePack)}
 * first, then a cancellable {@link ResourcePackApplyEvent} per pack. Player join, world change and
 * pack status are handled by {@link ResourcePackListener}.
 */
@AllArgsConstructor
@Getter
@Singleton
public final class ResourcePackManager {

    @Getter
    @Setter
    private static BiPredicate<Player, String> permissionCheckPredicate = UtilPermission::hasPermission;

    /**
     * The configuration providing the packs, prompt and kick message.
     */
    private final ResourcePackConfig resourcePackConfig;

    /**
     * Sends every pack the player is eligible for in their current world.
     *
     * @param player the player to send the packs to
     */
    public void apply(final Player player) {
        this.apply(player, this.getEligible(player, player.getWorld()));
    }

    /**
     * Sends the given packs to the player in a single request, skipping any whose
     * {@link ResourcePackApplyEvent} is cancelled. The request is required if any sent pack is.
     *
     * @param player        the player to send the packs to
     * @param resourcePacks the packs to send
     */
    public void apply(final Player player, final List<ResourcePack> resourcePacks) {
        final List<ResourcePack> approvedList = resourcePacks.stream()
                .filter(resourcePack -> !UtilEvent.supply(new ResourcePackApplyEvent(player, resourcePack)).isCancelled())
                .toList();

        if (approvedList.isEmpty()) {
            return;
        }

        final String prompt = this.resourcePackConfig.getPrompt();

        player.sendResourcePacks(ResourcePackRequest.resourcePackRequest()
                .packs(approvedList.stream().map(ResourcePack::toResourcePackInfo).toList())
                .required(approvedList.stream().anyMatch(ResourcePack::isRequired))
                .prompt(prompt.isBlank() ? null : MiniMessage.miniMessage().deserialize(prompt))
                .replace(false)
                .build());
    }

    /**
     * Removes the given packs from the player, leaving any other loaded packs in place.
     *
     * @param player        the player to remove the packs from
     * @param resourcePacks the packs to remove
     */
    public void remove(final Player player, final List<ResourcePack> resourcePacks) {
        if (resourcePacks.isEmpty()) {
            return;
        }

        player.removeResourcePacks(resourcePacks.stream().map(ResourcePack::getUuid).toList());
    }

    /**
     * Returns the packs the player is eligible for in the given world, in stacking order, or an
     * empty list when resource packs are disabled.
     *
     * @param player the player to check
     * @param world  the world to check eligibility in
     * @return the eligible packs
     */
    public List<ResourcePack> getEligible(final Player player, final World world) {
        if (!(this.resourcePackConfig.isEnabled())) {
            return Collections.emptyList();
        }

        return this.resourcePackConfig.getResourcePacks().stream().filter(resourcePack -> this.isEligible(player, world, resourcePack)).toList();
    }

    /**
     * Returns whether the player is eligible for the pack in the given world: the pack must have a
     * URL, the world must be listed (or the list empty), and the player must hold the permission
     * (or none be set).
     *
     * @param player       the player to check
     * @param world        the world to check eligibility in
     * @param resourcePack the pack to check
     * @return {@code true} if the player should have the pack in that world
     */
    public boolean isEligible(final Player player, final World world, final ResourcePack resourcePack) {
        if (resourcePack.getUrl().isBlank()) {
            return false;
        }

        if (!resourcePack.getWorlds().isEmpty() && !resourcePack.getWorlds().contains(world.getName())) {
            return false;
        }

        if (!resourcePack.getPermission().isBlank() && !permissionCheckPredicate.test(player, resourcePack.getPermission())) {
            return false;
        }

        return true;
    }

    /**
     * Finds the configured pack with the given identifier.
     *
     * @param uuid the pack identifier
     * @return an {@link Optional} containing the pack, or empty if none matches
     */
    public Optional<ResourcePack> getResourcePack(final UUID uuid) {
        return this.resourcePackConfig.getResourcePacks().stream().filter(resourcePack -> resourcePack.getUuid().equals(uuid)).findFirst();
    }
}