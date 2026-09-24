package io.github.trae.spigot.framework.resourcepack.configs;

import io.github.trae.di.configuration.annotations.Configuration;
import io.github.trae.di.configuration.enums.ConfigType;
import io.github.trae.spigot.framework.resourcepack.ResourcePack;
import io.github.trae.utilities.UtilJava;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for the server resource packs sent by
 * {@link io.github.trae.spigot.framework.resourcepack.ResourcePackManager}.
 * <p>
 * Each {@link ResourcePack} carries its own URL, hash, requirement and world and permission gates.
 * Packs are stacked in list order, so a later entry overrides assets from an earlier one.
 */
@NoArgsConstructor
@Getter
@Setter
@Configuration(value = "Resourcepack", type = ConfigType.JSON)
public class ResourcePackConfig {

    /**
     * Whether any resource pack is sent at all.
     */
    private boolean enabled = false;

    /**
     * The MiniMessage prompt shown on the download screen, or empty for the vanilla prompt.
     */
    private String prompt = "";

    /**
     * The MiniMessage kick message used when a required pack is declined or fails to load.
     */
    private String kickMessage = "<red>You must accept the resource pack to play.";

    /**
     * The resource packs, stacked in list order. Seeded with one empty entry to show the shape.
     */
    private List<ResourcePack> resourcePacks = UtilJava.createCollection(new ArrayList<>(), list -> list.add(new ResourcePack()));
}