package dev.emortal.velocity.messaging;

import com.google.protobuf.AbstractMessage;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.emortal.api.utils.kafka.KafkaCore;
import dev.emortal.api.utils.kafka.KafkaSettings;
import dev.emortal.api.utils.parser.MessageProtoConfig;
import dev.emortal.api.utils.parser.ProtoParserRegistry;
import dev.emortal.velocity.CorePlugin;

import java.util.function.Consumer;

public class MessagingCore {
    private static final String KAFKA_HOST = System.getenv("KAFKA_HOST");
    private static final String KAFKA_PORT = System.getenv("KAFKA_PORT");

    private final RabbitMqCore rabbitMqCore;
    private final KafkaCore kafkaCore;

    public MessagingCore(ProxyServer proxy, CorePlugin plugin) {
        this.rabbitMqCore = new RabbitMqCore();
        this.kafkaCore = new KafkaCore(
                KafkaSettings.builder()
                        .autoCommit(true)
                        .bootstrapServers(KAFKA_HOST + ":" + KAFKA_PORT).build()
        );

        // Register RabbitMQ events
        proxy.getEventManager().register(plugin, this.rabbitMqCore);
    }

    public <T extends AbstractMessage> void setListener(Class<T> messageType, Consumer<T> listener) {
        MessageProtoConfig<T> parser = ProtoParserRegistry.getParser(messageType);

        switch (parser.service()) {
            case KAFKA -> this.kafkaCore.setListener(messageType, listener);
            case RABBIT_MQ -> this.rabbitMqCore.setListener(messageType, listener);
            default -> throw new IllegalStateException("Unexpected value: " + parser.service());
        }
    }

    public void shutdown() {
        this.rabbitMqCore.shutdown();
        this.kafkaCore.shutdown();
    }
}
