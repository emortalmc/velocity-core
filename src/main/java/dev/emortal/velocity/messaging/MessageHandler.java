package dev.emortal.velocity.messaging;

import com.google.protobuf.AbstractMessage;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public interface MessageHandler {

    <T extends AbstractMessage> void addListener(@NotNull Class<T> messageType, @NotNull Consumer<T> listener);
}
