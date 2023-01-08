package dev.emortal.velocity.permissions.commands.subs.user;

import dev.emortal.velocity.permissions.PermissionCache;
import dev.emortal.velocity.permissions.commands.subs.role.RoleSubUtils;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Empty;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import dev.emortal.api.service.PermissionProto;
import dev.emortal.api.service.PermissionServiceGrpc;
import dev.emortal.api.utils.callback.FunctionalFutureCallback;
import dev.emortal.api.utils.resolvers.PlayerResolver;
import io.grpc.Metadata;
import io.grpc.Status;
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

        Optional<PermissionCache.Role> optionalRole = RoleSubUtils.getRole(this.permissionCache, context);
        if (optionalRole.isEmpty()) return 1;
        PermissionCache.Role role = optionalRole.get();

        PlayerResolver.retrievePlayerData(targetUsername, playerData -> {
            UUID targetId = playerData.uuid();
            String correctUsername = playerData.username();

            ListenableFuture<Empty> addRoleFuture = this.permissionService.addRoleToPlayer(PermissionProto.AddRoleToPlayerRequest.newBuilder()
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
                        Status status = Status.fromThrowable(throwable);
                        if (status == Status.NOT_FOUND) {
                            source.sendMessage(MINI_MESSAGE.deserialize(ROLE_NOT_FOUND, Placeholder.unparsed("role_id", roleId)));
                            return;
                        }

                        Metadata metadata = Status.trailersFromThrowable(throwable);
                        Metadata.Key<String> customCauseKey = Metadata.Key.of("custom_cause", Metadata.ASCII_STRING_MARSHALLER);
                        String customCause = metadata.get(customCauseKey);
                        if (status == Status.ALREADY_EXISTS && customCause.equals("ALREADY_HAS_ROLE")) {
                            source.sendMessage(MINI_MESSAGE.deserialize(ROLE_ALREADY_ADDED,
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

        return 0;
    }
}
