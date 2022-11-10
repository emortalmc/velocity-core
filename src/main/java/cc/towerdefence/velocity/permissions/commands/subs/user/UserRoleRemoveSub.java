package cc.towerdefence.velocity.permissions.commands.subs.user;

import cc.towerdefence.api.service.PermissionProto;
import cc.towerdefence.api.service.PermissionServiceGrpc;
import cc.towerdefence.api.utils.resolvers.PlayerResolver;
import cc.towerdefence.api.utils.utils.FunctionalFutureCallback;
import cc.towerdefence.velocity.permissions.PermissionCache;
import cc.towerdefence.velocity.permissions.commands.subs.role.RoleSubUtils;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Empty;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import io.grpc.Metadata;
import io.grpc.Status;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;

public class UserRoleRemoveSub {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserRoleRemoveSub.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String ROLE_NOT_FOUND = "<red>Role <role_id> not found";
    private static final String ROLE_REMOVED = "<green>Role <role_id> removed from user <user_id>";
    private static final String DOESNT_HAVE_ROLE = "<red>User <user_id> doesn't have role <role_id>";

    private final PermissionServiceGrpc.PermissionServiceFutureStub permissionService;
    private final PermissionCache permissionCache;

    public UserRoleRemoveSub(PermissionServiceGrpc.PermissionServiceFutureStub permissionService, PermissionCache permissionCache) {
        this.permissionService = permissionService;
        this.permissionCache = permissionCache;
    }

    public int execute(CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();
        String targetUsername = context.getArgument("username", String.class);
        String roleId = context.getArgument("roleId", String.class);

        Optional<PermissionCache.Role> optionalRole = RoleSubUtils.getRole(this.permissionCache, context);
        if (optionalRole.isEmpty()) return 1;
        PermissionCache.Role role = optionalRole.get();

        PlayerResolver.retrievePlayerData(targetUsername, playerData -> {
            UUID targetId = playerData.uuid();
            String correctUsername = playerData.username();

            ListenableFuture<Empty> removeRoleFuture = this.permissionService.removeRoleFromPlayer(PermissionProto.RemoveRoleFromPlayerRequest.newBuilder()
                    .setRoleId(roleId)
                    .setPlayerId(targetId.toString())
                    .build());

            Futures.addCallback(removeRoleFuture, FunctionalFutureCallback.create(
                    response -> {
                        source.sendMessage(MINI_MESSAGE.deserialize(ROLE_REMOVED,
                                Placeholder.unparsed("role_id", roleId),
                                Placeholder.unparsed("username", correctUsername))
                        );

                        this.permissionCache.getUser(targetId).ifPresent(user -> user.getRoleIds().removeIf(loopRoleId -> loopRoleId.equals(roleId)));
                    },
                    throwable -> {
                        Status status = Status.fromThrowable(throwable);
                        if (status == Status.NOT_FOUND) {
                            source.sendMessage(MINI_MESSAGE.deserialize(ROLE_NOT_FOUND, Placeholder.unparsed("role_id", roleId)));
                            return;
                        }

                        Metadata metadata = Status.trailersFromThrowable(throwable);
                        Metadata.Key<String> customCauseKey = Metadata.Key.of("custom_cause", Metadata.ASCII_STRING_MARSHALLER);
                        String customCause = metadata.get(customCauseKey);
                        if (status == Status.ALREADY_EXISTS && customCause.equals("NO_ROLE")) {
                            source.sendMessage(MINI_MESSAGE.deserialize(DOESNT_HAVE_ROLE,
                                    Placeholder.unparsed("username", correctUsername),
                                    Placeholder.unparsed("role_id", roleId))
                            );
                        } else {
                            source.sendMessage(MINI_MESSAGE.deserialize("<red>Something went wrong"));
                            LOGGER.error("Something went wrong add role to user", throwable);
                        }
                    }
            ), ForkJoinPool.commonPool());
        }, status -> {
            if (status.getCode() == Status.Code.NOT_FOUND) {
                source.sendMessage(MINI_MESSAGE.deserialize("<red>Player <username> not found", Placeholder.unparsed("username", targetUsername)));
            } else {
                source.sendMessage(MINI_MESSAGE.deserialize("<red>Failed to retrieve player data for <username>", Placeholder.unparsed("username", targetUsername)));
            }
        });

        return 1;
    }

}
