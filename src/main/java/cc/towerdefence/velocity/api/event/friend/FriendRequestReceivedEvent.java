package cc.towerdefence.velocity.api.event.friend;

import java.util.UUID;

public record FriendRequestReceivedEvent(UUID senderId, String senderUsername, UUID recipientId) {
}
