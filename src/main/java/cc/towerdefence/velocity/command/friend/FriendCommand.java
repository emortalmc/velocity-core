package cc.towerdefence.velocity.command.friend;

import cc.towerdefence.api.service.FriendGrpc;
import cc.towerdefence.api.service.FriendProto;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ForkJoinPool;

public class FriendCommand {
    private static final Component HELP_MESSAGE = MiniMessage.miniMessage().deserialize(
            """
                    <yellow>----- Friend Help -----
                    <click:suggest_command:'/friend add '>/friend add <name></click>
                    <click:suggest_command:'/friend remove '>/friend remove <name></click>
                    <click:suggest_command:'/friend list'>/friend list</click>
                    <click:suggest_command:'/friend pending'>/friend pending</click>
                    ---------------------"""
    );

    private final FriendGrpc.FriendFutureStub friendService;

    public FriendCommand(ProxyServer proxyServer) {
        proxyServer.getCommandManager().register(this.createCommand());

        ManagedChannel channel = ManagedChannelBuilder.forAddress("friend-manager.towerdefence.svc", 9090)
                .defaultLoadBalancingPolicy("round_robin")
                .usePlaintext()
                .build();

        this.friendService = FriendGrpc.newFutureStub(channel);
    }

    private int executeBase(CommandContext<CommandSource> context) {
        context.getSource().sendMessage(HELP_MESSAGE);
        return 1;
    }

    private int executePending(CommandContext<CommandSource> context) {
        Integer argPage = context.getArgument("page", Integer.class);
        int page = argPage == null ? 1 : argPage;

        return -1; // todo
    }

    private int executeAddFriend(CommandContext<CommandSource> context) {

        return -1; // todo
    }

    private int executeRemoveFriend(CommandContext<CommandSource> context) {

        return -1; // todo
    }

    private BrigadierCommand createCommand() {
        return new BrigadierCommand(
                LiteralArgumentBuilder.<CommandSource>literal("friend")
                        .requires(source -> source instanceof Player)
                        .executes(this::executeBase)
                        .then(LiteralArgumentBuilder.<CommandSource>literal("list")
//                                .executes(this::executeList)
//                                .then(RequiredArgumentBuilder.<CommandSource, Integer>argument("page", IntegerArgumentType.integer(1))
//                                        .executes(this::executeList))
                        )
                        .then(LiteralArgumentBuilder.<CommandSource>literal("pending")
                                .executes(this::executePending)
                                .then(RequiredArgumentBuilder.<CommandSource, Integer>argument("page", IntegerArgumentType.integer(1))
                                        .executes(this::executePending))
                        )
                        .then(LiteralArgumentBuilder.<CommandSource>literal("add")
                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("username", StringArgumentType.string())
                                        .executes(this::executeAddFriend)
                                )
                        )
                        .then(LiteralArgumentBuilder.<CommandSource>literal("remove")
                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("username", StringArgumentType.string())
                                        .executes(this::executeRemoveFriend)
                                )
                        )
                        .build()
        );
    }
}
