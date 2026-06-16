package io.github.trae.spigot.framework.team;

import io.github.trae.di.InjectorApi;
import io.github.trae.hf.Manager;
import io.github.trae.spigot.framework.SpigotPlugin;
import io.github.trae.spigot.framework.team.events.TeamUpdateEvent;
import io.github.trae.spigot.framework.utility.UtilNms;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Comparator;
import java.util.Optional;

public class AbstractTeamManager<Plugin extends SpigotPlugin> implements Manager<Plugin>, Listener {

    private void create(final Player player, final Player viewer) {
        this.getEligibleTeam(player, viewer).ifPresent(team -> {
            final PlayerTeam playerTeam = this.buildPlayerTeam(player, viewer, team);

            UtilNms.sendPacket(viewer, ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(playerTeam, true));
            UtilNms.sendPacket(viewer, ClientboundSetPlayerTeamPacket.createPlayerPacket(playerTeam, player.getName(), ClientboundSetPlayerTeamPacket.Action.ADD));
        });
    }

    private void update(final Player player, final Player viewer, final Team team) {
        final PlayerTeam playerTeam = this.buildPlayerTeam(player, viewer, team);

        UtilNms.sendPacket(viewer, ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(playerTeam, false));
    }

    private void remove(final Player player, final Player viewer) {
        final PlayerTeam playerTeam = new PlayerTeam(new Scoreboard(), this.getTeamName(player, viewer));

        UtilNms.sendPacket(viewer, ClientboundSetPlayerTeamPacket.createPlayerPacket(playerTeam, player.getName(), ClientboundSetPlayerTeamPacket.Action.REMOVE));
        UtilNms.sendPacket(viewer, ClientboundSetPlayerTeamPacket.createRemovePacket(playerTeam));
    }

    private PlayerTeam buildPlayerTeam(final Player player, final Player viewer, final Team team) {
        final PlayerTeam playerTeam = new PlayerTeam(new Scoreboard(), this.getTeamName(player, viewer));

        // Display Name
        Optional.ofNullable(team.getDisplayName(player, viewer)).ifPresent(component -> playerTeam.setDisplayName(UtilNms.toNms(component)));

        // Prefix & Suffix
        Optional.ofNullable(team.getPrefix(player, viewer)).ifPresent(component -> playerTeam.setPlayerPrefix(UtilNms.toNms(component)));
        Optional.ofNullable(team.getSuffix(player, viewer)).ifPresent(component -> playerTeam.setPlayerSuffix(UtilNms.toNms(component)));

        // Allow Friendly Fire & See Friendly Invisibles
        Optional.ofNullable(team.allowFriendlyFire(player, viewer)).ifPresent(playerTeam::setAllowFriendlyFire);
        Optional.ofNullable(team.seeFriendlyInvisibles(player, viewer)).ifPresent(playerTeam::setSeeFriendlyInvisibles);

        // Name Tag Visibility & Death Message Visibility
        Optional.ofNullable(team.getNameTagVisibility(player, viewer)).ifPresent(playerTeam::setNameTagVisibility);
        Optional.ofNullable(team.getDeathMessageVisibility(player, viewer)).ifPresent(playerTeam::setDeathMessageVisibility);

        // Collision Rule
        Optional.ofNullable(team.getCollisionRule(player, viewer)).ifPresent(playerTeam::setCollisionRule);

        // Color
        Optional.ofNullable(team.getColor(player, viewer)).ifPresent(playerTeam::setColor);

        return playerTeam;
    }

    private String getTeamName(final Player player, final Player viewer) {
        return "%s:%s".formatted(player.getUniqueId(), viewer.getUniqueId());
    }

    private Optional<Team> getEligibleTeam(final Player player, final Player viewer) {
        return InjectorApi.getAll(Team.class)
                .stream()
                .sorted(Comparator.comparingInt(Team::getPriority))
                .filter(team -> team.canDisplay() && team.canDisplay(player, viewer))
                .findFirst();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTeamUpdate(final TeamUpdateEvent event) {
        final Player player = event.getPlayer();

        for (final Player viewer : Bukkit.getServer().getOnlinePlayers()) {
            final Optional<Team> eligibleTeamOptional = this.getEligibleTeam(player, viewer);

            if (event.getIdentifier() == null) {
                eligibleTeamOptional.ifPresentOrElse(team -> this.update(player, viewer, team), () -> this.remove(player, viewer));
                continue;
            }

            eligibleTeamOptional.filter(team -> team.getIdentifier().equals(event.getIdentifier())).ifPresent(team -> this.update(player, viewer, team));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        for (final Player viewer : Bukkit.getServer().getOnlinePlayers()) {
            this.create(player, viewer);

            if (!(player.equals(viewer))) {
                this.create(viewer, player);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();

        for (final Player target : Bukkit.getServer().getOnlinePlayers()) {
            this.remove(player, target);
        }
    }
}