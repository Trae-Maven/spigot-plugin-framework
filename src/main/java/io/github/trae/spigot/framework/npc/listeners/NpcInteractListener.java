package io.github.trae.spigot.framework.npc.listeners;

import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.npc.Npc;
import io.github.trae.spigot.framework.npc.NpcManager;
import io.github.trae.spigot.framework.npc.events.NpcInteractEvent;
import io.github.trae.spigot.framework.npc.types.InteractableNpc;
import io.github.trae.spigot.framework.utility.UtilEvent;
import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Suppresses vanilla interactions on every NPC's backing entity and routes main-hand right-clicks on
 * an {@link InteractableNpc} to the NPC.
 */
@AllArgsConstructor
@Singleton
public final class NpcInteractListener implements Listener {

    /**
     * The manager NPC lookups are delegated to.
     */
    private final NpcManager npcManager;

    /**
     * Handles a right-click on an entity.
     * <p>
     * The vanilla interaction is cancelled for any NPC, in either hand and whether or not it is an
     * {@link InteractableNpc}, so no NPC ever opens a trade screen or reacts to a vanilla click.
     * Off-hand clicks and NPCs that are not an {@link InteractableNpc} then stop there. Otherwise the
     * gate pattern applies: a cancellable {@link NpcInteractEvent} is dispatched first, then the NPC's
     * own {@link InteractableNpc#canInteract(Player)} check, and only if both pass is
     * {@link InteractableNpc#onInteract(Player)} called.
     *
     * @param event the player interact entity event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteractEntity(final PlayerInteractEntityEvent event) {
        if (event.isCancelled()) {
            return;
        }

        final Npc<?> npc = this.npcManager.getNpcByEntity(event.getRightClicked()).orElse(null);
        if (npc == null) {
            return;
        }

        event.setCancelled(true);

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (!(npc instanceof final InteractableNpc interactableNpc)) {
            return;
        }

        final Player player = event.getPlayer();

        if (UtilEvent.supply(new NpcInteractEvent(npc, player)).isCancelled()) {
            return;
        }

        if (!interactableNpc.canInteract(player)) {
            return;
        }

        interactableNpc.onInteract(player);
    }

    /**
     * Cancels positional right-clicks on any NPC, which have their own handler list and so never
     * reach {@link #onPlayerInteractEntity(PlayerInteractEntityEvent)}. Without this, an armor stand
     * NPC would let players swap its equipment. Not routed to the NPC, as the plain interact event
     * that accompanies it already is.
     *
     * @param event the player interact at entity event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteractAtEntity(final PlayerInteractAtEntityEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (this.npcManager.getNpcByEntity(event.getRightClicked()).isEmpty()) {
            return;
        }

        event.setCancelled(true);
    }
}