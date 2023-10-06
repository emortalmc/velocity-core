package dev.emortal.velocity.lang;

import com.velocitypowered.api.permission.Tristate;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

public interface ChatMessages {

    Args0 GENERIC_ERROR = () -> red("An unknown error occurred");

    Args1<String> PLAYER_NOT_FOUND = username -> red("Player " + username + " not found");
    Args1<String> PLAYER_NOT_ONLINE = username -> red("Player " + username + " is not online");

    Args0 NO_LUNAR = () -> Component.text("""
            EmortalMC has a strict no Lunar Client policy

            Please consider using a more capable client, such as Fabric""");

    Args0 SENDING_TO_LOBBY = () -> green("Sending you to the lobby...");
    Args0 ERROR_SENDING_TO_LOBBY = () -> red("Something went wrong while sending you to the lobby!");
    Args0 ERROR_CONNECTING_TO_LOBBY = () -> red("Failed to connect to lobby");

    Args1<String> SENDING_TO_SERVER = serverName -> Component.text()
            .append(green("Sending you to "))
            .append(Component.text(serverName, NamedTextColor.GOLD))
            .append(green("..."))
            .build();

    // Command responses
    Args0 YOU_CLOSED_PARTY = () -> green("The party is now closed");
    Args0 YOU_DISBANDED_PARTY = () -> green("Party disbanded");
    Args1<String> YOU_INVITED_PLAYER_TO_PARTY = username -> green(username + " has been invited to the party");
    Args1<String> YOU_JOINED_PARTY = username -> green("Joined " + username + "'s party");
    Args1<String> YOU_KICKED_PLAYER_FROM_PARTY = username -> green("Kicked " + username + " from your party");
    Args1<String> YOU_UPDATED_PARTY_LEADER = username -> green("Updated the party leader to " + username);
    Args0 YOU_LEFT_PARTY = () -> green("Left party");
    Args2<Integer, Component> PARTY_LIST = (size, members) -> {
        Component beforeAndAfterHeader = Component.text("          ", MessageColors.PARTY_LIST_BLUE, TextDecoration.STRIKETHROUGH);
        Component header = Component.text()
                .append(beforeAndAfterHeader)
                .append(Component.text(" ʏᴏᴜʀ ᴘᴀʀᴛʏ ", MessageColors.PARTY_LIST_HEADER_BLUE, TextDecoration.BOLD))
                .append(Component.text("(" + size + ") ", MessageColors.PARTY_LIST_HEADER_BLUE))
                .append(beforeAndAfterHeader)
                .build();
        Component footer = Component.text("                                            ", MessageColors.PARTY_LIST_BLUE, TextDecoration.STRIKETHROUGH);
        return Component.text()
                .append(header)
                .appendNewline()
                .appendNewline()
                .append(members)
                .appendNewline()
                .appendNewline()
                .append(footer)
                .build();
    };
    Args0 YOU_OPENED_PARTY = () -> green("The party is now open");
    Args0 PARTY_SETTINGS_HELP = () -> lightPurple("""
            ----- Party Settings Help -----
            /party settings
            /party settings <setting> <value>
            ---------------------------""");
    Args0 PARTY_HELP = () -> lightPurple("""
            ------ Party Help ------
            /party invite <player>
            /party join <player>
            /party leave
            /party list
            /party open

            /party kick <player>
            /party leader <player>
            /party disband

            /party settings
            ----------------------""");

    // Notifications
    Args0 PARTY_DISBANDED = () -> red("The party you were in has been disbanded");
    Args1<String> PLAYER_JOINED_PARTY = username -> green(username + " has joined the party");
    Args1<String> PLAYER_LEFT_PARTY = username -> red(username + " has left the party");
    Args2<String, String> PLAYER_KICKED_FROM_PARTY = (target, sender) -> red(target + " has been kicked from the party by " + sender);
    Args1<String> YOU_KICKED_FROM_PARTY = username -> red("You have been kicked from the party by " + username);
    Args1<String> PARTY_LEADER_CHANGED = username -> green(username + " is now the party leader");
    Args2<String, String> PLAYER_INVITED_TO_PARTY = (sender, target) -> green(sender + " has invited " + target + " to the party");
    // You have been invited to join <username>'s party. Click to accept
    Args1<String> YOU_INVITED_TO_PARTY = username -> {
        ClickEvent clickEvent = ClickEvent.runCommand("/party join " + username);
        return Component.text()
                .append(Component.text("You have been invited to join ", TextColor.color(0x3db83d)))
                .append(Component.text(username + "'s", NamedTextColor.GREEN))
                .append(Component.text("party. ", TextColor.color(0x3db83d)))
                .append(miniMessage("<b><gradient:light_purple:gold>Click to accept</gradient></b>").clickEvent(clickEvent))
                .build();
    };

