package dev.emortal.velocity.permissions.commands.subs.role;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import dev.emortal.api.command.CommandExecutor;
import dev.emortal.api.model.permission.Role;
import dev.emortal.api.service.permission.CreateRoleResult;
import dev.emortal.api.service.permission.PermissionService;
import dev.emortal.velocity.lang.ChatMessages;
import dev.emortal.velocity.permissions.PermissionCache;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RoleCreateSub implements CommandExecutor<CommandSource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoleCreateSub.class);

    private final PermissionService permissionService;
    private final PermissionCache permissionCache;

    public RoleCreateSub(@NotNull PermissionService permissionService, @NotNull PermissionCache permissionCache) {
        this.permissionService = permissionService;
        this.permissionCache = permissionCache;
    }

    @Override
    public void execute(@NotNull CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();
        String roleId = context.getArgument("roleId", String.class).toLowerCase();

        CreateRoleResult result;
        try {
            result = this.permissionService.createRole(roleId, 0, "<username>");
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to create role '{}'", roleId, exception);
            ChatMessages.GENERIC_ERROR.send(source);
            return;
        }

        switch (result) {
            case CreateRoleResult.Success(Role role) -> {
                this.permissionCache.setRole(role);
                ChatMessages.ROLE_CREATED.send(source, Component.text(roleId));
            }
            case CreateRoleResult.Error error -> {
                switch (error) {
                    case ROLE_ALREADY_EXISTS -> ChatMessages.ERROR_ROLE_ALREADY_EXISTS.send(source, Component.text(roleId));
                }
            }
        }
    }
}
