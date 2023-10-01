package dev.emortal.velocity.permissions.commands.subs.user;

import com.velocitypowered.api.command.CommandSource;
import dev.emortal.api.service.permission.AddRoleToPlayerResult;
import dev.emortal.api.service.permission.PermissionService;
import dev.emortal.velocity.command.ArgumentProvider;
import dev.emortal.velocity.command.EmortalCommandExecutor;
import dev.emortal.velocity.lang.ChatMessages;
import dev.emortal.velocity.permissions.PermissionCache;
import dev.emortal.velocity.player.resolver.CachedMcPlayer;
import dev.emortal.velocity.player.resolver.PlayerResolver;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public final class UserRoleAddSub implements EmortalCommandExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserRoleAddSub.class);

    private final @NotNull PermissionService permissionService;
    private final @NotNull PermissionCache permissionCache;
    private final @NotNull PlayerResolver playerResolver;

    public UserRoleAddSub(@NotNull PermissionService permissionService, @NotNull PermissionCache permissionCache,
                          @NotNull PlayerResolver playerResolver) {
        this.permissionService = permissionService;
        this.permissionCache = permissionCache;
        this.playerResolver = playerResolver;
    }

    @Override
    public void execute(@NotNull CommandSource source, @NotNull ArgumentProvider arguments) {
        String targetUsername = arguments.getArgument("username", String.class);
        String roleId = arguments.getArgument("roleId", String.class);

        PermissionCache.CachedRole role = this.permissionCache.getRole(roleId);
        if (role == null) {
            ChatMessages.ERROR_ROLE_NOT_FOUND.send(source, roleId);
            return;
        }

        CachedMcPlayer target;
        try {
            target = this.playerResolver.getPlayer(targetUsername);
        } catch (StatusException exception) {
            LOGGER.error("Failed to get player data for '{}'", targetUsername, exception);
            ChatMessages.GENERIC_ERROR.send(source);
            return;
        }

        if (target == null) {
            ChatMessages.PLAYER_NOT_FOUND.send(source, targetUsername);
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

                ChatMessages.USER_ROLE_ADDED.send(source, roleId, correctUsername);
            }
            case PLAYER_NOT_FOUND -> ChatMessages.PLAYER_NOT_FOUND.send(source, correctUsername);
            case ROLE_NOT_FOUND -> ChatMessages.ERROR_ROLE_NOT_FOUND.send(source, roleId);
            case ALREADY_HAS_ROLE -> ChatMessages.ERROR_USER_ALREADY_HAS_ROLE.send(source, correctUsername, roleId);
        }
    }
}
