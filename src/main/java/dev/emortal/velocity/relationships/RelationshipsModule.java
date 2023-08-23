package dev.emortal.velocity.relationships;

import dev.emortal.api.liveconfigparser.configs.gamemode.GameModeCollection;
import dev.emortal.api.modules.annotation.Dependency;
import dev.emortal.api.modules.annotation.ModuleData;
import dev.emortal.api.service.mcplayer.McPlayerService;
import dev.emortal.api.service.relationship.RelationshipService;
import dev.emortal.api.utils.GrpcStubCollection;
import dev.emortal.velocity.liveconfig.LiveConfigModule;
import dev.emortal.velocity.messaging.MessagingModule;
import dev.emortal.velocity.module.VelocityModule;
import dev.emortal.velocity.module.VelocityModuleEnvironment;
import dev.emortal.velocity.player.suggestions.UsernameSuggesterProvider;
import dev.emortal.velocity.relationships.commands.block.BlockCommand;
import dev.emortal.velocity.relationships.commands.block.ListBlocksCommand;
import dev.emortal.velocity.relationships.commands.block.UnblockCommand;
import dev.emortal.velocity.relationships.commands.friend.FriendCommand;
import dev.emortal.velocity.relationships.listeners.FriendConnectionListener;
import dev.emortal.velocity.relationships.listeners.FriendListener;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ModuleData(name = "relationships", dependencies = {@Dependency(name = "messaging"), @Dependency(name = "live-config")})
public final class RelationshipsModule extends VelocityModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(RelationshipsModule.class);

    public RelationshipsModule(@NotNull VelocityModuleEnvironment environment) {
        super(environment);
    }

    @Override
    public boolean onLoad() {
        McPlayerService playerService = GrpcStubCollection.getPlayerService().orElse(null);
        MessagingModule messaging = this.getModule(MessagingModule.class);

        RelationshipService service = GrpcStubCollection.getRelationshipService().orElse(null);
        if (service == null) {
            LOGGER.warn("Relationship service unavailable. Relationships will not work.");
            return false;
        }

        FriendCache cache = new FriendCache(service);

        new FriendListener(super.adapters().playerProvider(), messaging, cache);
        new FriendConnectionListener(super.adapters().playerProvider(), messaging);
        super.registerEventListener(cache);

        LiveConfigModule liveConfig = this.getModule(LiveConfigModule.class);
        GameModeCollection gameModes = liveConfig.getGameModes();

        if (playerService == null) {
            LOGGER.warn("Relationship commands will not be registered due to missing player service.");
            return true;
        }

        UsernameSuggesterProvider usernameSuggesters = super.adapters().commandManager().usernameSuggesters();
        super.registerCommand(new FriendCommand(playerService, service, usernameSuggesters, cache, gameModes));
        super.registerCommand(new BlockCommand(playerService, service, usernameSuggesters));
        super.registerCommand(new UnblockCommand(playerService, service, usernameSuggesters));
        super.registerCommand(new ListBlocksCommand(playerService, service));

        return true;
    }

    @Override
    public void onUnload() {
        // do nothing
    }
}
