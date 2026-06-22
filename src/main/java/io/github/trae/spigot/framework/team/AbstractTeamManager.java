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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages per-viewer player teams using direct NMS team packets.
 * <p>
 * For each player/viewer pair, resolves the lowest-priority eligible {@link Team} (discovered via
 * the dependency injector) and sends a {@link PlayerTeam} packet to the viewer carrying that
 * team's prefix, suffix, and other options. Because resolution is per-pair, the same target player
 * can present different nametag decorations to different viewers — enabling relation-aware coloring.
 * <p>
 * Teams are keyed uniquely per player/viewer pair so they never collide, and the set of currently
 * displayed pairs is tracked so removals only fire for pairs that actually have a team registered.
 * Player join and quit are handled automatically, as are {@link TeamUpdateEvent}s. Whether a pair
 * is eligible at all is decided entirely by {@link #getEligibleTeam(Player, Player)}, so any
 * suppression (such as a per-player preference) is expressed through a team's display checks rather
 * than at the event level.
 *
 * @param <Plugin> the plugin type this manager belongs to
 */
public class AbstractTeamManager<Plugin extends SpigotPlugin> implements Manager<Plugin>, Listener {

    private final Set<String> activeTeamSet = ConcurrentHashMap.newKeySet();

    /**
     * Registers (or replaces) the eligible team for the given player/viewer pair on the viewer's
     * client and binds the target player as its member.
     * <p>
     * Sends two packets: an add-or-modify packet with {@code updatePlayers = true} (method
     * {@code ADD}), then a player packet with {@code ADD}. The {@code false} (method {@code CHANGE})
     * variant is deliberately not used: {@code CHANGE} only updates a team the client already has
     * registered, so a pair whose team first becomes eligible after join — having never received an
     * initial {@code ADD} — would have a {@code CHANGE} packet silently dropped. An {@code ADD}
     * packet built from a memberless {@link PlayerTeam} also carries an empty roster, so the explicit
     * player {@code ADD} packet is required to (re)bind the member. Both calls together are
     * idempotent, so this serves equally for the initial display and for later updates.
     *
     * @param player the target player the team applies to
     * @param viewer the viewer the team is sent to
     * @param team   the team supplying the options
     */
    private void create(final Player player, final Player viewer, final Team team) {
        final String teamName = this.getTeamName(player, viewer);

        final PlayerTeam playerTeam = this.buildPlayerTeam(teamName, player, viewer, team);

        UtilNms.sendPacket(viewer, ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(playerTeam, true));
        UtilNms.sendPacket(viewer, ClientboundSetPlayerTeamPacket.createPlayerPacket(playerTeam, player.getName(), ClientboundSetPlayerTeamPacket.Action.ADD));

        this.activeTeamSet.add(teamName);
    }

    /**
     * Removes the player/viewer team from the viewer's client by detaching the player entry and
     * removing the team itself.
     * <p>
     * No-ops when the pair has no team currently registered, so this is safe to call
     * unconditionally and never emits stray removal packets.
     *
     * @param player the target player whose team to remove
     * @param viewer the viewer the removal is sent to
     */
    private void remove(final Player player, final Player viewer) {
        final String teamName = this.getTeamName(player, viewer);

        if (!(this.activeTeamSet.remove(teamName))) {
            return;
        }

        final PlayerTeam playerTeam = new PlayerTeam(new Scoreboard(), teamName);

        UtilNms.sendPacket(viewer, ClientboundSetPlayerTeamPacket.createPlayerPacket(playerTeam, player.getName(), ClientboundSetPlayerTeamPacket.Action.REMOVE));
        UtilNms.sendPacket(viewer, ClientboundSetPlayerTeamPacket.createRemovePacket(playerTeam));
    }

    /**
     * Re-resolves the eligible team for the given pair and applies the result: creates the team if
     * one qualifies, otherwise removes any team the pair currently has.
     *
     * @param player the target player the team applies to
     * @param viewer the viewer the team is rendered for
     */
    private void refresh(final Player player, final Player viewer) {
        this.getEligibleTeam(player, viewer).ifPresentOrElse(team -> this.create(player, viewer, team), () -> this.remove(player, viewer));
    }

    /**
     * Builds a {@link PlayerTeam} for the given pair, applying each option the {@link Team} provides.
     * <p>
     * Options that return {@code null} are skipped, leaving the underlying {@link PlayerTeam}
     * default in place rather than overriding it.
     *
     * @param teamName the pre-computed unique team name for the pair
     * @param player   the target player the team applies to
     * @param viewer   the viewer the team is rendered for
     * @param team     the team supplying the options
     * @return the populated {@link PlayerTeam}
     */
    private PlayerTeam buildPlayerTeam(final String teamName, final Player player, final Player viewer, final Team team) {
        final PlayerTeam playerTeam = new PlayerTeam(new Scoreboard(), teamName);

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

    /**
     * Builds the unique team name for a player/viewer pair from their UUIDs, ensuring each viewer
     * has its own distinct team for each target player.
     *
     * @param player the target player
     * @param viewer the viewer
     * @return the unique team name
     */
    private String getTeamName(final Player player, final Player viewer) {
        return "%s:%s".formatted(player.getUniqueId(), viewer.getUniqueId());
    }

    /**
     * Resolves the eligible team for the player/viewer pair — the one with the lowest priority that
     * passes both the global and per-pair display checks.
     *
     * @param player the target player
     * @param viewer the viewer
     * @return an {@link Optional} containing the eligible team, or empty if none qualify
     */
    private Optional<Team> getEligibleTeam(final Player player, final Player viewer) {
        return InjectorApi.getAll(Team.class)
                .stream()
                .sorted(Comparator.comparingInt(Team::getPriority))
                .filter(team -> team.canDisplay() && team.canDisplay(player, viewer))
                .findFirst();
    }

    /**
     * Handles a {@link TeamUpdateEvent} by re-resolving the player's team for every online viewer.
     * <p>
     * An unscoped event re-resolves each pair and creates or removes the team accordingly. A scoped
     * event creates the team only for pairs whose eligible team matches the event identifier, and
     * removes it from all others. A pair that resolves to no eligible team is always removed, so
     * suppression handled inside the team display checks tears the pair down on the next update.
     *
     * @param event the team update event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onTeamUpdate(final TeamUpdateEvent event) {
        final Player player = event.getPlayer();

        for (final Player viewer : Bukkit.getServer().getOnlinePlayers()) {
            if (event.getIdentifier() == null) {
                this.refresh(player, viewer);
                continue;
            }

            this.getEligibleTeam(player, viewer)
                    .filter(team -> team.getIdentifier().equals(event.getIdentifier()))
                    .ifPresentOrElse(team -> this.create(player, viewer, team), () -> this.remove(player, viewer));
        }
    }

    /**
     * On join, sends the joining player's team to every viewer and, reciprocally, sends every other
     * online player's team to the joining player.
     *
     * @param event the player join event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        for (final Player viewer : Bukkit.getServer().getOnlinePlayers()) {
            this.refresh(player, viewer);

            if (!(player.equals(viewer))) {
                this.refresh(viewer, player);
            }
        }
    }

    /**
     * On quit, removes the quitting player's team in both directions for every other online player:
     * the quitting player's team is removed from each viewer, and each viewer's team is removed from
     * the quitting player. This clears every tracked pair involving the quitting player, leaving no
     * stale entries.
     *
     * @param event the player quit event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();

        for (final Player viewer : Bukkit.getServer().getOnlinePlayers()) {
            this.remove(player, viewer);

            if (!(player.equals(viewer))) {
                this.remove(viewer, player);
            }
        }
    }
}