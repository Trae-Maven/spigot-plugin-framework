package io.github.trae.spigot.framework.utility;

import lombok.experimental.UtilityClass;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

/**
 * Damage helpers shared across the damage subsystem.
 *
 * @see io.github.trae.spigot.framework.damage.DamageManager
 */
@UtilityClass
public class UtilDamage {

    /**
     * Whether an attack from the given entity lands as a critical hit.
     *
     * <p>Mirrors vanilla's conditions with two deliberate differences. The attack-charge check is
     * omitted, since the framework raises attack speed high enough that every swing is fully
     * charged. Ground state is inferred from fall distance rather than
     * {@link Player#isOnGround()}, which is reported by the client and therefore spoofable; the
     * only case this diverges on is a player landing within the same tick as their swing, which
     * vanilla refuses and this allows.</p>
     *
     * <p>Only players crit in vanilla, so anything else returns {@code false} regardless of its
     * state.</p>
     *
     * @param damager the attacking entity
     * @return whether the attack is critical
     */
    public static boolean isCritical(final Entity damager) {
        if (!(damager instanceof final Player player)) {
            return false;
        }

        if (player.getFallDistance() <= 0.0F) {
            return false;
        }

        if (player.isInWater() || player.isInsideVehicle() || player.isSprinting() || player.isClimbing()) {
            return false;
        }

        if (player.hasPotionEffect(PotionEffectType.BLINDNESS)) {
            return false;
        }

        return true;
    }
}