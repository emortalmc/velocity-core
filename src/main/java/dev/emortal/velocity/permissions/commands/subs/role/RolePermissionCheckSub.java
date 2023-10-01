package dev.emortal.velocity.permissions.commands.subs.role;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import dev.emortal.velocity.command.ArgumentProvider;
import dev.emortal.velocity.command.EmortalCommandExecutor;
import dev.emortal.velocity.lang.ChatMessages;
import dev.emortal.velocity.permissions.PermissionCache;
import org.jetbrains.annotations.NotNull;

public final class RolePermissionCheckSub implements EmortalCommandExecutor {

    private final @NotNull PermissionCache permissionCache;

    public RolePermissionCheckSub(@NotNull PermissionCache permissionCache) {
        this.permissionCache = permissionCache;
    }

    @Override
    public void execute(@NotNull CommandSource source, @NotNull ArgumentProvider arguments) {
        String roleId = arguments.getArgument("roleId", String.class);
        String permission = arguments.getArgument("permission", String.class);

        PermissionCache.CachedRole role = this.permissionCache.getRole(roleId);
        if (role == null) {
            ChatMessages.ERROR_ROLE_NOT_FOUND.send(source, roleId);
            return;
        }

        Tristate permissionState = role.getPermissionState(permission);
        ChatMessages.PERMISSION_STATE.send(source, roleId, permission, permissionState);
    }
}
