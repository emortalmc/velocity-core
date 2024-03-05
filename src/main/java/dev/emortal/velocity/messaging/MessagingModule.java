package dev.emortal.velocity.messaging;

import com.google.protobuf.AbstractMessage;
import dev.emortal.api.modules.annotation.ModuleData;
import dev.emortal.api.utils.kafka.FriendlyKafkaConsumer;
import dev.emortal.api.utils.kafka.FriendlyKafkaProducer;
import dev.emortal.api.utils.kafka.KafkaSettings;
import dev.emortal.velocity.module.VelocityModule;
import dev.emortal.velocity.module.VelocityModuleEnvironment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

@ModuleData(name = "messaging")
public final class MessagingModule extends VelocityModule implements MessageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessagingModule.class);

    private static final String KAFKA_HOST = System.getenv("KAFKA_HOST");
    private static final String KAFKA_PORT = System.getenv("KAFKA_PORT");

    private @Nullable FriendlyKafkaConsumer kafkaConsumer;
    private @Nullable FriendlyKafkaProducer kafkaProducer;

    public MessagingModule(@NotNull VelocityModuleEnvironment environment) {
        super(environment);
    }

    @Override
    public <T extends AbstractMessage> void addListener(@NotNull Class<T> messageType, @NotNull Consumer<T> listener) {
        if (this.kafkaConsumer != null) this.kafkaConsumer.addListener(messageType, listener);
    }

    @Override
    public boolean onLoad() {
        if (KAFKA_HOST == null || KAFKA_PORT == null) {
            LOGGER.warn("Kafka host or port not available. Messaging will not be available.");
            return false;
        }

        KafkaSettings settings = KafkaSettings.builder().bootstrapServers(KAFKA_HOST + ":" + KAFKA_PORT).build();
        this.kafkaConsumer = new FriendlyKafkaConsumer(settings);
        this.kafkaProducer = new FriendlyKafkaProducer(settings);

        super.registerEventListener(new PlayerUpdateListener(this.kafkaProducer));
        return true;
    }

    public @NotNull FriendlyKafkaProducer getKafkaProducer() {
        if (this.kafkaProducer == null) throw new IllegalStateException("Kafka producer not available");
        return this.kafkaProducer;
    }

    @Override
    public void onUnload() {
        if (this.kafkaProducer != null) this.kafkaProducer.shutdown();
        if (this.kafkaConsumer != null) this.kafkaConsumer.close();
    }
}
