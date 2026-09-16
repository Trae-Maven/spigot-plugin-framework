package io.github.trae.spigot.framework.damage.events.damage;

import io.github.trae.spigot.framework.damage.data.Reason;
import io.github.trae.spigot.framework.damage.events.damage.abstracts.AbstractCustomDamageEvent;
import io.github.trae.spigot.framework.damage.modifier.DamageModifier;
import io.github.trae.spigot.framework.displayname.DisplayName;
import io.github.trae.spigot.framework.sound.SoundProvider;
import io.github.trae.spigot.framework.utility.UtilColor;
import io.github.trae.spigot.framework.utility.enums.ChatColor;
import io.github.trae.utilities.UtilString;
import net.kyori.adventure.text.Component;
import org.bukkit.SoundCategory;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
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

    /**
     * Assigns the pass state directly.
     *
     * <p>Protected because this stage is never built field by field from outside: use one of the
     * {@code of} factories to take over a vanilla event, or a {@code create} factory to originate
     * damage the framework owns.</p>
     *
     * @param systemTime     when the pass began, in milliseconds
     * @param additiveMap    the flat contributions, shared between stages
     * @param multiplierMap  the proportional contributions, shared between stages
     * @param damagee        the entity taking the damage
     * @param damager        the entity dealing it, or {@code null} for environmental causes
     * @param projectile     the projectile that carried it, or {@code null} for a direct attack
     * @param source         the vanilla damage source
     * @param cause          what caused the damage
     * @param itemStack      the damager's held item, or {@code null}
     * @param armourContents the damagee's worn armour, or {@code null}
     * @param originalDamage what vanilla would have dealt
     * @param critical       whether the attack was a critical hit
     * @param soundProvider  the sound played when the damage lands, or {@code null}
     * @param damage         the base damage before modifiers
     * @param delay          the immunity period afterwards, in milliseconds
     * @param damageeName    how the damagee is named in messages
     * @param damagerName    how the damager is named in messages, or {@code null}
     * @param causeName      how the cause reads in messages
     * @param reason         what the damage is attributed to, or {@code null}
     */
    protected CustomPreDamageEvent(final long systemTime, final Map<DamageModifier, Double> additiveMap, final Map<DamageModifier, Double> multiplierMap, final Entity damagee, final Entity damager, final Projectile projectile, final DamageSource source, final EntityDamageEvent.DamageCause cause, final ItemStack itemStack, final ItemStack[] armourContents, final double originalDamage, final boolean critical, final SoundProvider soundProvider, final double damage, final long delay, final DisplayName damageeName, final DisplayName damagerName, final Component causeName, final Reason reason) {
        super(systemTime, additiveMap, multiplierMap, damagee, damager, projectile, source, cause, itemStack, armourContents, originalDamage, critical, soundProvider, damage, delay, damageeName, damagerName, causeName, reason);
    }

    /**
     * Builds the pre stage from an environmental damage event.
     *
     * <p>There is no damager, projectile or held item on this path, and the attack is never
     * critical. The base is seeded with vanilla's figure, since nothing in the pipeline recomputes
     * damage for causes like fire or falling.</p>
     *
     * <p>The sound is seeded from the damagee, so it is whatever that entity type makes when struck,
     * replaceable by anything that wants a different one.</p>
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
                getHurtSound(entity),
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
     * <p>Whether the attack is critical is taken from the vanilla event rather than worked out here,
     * so it is vanilla's own decision: the attacker falling, not climbing, swimming, blinded, riding
     * or sprinting, against a living target. Reading it off the event also keeps it server-side,
     * where a client-reported ground state could be spoofed.</p>
     *
     * <p>The reason is seeded from the damager's held item, carrying its display name and hover
     * tooltip, so a death message can show what killed someone without rebuilding it. An empty hand
     * still produces a reason, but one with no name, so anything reading it should check the name
     * rather than the reason itself.</p>
     *
     * <p>The sound is seeded from the damagee rather than the weapon, so what a player hears is the
     * thing being hit rather than the thing hitting it.</p>
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

        final Component reasonName = Optional.ofNullable(itemStack)
                .map(value -> value.effectiveName().hoverEvent(value.asHoverEvent()))
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
                entityDamageByEntityEvent.isCritical(),
                getHurtSound(entity),
                entityDamageByEntityEvent.getDamage(),
                0L,
                getName(entity),
                getName(damager),
                getCauseName(entityDamageByEntityEvent.getCause()),
                Reason.of(reasonName)
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
    public static CustomPreDamageEvent create(final Entity damagee, final DamageCause cause, final double damage, final Reason reason) {
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
                null,
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
    public static CustomPreDamageEvent create(final Entity damagee, final Entity damager, final DamageCause cause, final double damage, final Reason reason) {
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
                null,
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
    private static DisplayName getName(final Entity entity) {
        return DisplayName.of(Component.text(entity.getName()).color(UtilColor.toTextColor(ChatColor.YELLOW.getColor())));
    }

    /**
     * The damagee's own hurt sound, or {@code null} when it is not a living entity.
     *
     * <p>Read from the entity rather than the cause, so a zombie grunts and a skeleton rattles
     * without the pipeline keeping a table of its own. The sound plays under the entity's own
     * category, matching vanilla, so it follows the same client volume slider. Non-living entities
     * have no hurt sound at all, which is what the filter handles, while a living entity with no hurt
     * sound still gets a provider, one that plays nothing.</p>
     *
     * @param damagee the entity being damaged
     * @return the hurt sound, or {@code null} for a non-living entity
     */
    private static SoundProvider getHurtSound(final Entity damagee) {
        return Optional.of(damagee)
                .filter(LivingEntity.class::isInstance)
                .map(CraftLivingEntity.class::cast)
                .map(damageeLivingEntity -> SoundProvider.of(damageeLivingEntity.getHurtSound(), SoundCategory.valueOf(damageeLivingEntity.getHandle().getSoundSource().name())))
                .orElse(null);
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