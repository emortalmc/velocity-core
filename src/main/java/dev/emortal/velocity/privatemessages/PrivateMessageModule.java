package dev.emortal.velocity.privatemessages;

import dev.emortal.api.modules.annotation.Dependency;
import dev.emortal.api.modules.annotation.ModuleData;
import dev.emortal.api.service.messagehandler.MessageService;
import dev.emortal.api.utils.GrpcStubCollection;
import dev.emortal.velocity.command.CommandModule;
import dev.emortal.velocity.messaging.MessagingModule;
import dev.emortal.velocity.module.VelocityModule;
import dev.emortal.velocity.module.VelocityModuleEnvironment;
import dev.emortal.velocity.privatemessages.commands.MessageCommand;
import dev.emortal.velocity.privatemessages.commands.MessageSender;
import dev.emortal.velocity.privatemessages.commands.ReplyCommand;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ModuleData(name = "private-messages", dependencies = {@Dependency(name = "messaging"), @Dependency(name = "command")})
public final class PrivateMessageModule extends VelocityModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrivateMessageModule.class);

    public PrivateMessageModule(@NotNull VelocityModuleEnvironment environment) {
        super(environment);
    }

    @Override
    public boolean onLoad() {
        MessageService service = GrpcStubCollection.getMessageHandlerService().orElse(null);
        if (service == null) {
            LOGGER.warn("Message service unavailable. Private messaging will not work.");
            return false;
        }

        MessagingModule messaging = this.getModule(MessagingModule.class);
        LastMessageCache lastMessageCache = new LastMessageCache();

        super.registerEventListener(new PrivateMessageListener(super.getProxy(), messaging, lastMessageCache));
        super.registerEventListener(lastMessageCache);

        MessageSender messageSender = new MessageSender(service);
        CommandModule commandModule = this.getModule(CommandModule.class);
        commandModule.registerCommand(new MessageCommand(messageSender, commandModule.getUsernameSuggestions()));
        commandModule.registerCommand(new ReplyCommand(messageSender, lastMessageCache));

        return true;
    }

    @Override
    public void onUnload() {
        // do nothing
    }
}
