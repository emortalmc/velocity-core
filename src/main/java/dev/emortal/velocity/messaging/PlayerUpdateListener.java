package dev.emortal.velocity.messaging;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.message.common.PlayerConnectMessage;
import dev.emortal.api.message.common.PlayerDisconnectMessage;
import dev.emortal.api.message.common.PlayerSwitchServerMessage;
import dev.emortal.api.utils.kafka.FriendlyKafkaProducer;
import dev.emortal.velocity.Environment;
import org.jetbrains.annotations.NotNull;

final class PlayerUpdateListener {
    private static final String KAFKA_CONNECTIONS_TOPIC = "mc-connections";

    private final FriendlyKafkaProducer kafkaProducer;

    PlayerUpdateListener(@NotNull FriendlyKafkaProducer kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }

    // Kafka producing
    @Subscribe
    private void onPlayerLogin(@NotNull PostLoginEvent event) {
        Player player = event.getPlayer();

        PlayerConnectMessage message = PlayerConnectMessage.newBuilder()
                .setPlayerId(player.getUniqueId().toString())
                .setPlayerUsername(player.getUsername())
                .setServerId(Environment.getHostname())
                .setSkin(PlayerConnectMessage.PlayerSkin.newBuilder()
                        .setTexture("")
                        .setSignature(""))
                .build();

        System.out.println("PLAYER PROPERTIES: " + player.getGameProfileProperties());

        this.kafkaProducer.produceAndForget(KAFKA_CONNECTIONS_TOPIC, message);
    }

    @Subscribe
    private void onPlayerDisconnect(@NotNull DisconnectEvent event) {
        Player player = event.getPlayer();

        PlayerDisconnectMessage message = PlayerDisconnectMessage.newBuilder()
                .setPlayerId(player.getUniqueId().toString())
                .setPlayerUsername(player.getUsername())
                .build();

        this.kafkaProducer.produceAndForget(KAFKA_CONNECTIONS_TOPIC, message);
    }

    @Subscribe
    private void onServerSwitch(@NotNull ServerConnectedEvent event) {
        PlayerSwitchServerMessage message = PlayerSwitchServerMessage.newBuilder()
                .setPlayerId(event.getPlayer().getUniqueId().toString())
                .setServerId(event.getServer().getServerInfo().getName())
                .build();

        this.kafkaProducer.produceAndForget(KAFKA_CONNECTIONS_TOPIC, message);
    }
}