    Args0 ERROR_PARTY_NO_PERMISSION = () -> red("You must be the leader of the party to do this");
    Args0 ERROR_NOT_PARTY_LEADER_DISBAND = () -> Component.text()
            .append(Component.text("You must be the leader of the party to do this", NamedTextColor.RED)).appendNewline()
            .append(Component.text("Use '/party leave' to leave the party instead", NamedTextColor.AQUA)).appendNewline()
            .build();
    Args1<String> ERROR_PLAYER_INVITED_TO_PARTY = username -> red(username + " has already been invited to your party");
    Args1<String> ERROR_PLAYER_IN_THIS_PARTY = username -> red(username + " is already in the party");
    Args0 ERROR_YOU_NOT_INVITED_TO_PARTY = () -> red("You were not invited to this party");
    Args0 ERROR_YOU_IN_THIS_PARTY = () -> red("You are already in the party");
    Args0 ERROR_CANNOT_KICK_LEADER = () -> red("You cannot kick the party leader");
    Args1<String> ERROR_PLAYER_NOT_IN_PARTY = username -> red(username + " is not in your party");
    Args0 ERROR_CANNOT_LEAVE_AS_LEADER = () -> Component.text()
            .append(red("You are the leader of the party")).appendNewline()
            .append(red("Use '/party disband' to disband the party or '/party leader <player>' to transfer leadership"))
            .build();
    Args0 ERROR_YOU_NOT_IN_PARTY = () -> red("You are not in a party");

