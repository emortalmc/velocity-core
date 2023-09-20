package dev.emortal.velocity.adapter.server;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import dev.emortal.api.model.matchmaker.Assignment;
import org.jetbrains.annotations.NotNull;

public interface ServerProvider {

    @NotNull RegisteredServer createServer(@NotNull String name, @NotNull String address, int port);

    default @NotNull RegisteredServer createServerFromAssignment(@NotNull Assignment assignment) {
        return this.createServer(assignment.getServerId(), assignment.getServerAddress(), assignment.getServerPort());
    }
}
