package io.github.trae.spigot.framework.damage.events.damage;

import io.github.trae.spigot.framework.damage.events.damage.abstracts.AbstractCustomDamageEvent;
import io.github.trae.spigot.framework.damage.modifier.DamageModifier;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * The first stage of a damage pass, for gating and for establishing the base.
 *
 * <p>Cancel here to refuse the damage outright; no later stage runs. This is where the damage delay
 * is enforced and where the weapon's own contribution is resolved, so by the time this stage ends
 * the base reflects what the attack is worth before anything custom touches it.</p>
 *
 * @see CustomDamageEvent
 */
public class CustomPreDamageEvent extends AbstractCustomDamageEvent {

    protected CustomPreDamageEvent(final long systemTime, final Map<DamageModifier, Double> additiveMap, final Map<DamageModifier, Double> multiplierMap, final Entity damagee, final Entity damager, final Projectile projectile, final DamageSource source, final EntityDamageEvent.DamageCause cause, final ItemStack itemStack, final ItemStack[] armourContents, final double originalDamage, final boolean critical, final double damage, final long delay, final String reason) {
        super(systemTime, additiveMap, multiplierMap, damagee, damager, projectile, source, cause, itemStack, armourContents, originalDamage, critical, damage, delay, reason);
    }

    /**
     * Builds the pre stage from an environmental damage event.
     *
     * <p>There is no damager, projectile or held item on this path, and the attack is never
     * critical. The base is seeded with vanilla's figure, since nothing in the pipeline recomputes
     * damage for causes like fire or falling.</p>
     *
     * @param entityDamageEvent the vanilla event being taken over
     * @return the pre stage event
     */
    public static CustomPreDamageEvent of(final EntityDamageEvent entityDamageEvent) {
        final Entity entity = entityDamageEvent.getEntity();

        final ItemStack[] armourContents = Optional.of(entity)
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast)
                .map(LivingEntity::getEquipment)
                .map(EntityEquipment::getArmorContents)
                .orElse(null);

        return new CustomPreDamageEvent(
                System.currentTimeMillis(),
                new EnumMap<>(DamageModifier.class),
                new EnumMap<>(DamageModifier.class),
                entity,
                null,
                null,
                entityDamageEvent.getDamageSource(),
                entityDamageEvent.getCause(),
                null,
                armourContents,
                entityDamageEvent.getDamage(),
                false,
                entityDamageEvent.getDamage(),
                0L,
                null
        );
    }

    /**
     * Builds the pre stage from an entity attack.
     *
     * <p>A projectile damager is resolved back to its shooter, with the projectile itself kept
     * separately, so a listener sees the player who fired the arrow as the damager rather than the
     * arrow.</p>
     *
     * @param entityDamageByEntityEvent the vanilla event being taken over
     * @return the pre stage event
     */
    public static CustomPreDamageEvent of(final EntityDamageByEntityEvent entityDamageByEntityEvent) {
        final Entity entity = entityDamageByEntityEvent.getEntity();

        Entity damager = entityDamageByEntityEvent.getDamager();
        Projectile projectile = null;

        if (damager instanceof final Projectile projectileDamager) {
            if (projectileDamager.getShooter() instanceof final Entity shooter) {
                damager = shooter;
            }

            projectile = projectileDamager;
        }

        final ItemStack itemStack = Optional.of(damager)
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast)
                .map(LivingEntity::getEquipment)
                .map(EntityEquipment::getItemInMainHand)
                .orElse(null);

        final ItemStack[] armourContents = Optional.of(entity)
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast)
                .map(LivingEntity::getEquipment)
                .map(EntityEquipment::getArmorContents)
                .orElse(null);

        return new CustomPreDamageEvent(
                System.currentTimeMillis(),
                new EnumMap<>(DamageModifier.class),
                new EnumMap<>(DamageModifier.class),
                entity,
                damager,
                projectile,
                entityDamageByEntityEvent.getDamageSource(),
                entityDamageByEntityEvent.getCause(),
                itemStack,
                armourContents,
                entityDamageByEntityEvent.getDamage(),
                !damager.isOnGround(),
                entityDamageByEntityEvent.getDamage(),
                0L,
                null
        );
    }
}