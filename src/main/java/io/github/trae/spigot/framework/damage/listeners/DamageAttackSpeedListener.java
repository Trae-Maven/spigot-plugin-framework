package io.github.trae.spigot.framework.damage.listeners;

import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.config.events.ConfigReloadEvent;
import io.github.trae.spigot.framework.damage.DamageManager;
import io.github.trae.spigot.framework.damage.configs.DamageConfig;
import io.github.trae.spigot.framework.utility.UtilServer;
import lombok.AllArgsConstructor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Sets the attack cooldown to match the configured combat rules.
 *
 * <p>With old combat enabled, attack speed is raised far above vanilla, so the swing meter refills
 * within a tick and every hit lands at full strength with no glancing blows. With it disabled, the
 * attribute is restored to its default, so the 1.9 cooldown applies again.</p>
 *
 * <p>The base value is saved with the player, which is why it is restored explicitly rather than
 * left alone. Attributes survive world changes but are reset on respawn, which is why both join and
 * respawn are handled and world change is not.</p>
 */
@AllArgsConstructor
@Singleton
public class DamageAttackSpeedListener implements Listener {

    private static final double ATTACK_SPEED = 1024.0D;

    private final DamageManager damageManager;

    /**
     * Applies the attack speed to a joining player.
     *
     * @param event the join event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public final void onPlayerJoin(final PlayerJoinEvent event) {
        this.apply(event.getPlayer());
    }

    /**
     * Re-applies the attack speed after a respawn resets it.
     *
     * @param event the respawn event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public final void onPlayerRespawn(final PlayerRespawnEvent event) {
        this.apply(event.getPlayer());
    }

    /**
     * Re-applies the attack speed to every online player when the damage config is reloaded, so a
     * change to the combat rules takes effect immediately.
     *
     * @param event the config reload event
     */
    @EventHandler
    public final void onConfigReload(final ConfigReloadEvent event) {
        if (!event.getConfigurationClass().equals(DamageConfig.class)) {
            return;
        }

        UtilServer.getOnlinePlayers().forEach(this::apply);
    }

    /**
     * Sets the player's attack speed attribute for the configured combat rules.
     *
     * @param player the player to apply to
     */
    private void apply(final Player player) {
        final AttributeInstance attributeInstance = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attributeInstance == null) {
            return;
        }

        attributeInstance.setBaseValue(this.damageManager.getDamageConfig().isOldCombatEnabled() ? ATTACK_SPEED : attributeInstance.getDefaultValue());
    }
}