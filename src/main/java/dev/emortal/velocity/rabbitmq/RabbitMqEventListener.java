package dev.emortal.velocity.rabbitmq;

import dev.emortal.velocity.Environment;
import dev.emortal.velocity.rabbitmq.types.DisconnectEventDataPackage;
import dev.emortal.velocity.rabbitmq.types.ConnectEventDataPackage;
import com.google.gson.Gson;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class RabbitMqEventListener {
    private static final String CONNECTIONS_EXCHANGE = "mc:connections";

    private static final String HOST = System.getenv("RABBITMQ_HOST");
    private static final String USERNAME = System.getenv("RABBITMQ_USERNAME");
    private static final String PASSWORD = System.getenv("RABBITMQ_PASSWORD");

    private static final Gson GSON = new Gson();

    private final Connection connection;
    private final Channel channel;

    public RabbitMqEventListener() {
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

    public void shutdown() {
        try {
            this.channel.close();
            this.connection.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
