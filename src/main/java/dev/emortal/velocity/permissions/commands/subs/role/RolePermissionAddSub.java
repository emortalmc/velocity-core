package dev.emortal.velocity.permissions.commands.subs.role;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import dev.emortal.api.model.permission.PermissionNode;
import dev.emortal.api.model.permission.PermissionNode.PermissionState;
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

public final class RolePermissionAddSub implements EmortalCommandExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(RolePermissionAddSub.class);

    private final PermissionService permissionService;
    private final PermissionCache permissionCache;

    public RolePermissionAddSub(@NotNull PermissionService permissionService, @NotNull PermissionCache permissionCache) {
        this.permissionService = permissionService;
        this.permissionCache = permissionCache;
    }

    @Override
    public void execute(@NotNull CommandSource source, @NotNull ArgumentProvider arguments) {
        String roleId = arguments.getArgument("roleId", String.class);
        String permission = arguments.getArgument("permission", String.class);
        boolean newValue = arguments.getArgument("value", Boolean.class);
        Tristate newState = Tristate.fromBoolean(newValue);

        PermissionCache.CachedRole role = this.permissionCache.getRole(roleId);
        if (role == null) {
            ChatMessages.ERROR_ROLE_NOT_FOUND.send(source, Component.text(roleId));
            return;
        }

        Tristate oldState = role.getPermissionState(permission);
        if (oldState == newState) {
            ChatMessages.ERROR_PERMISSION_ALREADY_SET.send(source, Component.text(roleId), Component.text(permission), Component.text(newValue));
            return;
        }

        RoleUpdate.Builder updateBuilder = RoleUpdate.builder(roleId)
                .setPermission(PermissionNode.newBuilder()
                        .setState(newState == Tristate.TRUE ? PermissionState.ALLOW : PermissionState.DENY)
                        .setNode(permission)
                        .build());
        if (oldState != Tristate.UNDEFINED) updateBuilder.unsetPermission(permission);
        RoleUpdate update = updateBuilder.build();

        UpdateRoleResult result;
        try {
            result = this.permissionService.updateRole(update);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to set permission '{}' on role '{}' to '{}'", permission, roleId, newValue, exception);
            ChatMessages.GENERIC_ERROR.send(source);
            return;
        }

        switch (result) {
            case UpdateRoleResult.Success(Role newRole) -> {
                this.permissionCache.setRole(newRole);
                ChatMessages.PERMISSION_ADDED_TO_ROLE.send(source, Component.text(roleId), Component.text(permission), Component.text(newValue));
            }
            case UpdateRoleResult.Error error -> {
                switch (error) {
                    case ROLE_NOT_FOUND -> ChatMessages.ERROR_ROLE_NOT_FOUND.send(source, Component.text(roleId));
                }
            }
        }
    }
}
