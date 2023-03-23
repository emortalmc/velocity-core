package dev.emortal.velocity.permissions.listener;

import dev.emortal.api.message.permission.RoleUpdateMessage;
import dev.emortal.velocity.messaging.MessagingCore;
import dev.emortal.velocity.permissions.PermissionCache;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PermissionUpdateListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionUpdateListener.class);

    public PermissionUpdateListener(PermissionCache cache, @NotNull MessagingCore messagingCore) {
        messagingCore.setListener(RoleUpdateMessage.class, message -> {
            switch (message.getChangeType()) {
                case CREATE -> cache.addRole(message.getRole());
                case DELETE -> {
                    boolean result = cache.removeRole(message.getRole().getId());
                    if (!result) LOGGER.warn("Failed to remove role {} from cache", message.getRole().getId());
                }
                case MODIFY -> {
                    // TODO: Implement
                }
                default -> LOGGER.warn("Unknown change type: {}", message.getChangeType());
            }
        });
    }
}