    Args1<String> ROLE_CREATED = role -> green("Role " + role + " created");
    Args4<String, Integer, Integer, Component> ROLE_DESCRIPTION = (id, priority, permissions, displayName) ->
            Component.text().color(NamedTextColor.LIGHT_PURPLE)
                    .append(Component.text("----- Role Summary -----")).appendNewline()
                    .append(Component.text("ID: " + id)).appendNewline()
                    .append(Component.text("Priority: " + priority)).appendNewline()
                    .append(Component.text("Permissions: " + permissions)).appendNewline()
                    .append(Component.text("Display Name: ")).appendNewline()
                    .append(displayName).appendNewline()
                    .append(Component.text("-----------------------"))
                    .build();
    Args1<Integer> ROLE_LIST_HEADER = size -> Component.text("Role List (" + size + "):", NamedTextColor.LIGHT_PURPLE);
    Args5<String, Integer, Integer, Component, Component> ROLE_LIST_LINE = (id, priority, permissionCount, displayName, exampleChat) -> {
        Component hoverText = Component.text().color(NamedTextColor.LIGHT_PURPLE)
                .append(Component.text("ID: " + id)).appendNewline()
                .append(Component.text("Priority: " + priority)).appendNewline()
                .append(Component.text("Permissions: " + permissionCount)).appendNewline()
                .append(Component.text("Display Name: ")).append(displayName).appendNewline()
                .appendNewline()
                .append(Component.text("Example Chat: ")).append(exampleChat)
                .build();
        return Component.text(priority + ") " + id, NamedTextColor.LIGHT_PURPLE).hoverEvent(HoverEvent.showText(hoverText));
    };
    Args3<String, Boolean, String> PERMISSION_ADDED_TO_ROLE = (permission, value, role) ->
            green("Set " + permission + " to " + value + " for role " + role);
    Args3<String, String, Tristate> PERMISSION_STATE = (role, permission, state) ->
            green("Role " + role + " has permission " + permission + " set to " + state);
    Args2<String, String> PERMISSION_REMOVED_FROM_ROLE = (permission, role) -> green("Unset " + permission + " for role " + role);
    Args2<String, Integer> ROLE_PRIORITY_SET = (role, priority) -> green("Role " + role + " priority set to " + priority);
    Args2<String, String> ROLE_USERNAME_SET = (role, usernameFormat) -> green("Role " + role + "username set to '" + usernameFormat + "'");
    Args5<String, Component, Integer, String, Component> USER_DESCRIPTION = (username, groups, permissionCount, displayName, exampleChat) ->
            Component.text().color(NamedTextColor.LIGHT_PURPLE)
                    .append(Component.text("----- User Summary -----")).appendNewline()
                    .append(Component.text("Username: " + username)).appendNewline()
                    .append(Component.text("Groups: ")).append(groups).appendNewline()
                    .append(Component.text("Permissions: " + permissionCount)).appendNewline()
                    .append(Component.text("Display Name: " + displayName)).appendNewline()
                    .append(Component.text("Example Chat: ")).append(exampleChat).appendNewline()
                    .append(Component.text("------------------------"))
                    .build();
    Args2<String, String> USER_ROLE_ADDED = (role, user) -> green("Role " + role + " added to " + user);
    Args2<String, String> USER_ROLE_REMOVED = (role, user) -> green("Role " + role + " removed from " + user);
    Args0 PERMISSION_HELP = () -> miniMessage("""
            <light_purple>-- Permission Help --
            <click:suggest_command:'/perm role'>/perm role</click>
            <click:suggest_command:'/perm user'>/perm user</click>
            <click:suggest_command:'/perm listroles'>/perm listroles</click>
            ------------------""");
    Args0 ROLE_HELP = () -> miniMessage("""
            <light_purple>--------- Role Permission Help ---------
            <click:suggest_command:'/perm role '>/perm role <name> create</click>
            <click:suggest_command:'/perm role '>/perm role <name> setusername <state></click>
            <click:suggest_command:'/perm role '>/perm role <name> setpriority <state></click>
            <click:suggest_command:'/perm role '>/perm role <name> permission add <perm></click>
            <click:suggest_command:'/perm role '>/perm role <name> permission unset <perm></click>
            <click:suggest_command:'/perm role '>/perm role <name> permission check <perm></click>
            ------------------------------------""");
    Args0 USER_HELP = () -> miniMessage("""
            <light_purple>--------- User Permission Help ---------
            <click:suggest_command:'/perm user '>/perm user <name> role add <group></click>
            <click:suggest_command:'/perm user '>/perm user <name> role remove <group></click>
            <click:suggest_command:'/perm user '>/perm user <name> permission check <perm></click>
            ------------------------------------""");

    Args1<String> ERROR_ROLE_ALREADY_EXISTS = role -> red("Role " + role + " already exists");
    Args1<String> ERROR_ROLE_NOT_FOUND = role -> red("Role " + role + " not found");
    Args3<String, String, Boolean> ERROR_PERMISSION_ALREADY_SET = (role, permission, value) ->
            red("Role " + role + " already has " + permission + " set to " + value);
    Args2<String, String> ERROR_MISSING_PERMISSION = (role, permission) -> red("Role " + role + " does not have " + permission + " set");
    Args1<String> ERROR_USER_NOT_FOUND = user -> red("User " + user + " not found");
    Args2<String, String> ERROR_USER_ALREADY_HAS_ROLE = (user, role) -> red("User " + user + " already has role " + role);
    Args2<String, String> ERROR_USER_MISSING_ROLE = (user, role) -> red("User " + user + " does not have role " + role);

    Args1<String> YOUR_PLAYTIME = playtime -> lightPurple("Your playtime is " + playtime);
    Args2<String, String> OTHER_PLAYTIME = (player, playtime) -> lightPurple(player + "'s playtime is " + playtime);

