package dev.emortal.velocity.lang;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public enum ChatMessages {

    GENERIC_ERROR(red("An unknown error occurred")),

    PLAYER_NOT_FOUND(red("Player {0} not found")),
    PLAYER_NOT_ONLINE(red("{0} is not online")),

    NO_LUNAR(Component.text("""
            EmortalMC has a strict no Lunar Client policy

            Please consider using a more capable client, such as Fabric""")),

    SENDING_TO_LOBBY(green("Sending you to the lobby...")),
    ERROR_SENDING_TO_LOBBY(red("Something went wrong while sending you to the lobby!")),
    ERROR_CONNECTING_TO_LOBBY(red("Failed to connect to lobby")),

    SENDING_TO_SERVER("<green>Sending you to <gold>{0}<green>..."),

    // Command responses
    YOU_CLOSED_PARTY(green("The party is now closed")),
    YOU_DISBANDED_PARTY(green("Party disbanded")),
    YOU_INVITED_PLAYER_TO_PARTY(green("{0} has been invited to the party")),
    YOU_JOINED_PARTY(green("Joined {0}'s party")),
    YOU_KICKED_PLAYER_FROM_PARTY(green("Kicked {0} from your party")),
    YOU_UPDATED_PARTY_LEADER(green("Updated the party leader to {0}")),
    YOU_LEFT_PARTY(green("Left party")),
    PARTY_LIST("""
            <color:#2383d1><strikethrough>          </strikethrough> <bold><color:#2ba0ff>ʏᴏᴜʀ ᴘᴀʀᴛʏ </bold>({0}) <strikethrough>          </strikethrough></color>

            {1}

            <color:#2383d1><strikethrough>                                          </strikethrough>"""),
    YOU_OPENED_PARTY(green("The party is now open")),
    PARTY_SETTINGS_HELP("""
            <light_purple>----- Party Settings Help -----
            /party settings
            /party settings <setting> <value>
            ---------------------------</light_purple>"""),
    PARTY_HELP("""
            <light_purple>------ Party Help ------
            /party invite <player>
            /party join <player>
            /party leave
            /party list
            /party open
                        
            /party kick <player>
            /party leader <player>
            /party disband
                        
            /party settings
            ----------------------</light_purple>"""),

    // Notifications
    PARTY_DISBANDED(red("The party you were in has been disbanded")),
    PLAYER_JOINED_PARTY(green("{0} has joined the party")),
    PLAYER_LEFT_PARTY(red("{0} has left the party")),
    PLAYER_KICKED_FROM_PARTY(red("{0} has been kicked from the party by {1}")),
    YOU_KICKED_FROM_PARTY(red("You have been kicked from the party by {0}")),
    PARTY_LEADER_CHANGED(green("{0} is now the party leader")),
    PLAYER_INVITED_TO_PARTY(green("{0} has invited {1} to the party")),
    // You have been invited to join {0}'s party. Click to accept
    YOU_INVITED_TO_PARTY("<color:#3db83d>You have been invited to join <green>{0}'s</green> party. " +
            "<b><gradient:light_purple:gold><click:run_command:'/party join {0}'>Click to accept</click></gradient></b>"),

    ERROR_PARTY_NO_PERMISSION(red("You must be the leader of the party to do this")),
    ERROR_NOT_PARTY_LEADER_DISBAND("""
            <red>You must be the leader of the party to do this</red>
            <aqua>Use '/party leave' to leave the party instead</aqua>"""),
    ERROR_PLAYER_INVITED_TO_PARTY(red("{0} has already been invited to your party")),
    ERROR_PLAYER_IN_THIS_PARTY(red("{0} is already in the party")),
    ERROR_YOU_NOT_INVITED_TO_PARTY(red("You were not invited to this party")),
    ERROR_YOU_IN_THIS_PARTY(red("You are already in the party")),
    ERROR_CANNOT_KICK_LEADER(red("You cannot kick the party leader")),
    ERROR_PLAYER_NOT_IN_PARTY(red("{0} is not in your party")),
    ERROR_CANNOT_LEAVE_AS_LEADER("""
            <red>You are the leader of the party
            <red>Use '/party disband' to disband the party or '/party leader <player>' to transfer leadership"""),
    ERROR_YOU_NOT_IN_PARTY(red("You are not in a party")),

    ROLE_CREATED(green("Role {0} created")),
    ROLE_DESCRIPTION("""
            <light_purple>----- Role Summary -----
            ID: {0}
            Priority: {1}
            Permissions: {2}
            Display Name: {3}
            -----------------------"""),
    ROLE_LIST_HEADER(Component.text("Role List ({0}):", NamedTextColor.LIGHT_PURPLE)),
    ROLE_LIST_LINE(Component.text()
            .content("{1}) {0}")
            .color(NamedTextColor.LIGHT_PURPLE)
            .hoverEvent(HoverEvent.showText(MiniMessage.miniMessage().deserialize("""
                    ID: {0}
                    Priority: {1}
                    Permissions: {2}
                    Display Name: {3}

                    Example Chat: {4}""")))
            .build()),
    PERMISSION_ADDED_TO_ROLE(green("Set {0} to {1} for role {2}")),
    PERMISSION_STATE(green("Role {0} has permission {1} set to {2}")),
    PERMISSION_REMOVED_FROM_ROLE(green("Unset {0} for role {1}")),
    ROLE_PRIORITY_SET(green("Role {0} priority set to {1}")),
    ROLE_USERNAME_SET(green("Role {0} username set to '{1}'")),
    USER_DESCRIPTION("""
            <light_purple>----- User Summary -----
            Username: {0}
            Groups: {1}
            Permissions: {2}
            Display Name: {3}
            Example Chat: <reset>{4}
            <light_purple>------------------------"""),
    USER_ROLE_ADDED(green("Role {0} added to {1}")),
    USER_ROLE_REMOVED(green("Role {0} removed from {1}")),
    PERMISSION_HELP("""
            <light_purple>-- Permission Help --
            <click:suggest_command:'/perm role'>/perm role</click>
            <click:suggest_command:'/perm user'>/perm user</click>
            <click:suggest_command:'/perm listroles'>/perm listroles</click>
            ------------------"""),
    ROLE_HELP("""
            <light_purple>--------- Role Permission Help ---------
            <click:suggest_command:'/perm role '>/perm role <name> create</click>
            <click:suggest_command:'/perm role '>/perm role <name> setusername <state></click>
            <click:suggest_command:'/perm role '>/perm role <name> setpriority <state></click>
            <click:suggest_command:'/perm role '>/perm role <name> permission add <perm></click>
            <click:suggest_command:'/perm role '>/perm role <name> permission unset <perm></click>
            <click:suggest_command:'/perm role '>/perm role <name> permission check <perm></click>
            ------------------------------------"""),
    USER_HELP("""
            <light_purple>--------- User Permission Help ---------
            <click:suggest_command:'/perm user '>/perm user <name> role add <group></click>
            <click:suggest_command:'/perm user '>/perm user <name> role remove <group></click>
            <click:suggest_command:'/perm user '>/perm user <name> permission check <perm></click>
            ------------------------------------"""),

    ERROR_ROLE_ALREADY_EXISTS(red("Role {0} already exists")),
    ERROR_ROLE_NOT_FOUND(red("Role {0} not found")),
    ERROR_PERMISSION_ALREADY_SET(red("Role {0} already has {1} set to {2}")),
    ERROR_MISSING_PERMISSION(red("Role {0} does not have {1} set")),
    ERROR_USER_NOT_FOUND(red("User {0} not found")),
    ERROR_USER_ALREADY_HAS_ROLE(red("User {0} already has role {1}")),
    ERROR_USER_MISSING_ROLE(red("User {0} does not have role {1}")),

    YOUR_PLAYTIME(Component.text("Your playtime is {0}", NamedTextColor.LIGHT_PURPLE)),
    OTHER_PLAYTIME(Component.text("{0}'s playtime is {1}", NamedTextColor.LIGHT_PURPLE)),

    PRIVATE_MESSAGE_SENT("<dark_purple>[<light_purple><bold>YOU</bold></light_purple> <gray>→</gray> <color:#ff9ef5>{0}</color>] <gray>{1}"),
    PRIVATE_MESSAGE_RECEIVED("<dark_purple>[<color:#ff9ef5>{0}</color> <gray>→</gray> <light_purple><bold>YOU</bold></light_purple>] <gray>{1}"),
    MESSAGE_USAGE(red("Usage: /msg <player> <message>")),
    REPLY_USAGE(red("Usage: /r <message>")),

    ERROR_CANNOT_MESSAGE_SELF(red("You cannot send a message to yourself")),
    ERROR_NO_ONE_TO_REPLY_TO(red("You have not received any messages yet")),
    ERROR_YOU_BLOCKED(red("You have blocked {0} so you cannot message them")),
    ERROR_THEY_BLOCKED(red("{0} has blocked you so you cannot message them")),

    YOU_BLOCKED(green("You have blocked {0}")),
    BLOCKED_PLAYERS(red("Blocked Players ({0}): {1}")),
    YOU_UNBLOCKED(green("{0} has been unblocked")),
    BLOCK_USAGE(red("Usage: /block <username>")),
    UNBLOCK_USAGE(red("Usage: /unblock <username>")),

    ERROR_CANNOT_BLOCK_SELF(red("You can't block yourself")),
    ERROR_ALREADY_BLOCKED(red("You have already blocked {0}")),
    ERROR_CANNOT_BLOCK_FRIEND(red("You must unfriend {0} before blocking them")),
    ERROR_BLOCKED_LIST_EMPTY(red("You have not blocked any players")),
    ERROR_CANNOT_UNBLOCK_SELF(red("You can't unblock yourself")),
    ERROR_NOT_BLOCKED(red("<red>You have not blocked the player")),

    FRIEND_ADDED("<light_purple>You are now friends with <color:#c98fff>{0}</color>"),
    SENT_FRIEND_REQUEST("<light_purple>Sent a friend request to <color:#c98fff>{0}</color>"),
    RECEIVED_FRIEND_REQUEST("<light_purple>You have received a friend request from <color:#c98fff>{0}</color> <click:run_command:'/friend add {0}'><green>ACCEPT</click> <reset><gray>| <click:run_command:'/friend deny {0}'><red>DENY</click>"),
    FRIEND_HELP("""
            <light_purple>------ Friend Help ------
            <click:run_command:'/friend list'>/friend list</click>
            <click:suggest_command:'/friend add '>/friend add <name></click>
            <click:suggest_command:'/friend remove '>/friend remove <name></click>
            <click:suggest_command:'/friend requests '>/friend requests <incoming/outgoing> [page]</click>
            <click:suggest_command:'/friend purge requests '>/friend purge requests <incoming/outgoing></click>
            -----------------------"""),
    FRIEND_REQUEST_DENIED("<light_purple>Removed your friend request from <color:#c98fff>{0}</color>"),
    FRIEND_REQUEST_REVOKED("<light_purple>Revoked your friend request to <color:#c98fff>{0}</color>"),
    FRIEND_LIST_HEADER(lightPurple("----- Friends (Page {0}/{1}) -----")),
    FRIEND_LIST_ONLINE_LINE("<click:suggest_command:'/message {0} '><green>{0} - {1}</green></click>"),
    FRIEND_LIST_OFFLINE_LINE(red("{0} - Seen {1}")),
    FRIEND_REMOVED("<light_purple>You are no longer friends with <color:#c98fff>{0}</color>"),
    PURGED_INCOMING_FRIEND_REQUESTS(lightPurple("Purged {0} incoming friend requests")),
    PURGED_OUTGOING_FRIEND_REQUESTS(lightPurple("Purged {0} outgoing friend requests")),
    INCOMING_FRIEND_REQUESTS_HEADER(lightPurple("--- Incoming Requests (Page {0}/{1}) ---")),
    OUTGOING_FRIEND_REQUESTS_HEADER(lightPurple("--- Outgoing Requests (Page {0}/{1}) ---")),
    INCOMING_FRIEND_REQUEST_LINE("<light_purple>{0} ago <dark_purple>- <light_purple>{1} <dark_purple>| " +
            "<green><click:run_command:'/friend add {1}'>Accept</click><dark_purple>/<red><click:run_command:'/friend deny {1}'>Deny</click>"),
    OUTGOING_FRIEND_REQUEST_LINE("<light_purple>{0} ago <dark_purple>- <light_purple>{1} <dark_purple>| " +
            "<red><click:run_command:'/friend revoke {1}'>Revoke</click>"),
    FRIEND_CONNECTED(green("Friend > {0} has connected")),
    FRIEND_DISCONNECTED(green("Friend > {0} has disconnected")),

    ERROR_ALREADY_FRIENDS("<light_purple>You are already friends with <color:#c98fff>{0}</color>"),
    ERROR_PRIVACY_BLOCKED("<color:#c98fff>{0}'s</color> <light_purple>privacy settings don't allow you you add them as a friend."),
    ERROR_FRIEND_ALREADY_REQUESTED("<light_purple>You have already sent a friend request to <color:#c98fff>{0}</color>"),
    ERROR_CANNOT_FRIEND_BLOCKED(red("<red>You cannot add <color:#c98fff>{0}</color> as a friend as you have blocked them!</red>")),
    ERROR_CANNOT_FRIEND_SELF(red("You can't add yourself as a friend")),
    ERROR_NO_FRIEND_REQUEST_SENT("<light_purple>You have not sent a friend request to <color:#c98fff>{0}</color>"),
    ERROR_NO_FRIEND_REQUEST_RECEIVED("<light_purple>You have not received a friend request from <color:#c98fff>{0}</color>"),
    ERROR_NO_FRIENDS(lightPurple("You have no friends. Use /friend add <name> to add someone.")),
    ERROR_NOT_FRIENDS("<light_purple>You are not friends with <color:#c98fff>{0}</color>"),
    ERROR_NO_INCOMING_FRIEND_REQUESTS(lightPurple("You have no incoming friend requests")),
    ERROR_NO_OUTGOING_FRIEND_REQUESTS(lightPurple("You have no outgoing friend requests")),

    RESOURCE_PACK_DOWNLOAD(Component.text("We love you")),
    RESOURCE_PACK_DECLINED(Component.text().color(NamedTextColor.GRAY)
            .append(Component.text("Using the resource pack is required. It isn't big and only has to be downloaded once.")).appendNewline()
            .append(Component.text("If the dialog is annoying, you can enable 'Server Resource Packs' when adding the server and the prompt will disappear."))
            .build()),
    RESOURCE_PACK_FAILED(Component.text().color(NamedTextColor.RED)
            .append(Component.text("The resource pack download failed.")).appendNewline()
            .append(Component.text("If the issue persists, contact a staff member"))
            .build()),

    // ▓▒░              ⚡   EmortalMC   ⚡              ░▒▓
    PING_MOTD("<light_purple>▓▒░              <bold>⚡   </bold></light_purple><gradient:gold:light_purple><bold>EmortalMC</bold></gradient><gold><bold>   ⚡</bold>              ░▒▓</gold>"),

    TAB_LIST_HEADER("""
            <gold>┌                                                  </gold><light_purple>┐ </light_purple>
            <gradient:gold:light_purple><bold>EmortalMC</bold></gradient>
            """),
    TAB_LIST_FOOTER("""
            <gray> </gray>
            <gray>{0} online</gray>
            <color:#266ee0>ᴍᴄ.ᴇᴍᴏʀᴛᴀʟ.ᴅᴇᴠ</color>
            <light_purple>└                                                  </light_purple><gold>┘ </gold>
            """);

    private static @NotNull Component green(@NotNull String text) {
        return Component.text(text, NamedTextColor.GREEN);
    }

    private static @NotNull Component red(@NotNull String text) {
        return Component.text(text, NamedTextColor.RED);
    }

    private static @NotNull Component lightPurple(@NotNull String text) {
        return Component.text(text, NamedTextColor.LIGHT_PURPLE);
    }

    private final @NotNull Component component;

    ChatMessages(@NotNull Component component) {
        this.component = component;
    }

    ChatMessages(@NotNull String miniMessageText) {
        this(MiniMessage.miniMessage().deserialize(miniMessageText));
    }

    public @NotNull Component parse(@NotNull Component... args) {
        return ChatMessageTranslator.translate(this.component, List.of(args));
    }

    public void send(@NotNull Audience target, @NotNull Component... args) {
        target.sendMessage(this.parse(args));
    }

    public void send(@NotNull Audience target) {
        target.sendMessage(this.component);
    }
}
