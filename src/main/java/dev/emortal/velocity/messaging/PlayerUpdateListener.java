package dev.emortal.velocity.messaging;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.message.common.PlayerConnectMessage;
import dev.emortal.api.message.common.PlayerDisconnectMessage;
import dev.emortal.api.message.common.PlayerSwitchServerMessage;
import dev.emortal.api.model.common.PlayerSkin;
import dev.emortal.api.utils.kafka.FriendlyKafkaProducer;
import dev.emortal.velocity.Environment;
import dev.emortal.velocity.utils.SkinUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PlayerUpdateListener {
    private static final String KAFKA_CONNECTIONS_TOPIC = "mc-connections";
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerUpdateListener.class);

    private final FriendlyKafkaProducer kafkaProducer;

    PlayerUpdateListener(@NotNull FriendlyKafkaProducer kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }

    // Kafka producing
    @Subscribe
    private void onPlayerLogin(@NotNull PostLoginEvent event) {
        Player player = event.getPlayer();

        PlayerSkin skin = SkinUtils.getProtoSkin(player);
        if (skin == null) {
            LOGGER.warn("Player {} has no skin", player.getUsername());
        }

        PlayerConnectMessage.Builder messageBuilder = PlayerConnectMessage.newBuilder()
                .setPlayerId(player.getUniqueId().toString())
                .setPlayerUsername(player.getUsername())
                .setServerId(Environment.getHostname());

        if (skin != null) messageBuilder.setPlayerSkin(skin);

        this.kafkaProducer.produceAndForget(KAFKA_CONNECTIONS_TOPIC, messageBuilder.build());
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
