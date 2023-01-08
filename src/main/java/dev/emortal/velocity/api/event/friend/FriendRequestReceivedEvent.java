package dev.emortal.velocity.api.event.friend;

import java.util.UUID;

public record FriendRequestReceivedEvent(UUID senderId, String senderUsername, UUID recipientId) {
}
