package dev.emortal.velocity.adapter.scheduler;

import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.Scheduler;
import dev.emortal.velocity.CorePlugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public final class VelocityEmortalScheduler implements EmortalScheduler {

    private final Scheduler scheduler;
    private final CorePlugin plugin;

    public VelocityEmortalScheduler(@NotNull Scheduler scheduler, @NotNull CorePlugin plugin) {
        this.scheduler = scheduler;
        this.plugin = plugin;
    }

    @Override
    public @NotNull ScheduledTask repeat(@NotNull Runnable task, long period, @NotNull TimeUnit unit) {
        return this.scheduler.buildTask(this.plugin, task).repeat(period, unit).schedule();
    }
}
