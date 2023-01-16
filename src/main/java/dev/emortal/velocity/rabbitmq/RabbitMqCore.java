package dev.emortal.velocity.rabbitmq;

import com.google.gson.Gson;
import com.google.protobuf.AbstractMessage;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import dev.emortal.api.utils.parser.ProtoParserRegistry;
import dev.emortal.velocity.Environment;
import dev.emortal.velocity.rabbitmq.types.ConnectEventDataPackage;
import dev.emortal.velocity.rabbitmq.types.DisconnectEventDataPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class RabbitMqCore {
    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMqCore.class);

    private static final String CONNECTIONS_EXCHANGE = "mc:connections";
    private static final String PROXY_ALL_EXCHANGE = "mc:proxy:all";

    private static final String HOST = System.getenv("RABBITMQ_HOST");
    private static final String USERNAME = System.getenv("RABBITMQ_USERNAME");
    private static final String PASSWORD = System.getenv("RABBITMQ_PASSWORD");

    private static final Gson GSON = new Gson();

    private final Map<Class<?>, Consumer<AbstractMessage>> protoListeners = new ConcurrentHashMap<>();
    private final Connection connection;
    private final Channel channel;
    private final String selfQueueName;

    public RabbitMqCore() {
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
            selfQueueName = this.channel.queueDeclare().getQueue();
            this.channel.queueBind(selfQueueName, PROXY_ALL_EXCHANGE, "");

            LOGGER.info("Listening for messages on queue {}", selfQueueName);
            this.channel.basicConsume(selfQueueName, true, (consumerTag, delivery) -> {
                String type = delivery.getProperties().getType();

                AbstractMessage message = ProtoParserRegistry.parse(type, delivery.getBody());

                Consumer<AbstractMessage> listener = this.protoListeners.get(message.getClass());
                if (listener == null) LOGGER.warn("No listener registered for message of type {}", type);
                else listener.accept(message);

            }, consumerTag -> LOGGER.warn("Consumer cancelled"));

        } catch (IOException ex) {
            LOGGER.error("Failed to bind to proxy all exchange", ex);
        }

        this.selfQueueName = selfQueueName;
    }

    @Subscribe
    public void onPlayerLogin(PostLoginEvent event) {
        ConnectEventDataPackage dataPackage = new ConnectEventDataPackage(event.getPlayer().getUniqueId(), event.getPlayer().getUsername());
        AMQP.BasicProperties basicProperties = new AMQP.BasicProperties.Builder()
                .timestamp(new Date())
                .type("connect")
                .appId(Environment.getHostname())
                .build();

        try {
            this.channel.basicPublish(CONNECTIONS_EXCHANGE, "", basicProperties, GSON.toJson(dataPackage).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        DisconnectEventDataPackage dataPackage = new DisconnectEventDataPackage(event.getPlayer().getUniqueId());
        AMQP.BasicProperties basicProperties = new AMQP.BasicProperties.Builder()
                .timestamp(new Date())
                .type("disconnect")
                .appId(Environment.getHostname())
                .build();

        try {
            this.channel.basicPublish(CONNECTIONS_EXCHANGE, "", basicProperties, GSON.toJson(dataPackage).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public <T extends AbstractMessage> void addListener(Class<T> message, Consumer<AbstractMessage> listener) {
        this.protoListeners.put(message, listener);
    }

    public void shutdown() {
        try {
            this.channel.close();
            this.connection.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
