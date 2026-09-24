package io.github.trae.spigot.framework.resourcepack;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.kyori.adventure.resource.ResourcePackInfo;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A single resource pack entry in {@link io.github.trae.spigot.framework.resourcepack.configs.ResourcePackConfig}.
 * <p>
 * A player receives the pack only while in one of its {@link #worlds} and while holding its
 * {@link #permission}. Leaving either empty disables that check. Packs are addressed by their
 * {@link #id}, so each can be applied and removed without touching any other.
 */
@NoArgsConstructor
@Getter
@Setter
public final class ResourcePack {

    /**
     * The pack's unique identifier. Generated once and persisted on first write.
     */
    private String id = UUID.randomUUID().toString();

    /**
     * The direct download URL of the pack zip. An empty URL is never sent.
     */
    private String url = "";

    /**
     * The SHA-1 hash of the pack zip as a 40 character hex string, or empty to skip verification.
     */
    private String hash = "";

    /**
     * Whether the pack is required. A required pack that is declined or fails to load kicks the player.
     */
    private boolean required = false;

    /**
     * The names of the worlds the pack is active in, or empty for every world.
     */
    private List<String> worlds = new ArrayList<>();

    /**
     * The permission a player needs to receive the pack, or empty for no permission check.
     */
    private String permission = "";

    /**
     * Returns the pack's configured {@link #id} as a {@link UUID}, the key the client tracks the
     * pack by for applying, removing and status reporting.
     *
     * @return the pack identifier
     * @throws IllegalArgumentException if the configured id is not a valid UUID
     */
    public UUID getUuid() {
        return UUID.fromString(this.id);
    }

    /**
     * Builds the Adventure pack info sent in a resource pack request, from this pack's identifier,
     * URL and hash.
     *
     * @return the pack info
     * @throws IllegalArgumentException if the configured id is not a valid UUID or the URL is malformed
     */
    public ResourcePackInfo toResourcePackInfo() {
        return ResourcePackInfo.resourcePackInfo(
                this.getUuid(),
                URI.create(this.url),
                this.hash
        );
    }
}