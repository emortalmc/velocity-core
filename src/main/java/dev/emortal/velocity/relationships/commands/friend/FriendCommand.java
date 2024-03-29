package dev.emortal.velocity.relationships.commands.friend;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.emortal.api.liveconfigparser.configs.ConfigProvider;
import dev.emortal.api.liveconfigparser.configs.gamemode.GameModeConfig;
import dev.emortal.api.service.mcplayer.McPlayerService;
import dev.emortal.api.service.relationship.RelationshipService;
import dev.emortal.velocity.command.EmortalCommand;
import dev.emortal.velocity.lang.ChatMessages;
import dev.emortal.velocity.player.resolver.PlayerResolver;
import dev.emortal.velocity.player.suggestions.UsernameSuggesterProvider;
import dev.emortal.velocity.relationships.FriendCache;
import dev.emortal.velocity.utils.CommandUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class FriendCommand extends EmortalCommand {

    public FriendCommand(@NotNull RelationshipService relationshipService, @NotNull McPlayerService playerService,
                         @NotNull PlayerResolver playerResolver, @NotNull UsernameSuggesterProvider usernameSuggesters,
                         @NotNull FriendCache friendCache, @Nullable ConfigProvider<GameModeConfig> gameModes) {
        super("friend", "f");

        super.setPlayerOnly();
        super.setDefaultExecutor(context -> ChatMessages.FRIEND_HELP.send(context.getSource(), CommandUtils.getCommandName(context.getInput())));

        var pageArgument = argument("page", IntegerArgumentType.integer(1), null);
        var usernameArgument = argument("username", StringArgumentType.string(), usernameSuggesters.all());
        var friendsArgument = argument("username", StringArgumentType.string(), usernameSuggesters.friends());

        var listSub = new FriendListSub(playerService, friendCache, gameModes);
        super.addSyntax(listSub, literal("list"));
        super.addSyntax(listSub, literal("list"), pageArgument);

        FriendRequestsSub incomingRequestsSub = FriendRequestsSub.incoming(relationshipService, playerService);
        super.addSyntax(incomingRequestsSub, literal("requests"), literal("incoming"));
        super.addSyntax(incomingRequestsSub, literal("requests"), literal("incoming"), pageArgument);

        FriendRequestsSub outgoingRequestsSub = FriendRequestsSub.outgoing(relationshipService, playerService);
        super.addSyntax(outgoingRequestsSub, literal("requests"), literal("outgoing"));
        super.addSyntax(outgoingRequestsSub, literal("requests"), literal("outgoing"), pageArgument);

        super.addSyntax(FriendRequestPurgeSub.incoming(relationshipService), literal("purge"), literal("requests"), literal("incoming"));
        super.addSyntax(FriendRequestPurgeSub.outgoing(relationshipService), literal("purge"), literal("requests"), literal("outgoing"));

        super.addSyntax(new FriendAddSub(relationshipService, friendCache, playerResolver), literal("add"), usernameArgument);
        super.addSyntax(new FriendRemoveSub(relationshipService, friendCache, playerResolver), literal("remove"), friendsArgument);

        super.addSyntax(FriendDenySub.deny(relationshipService, playerResolver), literal("deny"), usernameArgument);
        super.addSyntax(FriendDenySub.revoke(relationshipService, playerResolver), literal("revoke"), usernameArgument);
    }
}
