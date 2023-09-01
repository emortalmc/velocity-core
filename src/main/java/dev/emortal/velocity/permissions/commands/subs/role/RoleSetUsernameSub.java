package dev.emortal.velocity.permissions.commands.subs.role;

import com.velocitypowered.api.command.CommandSource;
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

public final class RoleSetUsernameSub implements EmortalCommandExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoleSetUsernameSub.class);

    private final @NotNull PermissionService permissionService;
    private final @NotNull PermissionCache permissionCache;

    public RoleSetUsernameSub(@NotNull PermissionService permissionService, @NotNull PermissionCache permissionCache) {
        this.permissionService = permissionService;
        this.permissionCache = permissionCache;
    }

    @Override
    public void execute(@NotNull CommandSource source, @NotNull ArgumentProvider arguments) {
        String roleId = arguments.getArgument("roleId", String.class).toLowerCase();
        String usernameFormat = arguments.getArgument("usernameFormat", String.class);

        PermissionCache.CachedRole role = this.permissionCache.getRole(roleId);
        if (role == null) {
            ChatMessages.ERROR_ROLE_NOT_FOUND.send(source, Component.text(roleId));
            return;
        }

        RoleUpdate update = RoleUpdate.builder(roleId).displayName(usernameFormat).build();

        UpdateRoleResult result;
        try {
            result = this.permissionService.updateRole(update);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to set username format to '{}' for role '{}'", usernameFormat, roleId, exception);
            ChatMessages.GENERIC_ERROR.send(source);
            return;
        }

        switch (result) {
            case UpdateRoleResult.Success(Role newRole) -> {
                this.permissionCache.setRole(newRole);
                ChatMessages.ROLE_USERNAME_SET.send(source, Component.text(roleId), Component.text(usernameFormat));
            }
            case UpdateRoleResult.Error error -> {
                switch (error) {
                    case ROLE_NOT_FOUND -> ChatMessages.ERROR_ROLE_NOT_FOUND.send(source, Component.text(roleId));
                }
            }
        }
    }
}
