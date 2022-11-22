package cc.towerdefence.velocity.permissions.commands.subs.user;

import cc.towerdefence.api.service.PermissionProto;
import cc.towerdefence.api.service.PermissionServiceGrpc;
import cc.towerdefence.api.utils.resolvers.PlayerResolver;
import cc.towerdefence.api.utils.utils.FunctionalFutureCallback;
import cc.towerdefence.velocity.permissions.PermissionCache;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import io.grpc.Status;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;

public class UserDescribeSub {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserDescribeSub.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String USER_NOT_FOUND = "<red>User <user_id> not found";
    private static final String USER_DESCRIPTION = """
            <light_purple>----- User Summary (<username>) -----
            Groups: <groups>
            Permissions: <permission_count>
            Prefix: <group_prefix>
            Display Name: <group_display_name>
            Example Chat: <reset><example_chat>
            <light_purple>-----------<footer_addon>-------------""";

    private final PermissionServiceGrpc.PermissionServiceFutureStub permissionService;
    private final PermissionCache permissionCache;

    public UserDescribeSub(PermissionServiceGrpc.PermissionServiceFutureStub permissionService, PermissionCache permissionCache) {
        this.permissionService = permissionService;
        this.permissionCache = permissionCache;
    }

    public int execute(CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();
        String targetUsername = context.getArgument("username", String.class);

        PlayerResolver.retrievePlayerData(targetUsername, playerData -> {
            UUID targetId = playerData.uuid();
            String correctedUsername = playerData.username();

            ListenableFuture<PermissionProto.PlayerRolesResponse> rolesResponseFuture = this.permissionService.getPlayerRoles(PermissionProto.PlayerRequest.newBuilder()
                    .setPlayerId(targetId.toString()).build());

            Futures.addCallback(rolesResponseFuture, FunctionalFutureCallback.create(
                    response -> {
                        List<String> roleIds = response.getRoleIdsList();
                        List<PermissionCache.Role> sortedRoles = this.sortRolesByWeight(roleIds);

                        List<Component> roleComponents = new ArrayList<>();
                        for (int i = 0; i < sortedRoles.size(); i++) {
                            PermissionCache.Role role = sortedRoles.get(i);
                            if (i == 0) {
                                roleComponents.add(Component.text(role.getId(), Style.style(TextDecoration.BOLD)));
                            } else {
                                roleComponents.add(Component.text(role.getId()));
                            }
                        }
                        Component groupsValue = Component.join(JoinConfiguration.commas(true), roleComponents);
                        Component activePrefix = this.permissionCache.determineActivePrefix(roleIds);
                        String activeDisplayName = this.permissionCache.determineActiveName(roleIds);

                        TextComponent.Builder exampleChatBuilder = Component.text();
                        if (activePrefix != null) {
                            exampleChatBuilder.append(activePrefix).append(Component.text(" "));
                        }

                        if (activeDisplayName != null) {
                            exampleChatBuilder.append(MINI_MESSAGE.deserialize(activeDisplayName, Placeholder.unparsed("username", correctedUsername)));
                        } else {
                            exampleChatBuilder.append(Component.text(correctedUsername));
                        }
                        exampleChatBuilder.append(Component.text(": Test Chat", NamedTextColor.WHITE));

                        source.sendMessage(MINI_MESSAGE.deserialize(USER_DESCRIPTION,
                                Placeholder.unparsed("username", correctedUsername),
                                Placeholder.component("groups", groupsValue),
                                Placeholder.unparsed("permission_count", String.valueOf(sortedRoles.stream().map(PermissionCache.Role::getPermissions).mapToInt(Set::size).sum())),
                                Placeholder.component("group_prefix", activePrefix == null ? Component.text("null") : activePrefix),
                                Placeholder.unparsed("group_display_name", activeDisplayName == null ? "null" : activeDisplayName),
                                Placeholder.component("example_chat", exampleChatBuilder.build()),
                                Placeholder.unparsed("footer_addon", this.createFooterAddon(correctedUsername))
                        ));
                    },
                    throwable -> {
                        LOGGER.error("Failed to retrieve roles for user {}", targetId, throwable);
                        source.sendMessage(Component.text("An error occurred retrieving roles", NamedTextColor.RED));
                    }
            ), ForkJoinPool.commonPool());
        }, status -> {
            if (status == Status.NOT_FOUND) {
                source.sendMessage(MINI_MESSAGE.deserialize(USER_NOT_FOUND, Placeholder.unparsed("user_id", targetUsername)));
            } else {
                LOGGER.error("Failed to retrieve player data for user {}", targetUsername, status.asRuntimeException());
            }
        });

        return 1;
    }

    private List<PermissionCache.Role> sortRolesByWeight(List<String> roleIds) {
        return roleIds.stream()
                .map(this.permissionCache::getRole)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted((a, b) -> Integer.compare(b.getPriority(), a.getPriority()))
                .toList();
    }

    private String createFooterAddon(String headerText) {
        char[] footerAddon = new char[headerText.length()];
        Arrays.fill(footerAddon, '-');
        return new String(footerAddon);
    }
}
