package dev.emortal.velocity.relationships.commands.friend;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import dev.emortal.api.liveconfigparser.configs.gamemode.GameModeCollection;
import dev.emortal.api.service.mcplayer.McPlayerService;
import dev.emortal.api.service.relationship.RelationshipService;
import dev.emortal.velocity.command.CommandConditions;
import dev.emortal.velocity.command.EmortalCommand;
import dev.emortal.velocity.lang.ChatMessages;
import dev.emortal.velocity.player.resolver.PlayerResolver;
import dev.emortal.velocity.player.suggestions.UsernameSuggesterProvider;
import dev.emortal.velocity.relationships.FriendCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class FriendCommand extends EmortalCommand {

    public FriendCommand(@NotNull RelationshipService relationshipService, @NotNull McPlayerService playerService,
                         @NotNull PlayerResolver playerResolver, @NotNull UsernameSuggesterProvider usernameSuggesters,
                         @NotNull FriendCache friendCache, @Nullable GameModeCollection gameModeCollection) {
        super("friend");

        super.setCondition(CommandConditions.playerOnly());
        super.setDefaultExecutor(this::sendHelp);

        var pageArgument = argument("page", IntegerArgumentType.integer(1), null);
        var usernameArgument = argument("username", StringArgumentType.string(), usernameSuggesters.all());
        var friendsArgument = argument("username", StringArgumentType.string(), usernameSuggesters.friends());

        var listSub = new FriendListSub(playerService, friendCache, gameModeCollection);
        super.addSyntax(listSub, literal("list"));
        super.addSyntax(listSub, literal("list"), pageArgument);

        var requestsSub = new FriendRequestsSub(relationshipService, playerService);
        super.addSyntax(requestsSub::executeIncoming, literal("requests"), literal("incoming"));
        super.addSyntax(requestsSub::executeIncoming, literal("requests"), literal("incoming"), pageArgument);
        super.addSyntax(requestsSub::executeOutgoing, literal("requests"), literal("outgoing"));
        super.addSyntax(requestsSub::executeOutgoing, literal("requests"), literal("outgoing"), pageArgument);

        var purgeSub = new FriendRequestPurgeSub(relationshipService);
        super.addSyntax(purgeSub::executeIncoming, literal("purge"), literal("requests"), literal("incoming"));
        super.addSyntax(purgeSub::executeOutgoing, literal("purge"), literal("requests"), literal("outgoing"));

        super.addSyntax(new FriendAddSub(relationshipService, friendCache, playerResolver), literal("add"), usernameArgument);
        super.addSyntax(new FriendRemoveSub(relationshipService, friendCache, playerResolver), literal("remove"), friendsArgument);

        var denySubs = new FriendDenySubs(relationshipService, playerResolver);
        super.addSyntax(denySubs::executeDeny, literal("deny"), usernameArgument);
        super.addSyntax(denySubs::executeRevoke, literal("revoke"), usernameArgument);
    }

    private void sendHelp(@NotNull CommandContext<CommandSource> context) {
        ChatMessages.FRIEND_HELP.send(context.getSource());
    }
}
