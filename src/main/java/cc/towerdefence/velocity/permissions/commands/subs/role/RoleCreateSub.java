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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.relation.Role;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

public class RoleCreateSub {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoleCreateSub.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String ROLE_ALREADY_EXISTS = "<red>Role <role_id> already exists";
    private static final String ROLE_CREATED = "<green>Role <role_id> created";

    private final PermissionServiceGrpc.PermissionServiceFutureStub permissionService;
    private final PermissionCache permissionCache;

    public RoleCreateSub(PermissionServiceGrpc.PermissionServiceFutureStub permissionService, PermissionCache permissionCache) {
        this.permissionService = permissionService;
        this.permissionCache = permissionCache;
    }

    public int execute(CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();
        String roleId = context.getArgument("roleId", String.class).toLowerCase();

        Optional<PermissionCache.Role> optionalRole = this.permissionCache.getRole(roleId);
        if (optionalRole.isPresent()) return 1;

        ListenableFuture<PermissionProto.RoleResponse> roleResponseFuture = this.permissionService.createRole(PermissionProto.RoleCreateRequest.newBuilder()
                .setId(roleId)
                .setDisplayName("<username>")
                .build());

        Futures.addCallback(roleResponseFuture, FunctionalFutureCallback.create(
                response -> {
                    this.permissionCache.addRole(response);
                    source.sendMessage(MINI_MESSAGE.deserialize(ROLE_CREATED, Placeholder.unparsed("role_id", roleId)));
                },
                throwable -> {
                    if (Status.fromThrowable(throwable) == Status.ALREADY_EXISTS) {
                        source.sendMessage(MINI_MESSAGE.deserialize(ROLE_ALREADY_EXISTS, Placeholder.unparsed("role_id", roleId)));
                    } else {
                        source.sendMessage(Component.text("An error occurred while creating role", NamedTextColor.RED));
                        LOGGER.error("An error occurred while creating role", throwable);
                    }
                }
        ), ForkJoinPool.commonPool());

        return 1;
    }
}
