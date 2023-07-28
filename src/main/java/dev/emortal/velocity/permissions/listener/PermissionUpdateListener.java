package dev.emortal.velocity.permissions.listener;

import dev.emortal.api.message.permission.PlayerRolesUpdateMessage;
import dev.emortal.api.message.permission.RoleUpdateMessage;
import dev.emortal.api.model.permission.Role;
import dev.emortal.velocity.messaging.MessagingCore;
import dev.emortal.velocity.permissions.PermissionCache;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public final class PermissionUpdateListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionUpdateListener.class);

    private final PermissionCache cache;

    public PermissionUpdateListener(@NotNull PermissionCache cache, @NotNull MessagingCore messagingCore) {
        this.cache = cache;

        messagingCore.addListener(RoleUpdateMessage.class, message -> this.onRoleUpdate(message.getRole(), message.getChangeType()));
        messagingCore.addListener(PlayerRolesUpdateMessage.class, message ->
                this.onPlayerRolesUpdate(message.getPlayerId(), message.getRoleId(), message.getChangeType()));
    }

    private void onRoleUpdate(@NotNull Role role, @NotNull RoleUpdateMessage.ChangeType changeType) {
        switch (changeType) {
            // modify could be more efficient and just update what's changed but that's overcomplicated
            case CREATE, MODIFY -> this.cache.setRole(role);
            case DELETE -> {
                boolean result = this.cache.removeRole(role.getId());
                if (!result) LOGGER.warn("Failed to remove role {} from cache", role.getId());
            }
            default -> LOGGER.warn("Unknown change type: {}", changeType);
        }
    }

    private void onPlayerRolesUpdate(@NotNull String playerId, @NotNull String roleId, @NotNull PlayerRolesUpdateMessage.ChangeType changeType) {
        UUID uuid = UUID.fromString(playerId);

        PermissionCache.User user = this.cache.getUser(uuid);
        if (user == null) return;

        switch (changeType) {
            case ADD -> user.roleIds().add(roleId);
            case REMOVE -> user.roleIds().remove(roleId);
        }
    }
}
