package dev.emortal.velocity.permissions.commands.subs.user;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import dev.emortal.api.grpc.permission.PermissionProto;
import dev.emortal.api.service.permission.PermissionService;
import dev.emortal.api.utils.resolvers.PlayerResolver;
import dev.emortal.velocity.permissions.PermissionCache;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class UserDescribeSub {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserDescribeSub.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String USER_NOT_FOUND = "<red>User <user_id> not found";
    private static final String USER_DESCRIPTION = """
            <light_purple>----- User Summary (<username>) -----
            Groups: <groups>
            Permissions: <permission_count>
            Display Name: <group_display_name>
            Example Chat: <reset><example_chat>
            <light_purple>-----------<footer_addon>-------------""";

    private final PermissionService permissionService;
    private final PermissionCache permissionCache;

    public UserDescribeSub(@NotNull PermissionService permissionService, @NotNull PermissionCache permissionCache) {
        this.permissionService = permissionService;
        this.permissionCache = permissionCache;
    }

    public void execute(@NotNull CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();
        String targetUsername = context.getArgument("username", String.class);

        PlayerResolver.CachedMcPlayer playerData;
        try {
            playerData = PlayerResolver.getPlayerData(targetUsername);
        } catch (StatusException exception) {
            Status status = exception.getStatus();
            if (status.getCode() == Status.Code.NOT_FOUND) {
                source.sendMessage(MINI_MESSAGE.deserialize(USER_NOT_FOUND, Placeholder.unparsed("user_id", targetUsername)));
            } else {
                LOGGER.error("Failed to retrieve player data for user {}", targetUsername, status.asRuntimeException());
            }
            return;
        }

        UUID targetId = playerData.uuid();
        String correctedUsername = playerData.username();

        PermissionProto.PlayerRolesResponse response;
        try {
            response = this.permissionService.getPlayerRoles(targetId);
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to retrieve roles for user {}", targetId, exception);
            source.sendMessage(Component.text("An error occurred retrieving roles", NamedTextColor.RED));
            return;
        }

        List<String> roleIds = response.getRoleIdsList();
        List<PermissionCache.CachedRole> sortedRoles = this.sortRolesByWeight(roleIds);

        List<Component> roleComponents = new ArrayList<>();
        for (int i = 0; i < sortedRoles.size(); i++) {
            PermissionCache.CachedRole role = sortedRoles.get(i);
            if (i == 0) {
                roleComponents.add(Component.text(role.getId(), Style.style(TextDecoration.BOLD)));
            } else {
                roleComponents.add(Component.text(role.getId()));
            }
        }
        Component groupsValue = Component.join(JoinConfiguration.commas(true), roleComponents);
        String activeDisplayName = this.permissionCache.determineActiveName(roleIds);

        TextComponent.Builder exampleChatBuilder = Component.text();

        if (activeDisplayName != null) {
            exampleChatBuilder.append(MINI_MESSAGE.deserialize(activeDisplayName, Placeholder.unparsed("username", correctedUsername)));
        } else {
            exampleChatBuilder.append(Component.text(correctedUsername));
        }
        exampleChatBuilder.append(Component.text(": Test Chat", NamedTextColor.WHITE));

        int permissionCount = 0;
        for (PermissionCache.CachedRole role : sortedRoles) {
            permissionCount += role.getPermissions().size();
        }

        source.sendMessage(MINI_MESSAGE.deserialize(USER_DESCRIPTION,
                Placeholder.unparsed("username", correctedUsername),
                Placeholder.component("groups", groupsValue),
                Placeholder.unparsed("permission_count", String.valueOf(permissionCount)),
                Placeholder.unparsed("group_display_name", activeDisplayName == null ? "null" : activeDisplayName),
                Placeholder.component("example_chat", exampleChatBuilder.build()),
                Placeholder.unparsed("footer_addon", this.createFooterAddon(correctedUsername))
        ));
    }

    private @NotNull List<PermissionCache.CachedRole> sortRolesByWeight(@NotNull List<String> roleIds) {
        return roleIds.stream()
                .map(this.permissionCache::getRole)
                .filter(Objects::nonNull)
                .sorted((a, b) -> Integer.compare(b.priority(), a.priority()))
                .toList();
    }

    private @NotNull String createFooterAddon(@NotNull String headerText) {
        char[] footerAddon = new char[headerText.length()];
        Arrays.fill(footerAddon, '-');
        return new String(footerAddon);
    }
}
