package io.github.trae.spigot.framework.utility;

import io.papermc.paper.adventure.PaperAdventure;
import lombok.experimental.UtilityClass;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

/**
 * Utility class for NMS (net.minecraft.server) operations.
 *
 * <p>Provides conversion between Adventure and vanilla Minecraft components,
 * and direct packet sending via the Netty pipeline.</p>
 */
@UtilityClass
public class UtilNms {

    /**
     * Converts an Adventure {@link net.kyori.adventure.text.Component} to a
     * vanilla Minecraft {@link Component} using Paper's internal adapter.
     *
     * @param component the Adventure component to convert
     * @return the equivalent vanilla Minecraft component
     */
    public static Component toNms(final net.kyori.adventure.text.Component component) {
        return PaperAdventure.asVanilla(component);
    }

    /**
     * Sends a raw NMS packet to a player.
     *
     * <p>Safe to call from any thread — packets are written directly
     * to the Netty channel pipeline, bypassing the main thread.</p>
     *
     * @param player the Bukkit player to send the packet to
     * @param packet the NMS packet to send
     */
    public static void sendPacket(final Player player, final Packet<?> packet) {
        if (player instanceof final CraftPlayer craftPlayer && craftPlayer.isOnline()) {
            craftPlayer.getHandle().connection.send(packet);
        }
    }
}