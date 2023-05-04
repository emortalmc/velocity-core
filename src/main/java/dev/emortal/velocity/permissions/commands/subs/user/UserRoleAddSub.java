package dev.emortal.velocity.permissions.commands.subs.user;

import com.google.common.util.concurrent.Futures;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.Status;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import dev.emortal.api.grpc.permission.PermissionProto;
import dev.emortal.api.grpc.permission.PermissionServiceGrpc;
import dev.emortal.api.utils.callback.FunctionalFutureCallback;
import dev.emortal.api.utils.resolvers.PlayerResolver;
import dev.emortal.velocity.permissions.PermissionCache;
import dev.emortal.velocity.permissions.commands.subs.role.RoleSubUtils;
import io.grpc.protobuf.StatusProto;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;

public class UserRoleAddSub {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserRoleAddSub.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String ROLE_NOT_FOUND = "<red>Role <role_id> not found";
    private static final String ROLE_ADDED = "<green>Role <role_id> added to user <username>";
    private static final String ROLE_ALREADY_ADDED = "<red>User <username> already has role <role_id>";

    private static final String PERMISSION_PLAYER_NOT_FOUND = "<red>Player <uuid> not found in permission service";

    private final PermissionServiceGrpc.PermissionServiceFutureStub permissionService;
    private final PermissionCache permissionCache;

    public UserRoleAddSub(PermissionServiceGrpc.PermissionServiceFutureStub permissionService, PermissionCache permissionCache) {
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

            var addRoleFuture = this.permissionService.addRoleToPlayer(PermissionProto.AddRoleToPlayerRequest.newBuilder()
                    .setRoleId(roleId)
                    .setPlayerId(targetId.toString())
                    .build());

            Futures.addCallback(addRoleFuture, FunctionalFutureCallback.create(
                    response -> {
                        source.sendMessage(MINI_MESSAGE.deserialize(ROLE_ADDED,
                                Placeholder.unparsed("role_id", roleId),
                                Placeholder.unparsed("username", correctUsername))
                        );

                        this.permissionCache.getUser(targetId).ifPresent(user -> user.getRoleIds().add(role.getId()));
                    },
                    throwable -> {
                        Status status = StatusProto.fromThrowable(throwable);

                        if (status == null || status.getDetailsCount() == 0) {
                            source.sendMessage(MINI_MESSAGE.deserialize("<red>Something went wrong"));
                            LOGGER.error("Something went wrong adding role to user", throwable);
                            return;
                        }
                        try {
                            PermissionProto.AddRoleToPlayerError errorDetails = status.getDetails(0).unpack(PermissionProto.AddRoleToPlayerError.class);
                            source.sendMessage(switch (errorDetails.getErrorType()) {
                                case PLAYER_NOT_FOUND ->
                                        MINI_MESSAGE.deserialize(PERMISSION_PLAYER_NOT_FOUND, Placeholder.unparsed("uuid", targetId.toString()));
                                case ROLE_NOT_FOUND ->
                                        MINI_MESSAGE.deserialize(ROLE_NOT_FOUND, Placeholder.unparsed("role_id", roleId));
                                case ALREADY_HAS_ROLE -> MINI_MESSAGE.deserialize(ROLE_ALREADY_ADDED,
                                        Placeholder.unparsed("username", correctUsername),
                                        Placeholder.unparsed("role_id", roleId));
                                default -> {
                                    LOGGER.error("Something went wrong adding role to user", throwable);
                                    yield MINI_MESSAGE.deserialize("<red>Something went wrong");
                                }
                            });
                        } catch (InvalidProtocolBufferException e) {
                            source.sendMessage(MINI_MESSAGE.deserialize("<red>Something went wrong"));
                            LOGGER.error("Something went wrong adding role to user", throwable);
                            return;
                        }
                    }
            ), ForkJoinPool.commonPool());
        }, status -> {
            if (status.getCode() == io.grpc.Status.Code.NOT_FOUND) {
                source.sendMessage(MINI_MESSAGE.deserialize("<red>Player <username> not found", Placeholder.unparsed("username", targetUsername)));
            } else {
                source.sendMessage(MINI_MESSAGE.deserialize("<red>Failed to retrieve player data for <username>", Placeholder.unparsed("username", targetUsername)));
            }
        });

        return 0;
    }
}
