package dev.emortal.velocity.permissions.commands.subs.user;

import com.google.common.util.concurrent.Futures;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import dev.emortal.api.grpc.permission.PermissionProto;
import dev.emortal.api.grpc.permission.PermissionServiceGrpc;
import dev.emortal.api.utils.callback.FunctionalFutureCallback;
import dev.emortal.api.utils.resolvers.PlayerResolver;
import dev.emortal.velocity.permissions.PermissionCache;
import dev.emortal.velocity.permissions.commands.subs.role.RoleSubUtils;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.protobuf.ProtoUtils;
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

    private static final String PERMISSION_PLAYER_NOT_FOUND = "<red>Player <uuid> not found in permission service";

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

        Optional<PermissionCache.CachedRole> optionalRole = RoleSubUtils.getRole(this.permissionCache, context);
        if (optionalRole.isEmpty()) return 1;
        PermissionCache.CachedRole role = optionalRole.get();

        PlayerResolver.retrievePlayerData(targetUsername, playerData -> {
            UUID targetId = playerData.uuid();
            String correctUsername = playerData.username();

            var removeRoleFuture = this.permissionService.removeRoleFromPlayer(PermissionProto.RemoveRoleFromPlayerRequest.newBuilder()
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
                        Metadata metadata = Status.trailersFromThrowable(throwable);

                        PermissionProto.AddRoleToPlayerError error = metadata
                                .get(ProtoUtils.keyForProto(PermissionProto.AddRoleToPlayerError.getDefaultInstance()));

                        if (error == null) {
                            source.sendMessage(MINI_MESSAGE.deserialize("<red>Something went wrong"));
                            LOGGER.error("Something went wrong removing role from user", throwable);
                            return;
                        }

                        switch (error.getErrorType()) {
                            case ALREADY_HAS_ROLE -> source.sendMessage(MINI_MESSAGE.deserialize(DOESNT_HAVE_ROLE,
                                    Placeholder.unparsed("username", correctUsername),
                                    Placeholder.unparsed("role_id", roleId))
                            );
                            case ROLE_NOT_FOUND -> source.sendMessage(MINI_MESSAGE.deserialize(ROLE_NOT_FOUND,
                                    Placeholder.unparsed("role_id", roleId))
                            );
                            case PLAYER_NOT_FOUND ->
                                    source.sendMessage(MINI_MESSAGE.deserialize(PERMISSION_PLAYER_NOT_FOUND,
                                            Placeholder.unparsed("uuid", targetId.toString()))
                                    );
                            default -> {
                                source.sendMessage(MINI_MESSAGE.deserialize("<red>Something went wrong"));
                                LOGGER.error("Something went wrong removing role from user", throwable);
                            }
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
