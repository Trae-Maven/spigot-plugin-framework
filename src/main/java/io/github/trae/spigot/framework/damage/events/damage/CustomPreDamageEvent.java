package io.github.trae.spigot.framework.damage.events.damage;

import io.github.trae.spigot.framework.damage.events.damage.abstracts.AbstractCustomDamageEvent;
import io.github.trae.spigot.framework.damage.modifier.DamageModifier;
import io.github.trae.spigot.framework.utility.UtilColor;
import io.github.trae.spigot.framework.utility.enums.ChatColor;
import io.github.trae.utilities.UtilString;
import net.kyori.adventure.text.Component;
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

    protected CustomPreDamageEvent(final long systemTime, final Map<DamageModifier, Double> additiveMap, final Map<DamageModifier, Double> multiplierMap, final Entity damagee, final Entity damager, final Projectile projectile, final DamageSource source, final EntityDamageEvent.DamageCause cause, final ItemStack itemStack, final ItemStack[] armourContents, final double originalDamage, final boolean critical, final double damage, final long delay, final Component damageeName, final Component damagerName, final Component causeName, final Component reason) {
        super(systemTime, additiveMap, multiplierMap, damagee, damager, projectile, source, cause, itemStack, armourContents, originalDamage, critical, damage, delay, damageeName, damagerName, causeName, reason);
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
                getArmorContents(entity),
                entityDamageEvent.getDamage(),
                false,
                entityDamageEvent.getDamage(),
                0L,
                getDamageeName(entity),
                null,
                getCauseName(entityDamageEvent.getCause()),
                null
        );
    }

    /**
     * Builds the pre stage from an entity attack.
     *
     * <p>A projectile damager is resolved back to its shooter, with the projectile itself kept
     * separately, so a listener sees the player who fired the arrow as the damager rather than the
     * arrow. Both names are seeded from the entities themselves and can be replaced by a plugin that
     * displays something other than the raw name.</p>
     *
     * <p>The reason is seeded from the damager's held item, carrying its display name and hover
     * tooltip, so a death message can show what killed someone without rebuilding it. An empty hand
     * leaves it {@code null}.</p>
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
                .filter(value -> !value.isEmpty())
                .orElse(null);

        final Component reason = Optional.ofNullable(itemStack)
                .map(value -> value.displayName().hoverEvent(value.asHoverEvent()))
                .map(component -> component.colorIfAbsent(UtilColor.toTextColor(ChatColor.GREEN.getColor())))
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
                getArmorContents(entity),
                entityDamageByEntityEvent.getDamage(),
                !damager.isOnGround(),
                entityDamageByEntityEvent.getDamage(),
                0L,
                getDamageeName(entity),
                Component.text(damager.getName()).color(UtilColor.toTextColor(ChatColor.YELLOW.getColor())),
                getCauseName(entityDamageByEntityEvent.getCause()),
                reason
        );
    }

    private static ItemStack[] getArmorContents(final Entity damagee) {
        return Optional.of(damagee)
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast)
                .map(LivingEntity::getEquipment)
                .map(EntityEquipment::getArmorContents)
                .orElse(null);
    }

    private static Component getDamageeName(final Entity damagee) {
        return Component.text(damagee.getName()).color(UtilColor.toTextColor(ChatColor.YELLOW.getColor()));
    }

    private static Component getCauseName(final EntityDamageEvent.DamageCause damageCause) {
        return Component.text(UtilString.clean(damageCause.name())).color(UtilColor.toTextColor(ChatColor.YELLOW.getColor()));
    }
}