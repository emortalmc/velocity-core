package dev.emortal.velocity.api.event.friend;

import java.util.UUID;

public record FriendRemoveReceivedEvent(UUID recipientId, UUID senderId) {
}
