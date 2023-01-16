package dev.emortal.velocity.listener;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import dev.emortal.api.messaging.general.ProxyServerSwitchMessage;
import dev.emortal.api.service.ServerDiscoveryProto;
import dev.emortal.velocity.rabbitmq.RabbitMqCore;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ServerChangeNotificationListener {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String TELEPORT_MESSAGE = "<green>Sending you to <gold><server_id><green>...</green>";

    public ServerChangeNotificationListener(ProxyServer proxy, RabbitMqCore rabbitMq) {
        rabbitMq.addListener(ProxyServerSwitchMessage.class, message -> {
            ProxyServerSwitchMessage switchMessage = (ProxyServerSwitchMessage) message;

            Set<Player> presentPlayers = new HashSet<>();
            for (String playerIdStr : switchMessage.getPlayerIdsList()) {
                UUID playerId = UUID.fromString(playerIdStr);
                proxy.getPlayer(playerId).ifPresent(presentPlayers::add);
            }

            if (presentPlayers.isEmpty()) return;

            ServerDiscoveryProto.ConnectableServer connectableServer = switchMessage.getServer();
            RegisteredServer server = proxy.getServer(connectableServer.getId()).orElse(null);
            if (server == null) {
                InetSocketAddress address = InetSocketAddress.createUnresolved(connectableServer.getAddress(), connectableServer.getPort());
                server = proxy.registerServer(new ServerInfo(connectableServer.getId(), address));
            }

            for (Player player : presentPlayers) {
                player.sendMessage(MINI_MESSAGE.deserialize(TELEPORT_MESSAGE, Placeholder.unparsed("server_id", connectableServer.getId())));
                player.createConnectionRequest(server).fireAndForget();
            }
        });
    }
}
