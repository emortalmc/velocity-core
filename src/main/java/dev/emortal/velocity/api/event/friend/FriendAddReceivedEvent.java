package dev.emortal.velocity.api.event.friend;

import java.util.UUID;

public record FriendAddReceivedEvent(UUID senderId, String senderUsername, UUID recipientId) {

}
