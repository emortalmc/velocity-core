package dev.emortal.velocity.api.event.transport;

import dev.emortal.api.service.ServerDiscoveryProto;

import java.util.Set;
import java.util.UUID;

public record PlayerTransportEvent(Set<UUID> players, ServerDiscoveryProto.ConnectableServer server) {
}
