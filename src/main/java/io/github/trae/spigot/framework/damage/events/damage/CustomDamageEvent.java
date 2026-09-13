package io.github.trae.spigot.framework.damage.events.damage;

import io.github.trae.spigot.framework.damage.data.Reason;
import io.github.trae.spigot.framework.damage.events.damage.abstracts.AbstractCustomDamageEvent;
import io.github.trae.spigot.framework.damage.modifier.DamageModifier;
import net.kyori.adventure.text.Component;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * The middle stage of a damage pass, reserved for consuming plugins.
 *
 * <p>Nothing in the framework writes damage here. This is where an ability sets its own figure,
 * either by replacing the base outright or by contributing under
 * {@link DamageModifier#CUSTOM}. An ability that should ignore the weapon entirely also removes
 * {@link DamageModifier#WEAPON}.</p>
 *
 * @see CustomPreDamageEvent
 * @see CustomPostDamageEvent
 */
public class CustomDamageEvent extends AbstractCustomDamageEvent {

    protected CustomDamageEvent(final long systemTime, final Map<DamageModifier, Double> additiveMap, final Map<DamageModifier, Double> multiplierMap, final Entity damagee, final Entity damager, final Projectile projectile, final DamageSource source, final EntityDamageEvent.DamageCause cause, final ItemStack itemStack, final ItemStack[] armourContents, final double originalDamage, final boolean critical, final double damage, final long delay, final Component damageeName, final Component damagerName, final Component causeName, final Reason reason) {
        super(systemTime, additiveMap, multiplierMap, damagee, damager, projectile, source, cause, itemStack, armourContents, originalDamage, critical, damage, delay, damageeName, damagerName, causeName, reason);
    }

    /**
     * Carries the pre stage forward.
     *
     * <p>The modifier maps are passed by reference rather than copied, so anything contributed
     * during the pre stage is still in place here. Names and reason carry forward by value, so a
     * plugin that renamed either side at the pre stage keeps that name.</p>
     *
     * @param event the pre stage that produced this one
     */
    public CustomDamageEvent(final CustomPreDamageEvent event) {
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
                event.getDamageeName(),
                event.getDamagerName(),
                event.getCauseName(),
                event.getReason()
        );
    }
}