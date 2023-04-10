package dev.emortal.velocity.messaging;

import com.google.protobuf.AbstractMessage;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import dev.emortal.api.message.common.PlayerConnectMessage;
import dev.emortal.api.message.common.PlayerDisconnectMessage;
import dev.emortal.api.message.common.PlayerSwitchServerMessage;
import dev.emortal.api.utils.parser.MessageProtoConfig;
import dev.emortal.api.utils.parser.ProtoParserRegistry;
import dev.emortal.velocity.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class RabbitMqCore {
    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMqCore.class);

    private static final String PROXY_ALL_EXCHANGE = "mc:proxy:all";
    private static final String CONNECTIONS_EXCHANGE = "mc:connections";

    private static final String HOST = System.getenv("RABBITMQ_HOST");
    private static final String USERNAME = System.getenv("RABBITMQ_USERNAME");
    private static final String PASSWORD = System.getenv("RABBITMQ_PASSWORD");

    private final Map<Class<?>, Set<Consumer<AbstractMessage>>> protoListeners = new ConcurrentHashMap<>();
    // boundExchanges contains "exchange:routingKey"
    private final Set<String> boundExchanges = ConcurrentHashMap.newKeySet();

    private final String selfQueueName;
    private final Connection connection;
    private final Channel channel;

    RabbitMqCore() {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(HOST);
        connectionFactory.setUsername(USERNAME);
        connectionFactory.setPassword(PASSWORD);

        try {
            this.connection = connectionFactory.newConnection();
            this.channel = this.connection.createChannel();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String selfQueueName = null;

        try {
            AMQP.Queue.DeclareOk declareOk = this.channel.queueDeclare(Environment.getHostname(), false, true, true, null);
            selfQueueName = declareOk.getQueue();
            this.channel.queueBind(selfQueueName, PROXY_ALL_EXCHANGE, "");

            LOGGER.info("Listening for messages on queue {}", selfQueueName);
            this.channel.basicConsume(selfQueueName, true, (consumerTag, delivery) -> {
                String type = delivery.getProperties().getType();

                if (type == null) {
                    LOGGER.warn("Received message with no type header {}", delivery);
                    return;
                }

                AbstractMessage message = ProtoParserRegistry.parse(type, delivery.getBody());
                Set<Consumer<AbstractMessage>> listeners = this.protoListeners.get(message.getClass());

                try {
                    listeners.forEach(consumer -> consumer.accept(message));
                } catch (Exception ex) {
                    LOGGER.error("Failed to handle message of type {}", type, ex);
                }
            }, consumerTag -> LOGGER.warn("Consumer cancelled"));
        } catch (IOException ex) {
            LOGGER.error("Failed to bind to proxy all exchange", ex);
        }

        this.selfQueueName = selfQueueName;
    }

    @SuppressWarnings("unchecked")
    <T extends AbstractMessage> void addListener(Class<T> messageType, Consumer<T> listener) {
        MessageProtoConfig<T> parser = ProtoParserRegistry.getParser(messageType);
        if (parser.exchangeName() != null) {
            String routingKey = parser.routingKey() == null ? "" : parser.routingKey();
            try {
                this.channel.queueBind(this.selfQueueName, parser.exchangeName(), routingKey);
                this.boundExchanges.add(parser.exchangeName() + ":" + routingKey);
            } catch (IOException e) {
                LOGGER.error("Failed to bind to exchange {} with routing key {}", parser.exchangeName(), parser.routingKey(), e);
            }
        }

        this.protoListeners.computeIfAbsent(messageType, k -> ConcurrentHashMap.newKeySet()).add((Consumer<AbstractMessage>) listener);
    }

    @Subscribe
    public void onPlayerLogin(PostLoginEvent event) {
        PlayerConnectMessage message = PlayerConnectMessage.newBuilder()
                .setPlayerId(event.getPlayer().getUniqueId().toString())
                .setPlayerUsername(event.getPlayer().getUsername())
                .setServerId(Environment.getHostname())
                .build();


        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .timestamp(new Date())
                .type(message.getDescriptorForType().getFullName())
                .appId(Environment.getHostname())
                .build();

        try {
            this.channel.basicPublish(CONNECTIONS_EXCHANGE, "player_login", properties, message.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        PlayerDisconnectMessage message = PlayerDisconnectMessage.newBuilder()
                .setPlayerId(event.getPlayer().getUniqueId().toString())
                .build();

        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .timestamp(new Date())
                .type(message.getDescriptorForType().getFullName())
                .appId(Environment.getHostname())
                .build();

        try {
            this.channel.basicPublish(CONNECTIONS_EXCHANGE, "player_disconnect", properties, message.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Subscribe
    public void onServerSwitch(ServerConnectedEvent event) {
        PlayerSwitchServerMessage message = PlayerSwitchServerMessage.newBuilder()
                .setPlayerId(event.getPlayer().getUniqueId().toString())
                .setServerId(event.getServer().getServerInfo().getName())
                .build();

        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .timestamp(new Date())
                .type(message.getDescriptorForType().getFullName())
                .appId(Environment.getHostname())
                .build();

        try {
            this.channel.basicPublish(CONNECTIONS_EXCHANGE, "player_switch_server", properties, message.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void shutdown() {
        try {
            this.channel.close();
            this.connection.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
