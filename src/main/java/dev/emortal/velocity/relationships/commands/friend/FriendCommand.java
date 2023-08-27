package dev.emortal.velocity.relationships.commands.friend;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import dev.emortal.api.grpc.mcplayer.McPlayerProto.SearchPlayersByUsernameRequest.FilterMethod;
import dev.emortal.api.liveconfigparser.configs.gamemode.GameModeCollection;
import dev.emortal.api.service.mcplayer.McPlayerService;
import dev.emortal.api.service.relationship.RelationshipService;
import dev.emortal.velocity.command.CommandConditions;
import dev.emortal.velocity.command.EmortalCommand;
import dev.emortal.velocity.lang.ChatMessages;
import dev.emortal.velocity.player.UsernameSuggestions;
import dev.emortal.velocity.relationships.FriendCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class FriendCommand extends EmortalCommand {

    public FriendCommand(@NotNull McPlayerService mcPlayerService, @NotNull RelationshipService relationshipService,
                         @NotNull UsernameSuggestions usernameSuggestions, @NotNull FriendCache friendCache,
                         @Nullable GameModeCollection gameModeCollection) {
        super("friend");

        super.setCondition(CommandConditions.playerOnly());
        super.setDefaultExecutor(this::sendHelp);

        var pageArgument = argument("page", IntegerArgumentType.integer(1), null);
        var usernameArgument = argument("username", StringArgumentType.string(), usernameSuggestions.command(FilterMethod.NONE));
        var friendsArgument = argument("username", StringArgumentType.string(), usernameSuggestions.command(FilterMethod.FRIENDS));

        var listSub = new FriendListSub(mcPlayerService, friendCache, gameModeCollection);
        super.addSyntax(listSub, literal("list"));
        super.addSyntax(listSub, literal("list"), pageArgument);

        var requestsSub = new FriendRequestsSub(relationshipService, mcPlayerService);
        super.addSyntax(requestsSub::executeIncoming, literal("requests"), literal("incoming"));
        super.addSyntax(requestsSub::executeIncoming, literal("requests"), literal("incoming"), pageArgument);
        super.addSyntax(requestsSub::executeOutgoing, literal("requests"), literal("outgoing"));
        super.addSyntax(requestsSub::executeOutgoing, literal("requests"), literal("outgoing"), pageArgument);

        var purgeSub = new FriendRequestPurgeSub(relationshipService);
        super.addSyntax(purgeSub::executeIncoming, literal("purge"), literal("requests"), literal("incoming"));
        super.addSyntax(purgeSub::executeOutgoing, literal("purge"), literal("requests"), literal("outgoing"));

        super.addSyntax(new FriendAddSub(relationshipService, friendCache), literal("add"), usernameArgument);
        super.addSyntax(new FriendRemoveSub(mcPlayerService, relationshipService, friendCache), literal("remove"), friendsArgument);

        var denySubs = new FriendDenySubs(relationshipService);
        super.addSyntax(denySubs::executeDeny, literal("deny"), usernameArgument);
        super.addSyntax(denySubs::executeRevoke, literal("revoke"), usernameArgument);
    }

    private void sendHelp(@NotNull CommandContext<CommandSource> context) {
        ChatMessages.FRIEND_HELP.send(context.getSource());
    }
}
