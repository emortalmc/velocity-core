package dev.emortal.velocity.permissions.commands.subs.role;

import com.google.common.util.concurrent.Futures;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import dev.emortal.api.grpc.permission.PermissionProto;
import dev.emortal.api.grpc.permission.PermissionServiceGrpc;
import dev.emortal.api.utils.callback.FunctionalFutureCallback;
import dev.emortal.velocity.permissions.PermissionCache;
import io.grpc.Status;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

public class RoleSetPrefixSub {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoleSetPrefixSub.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String ROLE_NOT_FOUND = "<red>Role <role_id> not found";
    private static final String ROLE_PREFIX_SET = "<green>Role <role_id> prefix set to \"<prefix>\"";

    private final PermissionServiceGrpc.PermissionServiceFutureStub permissionService;
    private final PermissionCache permissionCache;

    public RoleSetPrefixSub(PermissionServiceGrpc.PermissionServiceFutureStub permissionService, PermissionCache permissionCache) {
        this.permissionService = permissionService;
        this.permissionCache = permissionCache;
    }

    public int execute(CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();

        String roleId = context.getArgument("roleId", String.class);
        Optional<PermissionCache.CachedRole> optionalRole = RoleSubUtils.getRole(this.permissionCache, context);
        if (optionalRole.isEmpty()) return 1;

        String prefix = context.getArgument("prefix", String.class);
        var roleResponseFuture = this.permissionService.updateRole(PermissionProto.RoleUpdateRequest.newBuilder()
                .setId(roleId)
                .setDisplayPrefix(prefix)
                .build());

        Futures.addCallback(roleResponseFuture, FunctionalFutureCallback.create(
                response -> {
                    optionalRole.get().setDisplayPrefix(prefix);
                    source.sendMessage(MINI_MESSAGE.deserialize(ROLE_PREFIX_SET, Placeholder.unparsed("role_id", roleId), Placeholder.parsed("prefix", prefix)));
                },
                throwable -> {
                    Status status = Status.fromThrowable(throwable);
                    if (status == Status.NOT_FOUND) {
                        source.sendMessage(MINI_MESSAGE.deserialize(ROLE_NOT_FOUND, Placeholder.unparsed("role_id", roleId)));
                    } else {
                        source.sendMessage(MINI_MESSAGE.deserialize("<red>Error while setting role prefix"));
                        LOGGER.error("Error while setting role prefix", throwable);
                    }
                }
        ), ForkJoinPool.commonPool());
        return 1;
    }
}
