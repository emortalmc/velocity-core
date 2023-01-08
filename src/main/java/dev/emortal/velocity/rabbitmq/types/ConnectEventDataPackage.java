package dev.emortal.velocity.rabbitmq.types;

import java.util.UUID;

public record ConnectEventDataPackage(UUID playerId, String username) {

}
