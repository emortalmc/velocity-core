package dev.emortal.velocity.relationships;

import dev.emortal.api.liveconfigparser.configs.gamemode.GameModeCollection;
import dev.emortal.api.modules.ModuleData;
import dev.emortal.api.service.mcplayer.McPlayerService;
import dev.emortal.api.service.relationship.RelationshipService;
import dev.emortal.api.utils.GrpcStubCollection;
import dev.emortal.velocity.liveconfig.LiveConfigModule;
import dev.emortal.velocity.messaging.MessagingModule;
import dev.emortal.velocity.module.VelocityModule;
import dev.emortal.velocity.module.VelocityModuleEnvironment;
import dev.emortal.velocity.player.PlayerServiceModule;
import dev.emortal.velocity.player.UsernameSuggestions;
import dev.emortal.velocity.relationships.commands.block.BlockCommand;
import dev.emortal.velocity.relationships.commands.friend.FriendCommand;
import dev.emortal.velocity.relationships.listeners.FriendConnectionListener;
import dev.emortal.velocity.relationships.listeners.FriendListener;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ModuleData(name = "relationships", required = false, softDependencies = {PlayerServiceModule.class, MessagingModule.class, LiveConfigModule.class})
public final class RelationshipsModule extends VelocityModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(RelationshipsModule.class);

    public RelationshipsModule(@NotNull VelocityModuleEnvironment environment) {
        super(environment);
    }

    @Override
    public boolean onLoad() {
        PlayerServiceModule playerServiceModule = this.getModule(PlayerServiceModule.class);
        if (playerServiceModule == null || playerServiceModule.getPlayerService() == null) {
            LOGGER.warn("Relationship services will not work due to missing player services.");
            return false;
        }

        McPlayerService playerService = playerServiceModule.getPlayerService();
        UsernameSuggestions usernameSuggestions = playerServiceModule.getUsernameSuggestions();

        MessagingModule messaging = this.getModule(MessagingModule.class);
        if (messaging == null) {
            LOGGER.debug("Relationship services cannot be loaded due to missing messaging module.");
            return false;
        }

        RelationshipService service = GrpcStubCollection.getRelationshipService()
                .orElse(null);
        if (service == null) {
            LOGGER.warn("Relationship service unavailable. Relationships will not work.");
            return false;
        }

        FriendCache cache = new FriendCache(service);

        new FriendListener(super.getProxy(), messaging, cache);
        new FriendConnectionListener(super.getProxy(), messaging);
        super.registerEventListener(cache);

        LiveConfigModule liveConfig = this.getModule(LiveConfigModule.class);
        GameModeCollection gameModes = liveConfig != null ? liveConfig.getGameModes() : null;

        new FriendCommand(super.getProxy(), playerService, service, usernameSuggestions, cache, gameModes);
        new BlockCommand(super.getProxy(), playerService, service, usernameSuggestions);

        return true;
    }

    @Override
    public void onUnload() {
        // do nothing
    }
}
