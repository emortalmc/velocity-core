package dev.emortal.velocity.permissions.commands.subs.role;

import com.google.common.util.concurrent.Futures;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import dev.emortal.api.grpc.permission.PermissionProto;
import dev.emortal.api.grpc.permission.PermissionServiceGrpc;
import dev.emortal.api.utils.callback.FunctionalFutureCallback;
import dev.emortal.velocity.permissions.PermissionCache;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

public class RoleSetPrioritySub {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoleSetPrioritySub.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String ROLE_NOT_FOUND = "<red>Role <role_id> not found";
    private static final String ROLE_PRIORITY_SET = "<green>Role <role_id> priority set to <priority>";

    private final PermissionServiceGrpc.PermissionServiceFutureStub permissionService;
    private final PermissionCache permissionCache;

    public RoleSetPrioritySub(PermissionServiceGrpc.PermissionServiceFutureStub permissionService, PermissionCache permissionCache) {
        this.permissionService = permissionService;
        this.permissionCache = permissionCache;
    }

    public int execute(CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();
        String roleId = context.getArgument("roleId", String.class);
        int priority = context.getArgument("priority", Integer.class);

        Optional<PermissionCache.CachedRole> optionalRole = RoleSubUtils.getRole(this.permissionCache, context);
        if (optionalRole.isEmpty()) return 1;

        var roleResponseFuture = this.permissionService.updateRole(PermissionProto.RoleUpdateRequest.newBuilder()
                .setId(roleId)
                .setPriority(priority)
                .build());

        Futures.addCallback(roleResponseFuture, FunctionalFutureCallback.create(
                response -> {
                    optionalRole.get().setPriority(priority);
                    source.sendMessage(MINI_MESSAGE.deserialize(ROLE_PRIORITY_SET,
                            Placeholder.unparsed("role_id", roleId),
                            Placeholder.unparsed("priority", String.valueOf(priority)))
                    );
                },
                throwable -> {
                    LOGGER.error("Error while setting role priority", throwable);
                    source.sendMessage(MINI_MESSAGE.deserialize("<red>Error while setting role priority"));
                }
        ), ForkJoinPool.commonPool());

        return 1;
    }
}
