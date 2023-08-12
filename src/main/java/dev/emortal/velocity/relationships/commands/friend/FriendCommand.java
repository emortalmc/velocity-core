package dev.emortal.velocity.relationships.commands.friend;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.emortal.api.grpc.mcplayer.McPlayerProto.SearchPlayersByUsernameRequest.FilterMethod;
import dev.emortal.api.liveconfigparser.configs.gamemode.GameModeCollection;
import dev.emortal.api.service.mcplayer.McPlayerService;
import dev.emortal.api.service.relationship.RelationshipService;
import dev.emortal.velocity.player.UsernameSuggestions;
import dev.emortal.velocity.relationships.FriendCache;
import dev.emortal.velocity.utils.CommandUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.string;

public final class FriendCommand {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final Component HELP_MESSAGE = MINI_MESSAGE.deserialize(
            """
                    <light_purple>------ Friend Help ------
                    <click:run_command:'/friend list'>/friend list</click>
                    <click:suggest_command:'/friend add '>/friend add <name></click>
                    <click:suggest_command:'/friend remove '>/friend remove <name></click>
                    <click:suggest_command:'/friend requests '>/friend requests <incoming/outgoing> [page]</click>
                    <click:suggest_command:'/friend purge requests '>/friend purge requests <incoming/outgoing></click>
                    -----------------------"""//todo purge requests
    );

    private final UsernameSuggestions usernameSuggestions;

    private final FriendAddSub friendAddSub;
    private final FriendDenySubs friendDenySubs;
    private final FriendListSub friendListSub;
    private final FriendRemoveSub friendRemoveSub;
    private final FriendRequestPurgeSub friendRequestPurgeSub;
    private final FriendRequestsSub friendRequestsSub;

    public FriendCommand(@NotNull ProxyServer proxyServer, @NotNull McPlayerService mcPlayerService,
                         @NotNull RelationshipService relationshipService, @NotNull UsernameSuggestions usernameSuggestions,
                         @NotNull FriendCache friendCache, @Nullable GameModeCollection gameModeCollection) {
        this.usernameSuggestions = usernameSuggestions;

        this.friendAddSub = new FriendAddSub(relationshipService, friendCache);
        this.friendDenySubs = new FriendDenySubs(relationshipService);
        this.friendListSub = new FriendListSub(mcPlayerService, friendCache, gameModeCollection);
        this.friendRemoveSub = new FriendRemoveSub(mcPlayerService, relationshipService, friendCache);
        this.friendRequestPurgeSub = new FriendRequestPurgeSub(relationshipService);
        this.friendRequestsSub = new FriendRequestsSub(relationshipService, mcPlayerService);

        proxyServer.getCommandManager().register(this.createCommand());
    }

    private void executeBase(@NotNull CommandContext<CommandSource> context) {
        context.getSource().sendMessage(HELP_MESSAGE);
    }

    private @NotNull BrigadierCommand createCommand() {
        return new BrigadierCommand(
                LiteralArgumentBuilder.<CommandSource>literal("friend")
                        .requires(CommandUtils.isPlayer())
                        .executes(CommandUtils.execute(this::executeBase))
                        .then(LiteralArgumentBuilder.<CommandSource>literal("list")
                                .executes(CommandUtils.execute(this.friendListSub::execute))
                                .then(RequiredArgumentBuilder.<CommandSource, Integer>argument("page", integer(1))
                                        .executes(CommandUtils.execute(this.friendListSub::execute))))
                        .then(LiteralArgumentBuilder.<CommandSource>literal("requests")
                                .then(LiteralArgumentBuilder.<CommandSource>literal("incoming")
                                        .executes(CommandUtils.executeAsync(this.friendRequestsSub::executeIncoming))
                                        .then(RequiredArgumentBuilder.<CommandSource, Integer>argument("page", integer(1))
                                                .executes(CommandUtils.executeAsync(this.friendRequestsSub::executeIncoming))))
                                .then(LiteralArgumentBuilder.<CommandSource>literal("outgoing")
                                        .executes(CommandUtils.executeAsync(this.friendRequestsSub::executeOutgoing))
                                        .then(RequiredArgumentBuilder.<CommandSource, Integer>argument("page", integer(1))
                                                .executes(CommandUtils.executeAsync(this.friendRequestsSub::executeOutgoing)))))
                        .then(LiteralArgumentBuilder.<CommandSource>literal("purge")
                                .then(LiteralArgumentBuilder.<CommandSource>literal("requests")
                                        .then(LiteralArgumentBuilder.<CommandSource>literal("incoming")
                                                .executes(CommandUtils.executeAsync(this.friendRequestPurgeSub::executeIncoming)))
                                        .then(LiteralArgumentBuilder.<CommandSource>literal("outgoing")
                                                .executes(CommandUtils.executeAsync(this.friendRequestPurgeSub::executeOutgoing)))))
                        .then(LiteralArgumentBuilder.<CommandSource>literal("add")
                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("username", string())
                                        .suggests(this.usernameSuggestions.command(FilterMethod.NONE))
                                        .executes(CommandUtils.executeAsync(this.friendAddSub::execute))))
                        .then(LiteralArgumentBuilder.<CommandSource>literal("remove")
                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("username", string())
                                        .suggests(this.usernameSuggestions.command(FilterMethod.FRIENDS))
                                        .executes(CommandUtils.executeAsync(this.friendRemoveSub::execute))))
                        .then(LiteralArgumentBuilder.<CommandSource>literal("deny")
                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("username", string())
                                        .suggests(this.usernameSuggestions.command(FilterMethod.NONE))
                                        .executes(CommandUtils.executeAsync(this.friendDenySubs::executeDeny))))
                        .then(LiteralArgumentBuilder.<CommandSource>literal("revoke")
                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("username", string())
                                        .suggests(this.usernameSuggestions.command(FilterMethod.NONE))
                                        .executes(CommandUtils.executeAsync(this.friendDenySubs::executeRevoke))))
                        .build()
        );
    }
}
