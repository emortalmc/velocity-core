package dev.emortal.velocity.relationships;

import dev.emortal.api.liveconfigparser.configs.gamemode.GameModeCollection;
import dev.emortal.api.modules.annotation.Dependency;
import dev.emortal.api.modules.annotation.ModuleData;
import dev.emortal.api.service.mcplayer.McPlayerService;
import dev.emortal.api.service.relationship.RelationshipService;
import dev.emortal.api.utils.GrpcStubCollection;
import dev.emortal.velocity.command.CommandModule;
import dev.emortal.velocity.liveconfig.LiveConfigModule;
import dev.emortal.velocity.messaging.MessagingModule;
import dev.emortal.velocity.module.VelocityModule;
import dev.emortal.velocity.module.VelocityModuleEnvironment;
import dev.emortal.velocity.player.PlayerServiceModule;
import dev.emortal.velocity.player.UsernameSuggestions;
import dev.emortal.velocity.relationships.commands.block.BlockCommand;
import dev.emortal.velocity.relationships.commands.block.ListBlocksCommand;
import dev.emortal.velocity.relationships.commands.block.UnblockCommand;
import dev.emortal.velocity.relationships.commands.friend.FriendCommand;
import dev.emortal.velocity.relationships.listeners.FriendConnectionListener;
import dev.emortal.velocity.relationships.listeners.FriendListener;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ModuleData(
        name = "relationships",
        dependencies = {@Dependency(name = "player-service"), @Dependency(name = "messaging"), @Dependency(name = "live-config"), @Dependency(name = "command")}
)
public final class RelationshipsModule extends VelocityModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(RelationshipsModule.class);

    public RelationshipsModule(@NotNull VelocityModuleEnvironment environment) {
        super(environment);
    }

    @Override
    public boolean onLoad() {
        McPlayerService playerService = this.getModule(PlayerServiceModule.class).getPlayerService();
        MessagingModule messaging = this.getModule(MessagingModule.class);

        RelationshipService service = GrpcStubCollection.getRelationshipService().orElse(null);
        if (service == null) {
            LOGGER.warn("Relationship service unavailable. Relationships will not work.");
            return false;
        }

        FriendCache cache = new FriendCache(service);

        new FriendListener(super.getProxy(), messaging, cache);
        new FriendConnectionListener(super.getProxy(), messaging);
        super.registerEventListener(cache);

        LiveConfigModule liveConfig = this.getModule(LiveConfigModule.class);
        GameModeCollection gameModes = liveConfig.getGameModes();

        if (playerService == null) {
            LOGGER.warn("Relationship commands will not be registered due to missing player service.");
            return true;
        }

        CommandModule commandModule = this.getModule(CommandModule.class);
        UsernameSuggestions usernameSuggestions = commandModule.getUsernameSuggestions();
        commandModule.registerCommand(new FriendCommand(playerService, service, usernameSuggestions, cache, gameModes));
        commandModule.registerCommand(new BlockCommand(playerService, service, usernameSuggestions));
        commandModule.registerCommand(new UnblockCommand(playerService, service, usernameSuggestions));
        commandModule.registerCommand(new ListBlocksCommand(playerService, service));

        return true;
    }

    @Override
    public void onUnload() {
        // do nothing
    }
}
