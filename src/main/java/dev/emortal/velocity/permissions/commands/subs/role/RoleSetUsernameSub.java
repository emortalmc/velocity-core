package dev.emortal.velocity.permissions.commands.subs.role;

import com.google.common.util.concurrent.Futures;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.grpc.permission.PermissionProto;
import dev.emortal.api.grpc.permission.PermissionServiceGrpc;
import dev.emortal.api.utils.callback.FunctionalFutureCallback;
import dev.emortal.velocity.permissions.PermissionCache;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

public class RoleSetUsernameSub {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoleSetUsernameSub.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String ROLE_NOT_FOUND = "<red>Role <role_id> not found";
    private static final String ROLE_USERNAME_SET = "<green>Role <role_id> username set to \"<username_format>\" (e.g <formatted_username><green>)";

    private final PermissionServiceGrpc.PermissionServiceFutureStub permissionService;
    private final PermissionCache permissionCache;

    public RoleSetUsernameSub(PermissionServiceGrpc.PermissionServiceFutureStub permissionService, PermissionCache permissionCache) {
        this.permissionService = permissionService;
        this.permissionCache = permissionCache;
    }

    public int execute(CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();
        String roleId = context.getArgument("roleId", String.class).toLowerCase();
        String usernameFormat = context.getArgument("usernameFormat", String.class);

        Optional<PermissionCache.CachedRole> optionalRole = RoleSubUtils.getRole(this.permissionCache, context);
        if (optionalRole.isEmpty()) return 1;

        var roleResponseFuture = this.permissionService.updateRole(PermissionProto.RoleUpdateRequest.newBuilder()
                .setId(roleId)
                .setDisplayName(usernameFormat)
                .build());

        Futures.addCallback(roleResponseFuture, FunctionalFutureCallback.create(
                response -> {
                    PermissionCache.CachedRole role = this.permissionCache.getRole(roleId).orElseThrow();
                    role.setDisplayName(usernameFormat);

                    Component formattedUsername = role.getFormattedDisplayName(source instanceof Player player ? player.getUsername() : "CONSOLE");

                    source.sendMessage(MINI_MESSAGE.deserialize(ROLE_USERNAME_SET,
                            Placeholder.unparsed("role_id", roleId),
                            Placeholder.unparsed("username_format", usernameFormat),
                            Placeholder.component("formatted_username", MINI_MESSAGE.deserialize(usernameFormat, Placeholder.component("username", formattedUsername)))));
                },
                throwable -> {
                    LOGGER.error("Failed to set role username", throwable);
                    source.sendMessage(MINI_MESSAGE.deserialize("<red>Failed to set role username"));
                }
        ), ForkJoinPool.commonPool());

        return 1;
    }
}
