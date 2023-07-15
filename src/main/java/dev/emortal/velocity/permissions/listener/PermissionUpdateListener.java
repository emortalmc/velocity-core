package dev.emortal.velocity.permissions.listener;

import dev.emortal.api.message.permission.PlayerRolesUpdateMessage;
import dev.emortal.api.message.permission.RoleUpdateMessage;
import dev.emortal.velocity.messaging.MessagingCore;
import dev.emortal.velocity.permissions.PermissionCache;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class PermissionUpdateListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionUpdateListener.class);

    public PermissionUpdateListener(@NotNull PermissionCache cache, @NotNull MessagingCore messagingCore) {
        messagingCore.addListener(RoleUpdateMessage.class, message -> {
            switch (message.getChangeType()) {
                // modify could be more efficient and just update what's changed but that's overcomplicated
                case CREATE, MODIFY -> cache.setRole(message.getRole());
                case DELETE -> {
                    boolean result = cache.removeRole(message.getRole().getId());
                    if (!result) LOGGER.warn("Failed to remove role {} from cache", message.getRole().getId());
                }
                default -> LOGGER.warn("Unknown change type: {}", message.getChangeType());
            }
        });

        messagingCore.addListener(PlayerRolesUpdateMessage.class, message -> {
            UUID uuid = UUID.fromString(message.getPlayerId());

            PermissionCache.User user = cache.getUser(uuid);
            if (user == null) return;

            switch (message.getChangeType()) {
                case ADD -> user.getRoleIds().add(message.getRoleId());
                case REMOVE -> user.getRoleIds().remove(message.getRoleId());
            }
        });
    }
}
