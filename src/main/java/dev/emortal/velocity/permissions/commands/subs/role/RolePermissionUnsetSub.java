package dev.emortal.velocity.permissions.commands.subs.role;

import dev.emortal.velocity.permissions.PermissionCache;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import dev.emortal.api.service.PermissionProto;
import dev.emortal.api.service.PermissionServiceGrpc;
import dev.emortal.api.utils.callback.FunctionalFutureCallback;
import io.grpc.Status;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

public class RolePermissionUnsetSub {
    private static final Logger LOGGER = LoggerFactory.getLogger(RolePermissionUnsetSub.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String ROLE_NOT_FOUND = "<red>Role <role_id> not found";
    private static final String PERMISSION_UNSET = "<green>Permission <permission> unset from role <role_id>";
    private static final String PERMISSION_NOT_FOUND = "<red>Permission <permission> not found for role <role_id>";

    private final PermissionServiceGrpc.PermissionServiceFutureStub permissionService;
    private final PermissionCache permissionCache;

    public RolePermissionUnsetSub(PermissionServiceGrpc.PermissionServiceFutureStub permissionService, PermissionCache permissionCache) {
        this.permissionService = permissionService;
        this.permissionCache = permissionCache;
    }

    public int execute(CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();
        String roleId = context.getArgument("roleId", String.class);
        String permission = context.getArgument("permission", String.class);

        Optional<PermissionCache.Role> optionalRole = RoleSubUtils.getRole(this.permissionCache, context);
        if (optionalRole.isEmpty()) return 1;

        PermissionCache.Role role = optionalRole.get();
        if (role.getPermissionState(permission) == Tristate.UNDEFINED) {
            source.sendMessage(MINI_MESSAGE.deserialize(PERMISSION_NOT_FOUND, Placeholder.unparsed("role_id", roleId), Placeholder.unparsed("permission", permission)));
            return 1;
        }

        ListenableFuture<PermissionProto.RoleResponse> roleResponseFuture = this.permissionService.updateRole(PermissionProto.RoleUpdateRequest.newBuilder()
                .setId(roleId)
                .addUnsetPermissions(permission)
                .build());

        Futures.addCallback(roleResponseFuture, FunctionalFutureCallback.create(
                response -> {
                    optionalRole.get().getPermissions().removeIf(node -> node.permission().equals(permission));
                    source.sendMessage(MINI_MESSAGE.deserialize(PERMISSION_UNSET,
                            Placeholder.unparsed("role_id", roleId),
                            Placeholder.unparsed("permission", permission))
                    );
                },
                throwable -> {
                    if (Status.fromThrowable(throwable) == Status.NOT_FOUND) {
                        source.sendMessage(MINI_MESSAGE.deserialize(ROLE_NOT_FOUND, Placeholder.unparsed("role_id", roleId)));
                        return;
                    }
                    LOGGER.error("Error while unsetting permission from role", throwable);
                    source.sendMessage(MINI_MESSAGE.deserialize("<red>Error while unsetting permission from role"));
                }
        ), ForkJoinPool.commonPool());

        return 1;
    }
}