    Args2<String, String> PRIVATE_MESSAGE_SENT = (recipient, message) -> Component.text()
            .append(darkPurple("["))
            .append(Component.text("YOU", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
            .append(Component.text(" → ", NamedTextColor.GRAY))
            .append(Component.text(recipient, MessageColors.PRIVATE_MESSAGE_NAME))
            .append(darkPurple("] "))
            .append(Component.text(message, NamedTextColor.GRAY))
            .build();
    Args2<String, String> PRIVATE_MESSAGE_RECEIVED = (sender, message) -> Component.text()
            .append(darkPurple("["))
            .append(Component.text(sender, MessageColors.PRIVATE_MESSAGE_NAME))
            .append(Component.text(" → ", NamedTextColor.GRAY))
            .append(Component.text("YOU", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
            .append(darkPurple("] "))
            .append(Component.text(message, NamedTextColor.GRAY))
            .build();
    Args0 MESSAGE_USAGE = () -> red("Usage: /msg <player> <message>");
    Args0 REPLY_USAGE = () -> red("Usage: /r <message>");

    Args0 ERROR_CANNOT_MESSAGE_SELF = () -> red("You cannot send a message to yourself");
    Args0 ERROR_NO_ONE_TO_REPLY_TO = () -> red("You have not received any messages yet");
    Args1<String> ERROR_YOU_BLOCKED = recipient -> red("You have blocked " + recipient + " so you cannot message them");
    Args1<String> ERROR_THEY_BLOCKED = recipient -> red(recipient + " has blocked you so you cannot message them");

    Args1<String> YOU_BLOCKED = target -> green("You have blocked " + target);
    Args2<Integer, String> BLOCKED_PLAYERS = (count, list) -> red("Blocked Players (" + count + "): " + list);
    Args1<String> YOU_UNBLOCKED = target -> green(target + " has been unblocked");
    Args0 BLOCK_USAGE = () -> red("Usage: /block <username>");
    Args0 UNBLOCK_USAGE = () -> red("Usage: /unblock <username>");

    Args0 ERROR_CANNOT_BLOCK_SELF = () -> red("You can't block yourself");
    Args1<String> ERROR_ALREADY_BLOCKED = target -> red("You have already blocked " + target);
    Args1<String> ERROR_CANNOT_BLOCK_FRIEND = target -> red("You must unfriend " + target + " before blocking them");
    Args0 ERROR_BLOCKED_LIST_EMPTY = () -> red("You have not blocked any players");
    Args0 ERROR_CANNOT_UNBLOCK_SELF = () -> red("You can't unblock yourself");
    Args0 ERROR_NOT_BLOCKED = () -> red("You have not blocked the player");

    Args1<String> FRIEND_ADDED = target -> lightPurple("You are now friends with ").append(MessageColors.purpleName(target));
    Args1<String> SENT_FRIEND_REQUEST = target -> lightPurple("Sent a friend request to ").append(MessageColors.purpleName(target));
    Args1<String> RECEIVED_FRIEND_REQUEST = target -> {
        ClickEvent acceptClickEvent = ClickEvent.runCommand("/friend add " + target);
        ClickEvent denyClickEvent = ClickEvent.runCommand("/friend deny " + target);
        return Component.text()
                .append(lightPurple("You have received a friend request from "))
                .append(MessageColors.purpleName(target))
                .append(green("ACCEPT").clickEvent(acceptClickEvent))
                .append(Component.text(" | ", NamedTextColor.GRAY))
                .append(red("DENY").clickEvent(denyClickEvent))
                .build();
    };
    Args0 FRIEND_HELP = () -> miniMessage("""
            <light_purple>------ Friend Help ------
            <click:run_command:'/friend list'>/friend list</click>
            <click:suggest_command:'/friend add '>/friend add <name></click>
            <click:suggest_command:'/friend remove '>/friend remove <name></click>
            <click:suggest_command:'/friend requests '>/friend requests <incoming/outgoing> [page]</click>
            <click:suggest_command:'/friend purge requests '>/friend purge requests <incoming/outgoing></click>
            -----------------------""");
    Args1<String> FRIEND_REQUEST_DENIED = target -> lightPurple("Removed your friend request from ").append(MessageColors.purpleName(target));
    Args1<String> FRIEND_REQUEST_REVOKED = target -> lightPurple("Revoked your friend request to ").append(MessageColors.purpleName(target));
    Args2<Integer, Integer> FRIEND_LIST_HEADER = (currentPage, maxPage) -> lightPurple("----- Friends (Page " + currentPage + "/" + maxPage + ") -----");
    Args2<String, String> FRIEND_LIST_ONLINE_LINE = (friend, activity) ->
            Component.text(friend + " - " + activity, NamedTextColor.GREEN).clickEvent(ClickEvent.suggestCommand("/message " + friend));
    Args2<String, String> FRIEND_LIST_OFFLINE_LINE = (friend, lastSeen) -> red(friend + " - Seen " + lastSeen);
    Args1<String> FRIEND_REMOVED = target -> lightPurple("You are no longer friends with ").append(MessageColors.purpleName(target));
    Args1<Integer> PURGED_INCOMING_FRIEND_REQUESTS = count -> lightPurple("Purged " + count + " incoming friend requests");
    Args1<Integer> PURGED_OUTGOING_FRIEND_REQUESTS = count -> lightPurple("Purged " + count + " outgoing friend requests");
    Args2<Integer, Integer> INCOMING_FRIEND_REQUESTS_HEADER = (currentPage, maxPage) ->
            lightPurple("--- Incoming Requests (Page " + currentPage + "/" + maxPage + ") ---");
    Args2<Integer, Integer> OUTGOING_FRIEND_REQUESTS_HEADER = (currentPage, maxPage) ->
            lightPurple("--- Outgoing Requests (Page " + currentPage + "/" + maxPage + ") ---");
    Args2<String, String> INCOMING_FRIEND_REQUEST_LINE = (duration, player) -> Component.text()
            .append(lightPurple(duration + " ago "))
            .append(darkPurple("- "))
            .append(lightPurple(player + " "))
            .append(darkPurple("| "))
            .append(green("Accept").clickEvent(ClickEvent.runCommand("/friend add " + player)))
            .append(darkPurple("/"))
            .append(red("Deny").clickEvent(ClickEvent.runCommand("/friend deny " + player)))
            .build();
    Args2<String, String> OUTGOING_FRIEND_REQUEST_LINE = (duration, player) -> Component.text()
            .append(lightPurple(duration + " ago "))
            .append(darkPurple("- "))
            .append(lightPurple(player + " "))
            .append(darkPurple("| "))
            .append(red("Revoke").clickEvent(ClickEvent.runCommand("/friend revoke " + player)))
            .build();
    Args1<String> FRIEND_CONNECTED = friend -> green("Friend > " + friend + " has connected");
    Args1<String> FRIEND_DISCONNECTED = friend -> green("Friend > " + friend + " has disconnected");

    Args1<String> ERROR_ALREADY_FRIENDS = target -> lightPurple("You are already friends with ").append(MessageColors.purpleName(target));
    Args1<String> ERROR_PRIVACY_BLOCKED = target -> Component.text()
            .append(Component.text(target + "'s ", TextColor.color(0xc98fff)))
            .append(lightPurple("privacy settings don't allow you you add them as a friend."))
            .build();
    Args1<String> ERROR_FRIEND_ALREADY_REQUESTED = target ->
            lightPurple("You have already sent a friend request to ").append(MessageColors.purpleName(target));
    Args1<String> ERROR_CANNOT_FRIEND_BLOCKED = target -> Component.text()
            .append(red("You cannot add "))
            .append(Component.text(target, TextColor.color(0xc98fff)))
            .append(red(" as a friend as you have blocked them!"))
            .build();
    Args0 ERROR_CANNOT_FRIEND_SELF = () -> red("You can't add yourself as a friend");
    Args1<String> ERROR_NO_FRIEND_REQUEST_SENT = target ->
            lightPurple("You have not sent a friend request to ").append(MessageColors.purpleName(target));
    Args1<String> ERROR_NO_FRIEND_REQUEST_RECEIVED = target ->
            lightPurple("You have not received a friend request from ").append(MessageColors.purpleName(target));
    Args0 ERROR_NO_FRIENDS = () -> lightPurple("You have no friends. Use /friend add <name> to add someone.");
    Args1<String> ERROR_NOT_FRIENDS = target -> lightPurple("You are not friends with ").append(MessageColors.purpleName(target));
    Args0 ERROR_NO_INCOMING_FRIEND_REQUESTS = () -> lightPurple("You have no incoming friend requests");
    Args0 ERROR_NO_OUTGOING_FRIEND_REQUESTS = () -> lightPurple("You have no outgoing friend requests");

    Args0 RESOURCE_PACK_DOWNLOAD = () -> Component.text("We love you");
    Args0 RESOURCE_PACK_DECLINED = () -> Component.text().color(NamedTextColor.GRAY)
            .append(Component.text("Using the resource pack is required. It isn't big and only has to be downloaded once.")).appendNewline()
            .append(Component.text("If the dialog is annoying, you can enable 'Server Resource Packs' when adding the server and the prompt will disappear."))
            .build();
    Args0 RESOURCE_PACK_FAILED = () -> Component.text().color(NamedTextColor.RED)
            .append(Component.text("The resource pack download failed.")).appendNewline()
            .append(Component.text("If the issue persists, contact a staff member"))
            .build();

    // ▓▒░              ⚡   EmortalMC   ⚡              ░▒▓
    Args0 PING_MOTD = () -> Component.text()
            .append(Component.text("▓▒░              ", NamedTextColor.LIGHT_PURPLE))
            .append(Component.text("⚡   ", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
            .append(miniMessage("<gradient:gold:light_purple><bold>EmortalMC</bold></gradient>"))
            .append(Component.text("   ⚡", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(Component.text("              ░▒▓", NamedTextColor.GOLD))
            .build();

    Args0 TAB_LIST_HEADER = () -> Component.text()
            .append(Component.text("┌                                                  ", NamedTextColor.GOLD))
            .append(Component.text("┐ ", NamedTextColor.LIGHT_PURPLE))
            .appendNewline()
            .append(miniMessage("<gradient:gold:light_purple><bold>EmortalMC</bold></gradient>"))
            .appendNewline()
            .build();
    Args1<Long> TAB_LIST_FOOTER = online -> Component.text()
            .append(Component.text(" ", NamedTextColor.GRAY)).appendNewline()
            .append(Component.text(online + " online", NamedTextColor.GRAY)).appendNewline()
            .append(Component.text("ᴍᴄ.ᴇᴍᴏʀᴛᴀʟ.ᴅᴇᴠ", MessageColors.TAB_LIST_FOOTER_IP)).appendNewline()
            .append(Component.text("└                                                  ", NamedTextColor.LIGHT_PURPLE))
            .append(Component.text("┘ ", NamedTextColor.GOLD))
            .appendNewline()
            .build();

    @FunctionalInterface
    interface Args0 {

        @NotNull Component get();

        default void send(@NotNull Audience audience) {
            audience.sendMessage(this.get());
        }
    }

    interface Args1<A> {

        @NotNull Component get(@NotNull A a);

        default void send(@NotNull Audience audience, @NotNull A a) {
            audience.sendMessage(this.get(a));
        }
    }

    interface Args2<A, B> {

        @NotNull Component get(@NotNull A a, @NotNull B b);

        default void send(@NotNull Audience audience, @NotNull A a, @NotNull B b) {
            audience.sendMessage(this.get(a, b));
        }
    }

    interface Args3<A, B, C> {

        @NotNull Component get(@NotNull A a, @NotNull B b, @NotNull C c);

        default void send(@NotNull Audience audience, @NotNull A a, @NotNull B b, @NotNull C c) {
            audience.sendMessage(this.get(a, b, c));
        }
    }

    interface Args4<A, B, C, D> {

        @NotNull Component get(@NotNull A a, @NotNull B b, @NotNull C c, @NotNull D d);

        default void send(@NotNull Audience audience, @NotNull A a, @NotNull B b, @NotNull C c, @NotNull D d) {
            audience.sendMessage(this.get(a, b, c, d));
        }
    }

    interface Args5<A, B, C, D, E> {

        @NotNull Component get(@NotNull A a, @NotNull B b, @NotNull C c, @NotNull D d, @NotNull E e);

        default void send(@NotNull Audience audience, @NotNull A a, @NotNull B b, @NotNull C c, @NotNull D d, @NotNull E e) {
            audience.sendMessage(this.get(a, b, c, d, e));
        }
    }

    private static @NotNull Component green(@NotNull String text) {
        return Component.text(text, NamedTextColor.GREEN);
    }

    private static @NotNull Component red(@NotNull String text) {
        return Component.text(text, NamedTextColor.RED);
    }

    private static @NotNull Component lightPurple(@NotNull String text) {
        return Component.text(text, NamedTextColor.LIGHT_PURPLE);
    }

    private static @NotNull Component darkPurple(@NotNull String text) {
        return Component.text(text, NamedTextColor.DARK_PURPLE);
    }

    private static @NotNull Component miniMessage(@NotNull String text) {
        return MiniMessage.miniMessage().deserialize(text);
    }
}
