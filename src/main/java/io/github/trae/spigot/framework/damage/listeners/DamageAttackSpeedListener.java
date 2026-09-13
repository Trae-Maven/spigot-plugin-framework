package io.github.trae.spigot.framework.damage.listeners;

import io.github.trae.di.annotations.type.component.Singleton;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Removes the attack cooldown introduced in 1.9.
 *
 * <p>Raising attack speed far above vanilla means the swing meter refills within a tick, so every
 * hit lands at full strength with no glancing blows. That is the pre-1.9 combat feel, and it is also
 * why the pipeline never checks attack charge.</p>
 *
 * <p>Attributes survive world changes but are reset on respawn, which is why both join and respawn
 * are handled and world change is not.</p>
 */
@Singleton
public class DamageAttackSpeedListener implements Listener {

    private static final double ATTACK_SPEED = 1024.0D;

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
     * Sets the player's attack speed attribute.
     *
     * @param player the player to apply to
     */
    private void apply(final Player player) {
        final AttributeInstance attributeInstance = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attributeInstance == null) {
            return;
        }

        attributeInstance.setBaseValue(ATTACK_SPEED);
    }
}