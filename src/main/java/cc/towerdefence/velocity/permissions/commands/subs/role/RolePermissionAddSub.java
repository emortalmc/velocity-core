package cc.towerdefence.velocity.permissions.commands.subs.role;

import cc.towerdefence.api.service.PermissionProto;
import cc.towerdefence.api.service.PermissionServiceGrpc;
import cc.towerdefence.api.utils.utils.FunctionalFutureCallback;
import cc.towerdefence.velocity.permissions.PermissionCache;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

public class RolePermissionAddSub {
    private static final Logger LOGGER = LoggerFactory.getLogger(RolePermissionAddSub.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String ROLE_NOT_FOUND = "<red>Role <role_id> not found";
    private static final String PERMISSION_ADDED = "<green>Permission '<permission>' added to role <role_id>";
    private static final String PERMISSION_ALREADY_EXISTS = "<red>Permission '<permission>' already exists for <role_id>";

    private final PermissionServiceGrpc.PermissionServiceFutureStub permissionService;
    private final PermissionCache permissionCache;

    public RolePermissionAddSub(PermissionServiceGrpc.PermissionServiceFutureStub permissionService, PermissionCache permissionCache) {
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
        Tristate permissionState = role.getPermissionState(permission);
        if (permissionState == Tristate.TRUE) {
            source.sendMessage(MINI_MESSAGE.deserialize(PERMISSION_ALREADY_EXISTS, Placeholder.unparsed("role_id", roleId), Placeholder.unparsed("permission", permission)));
            return 1;
        }

        PermissionProto.RoleUpdateRequest.Builder requestBuilder = PermissionProto.RoleUpdateRequest.newBuilder()
                .setId(roleId)
                .addSetPermissions(PermissionProto.PermissionNode.newBuilder()
                        .setState(PermissionProto.PermissionNode.PermissionState.ALLOW)
                        .setNode(permission)
                );

        if (permissionState == Tristate.FALSE) requestBuilder.addUnsetPermissions(permission);

        ListenableFuture<PermissionProto.RoleResponse> roleResponseFuture = this.permissionService.updateRole(requestBuilder.build());

        Futures.addCallback(roleResponseFuture, FunctionalFutureCallback.create(
                response -> {
                    optionalRole.get().getPermissions().add(new PermissionCache.Role.PermissionNode(permission, Tristate.TRUE));
                    source.sendMessage(MINI_MESSAGE.deserialize(PERMISSION_ADDED,
                            Placeholder.unparsed("role_id", roleId),
                            Placeholder.unparsed("permission", permission))
                    );
                },
                throwable -> {
                    if (Status.fromThrowable(throwable) == Status.NOT_FOUND) {
                        source.sendMessage(MINI_MESSAGE.deserialize(ROLE_NOT_FOUND, Placeholder.unparsed("role_id", roleId)));
                        return;
                    }
                    LOGGER.error("Error while adding permission to role", throwable);
                    source.sendMessage(MINI_MESSAGE.deserialize("<red>Error while adding permission to role"));
                }
        ), ForkJoinPool.commonPool());

        return 1;
    }
}
