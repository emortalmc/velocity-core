package cc.towerdefence.velocity.friends.commands;

import cc.towerdefence.api.service.FriendGrpc;
import cc.towerdefence.api.service.McPlayerGrpc;
import cc.towerdefence.velocity.friends.FriendCache;
import cc.towerdefence.velocity.grpc.stub.GrpcStubManager;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FriendCommand {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Logger LOGGER = LoggerFactory.getLogger(FriendCommand.class);

    private static final Component HELP_MESSAGE = MINI_MESSAGE.deserialize(
            """
                    <light_purple>------ Friend Help ------
                    <click:suggest_command:'/friend add '>/friend add <name></click>
                    <click:suggest_command:'/friend remove '>/friend remove <name></click>
                    <click:suggest_command:'/friend deny '>/friend deny <name></click>
                    <click:suggest_command:'/friend requests '>/friend requests <incoming/outgoing> [page]</click>
                    <click:suggest_command:'/friend purgerequests '>/friend purgerequests <incoming/outgoing></click>
                    <click:suggest_command:'/friend pending'>/friend pending</click>
                    -----------------------"""//todo purge requests
    );

    private final FriendGrpc.FriendFutureStub friendService;
    private final McPlayerGrpc.McPlayerFutureStub mcPlayerService;

    private final FriendListSub friendListSub;
    private final FriendAddSub friendAddSub;
    private final FriendRemoveSub friendRemoveSub;
    private final FriendDenySubs friendDenySubs;
    private final FriendRequestsSub friendRequestsSub;

    public FriendCommand(ProxyServer proxyServer, FriendCache friendCache, GrpcStubManager stubManager) {
        this.friendListSub = new FriendListSub(stubManager.getMcPlayerService(), stubManager.getPlayerTrackerService(), friendCache);
        this.friendAddSub = new FriendAddSub(stubManager.getMcPlayerService(), stubManager.getFriendService(), friendCache);
        this.friendRemoveSub = new FriendRemoveSub(stubManager.getMcPlayerService(), stubManager.getFriendService(), friendCache);
        this.friendDenySubs = new FriendDenySubs(stubManager.getMcPlayerService(), stubManager.getFriendService());
        this.friendRequestsSub = new FriendRequestsSub(stubManager.getFriendService(), stubManager.getMcPlayerService());

        proxyServer.getCommandManager().register(this.createCommand());

        this.friendService = stubManager.getFriendService();
        this.mcPlayerService = stubManager.getMcPlayerService();
    }

    private int executeBase(CommandContext<CommandSource> context) {
        context.getSource().sendMessage(HELP_MESSAGE);
        return 1;
    }

    private BrigadierCommand createCommand() {
        return new BrigadierCommand(
                LiteralArgumentBuilder.<CommandSource>literal("friend")
                        .requires(source -> source instanceof Player)
                        .executes(this::executeBase)
                        .then(LiteralArgumentBuilder.<CommandSource>literal("list")
                                .executes(this.friendListSub::execute)
                                .then(RequiredArgumentBuilder.<CommandSource, Integer>argument("page", IntegerArgumentType.integer(1))
                                        .executes(this.friendListSub::execute)
                                ))
                        .then(LiteralArgumentBuilder.<CommandSource>literal("requests")
                                .then(LiteralArgumentBuilder.<CommandSource>literal("incoming")
                                        .executes(this.friendRequestsSub::executeIncoming)
                                        .then(RequiredArgumentBuilder.<CommandSource, Integer>argument("page", IntegerArgumentType.integer(1))
                                                .executes(this.friendRequestsSub::executeIncoming)
                                        ))
                                .then(LiteralArgumentBuilder.<CommandSource>literal("outgoing")
                                        .executes(this.friendRequestsSub::executeOutgoing)
                                        .then(RequiredArgumentBuilder.<CommandSource, Integer>argument("page", IntegerArgumentType.integer(1))
                                                .executes(this.friendRequestsSub::executeOutgoing)
                                        ))
                        )
                        .then(LiteralArgumentBuilder.<CommandSource>literal("add")
                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("username", StringArgumentType.string())
                                        .executes(this.friendAddSub::execute)
                                )
                        )
                        .then(LiteralArgumentBuilder.<CommandSource>literal("remove")
                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("username", StringArgumentType.string())
                                        .executes(this.friendRemoveSub::execute)
                                )
                        )
                        .then(LiteralArgumentBuilder.<CommandSource>literal("deny")
                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("username", StringArgumentType.string())
                                        .executes(this.friendDenySubs::executeDeny)
                                )
                        )
                        .then(LiteralArgumentBuilder.<CommandSource>literal("revoke")
                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("username", StringArgumentType.string())
                                        .executes(this.friendDenySubs::executeRevoke)
                                )
                        )
                        .build()
        );
    }
}
