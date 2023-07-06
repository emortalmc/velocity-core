package dev.emortal.velocity.messaging;

import com.google.protobuf.AbstractMessage;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.kurushimi.KurushimiUtils;
import dev.emortal.api.message.common.PlayerConnectMessage;
import dev.emortal.api.message.common.PlayerDisconnectMessage;
import dev.emortal.api.message.common.PlayerSwitchServerMessage;
import dev.emortal.api.utils.kafka.FriendlyKafkaConsumer;
import dev.emortal.api.utils.kafka.FriendlyKafkaProducer;
import dev.emortal.api.utils.kafka.KafkaSettings;
import dev.emortal.velocity.Environment;

import java.util.function.Consumer;

public class MessagingCore {
    private static final String KAFKA_HOST = System.getenv("KAFKA_HOST");
    private static final String KAFKA_PORT = System.getenv("KAFKA_PORT");

    private static final String KAFKA_CONNECTIONS_TOPIC = "mc-connections";

    private final FriendlyKafkaConsumer kafkaConsumer;
    private final FriendlyKafkaProducer kafkaProducer;

    public MessagingCore() {
        KafkaSettings kafkaSettings = KafkaSettings.builder().bootstrapServers(KAFKA_HOST + ":" + KAFKA_PORT).build();

        this.kafkaConsumer = new FriendlyKafkaConsumer(kafkaSettings);
        this.kafkaProducer = new FriendlyKafkaProducer(kafkaSettings);

        // Register matchmaker parsers
        KurushimiUtils.registerParserRegistry();
    }

    public <T extends AbstractMessage> void addListener(Class<T> messageType, Consumer<T> listener) {
        this.kafkaConsumer.addListener(messageType, listener);
    }

    // Kafka producing
    @Subscribe
    public void onPlayerLogin(PostLoginEvent event) {
        Player player = event.getPlayer();

        PlayerConnectMessage message = PlayerConnectMessage.newBuilder()
                .setPlayerId(player.getUniqueId().toString())
                .setPlayerUsername(player.getUsername())
                .setServerId(Environment.getHostname())
                .build();

        this.kafkaProducer.produce(KAFKA_CONNECTIONS_TOPIC, message);
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();

        PlayerDisconnectMessage message = PlayerDisconnectMessage.newBuilder()
                .setPlayerId(player.getUniqueId().toString())
                .setPlayerUsername(player.getUsername())
                .build();

        this.kafkaProducer.produce(KAFKA_CONNECTIONS_TOPIC, message);
    }

    @Subscribe
    public void onServerSwitch(ServerConnectedEvent event) {
        PlayerSwitchServerMessage message = PlayerSwitchServerMessage.newBuilder()
                .setPlayerId(event.getPlayer().getUniqueId().toString())
                .setServerId(event.getServer().getServerInfo().getName())
                .build();

        this.kafkaProducer.produce(KAFKA_CONNECTIONS_TOPIC, message);
    }

    public void shutdown() {
        this.kafkaConsumer.close();
    }
}
