package io.github.trae.spigot.framework.damage.events.damage;

import io.github.trae.spigot.framework.damage.events.damage.abstracts.AbstractCustomDamageEvent;
import io.github.trae.spigot.framework.damage.modifier.DamageModifier;
import io.github.trae.spigot.framework.utility.UtilColor;
import io.github.trae.spigot.framework.utility.enums.ChatColor;
import io.github.trae.utilities.UtilString;
import net.kyori.adventure.text.Component;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
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

    protected CustomPreDamageEvent(final long systemTime, final Map<DamageModifier, Double> additiveMap, final Map<DamageModifier, Double> multiplierMap, final Entity damagee, final Entity damager, final Projectile projectile, final DamageSource source, final DamageCause cause, final ItemStack itemStack, final ItemStack[] armourContents, final double originalDamage, final boolean critical, final double damage, final long delay, final Component damageeName, final Component damagerName, final Component causeName, final Component reason) {
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
                getName(entity),
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
                getName(entity),
                getName(damager),
                getCauseName(entityDamageByEntityEvent.getCause()),
                reason
        );
    }

    /**
     * Builds a pre stage for damage the framework is originating itself.
     *
     * <p>Unlike the {@code of} factories, nothing here comes from a vanilla event. Use this when a
     * plugin deals damage directly, such as an ability, a trap or a scripted effect, and dispatch
     * the returned event to run it through the full pipeline.</p>
     *
     * <p>The source is built as generic since there is no vanilla event to inherit one from. Both
     * the base damage and the cause are the caller's to decide, and the cause still drives which
     * reductions apply and what delay is enforced.</p>
     *
     * @param damagee the entity to damage
     * @param cause   the cause to attribute it to
     * @param damage  the base damage
     * @param reason  how it is described in messages, or {@code null}
     * @return the pre stage event
     */
    public static CustomPreDamageEvent create(final Entity damagee, final DamageCause cause, final double damage, final Component reason) {
        return new CustomPreDamageEvent(
                System.currentTimeMillis(),
                new EnumMap<>(DamageModifier.class),
                new EnumMap<>(DamageModifier.class),
                damagee,
                null,
                null,
                DamageSource.builder(DamageType.GENERIC).build(),
                cause,
                null,
                getArmorContents(damagee),
                damage,
                false,
                damage,
                0L,
                getName(damagee),
                null,
                getCauseName(cause),
                reason
        );
    }

    /**
     * Builds a pre stage for damage the framework is originating itself, attributed to an attacker.
     *
     * <p>The same as the damager-less overload, except the attacker is carried on the source as
     * well as the event, so the combat tracker names them in the death message and mobs retaliate
     * against them.</p>
     *
     * <p>No held item is captured, since damage originated this way is not a weapon swing. An
     * ability that should still be attributed to an item sets the reason itself.</p>
     *
     * @param damagee the entity to damage
     * @param damager the entity to attribute it to
     * @param cause   the cause to attribute it to
     * @param damage  the base damage
     * @param reason  how it is described in messages, or {@code null}
     * @return the pre stage event
     */
    public static CustomPreDamageEvent create(final Entity damagee, final Entity damager, final DamageCause cause, final double damage, final Component reason) {
        return new CustomPreDamageEvent(
                System.currentTimeMillis(),
                new EnumMap<>(DamageModifier.class),
                new EnumMap<>(DamageModifier.class),
                damagee,
                damager,
                null,
                DamageSource.builder(DamageType.GENERIC).withCausingEntity(damager).withDirectEntity(damager).build(),
                cause,
                null,
                getArmorContents(damagee),
                damage,
                false,
                damage,
                0L,
                getName(damagee),
                getName(damager),
                getCauseName(cause),
                reason
        );
    }

    /**
     * The entity's worn armour, or {@code null} when it has none to wear.
     *
     * <p>Captured once at the pre stage rather than read later, so a piece swapped out mid-pass is
     * not mistaken for what was worn when the hit landed. Non-living entities have no equipment at
     * all, which is what the filter handles.</p>
     *
     * @param damagee the entity being damaged
     * @return the armour contents, or {@code null}
     */
    private static ItemStack[] getArmorContents(final Entity damagee) {
        return Optional.of(damagee)
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast)
                .map(LivingEntity::getEquipment)
                .map(EntityEquipment::getArmorContents)
                .orElse(null);
    }

    /**
     * An entity's name as it appears in messages.
     *
     * <p>Seeded here and settable on the event afterwards, so a plugin with display names or ranks
     * replaces it once rather than at every message site.</p>
     *
     * @param entity the entity to name
     * @return the coloured name
     */
    private static Component getName(final Entity entity) {
        return Component.text(entity.getName()).color(UtilColor.toTextColor(ChatColor.YELLOW.getColor()));
    }

    /**
     * A damage cause as it appears in messages.
     *
     * <p>Cleaned from the enum constant, so {@code ENTITY_ATTACK} reads as ordinary text rather than
     * a constant name.</p>
     *
     * @param damageCause the cause to name
     * @return the coloured name
     */
    private static Component getCauseName(final DamageCause damageCause) {
        return Component.text(UtilString.clean(damageCause.name())).color(UtilColor.toTextColor(ChatColor.YELLOW.getColor()));
    }
}