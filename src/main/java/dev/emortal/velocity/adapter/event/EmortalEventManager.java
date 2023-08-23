package dev.emortal.velocity.adapter.event;

import org.jetbrains.annotations.NotNull;

public interface EmortalEventManager {

    void register(@NotNull Object listener);
}
