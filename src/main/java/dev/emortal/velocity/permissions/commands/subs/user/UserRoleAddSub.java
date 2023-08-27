package dev.emortal.velocity.permissions.commands.subs.user;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import dev.emortal.api.command.CommandExecutor;
import dev.emortal.api.service.permission.AddRoleToPlayerResult;
import dev.emortal.api.service.permission.PermissionService;
import dev.emortal.api.utils.resolvers.PlayerResolver;
import dev.emortal.velocity.lang.ChatMessages;
import dev.emortal.velocity.permissions.PermissionCache;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public final class UserRoleAddSub implements CommandExecutor<CommandSource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserRoleAddSub.class);

    private final PermissionService permissionService;
    private final PermissionCache permissionCache;

    public UserRoleAddSub(@NotNull PermissionService permissionService, @NotNull PermissionCache permissionCache) {
        this.permissionService = permissionService;
        this.permissionCache = permissionCache;
    }

    @Override
    public void execute(@NotNull CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();
        String targetUsername = context.getArgument("username", String.class);
        String roleId = context.getArgument("roleId", String.class);

        PermissionCache.CachedRole role = this.permissionCache.getRole(roleId);
        if (role == null) {
            ChatMessages.ERROR_ROLE_NOT_FOUND.send(source, Component.text(roleId));
            return;
        }

        PlayerResolver.CachedMcPlayer target;
        try {
            target = PlayerResolver.getPlayerData(targetUsername);
        } catch (StatusException exception) {
            LOGGER.error("Failed to get player data for '{}'", targetUsername, exception);
            ChatMessages.GENERIC_ERROR.send(source);
            return;
        }

        if (target == null) {
            ChatMessages.PLAYER_NOT_FOUND.send(source, Component.text(targetUsername));
            return;
        }

        UUID targetId = target.uuid();
        String correctUsername = target.username();

        AddRoleToPlayerResult result;
        try {
            result = this.permissionService.addRoleToPlayer(targetId, roleId);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to add role '{}' to '{}'", roleId, correctUsername, exception);
            ChatMessages.GENERIC_ERROR.send(source);
            return;
        }

        switch (result) {
            case SUCCESS -> {
                PermissionCache.User user = this.permissionCache.getUser(targetId);
                if (user != null) user.roleIds().add(role.id());

                ChatMessages.USER_ROLE_ADDED.send(source, Component.text(roleId), Component.text(correctUsername));
            }
            case PLAYER_NOT_FOUND -> ChatMessages.PLAYER_NOT_FOUND.send(source, Component.text(correctUsername));
            case ROLE_NOT_FOUND -> ChatMessages.ERROR_ROLE_NOT_FOUND.send(source, Component.text(roleId));
            case ALREADY_HAS_ROLE -> ChatMessages.ERROR_USER_ALREADY_HAS_ROLE.send(source, Component.text(correctUsername), Component.text(roleId));
        }
    }
}
