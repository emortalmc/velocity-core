package dev.emortal.velocity.permissions.commands.subs.user;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import dev.emortal.api.command.CommandExecutor;
import dev.emortal.api.service.permission.PermissionService;
import dev.emortal.api.service.permission.RemoveRoleFromPlayerResult;
import dev.emortal.api.utils.resolvers.PlayerResolver;
import dev.emortal.velocity.permissions.PermissionCache;
import dev.emortal.velocity.permissions.commands.subs.role.RoleSubUtils;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public final class UserRoleRemoveSub implements CommandExecutor<CommandSource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserRoleRemoveSub.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String ROLE_NOT_FOUND = "<red>Role <role_id> not found";
    private static final String ROLE_REMOVED = "<green>Role <role_id> removed from user <username>";
    private static final String ALREADY_HAS_ROLE = "<red>User <user_id> already has the <role_id> role";
    private static final String DOESNT_HAVE_ROLE = "<red>User <user_id> doesn't have role <role_id>";

    private static final String PERMISSION_PLAYER_NOT_FOUND = "<red>Player <uuid> not found in permission service";

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

        PermissionCache.CachedRole role = RoleSubUtils.getRole(this.permissionCache, context);
        if (role == null) return;

        PlayerResolver.CachedMcPlayer playerData;
        try {
            playerData = PlayerResolver.getPlayerData(targetUsername);
        } catch (StatusException exception) {
            Status status = exception.getStatus();
            var usernamePlaceholder = Placeholder.unparsed("username", targetUsername);
            if (status.getCode() == Status.Code.NOT_FOUND) {
                source.sendMessage(MINI_MESSAGE.deserialize("<red>Player <username> not found", usernamePlaceholder));
            } else {
                source.sendMessage(MINI_MESSAGE.deserialize("<red>Failed to retrieve player data for <username>", usernamePlaceholder));
            }
            return;
        }

        UUID targetId = playerData.uuid();
        String correctUsername = playerData.username();

        RemoveRoleFromPlayerResult result;
        try {
            result = this.permissionService.removeRoleFromPlayer(targetId, roleId);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Something went wrong removing role from user", exception);
            source.sendMessage(MINI_MESSAGE.deserialize("<red>Something went wrong"));
            return;
        }

        var roleIdPlaceholder = Placeholder.unparsed("role_id", roleId);
        var message = switch (result) {
            case SUCCESS -> MINI_MESSAGE.deserialize(ROLE_REMOVED, roleIdPlaceholder, Placeholder.unparsed("username", correctUsername));
            case DOES_NOT_HAVE_ROLE -> MINI_MESSAGE.deserialize(DOESNT_HAVE_ROLE, roleIdPlaceholder);
            case PLAYER_NOT_FOUND -> MINI_MESSAGE.deserialize(PERMISSION_PLAYER_NOT_FOUND, Placeholder.unparsed("uuid", targetId.toString()));
        };
        source.sendMessage(message);
    }
}
