package dev.emortal.velocity.permissions.commands.subs.user;

import com.velocitypowered.api.command.CommandSource;
import dev.emortal.api.grpc.permission.PermissionProto;
import dev.emortal.api.service.permission.PermissionService;
import dev.emortal.velocity.command.ArgumentProvider;
import dev.emortal.velocity.command.EmortalCommandExecutor;
import dev.emortal.velocity.lang.ChatMessages;
import dev.emortal.velocity.permissions.PermissionCache;
import dev.emortal.velocity.player.resolver.CachedMcPlayer;
import dev.emortal.velocity.player.resolver.PlayerResolver;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class UserDescribeSub implements EmortalCommandExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserDescribeSub.class);

    private final @NotNull PermissionService permissionService;
    private final @NotNull PermissionCache permissionCache;
    private final @NotNull PlayerResolver playerResolver;

    public UserDescribeSub(@NotNull PermissionService permissionService, @NotNull PermissionCache permissionCache,
                           @NotNull PlayerResolver playerResolver) {
        this.permissionService = permissionService;
        this.permissionCache = permissionCache;
        this.playerResolver = playerResolver;
    }

    @Override
    public void execute(@NotNull CommandSource source, @NotNull ArgumentProvider arguments) {
        String targetUsername = arguments.getArgument("username", String.class);

        CachedMcPlayer target;
        try {
            target = this.playerResolver.getPlayer(targetUsername);
        } catch (StatusException exception) {
            LOGGER.error("Failed to get player data for '{}'", targetUsername, exception);
            ChatMessages.GENERIC_ERROR.send(source);
            return;
        }

        if (target == null) {
            ChatMessages.ERROR_USER_NOT_FOUND.send(source, targetUsername);
            return;
        }

        UUID targetId = target.uuid();
        String correctedUsername = target.username();

        PermissionProto.PlayerRolesResponse response;
        try {
            response = this.permissionService.getPlayerRoles(targetId);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to retrieve roles for '{}'", targetId, exception);
            ChatMessages.GENERIC_ERROR.send(source);
            return;
        }

        List<String> roleIds = response.getRoleIdsList();
        List<PermissionCache.CachedRole> sortedRoles = this.sortRolesByWeight(roleIds);

        List<Component> roleComponents = new ArrayList<>();
        for (int i = 0; i < sortedRoles.size(); i++) {
            PermissionCache.CachedRole role = sortedRoles.get(i);
            if (i == 0) {
                roleComponents.add(Component.text(role.id(), Style.style(TextDecoration.BOLD)));
            } else {
                roleComponents.add(Component.text(role.id()));
            }
        }
        Component groupsValue = Component.join(JoinConfiguration.commas(true), roleComponents);
        PermissionCache.CachedRole primaryRole = this.permissionCache.determinePrimaryRole(roleIds);

        TextComponent.Builder exampleChatBuilder = Component.text();

        if (primaryRole != null) {
            exampleChatBuilder.append(primaryRole.formatDisplayName(correctedUsername));
        } else {
            exampleChatBuilder.append(Component.text(correctedUsername));
        }
        exampleChatBuilder.append(Component.text(": Test Chat", NamedTextColor.WHITE));

        int permissionCount = 0;
        for (PermissionCache.CachedRole role : sortedRoles) {
            permissionCount += role.permissions().size();
        }

        String activeDisplayName = primaryRole == null || primaryRole.displayName() == null ? "null" : primaryRole.displayName();
        ChatMessages.USER_DESCRIPTION.send(source, correctedUsername, groupsValue, permissionCount, activeDisplayName, exampleChatBuilder.build());
    }

    private @NotNull List<PermissionCache.CachedRole> sortRolesByWeight(@NotNull List<String> roleIds) {
        return roleIds.stream()
                .map(this.permissionCache::getRole)
                .filter(Objects::nonNull)
                .sorted((a, b) -> Integer.compare(b.priority(), a.priority()))
                .toList();
    }
}
