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

public class RolePermissionAddSub {
    private static final Logger LOGGER = LoggerFactory.getLogger(RolePermissionAddSub.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String ROLE_NOT_FOUND = "<red>Role <role_id> not found";
    private static final String PERMISSION_ADDED = "<green>Permission '<permission>' set to <value> for role <role_id>";
    private static final String PERMISSION_ALREADY_EXISTS = "<red>Permission '<permission>' already set to <value> for <role_id>";

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
        boolean value = context.getArgument("value", Boolean.class);
        Tristate tristateValue = value ? Tristate.TRUE : Tristate.FALSE;

        Optional<PermissionCache.Role> optionalRole = RoleSubUtils.getRole(this.permissionCache, context);
        if (optionalRole.isEmpty()) return 1;

        PermissionCache.Role role = optionalRole.get();
        Tristate oldState = role.getPermissionState(permission);
        if (oldState == tristateValue) {
            source.sendMessage(MINI_MESSAGE.deserialize(PERMISSION_ALREADY_EXISTS,
                    Placeholder.unparsed("role_id", roleId),
                    Placeholder.unparsed("permission", permission),
                    Placeholder.unparsed("value", String.valueOf(value)))
            );
            return 1;
        }

        PermissionProto.RoleUpdateRequest.Builder requestBuilder = PermissionProto.RoleUpdateRequest.newBuilder()
                .setId(roleId)
                .addSetPermissions(PermissionProto.PermissionNode.newBuilder()
                        .setState(tristateValue == Tristate.TRUE ? PermissionProto.PermissionNode.PermissionState.ALLOW : PermissionProto.PermissionNode.PermissionState.DENY)
                        .setNode(permission)
                );

        if (oldState != Tristate.UNDEFINED) requestBuilder.addUnsetPermissions(permission);

        ListenableFuture<PermissionProto.RoleResponse> roleResponseFuture = this.permissionService.updateRole(requestBuilder.build());

        Futures.addCallback(roleResponseFuture, FunctionalFutureCallback.create(
                response -> {
                    optionalRole.get().getPermissions().add(new PermissionCache.Role.PermissionNode(permission, Tristate.TRUE));
                    source.sendMessage(MINI_MESSAGE.deserialize(PERMISSION_ADDED,
                            Placeholder.unparsed("role_id", roleId),
                            Placeholder.unparsed("permission", permission),
                            Placeholder.unparsed("value", String.valueOf(value)))
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
