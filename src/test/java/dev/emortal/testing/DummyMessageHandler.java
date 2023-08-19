package dev.emortal.testing;

import com.google.protobuf.AbstractMessage;
import dev.emortal.velocity.messaging.MessageHandler;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public final class DummyMessageHandler implements MessageHandler {

    @Override
    public <T extends AbstractMessage> void addListener(@NotNull Class<T> messageType, @NotNull Consumer<T> listener) {
    }
}
