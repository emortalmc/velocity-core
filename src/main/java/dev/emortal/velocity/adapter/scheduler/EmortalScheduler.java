package dev.emortal.velocity.adapter.scheduler;

import com.velocitypowered.api.scheduler.ScheduledTask;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public interface EmortalScheduler {

    @NotNull ScheduledTask repeat(@NotNull Runnable task, long period, @NotNull TimeUnit unit);
}
