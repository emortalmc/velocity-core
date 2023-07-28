package dev.emortal.velocity.relationships.listeners;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.emortal.api.message.relationship.FriendAddedMessage;
import dev.emortal.api.message.relationship.FriendRemovedMessage;
import dev.emortal.api.message.relationship.FriendRequestReceivedMessage;
import dev.emortal.api.model.relationship.FriendRequest;
import dev.emortal.velocity.messaging.MessagingCore;
import dev.emortal.velocity.relationships.FriendCache;
import dev.emortal.velocity.relationships.commands.friend.FriendAddSub;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.UUID;

public final class FriendListener {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String FRIEND_REQUEST_RECEIVED_MESSAGE = "<light_purple>You have received a friend request from <color:#c98fff><sender_username></color> <click:run_command:'/friend add <sender_username>'><green>ACCEPT</click> <reset><gray>| <click:run_command:'/friend deny <sender_username>'><red>DENY</click>";

    private final ProxyServer proxy;
    private final FriendCache friendCache;

    public FriendListener(@NotNull ProxyServer proxy, @NotNull MessagingCore messaging, @NotNull FriendCache friendCache) {
        this.proxy = proxy;
        this.friendCache = friendCache;

        messaging.addListener(FriendRequestReceivedMessage.class, message -> this.onFriendRequestReceived(message.getRequest()));
        messaging.addListener(FriendAddedMessage.class, this::onFriendAdded);
        messaging.addListener(FriendRemovedMessage.class, this::onFriendRemoved);
    }

    private void onFriendRequestReceived(@NotNull FriendRequest request) {
        Player player = this.proxy.getPlayer(UUID.fromString(request.getTargetId())).orElse(null);
        if (player == null) return;

        player.sendMessage(MINI_MESSAGE.deserialize(FRIEND_REQUEST_RECEIVED_MESSAGE,
                Placeholder.parsed("sender_username", request.getSenderUsername())));
    }

    private void onFriendAdded(@NotNull FriendAddedMessage message) {
        UUID recipientId = UUID.fromString(message.getRecipientId());

        Player player = this.proxy.getPlayer(recipientId).orElse(null);
        if (player == null) return;

        player.sendMessage(MINI_MESSAGE.deserialize(FriendAddSub.FRIEND_ADDED_MESSAGE,
                Placeholder.parsed("username", message.getSenderUsername())));

        UUID senderId = UUID.fromString(message.getSenderId());
        this.friendCache.add(recipientId, new FriendCache.CachedFriend(senderId, Instant.now()));
    }

    private void onFriendRemoved(@NotNull FriendRemovedMessage message) {
        UUID senderId = UUID.fromString(message.getSenderId());
        UUID recipientId = UUID.fromString(message.getRecipientId());

        this.friendCache.remove(recipientId, senderId);
        this.friendCache.remove(senderId, recipientId);
    }
}
