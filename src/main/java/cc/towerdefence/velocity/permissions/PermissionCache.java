package cc.towerdefence.velocity.permissions;

import cc.towerdefence.api.model.common.PlayerProto;
import cc.towerdefence.api.service.PermissionProto;
import cc.towerdefence.api.service.PermissionServiceGrpc;
import cc.towerdefence.api.utils.utils.FunctionalFutureCallback;
import cc.towerdefence.velocity.grpc.stub.GrpcStubManager;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Empty;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class PermissionCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionCache.class);
    private final Map<String, Role> roleCache = Collections.synchronizedMap(new LinkedHashMap<>());
    private final Map<UUID, User> userCache = new ConcurrentHashMap<>();

    private final PermissionServiceGrpc.PermissionServiceFutureStub permissionService;

    public PermissionCache(GrpcStubManager stubManager) {
        this.permissionService = stubManager.getPermissionService();

        this.loadRoles();
    }

    private void loadRoles() {
        ListenableFuture<PermissionProto.RolesResponse> response = this.permissionService.getRoles(Empty.getDefaultInstance());

        Futures.addCallback(response, FunctionalFutureCallback.create(
                result -> {
                    for (PermissionProto.RoleResponse role : result.getRolesList()) {
                        this.roleCache.put(
                                role.getId(),
                                new Role(
                                        role.getId(), role.getPriority(),
                                        role.getDisplayPrefix(), role.getDisplayName(),
                                        Sets.newConcurrentHashSet(role.getPermissionsList().stream()
                                                .map(protoNode -> new Role.PermissionNode(
                                                                protoNode.getNode(),
                                                                protoNode.getState() == PermissionProto.PermissionNode.PermissionState.ALLOW ? Tristate.TRUE : Tristate.FALSE
                                                        )
                                                ).collect(Collectors.toSet()))
                                )
                        );
                    }
                },
                error -> LOGGER.error("Failed to load roles", error)
        ), ForkJoinPool.commonPool());
    }

    public void loadUser(UUID id, Runnable callback, Consumer<Throwable> errorCallback) {
        ListenableFuture<PermissionProto.PlayerRolesResponse> rolesResponseFuture = this.permissionService.getPlayerRoles(
                PlayerProto.PlayerRequest.newBuilder().setPlayerId(id.toString()).build()
        );

        Futures.addCallback(rolesResponseFuture, FunctionalFutureCallback.create(
                result -> {
                    Set<String> roleIds = Sets.newConcurrentHashSet(result.getRoleIdsList());
                    User user = new User(id, roleIds, this.determineActivePrefix(roleIds), this.determineActiveName(roleIds));
                    this.userCache.put(id, user);
                    callback.run();
                },
                error -> {
                    errorCallback.accept(error);
                    LOGGER.error("Failed to load user roles for " + id, error);
                }
        ), ForkJoinPool.commonPool());
    }

    public Tristate getPermission(UUID id, String permission) {
        User user = this.userCache.get(id);
        if (user == null) {
            return Tristate.UNDEFINED;
        }

        int currentPriority = 0;
        Tristate currentState = Tristate.UNDEFINED;

        for (String roleId : user.getRoleIds()) {
            Role role = this.roleCache.get(roleId);
            if (role == null || currentPriority > role.getPriority()) continue;

            for (Role.PermissionNode node : role.getPermissions()) {
                if (node.permission().equals(permission)) {
                    currentPriority = role.getPriority();
                    currentState = node.state();
                }
            }
        }

        return currentState;
    }

    public Map<String, Role> getRoleCache() {
        return roleCache;
    }

    public Map<UUID, User> getUserCache() {
        return userCache;
    }

    public Optional<Role> getRole(String id) {
        return Optional.ofNullable(this.roleCache.get(id));
    }

    public Optional<User> getUser(UUID id) {
        return Optional.ofNullable(this.userCache.get(id));
    }

    public void addRole(PermissionProto.RoleResponse roleResponse) {
        Role role = new Role(
                roleResponse.getId(), roleResponse.getPriority(),
                roleResponse.getDisplayPrefix(), roleResponse.getDisplayName(),
                Sets.newConcurrentHashSet(roleResponse.getPermissionsList().stream()
                        .map(protoNode -> new Role.PermissionNode(
                                        protoNode.getNode(),
                                        protoNode.getState() == PermissionProto.PermissionNode.PermissionState.ALLOW ? Tristate.TRUE : Tristate.FALSE
                                )
                        ).collect(Collectors.toSet()))
        );

        this.roleCache.put(roleResponse.getId(), role);
    }

    public Component determineActivePrefix(Collection<String> roleIds) {
        int currentPriority = 0;
        Component currentPrefix = null;
        for (Role role : this.roleCache.values()) {
            if (role.getDisplayPrefix() != null && roleIds.contains(role.getId())) {
                if (role.getPriority() > currentPriority) {
                    currentPriority = role.getPriority();
                    currentPrefix = role.getDisplayPrefix();
                }
            }
        }
        return currentPrefix;
    }

    public String determineActiveName(Collection<String> roleIds) {
        int currentPriority = 0;
        String currentActiveName = null;

        for (Role role : this.roleCache.values()) {
            if (role.getDisplayName() != null && roleIds.contains(role.getId())) {
                if (role.getPriority() > currentPriority) {
                    currentPriority = role.getPriority();
                    currentActiveName = role.getDisplayName();
                }
            }
        }
        return currentActiveName;
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        this.userCache.remove(event.getPlayer().getUniqueId());
    }

    public static final class User {
        private final UUID id;
        private final Set<String> roleIds;

        private Component displayPrefix;
        private String displayName;

        public User(UUID id, Set<String> roleIds, Component displayPrefix, String displayName) {
            this.id = id;
            this.roleIds = roleIds;
            this.displayPrefix = displayPrefix;
            this.displayName = displayName;
        }

        public UUID getId() {
            return this.id;
        }

        public Set<String> getRoleIds() {
            return this.roleIds;
        }

        public Component getDisplayPrefix() {
            return this.displayPrefix;
        }

        public void setDisplayPrefix(Component displayPrefix) {
            this.displayPrefix = displayPrefix;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
    }

    public static final class Role implements Comparable<Role> {
        private final String id;
        private final Set<PermissionNode> permissions;

        private int priority;
        private Component displayPrefix;
        private String displayName;

        public Role(String id, int priority, String displayPrefix, String displayName, Set<PermissionNode> permissions) {
            this.id = id;
            this.priority = priority;
            this.displayPrefix = MiniMessage.miniMessage().deserialize(displayPrefix);
            this.displayName = displayName;
            this.permissions = permissions;
        }

        @Override
        public int compareTo(@NotNull PermissionCache.Role o) {
            return Integer.compare(this.priority, o.priority);
        }

        public String getId() {
            return this.id;
        }

        public Set<PermissionNode> getPermissions() {
            return this.permissions;
        }

        public Tristate getPermissionState(String node) {
            for (PermissionNode permissionNode : this.permissions) {
                if (permissionNode.permission().equals(node)) {
                    return permissionNode.state();
                }
            }
            return Tristate.UNDEFINED;
        }

        public int getPriority() {
            return this.priority;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }

        public Component getDisplayPrefix() {
            return this.displayPrefix;
        }

        public void setDisplayPrefix(String displayPrefix) {
            this.displayPrefix = MiniMessage.miniMessage().deserialize(displayPrefix);
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public Component getFormattedDisplayName(String username) {
            return MiniMessage.miniMessage().deserialize(this.displayName, Placeholder.unparsed("username", username));
        }

        public record PermissionNode(String permission, Tristate state) {
        }
    }
}
