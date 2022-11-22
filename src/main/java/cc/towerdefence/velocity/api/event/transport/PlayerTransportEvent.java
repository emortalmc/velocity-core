package cc.towerdefence.velocity.api.event.transport;

import cc.towerdefence.api.service.ServerDiscoveryProto;

import java.util.Set;
import java.util.UUID;

public record PlayerTransportEvent(Set<UUID> players, ServerDiscoveryProto.ConnectableServer server) {
}
