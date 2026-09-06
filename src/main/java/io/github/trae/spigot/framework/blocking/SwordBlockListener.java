package io.github.trae.spigot.framework.blocking;

import io.github.trae.di.annotations.type.component.Singleton;
import io.github.trae.spigot.framework.item.events.ItemStackUpdateEvent;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.BlocksAttacks;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Set;

/**
 * Gives every sword this framework produces the vanilla shield's blocking behaviour.
 * <p>
 * A sword has no use action of its own, so right-clicking one does nothing and
 * {@link org.bukkit.entity.Player#isBlocking()} can never become true for it. Attaching the
 * {@code blocks_attacks} data component gives it one, which is what lets an item gate on the player
 * holding right click, and what drives the {@code minecraft:using_item} model condition a resource
 * pack needs to swap in a blocking pose.
 * <p>
 * The component is applied with no damage reductions, so blocking is a pure gesture that reduces
 * nothing, and with a disable cooldown scale of zero, so an axe hit cannot interrupt it. The block
 * delay is one tick rather than the shield's quarter second, so the raise registers immediately.
 * <p>
 * Applied through {@link ItemStackUpdateEvent}, which
 * {@link io.github.trae.spigot.framework.item.ItemManager#apply(ItemStack)} fires on every path:
 * custom swords, vanilla ones passing through their default definition, and swords already current
 * by version. That last group is why the refresh path exists at all, since an item's version hash
 * covers its own description and says nothing about what a listener adds on top.
 */
@Singleton
public class SwordBlockListener implements Listener {

    /**
     * Every sword material, in descending tier order.
     */
    private static final Set<Material> MATERIAL_SET = Set.of(
            Material.NETHERITE_SWORD, Material.DIAMOND_SWORD, Material.IRON_SWORD,
            Material.COPPER_SWORD, Material.STONE_SWORD, Material.GOLDEN_SWORD,
            Material.WOODEN_SWORD
    );

    /**
     * Attaches the blocking component to a sword stack.
     * <p>
     * Handled on the stack event rather than the meta one because a data component set during the
     * meta edit would be discarded: applying a meta replaces the stack's whole component set.
     *
     * @param event the stack update event
     */
    @SuppressWarnings("UnstableApiUsage")
    @EventHandler
    public final void onItemStackUpdate(final ItemStackUpdateEvent event) {
        final ItemStack itemStack = event.getItemStack();

        if (!MATERIAL_SET.contains(itemStack.getType())) {
            return;
        }

        itemStack.setData(DataComponentTypes.BLOCKS_ATTACKS, BlocksAttacks.blocksAttacks()
                .blockDelaySeconds(0.05F)
                .disableCooldownScale(0.0F)
                .damageReductions(List.of())
                .build());
    }
}