package dev.emortal.velocity.misc.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import dev.emortal.api.model.mcplayer.CurrentServer;
import dev.emortal.api.model.mcplayer.OnlinePlayer;
import dev.emortal.api.service.playertracker.PlayerTrackerService;
import dev.emortal.velocity.command.EmortalCommand;
import dev.emortal.velocity.lang.ChatMessages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ListCommand extends EmortalCommand {
    private final PlayerTrackerService playerTracker;

    protected ListCommand(@Nullable PlayerTrackerService playerTracker) {
        super("list");

        this.playerTracker = playerTracker;

        super.setDefaultExecutor(this::sendUsage);

        var fleetLiteralArg = literal("fleet");
        var fleetIdArg = argument("fleetId", StringArgumentType.word(), null);
        super.addSyntax(this::executeAllFleets, fleetLiteralArg);
        super.addSyntax(this::executeFleet, fleetLiteralArg, fleetIdArg);

        var serverLiteralArg = literal("server");
        var serverIdArg = argument("serverId", StringArgumentType.word(), null);
        super.addSyntax(this::executeAllServers, serverLiteralArg);
        super.addSyntax(this::executeServer, serverLiteralArg, serverIdArg);
    }

    private void sendUsage(@NotNull CommandContext<CommandSource> context) {
        ChatMessages.LIST_USAGE.send(context.getSource());
    }

    public record AllFleetsSummary(int playerCount, Map<String, List<OnlinePlayer>> fleetPlayers) {}

    private void executeAllFleets(@NotNull CommandContext<CommandSource> context) {
        List<OnlinePlayer> players = this.playerTracker.getGlobalPlayersSummary(null, null);

        Map<String, List<OnlinePlayer>> fleetPlayers = new HashMap<>();
        for (OnlinePlayer player : players) {
            CurrentServer server = player.getServer();
            fleetPlayers.computeIfAbsent(server.getFleetName(), k -> new ArrayList<>()).add(player);
        }

        ChatMessages.LIST_FLEET_ALL.send(context.getSource(), new AllFleetsSummary(players.size(), fleetPlayers));
    }

    public record FleetSummary(String fleetId, int playerCount, Map<String, List<OnlinePlayer>> serverPlayers) {}

    private void executeFleet(@NotNull CommandContext<CommandSource> context) {
        String fleetId = context.getArgument("fleetId", String.class);
        List<OnlinePlayer> players = this.playerTracker.getGlobalPlayersSummaryByFleet(Set.of(fleetId));

        Map<String, List<OnlinePlayer>> serverPlayers = createServerPlayers(players);

        ChatMessages.LIST_FLEET.send(context.getSource(), new FleetSummary(fleetId, players.size(), serverPlayers));
    }

    public record AllServersSummary(int playerCount, Map<String, List<OnlinePlayer>> serverPlayers) {}

    private void executeAllServers(@NotNull CommandContext<CommandSource> context) {
        List<OnlinePlayer> players = this.playerTracker.getGlobalPlayersSummary(null, null);

        Map<String, List<OnlinePlayer>> serverPlayers = createServerPlayers(players);

        ChatMessages.LIST_SERVER_ALL.send(context.getSource(), new AllServersSummary(players.size(), serverPlayers));
    }

    public record ServerSummary(String serverId, List<OnlinePlayer> players) {}

    private void executeServer(@NotNull CommandContext<CommandSource> context) {
        String serverId = context.getArgument("serverId", String.class);
        List<OnlinePlayer> players = this.playerTracker.getGlobalPlayersSummaryByServer(serverId);

        ChatMessages.LIST_SERVER.send(context.getSource(), new ServerSummary(serverId, players));
    }

    private Map<String, List<OnlinePlayer>> createServerPlayers(@NotNull List<OnlinePlayer> players) {
        Map<String, List<OnlinePlayer>> serverPlayers = new HashMap<>();
        for (OnlinePlayer player : players) {
            CurrentServer server = player.getServer();
            serverPlayers.computeIfAbsent(server.getServerId(), k -> new ArrayList<>()).add(player);
        }
        return serverPlayers;
    }
}
