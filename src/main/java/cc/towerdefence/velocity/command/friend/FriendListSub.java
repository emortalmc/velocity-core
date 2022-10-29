package cc.towerdefence.velocity.command.friend;

import cc.towerdefence.api.service.FriendGrpc;
import cc.towerdefence.api.service.FriendProto;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ForkJoinPool;

public class FriendListSub {
    private final FriendGrpc.FriendFutureStub friendService;

    public FriendListSub(FriendGrpc.FriendFutureStub friendService) {
        this.friendService = friendService;
    }

    private int execute(CommandContext<CommandSource> context) {
        Integer argPage = context.getArgument("page", Integer.class);
        int page = argPage == null ? 1 : argPage;
        Player player = (Player) context.getSource();

        ListenableFuture<FriendProto.FriendListResponse> responseFuture = this.friendService.getFriendList(
                FriendProto.PlayerRequest.newBuilder().setIssuerId(player.getUniqueId().toString()).build()
        );

        Futures.addCallback(responseFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(FriendProto.FriendListResponse result) {
                player.sendMessage();
            }

            @Override
            public void onFailure(@NotNull Throwable throwable) {

            }
        }, ForkJoinPool.commonPool());
    }
}
