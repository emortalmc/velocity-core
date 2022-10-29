package cc.towerdefence.velocity.api.event;

import java.util.UUID;

public record PrivateMessageReceivedEvent(String senderUsername, UUID receiverId, String message) {
}
