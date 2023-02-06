package dev.emortal.velocity.friends.listeners;

import com.velocitypowered.api.proxy.ProxyServer;
import dev.emortal.api.message.relationship.FriendAddedMessage;
import dev.emortal.api.message.relationship.FriendRemovedMessage;
import dev.emortal.api.message.relationship.FriendRequestReceivedMessage;
import dev.emortal.api.model.relationship.FriendRequest;
import dev.emortal.velocity.friends.FriendCache;
import dev.emortal.velocity.friends.commands.FriendAddSub;
import dev.emortal.velocity.rabbitmq.RabbitMqCore;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.time.Instant;
import java.util.UUID;

public class FriendRabbitMqListener {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String FRIEND_REQUEST_RECEIVED_MESSAGE = "<light_purple>You have received a friend request from <color:#c98fff><sender_username></color> <click:run_command:'/friend add <sender_username>'><green>ACCEPT</click> <reset><gray>| <click:run_command:'/friend deny <sender_username>'><red>DENY</click>";

    public FriendRabbitMqListener(RabbitMqCore core, ProxyServer proxy, FriendCache friendCache) {
        core.setListener(FriendRequestReceivedMessage.class, message -> {
            FriendRequest request = message.getRequest();

            proxy.getPlayer(UUID.fromString(request.getTargetId())).ifPresent(player -> {
                player.sendMessage(MINI_MESSAGE
                        .deserialize(FRIEND_REQUEST_RECEIVED_MESSAGE, Placeholder.parsed("sender_username", request.getSenderUsername())));
            });
        });

        core.setListener(FriendAddedMessage.class, message -> {
            proxy.getPlayer(UUID.fromString(message.getRecipientId())).ifPresent(player -> {
                player.sendMessage(MINI_MESSAGE
                        .deserialize(FriendAddSub.FRIEND_ADDED_MESSAGE, Placeholder.parsed("username", message.getSenderUsername())));

                friendCache.add(
                        UUID.fromString(message.getRecipientId()),
                        new FriendCache.CachedFriend(UUID.fromString(message.getSenderId()), Instant.now())
                );
            });
        });

        core.setListener(FriendRemovedMessage.class, message -> {
            UUID recipientId = UUID.fromString(message.getRecipientId());
            UUID senderId = UUID.fromString(message.getSenderId());

            friendCache.remove(recipientId, senderId);
            friendCache.remove(senderId, recipientId);
        });
    }
}
