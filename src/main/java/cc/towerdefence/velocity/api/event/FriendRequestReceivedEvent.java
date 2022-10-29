package cc.towerdefence.velocity.api.event;

import java.util.UUID;

public record FriendRequestReceivedEvent(UUID senderId, String senderUsername, UUID recipientId) {
}
