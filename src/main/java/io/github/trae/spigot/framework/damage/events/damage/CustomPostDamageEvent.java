package io.github.trae.spigot.framework.damage.events.damage;

import io.github.trae.spigot.framework.damage.events.damage.abstracts.AbstractCustomDamageEvent;
import io.github.trae.spigot.framework.damage.modifier.DamageModifier;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * The final stage of a damage pass, for reductions and side effects.
 *
 * <p>By the time this fires the base is settled, so reductions here act on whatever an ability
 * decided. Armour, protection and resistance run early in this stage; durability, knockback and the
 * damage delay run at the end of it.</p>
 *
 * <p>Cancelling still prevents the damage being applied, since the manager runs after this stage
 * completes.</p>
 *
 * @see CustomDamageEvent
 * @see io.github.trae.spigot.framework.damage.DamageManager
 */
public class CustomPostDamageEvent extends AbstractCustomDamageEvent {

    protected CustomPostDamageEvent(final long systemTime, final Map<DamageModifier, Double> additiveMap, final Map<DamageModifier, Double> multiplierMap, final Entity damagee, final Entity damager, final Projectile projectile, final DamageSource source, final EntityDamageEvent.DamageCause cause, final ItemStack itemStack, final ItemStack[] armourContents, final double originalDamage, final boolean critical, final double damage, final long delay, final String reason) {
        super(systemTime, additiveMap, multiplierMap, damagee, damager, projectile, source, cause, itemStack, armourContents, originalDamage, critical, damage, delay, reason);
    }

    /**
     * Carries the damage stage forward.
     *
     * <p>The modifier maps are passed by reference rather than copied, so everything contributed by
     * the earlier stages is still in place here.</p>
     *
     * @param event the damage stage that produced this one
     */
    public CustomPostDamageEvent(final CustomDamageEvent event) {
        this(
                event.getSystemTime(),
                event.getAdditiveMap(),
                event.getMultiplierMap(),
                event.getDamagee(),
                event.getDamager(),
                event.getProjectile(),
                event.getSource(),
                event.getCause(),
                event.getItemStack().orElse(null),
                event.getArmourContents(),
                event.getOriginalDamage(),
                event.isCritical(),
                event.getDamage(),
                event.getDelay(),
                event.getReason()
        );
    }
}