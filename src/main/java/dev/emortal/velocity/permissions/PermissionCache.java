package dev.emortal.velocity.permissions;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.permission.Tristate;
import dev.emortal.api.grpc.permission.PermissionProto;
import dev.emortal.api.grpc.permission.PermissionServiceGrpc;
import dev.emortal.api.model.permission.PermissionNode;
import dev.emortal.api.model.permission.Role;
import dev.emortal.api.utils.GrpcStubCollection;
import dev.emortal.api.utils.callback.FunctionalFutureCallback;
import dev.emortal.velocity.grpc.stub.GrpcStubManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class PermissionCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionCache.class);

    private final Map<String, CachedRole> roleCache = Collections.synchronizedMap(new LinkedHashMap<>());
    private final Map<UUID, User> userCache = new ConcurrentHashMap<>();

    private final PermissionServiceGrpc.PermissionServiceFutureStub permissionService;
    private final Set<PermissionBlocker> permissionBlockers;

    public PermissionCache(GrpcStubManager stubManager, PermissionBlocker... permissionBlockers) {
        this.permissionService = GrpcStubCollection.getPermissionService().orElse(null);
        this.permissionBlockers = Set.of(permissionBlockers);

        this.loadRoles();
    }

    private void loadRoles() {
        if (this.permissionService == null) {
            LOGGER.warn("Permission service is not available! Not loading roles");
            return;
        }
        var response = this.permissionService.getAllRoles(PermissionProto.GetAllRolesRequest.getDefaultInstance());

        Futures.addCallback(response, FunctionalFutureCallback.create(
                result -> {
                    for (dev.emortal.api.model.permission.Role role : result.getRolesList()) {
                        this.roleCache.put(
                                role.getId(),
                                new CachedRole(
                                        role.getId(), role.getPriority(),
                                        role.getDisplayPrefix(), role.getDisplayName(),
                                        Sets.newConcurrentHashSet(role.getPermissionsList().stream()
                                                .map(protoNode -> new CachedRole.PermissionNode(
                                                                protoNode.getNode(),
                                                                protoNode.getState() == PermissionNode.PermissionState.ALLOW ? Tristate.TRUE : Tristate.FALSE
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
        var rolesResponseFuture = this.permissionService.getPlayerRoles(
                PermissionProto.GetPlayerRolesRequest.newBuilder().setPlayerId(id.toString()).build()
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
        // check permission blockers
        for (PermissionBlocker permissionBlocker : this.permissionBlockers)
            if (permissionBlocker.isBlocked(id, permission)) return Tristate.FALSE;
        // continue for checks if not blocked

        User user = this.userCache.get(id);
        if (user == null) {
            return Tristate.UNDEFINED;
        }

        int currentPriority = 0;
        Tristate currentState = Tristate.UNDEFINED;

        for (String roleId : user.getRoleIds()) {
            CachedRole role = this.roleCache.get(roleId);
            if (role == null || currentPriority > role.getPriority()) continue;

            for (CachedRole.PermissionNode node : role.getPermissions()) {
                if (node.permission().equals(permission)) {
                    currentPriority = role.getPriority();
                    currentState = node.state();
                }
            }
        }

        return currentState;
    }

    public Map<String, CachedRole> getRoleCache() {
        return roleCache;
    }

    public Map<UUID, User> getUserCache() {
        return userCache;
    }

    public Optional<CachedRole> getRole(String id) {
        return Optional.ofNullable(this.roleCache.get(id));
    }

    public Optional<User> getUser(UUID id) {
        return Optional.ofNullable(this.userCache.get(id));
    }

    public void addRole(Role roleResponse) {
        CachedRole role = new CachedRole(
                roleResponse.getId(), roleResponse.getPriority(),
                roleResponse.getDisplayPrefix(), roleResponse.getDisplayName(),
                Sets.newConcurrentHashSet(roleResponse.getPermissionsList().stream()
                        .map(protoNode -> new CachedRole.PermissionNode(
                                        protoNode.getNode(),
                                        protoNode.getState() == PermissionNode.PermissionState.ALLOW ? Tristate.TRUE : Tristate.FALSE
                                )
                        ).collect(Collectors.toSet()))
        );

        this.roleCache.put(roleResponse.getId(), role);
    }

    public boolean removeRole(String id) {
        return this.roleCache.remove(id) != null;
    }

    public Component determineActivePrefix(Collection<String> roleIds) {
        int currentPriority = 0;
        Component currentPrefix = null;
        for (CachedRole role : this.roleCache.values()) {
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

        for (CachedRole role : this.roleCache.values()) {
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

    public static final class CachedRole implements Comparable<CachedRole> {
        private final String id;
        private final Set<PermissionNode> permissions;

        private int priority;
        private Component displayPrefix;
        private String displayName;

        public CachedRole(String id, int priority, String displayPrefix, String displayName, Set<PermissionNode> permissions) {
            this.id = id;
            this.priority = priority;
            this.displayPrefix = MiniMessage.miniMessage().deserialize(displayPrefix);
            this.displayName = displayName;
            this.permissions = permissions;
        }

        @Override
        public int compareTo(@NotNull PermissionCache.CachedRole o) {
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
