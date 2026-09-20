package io.github.trae.spigot.framework.utility;

import lombok.experimental.UtilityClass;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Optional;

@UtilityClass
public class UtilPotionEffect {

    public static void add(final LivingEntity livingEntity, final PotionEffectType type, final int amplifier, final long duration, final boolean ambient, final boolean particles, final boolean icon) {
        if (amplifier < 0 || duration <= 0L) {
            return;
        }

        final int ticks = duration == Long.MAX_VALUE ? PotionEffect.INFINITE_DURATION : (int) (duration / 50L);

        livingEntity.addPotionEffect(new PotionEffect(type, ticks, amplifier - 1, ambient, particles, icon));
    }

    public static void add(final LivingEntity livingEntity, final PotionEffectType type, final int amplifier, final long duration) {
        add(livingEntity, type, amplifier, duration, false, true, true);
    }

    public static void remove(final LivingEntity livingEntity, final PotionEffectType type) {
        if (!has(livingEntity, type)) {
            return;
        }

        livingEntity.removePotionEffect(type);
    }

    public static List<PotionEffect> getAll(final LivingEntity livingEntity) {
        return List.copyOf(livingEntity.getActivePotionEffects());
    }

    public static Optional<PotionEffect> get(final LivingEntity livingEntity, final PotionEffectType type) {
        return Optional.ofNullable(livingEntity.getPotionEffect(type));
    }

    public static int getAmplifier(final LivingEntity livingEntity, final PotionEffectType type) {
        return get(livingEntity, type).map(potionEffect -> potionEffect.getAmplifier() + 1).orElse(0);
    }

    public static long getDuration(final LivingEntity livingEntity, final PotionEffectType type) {
        return get(livingEntity, type).map(potionEffect -> potionEffect.isInfinite() ? Long.MAX_VALUE : potionEffect.getDuration() * 50L).orElse(0L);
    }

    public static boolean hasLeft(final LivingEntity livingEntity, final PotionEffectType type, final long duration) {
        return getDuration(livingEntity, type) >= duration;
    }

    public static boolean has(final LivingEntity livingEntity, final PotionEffectType type, final int amplifier) {
        return getAmplifier(livingEntity, type) == amplifier;
    }

    public static boolean has(final LivingEntity livingEntity, final PotionEffectType type) {
        return livingEntity.hasPotionEffect(type);
    }
}