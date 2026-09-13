package io.github.trae.spigot.framework.utility.enums;

import io.github.trae.utilities.UtilString;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@AllArgsConstructor
@Getter
public enum ArmourType {

    HELMET(
            EquipmentSlot.HEAD,
            Set.of(
                    Material.NETHERITE_HELMET,
                    Material.DIAMOND_HELMET,
                    Material.IRON_HELMET,
                    Material.CHAINMAIL_HELMET,
                    Material.GOLDEN_HELMET,
                    Material.COPPER_HELMET,
                    Material.TURTLE_HELMET,
                    Material.LEATHER_HELMET
            )
    ),
    CHESTPLATE(
            EquipmentSlot.CHEST,
            Set.of(
                    Material.NETHERITE_CHESTPLATE,
                    Material.DIAMOND_CHESTPLATE,
                    Material.IRON_CHESTPLATE,
                    Material.CHAINMAIL_CHESTPLATE,
                    Material.GOLDEN_CHESTPLATE,
                    Material.COPPER_CHESTPLATE,
                    Material.LEATHER_CHESTPLATE
            )
    ),
    LEGGINGS(
            EquipmentSlot.LEGS,
            Set.of(
                    Material.NETHERITE_LEGGINGS,
                    Material.DIAMOND_LEGGINGS,
                    Material.IRON_LEGGINGS,
                    Material.CHAINMAIL_LEGGINGS,
                    Material.GOLDEN_LEGGINGS,
                    Material.COPPER_LEGGINGS,
                    Material.LEATHER_LEGGINGS
            )
    ),
    BOOTS(
            EquipmentSlot.FEET,
            Set.of(
                    Material.NETHERITE_BOOTS,
                    Material.DIAMOND_BOOTS,
                    Material.IRON_BOOTS,
                    Material.CHAINMAIL_BOOTS,
                    Material.GOLDEN_BOOTS,
                    Material.COPPER_BOOTS,
                    Material.LEATHER_BOOTS
            )
    );

    private static final Map<Material, ArmourType> BY_MATERIAL_MAP = new EnumMap<>(Material.class);

    static {
        for (final ArmourType armourType : values()) {
            for (final Material material : armourType.getMaterials()) {
                BY_MATERIAL_MAP.put(material, armourType);
            }
        }
    }

    private final String name = UtilString.clean(this.name());
    private final EquipmentSlot slot;
    private final Set<Material> materials;

    public static Optional<ArmourType> getByMaterial(final Material material) {
        return Optional.ofNullable(BY_MATERIAL_MAP.get(material));
    }
}