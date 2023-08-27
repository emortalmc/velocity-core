package dev.emortal.velocity.permissions.commands.subs.user;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import dev.emortal.api.command.CommandExecutor;
import dev.emortal.api.service.permission.PermissionService;
import dev.emortal.api.service.permission.RemoveRoleFromPlayerResult;
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

public final class UserRoleRemoveSub implements CommandExecutor<CommandSource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserRoleRemoveSub.class);

    private final PermissionService permissionService;
    private final PermissionCache permissionCache;

    public UserRoleRemoveSub(@NotNull PermissionService permissionService, @NotNull PermissionCache permissionCache) {
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

        RemoveRoleFromPlayerResult result;
        try {
            result = this.permissionService.removeRoleFromPlayer(targetId, roleId);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to remove role '{}' from '{}'", roleId, targetUsername, exception);
            ChatMessages.GENERIC_ERROR.send(source);
            return;
        }

        switch (result) {
            case SUCCESS -> ChatMessages.USER_ROLE_REMOVED.send(source, Component.text(roleId), Component.text(correctUsername));
            case DOES_NOT_HAVE_ROLE -> ChatMessages.ERROR_USER_MISSING_ROLE.send(source, Component.text(roleId));
            case PLAYER_NOT_FOUND -> ChatMessages.PLAYER_NOT_FOUND.send(source, Component.text(correctUsername));
        }
    }
}
