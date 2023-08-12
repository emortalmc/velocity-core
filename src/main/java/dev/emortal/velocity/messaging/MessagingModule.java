package dev.emortal.velocity.messaging;

import com.google.protobuf.AbstractMessage;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.message.common.PlayerConnectMessage;
import dev.emortal.api.message.common.PlayerDisconnectMessage;
import dev.emortal.api.message.common.PlayerSwitchServerMessage;
import dev.emortal.api.modules.ModuleData;
import dev.emortal.api.utils.kafka.FriendlyKafkaConsumer;
import dev.emortal.api.utils.kafka.FriendlyKafkaProducer;
import dev.emortal.api.utils.kafka.KafkaSettings;
import dev.emortal.velocity.Environment;
import dev.emortal.velocity.module.VelocityModule;
import dev.emortal.velocity.module.VelocityModuleEnvironment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

@ModuleData(name = "messaging", required = false)
public final class MessagingModule extends VelocityModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessagingModule.class);

    private static final String KAFKA_HOST = System.getenv("KAFKA_HOST");
    private static final String KAFKA_PORT = System.getenv("KAFKA_PORT");

    private static final String KAFKA_CONNECTIONS_TOPIC = "mc-connections";

    private @Nullable FriendlyKafkaConsumer kafkaConsumer;
    private @Nullable FriendlyKafkaProducer kafkaProducer;

    public MessagingModule(@NotNull VelocityModuleEnvironment environment) {
        super(environment);
    }

    public <T extends AbstractMessage> void addListener(@NotNull Class<T> messageType, @NotNull Consumer<T> listener) {
        if (this.kafkaConsumer != null) this.kafkaConsumer.addListener(messageType, listener);
    }

    @Override
    public boolean onLoad() {
        if (KAFKA_HOST == null || KAFKA_PORT == null) {
            LOGGER.warn("Kafka host or port not available. Messaging will not be available.");
            return false;
        }

        KafkaSettings kafkaSettings = KafkaSettings.builder().bootstrapServers(KAFKA_HOST + ":" + KAFKA_PORT).build();

        this.kafkaConsumer = new FriendlyKafkaConsumer(kafkaSettings);
        this.kafkaProducer = new FriendlyKafkaProducer(kafkaSettings);

        super.registerEventListener(this);

        return true;
    }

    @Override
    public void onUnload() {
        if (this.kafkaProducer != null) this.kafkaProducer.shutdown();
        if (this.kafkaConsumer != null) this.kafkaConsumer.close();
    }

    // Kafka producing
    @Subscribe
    private void onPlayerLogin(@NotNull PostLoginEvent event) {
        if (this.kafkaProducer == null) return;

        Player player = event.getPlayer();

        PlayerConnectMessage message = PlayerConnectMessage.newBuilder()
                .setPlayerId(player.getUniqueId().toString())
                .setPlayerUsername(player.getUsername())
                .setServerId(Environment.getHostname())
                .build();

        this.kafkaProducer.produceAndForget(KAFKA_CONNECTIONS_TOPIC, message);
    }

    @Subscribe
    private void onPlayerDisconnect(@NotNull DisconnectEvent event) {
        if (this.kafkaProducer == null) return;

        Player player = event.getPlayer();

        PlayerDisconnectMessage message = PlayerDisconnectMessage.newBuilder()
                .setPlayerId(player.getUniqueId().toString())
                .setPlayerUsername(player.getUsername())
                .build();

        this.kafkaProducer.produceAndForget(KAFKA_CONNECTIONS_TOPIC, message);
    }

    @Subscribe
    private void onServerSwitch(@NotNull ServerConnectedEvent event) {
        if (this.kafkaProducer == null) return;

        PlayerSwitchServerMessage message = PlayerSwitchServerMessage.newBuilder()
                .setPlayerId(event.getPlayer().getUniqueId().toString())
                .setServerId(event.getServer().getServerInfo().getName())
                .build();

        this.kafkaProducer.produceAndForget(KAFKA_CONNECTIONS_TOPIC, message);
    }
}
