package dev.emortal.velocity.friends.listeners;

import dev.emortal.velocity.api.event.friend.FriendAddReceivedEvent;
import dev.emortal.velocity.friends.FriendCache;
import dev.emortal.velocity.friends.commands.FriendAddSub;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.time.Instant;

public class FriendAddListener {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final FriendCache friendCache;
    private final ProxyServer proxy;

    public FriendAddListener(FriendCache friendCache, ProxyServer proxy) {
        this.friendCache = friendCache;
        this.proxy = proxy;
    }

    @Subscribe
    public void onFriendAddReceived(FriendAddReceivedEvent event) {
        this.proxy.getPlayer(event.recipientId()).ifPresent(player -> {
            player.sendMessage(MINI_MESSAGE.deserialize(FriendAddSub.FRIEND_ADDED_MESSAGE, Placeholder.parsed("username", event.senderUsername())));
        });

        this.friendCache.add(event.recipientId(), new FriendCache.CachedFriend(event.senderId(), Instant.now()));
    }
}
