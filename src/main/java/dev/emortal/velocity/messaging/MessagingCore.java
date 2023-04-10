package dev.emortal.velocity.messaging;

import com.google.protobuf.AbstractMessage;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.emortal.api.kurushimi.KurushimiUtils;
import dev.emortal.api.message.common.PlayerConnectMessage;
import dev.emortal.api.message.common.PlayerDisconnectMessage;
import dev.emortal.api.message.common.PlayerSwitchServerMessage;
import dev.emortal.api.utils.kafka.FriendlyKafkaConsumer;
import dev.emortal.api.utils.kafka.FriendlyKafkaProducer;
import dev.emortal.api.utils.kafka.KafkaSettings;
import dev.emortal.api.utils.parser.MessageProtoConfig;
import dev.emortal.api.utils.parser.ProtoParserRegistry;
import dev.emortal.velocity.CorePlugin;
import dev.emortal.velocity.Environment;

import java.util.function.Consumer;

public class MessagingCore {
    private static final String KAFKA_HOST = System.getenv("KAFKA_HOST");
    private static final String KAFKA_PORT = System.getenv("KAFKA_PORT");

    private static final String KAFKA_CONNECTIONS_TOPIC = "mc-connections";

    private final RabbitMqCore rabbitMqCore;
    private final FriendlyKafkaConsumer kafkaConsumer;
    private final FriendlyKafkaProducer kafkaProducer;

    public MessagingCore(ProxyServer proxy, CorePlugin plugin) {
        this.rabbitMqCore = new RabbitMqCore();

        KafkaSettings kafkaSettings = new KafkaSettings()
                .setAutoCommit(true)
                .setBootstrapServers(KAFKA_HOST + ":" + KAFKA_PORT);

        this.kafkaConsumer = new FriendlyKafkaConsumer(kafkaSettings);
        this.kafkaProducer = new FriendlyKafkaProducer(kafkaSettings);

        // Register RabbitMQ events
        proxy.getEventManager().register(plugin, this.rabbitMqCore);
        // Register Kafka events
        proxy.getEventManager().register(plugin, this);

        // Register matchmaker parsers
        KurushimiUtils.registerParserRegistry();
    }

    public <T extends AbstractMessage> void addListener(Class<T> messageType, Consumer<T> listener) {
        MessageProtoConfig<T> parser = ProtoParserRegistry.getParser(messageType);

        switch (parser.service()) {
            case KAFKA -> this.kafkaConsumer.addListener(messageType, listener);
            case RABBIT_MQ -> this.rabbitMqCore.addListener(messageType, listener);
            default -> throw new IllegalStateException("Unexpected value: " + parser.service());
        }
    }

    // Kafka producing
    @Subscribe
    public void onPlayerLogin(PostLoginEvent event) {
        PlayerConnectMessage message = PlayerConnectMessage.newBuilder()
                .setPlayerId(event.getPlayer().getUniqueId().toString())
                .setPlayerUsername(event.getPlayer().getUsername())
                .setServerId(Environment.getHostname())
                .build();

        this.kafkaProducer.produce(KAFKA_CONNECTIONS_TOPIC, message);
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        PlayerDisconnectMessage message = PlayerDisconnectMessage.newBuilder()
                .setPlayerId(event.getPlayer().getUniqueId().toString())
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
        this.rabbitMqCore.shutdown();
        this.kafkaConsumer.close();
    }
}
