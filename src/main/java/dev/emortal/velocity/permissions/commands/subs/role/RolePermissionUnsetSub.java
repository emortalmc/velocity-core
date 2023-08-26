package dev.emortal.velocity.permissions.commands.subs.role;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import dev.emortal.api.model.permission.Role;
import dev.emortal.api.service.permission.PermissionService;
import dev.emortal.api.service.permission.RoleUpdate;
import dev.emortal.api.service.permission.UpdateRoleResult;
import dev.emortal.velocity.command.ArgumentProvider;
import dev.emortal.velocity.command.EmortalCommandExecutor;
import dev.emortal.velocity.lang.ChatMessages;
import dev.emortal.velocity.permissions.PermissionCache;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RolePermissionUnsetSub implements EmortalCommandExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(RolePermissionUnsetSub.class);

    private final PermissionService permissionService;
    private final PermissionCache permissionCache;

    public RolePermissionUnsetSub(@NotNull PermissionService permissionService, @NotNull PermissionCache permissionCache) {
        this.permissionService = permissionService;
        this.permissionCache = permissionCache;
    }

    @Override
    public void execute(@NotNull CommandSource source, @NotNull ArgumentProvider arguments) {
        String roleId = arguments.getArgument("roleId", String.class);
        String permission = arguments.getArgument("permission", String.class);

        PermissionCache.CachedRole role = this.permissionCache.getRole(roleId);
        if (role == null) {
            ChatMessages.ERROR_ROLE_NOT_FOUND.send(source, Component.text(roleId));
            return;
        }

        if (role.getPermissionState(permission) == Tristate.UNDEFINED) {
            ChatMessages.ERROR_MISSING_PERMISSION.send(source, Component.text(roleId), Component.text(permission));
            return;
        }

        RoleUpdate update = RoleUpdate.builder(roleId).unsetPermission(permission).build();

        UpdateRoleResult result;
        try {
            result = this.permissionService.updateRole(update);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to unset permission '{}' from role '{}'", permission, roleId, exception);
            ChatMessages.GENERIC_ERROR.send(source);
            return;
        }

        switch (result) {
            case UpdateRoleResult.Success(Role newRole) -> {
                this.permissionCache.setRole(newRole);
                ChatMessages.PERMISSION_REMOVED_FROM_ROLE.send(source, Component.text(permission), Component.text(roleId));
            }
            case UpdateRoleResult.Error error -> {
                switch (error) {
                    case ROLE_NOT_FOUND -> ChatMessages.ERROR_ROLE_NOT_FOUND.send(source, Component.text(roleId));
                }
            }
        }
    }
}
