//package cc.towerdefence.velocity.listener;
//
//
//import general.dev.emortal.velocity.ServerManager;
//import permissions.dev.emortal.velocity.PermissionBlocker;
//import com.google.common.util.concurrent.Futures;
//import com.google.common.util.concurrent.ListenableFuture;
//import com.velocitypowered.api.event.ubscribe;
//import com.velocitypowered.api.event.connection.DisconnectEvent;
//import com.velocitypowered.api.event.player.PlayerChatEvent;
//import com.velocitypowered.api.proxy.Player;
//import dev.emortal.api.utils.GrpcStubCollection;
//import dev.emortal.api.utils.callback.FunctionalFutureCallback;
//import net.kyori.adventure.text.Component;
//import net.kyori.adventure.text.minimessage.MiniMessage;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.HashSet;
//import java.util.Map;
//import java.util.Set;
//import java.util.UUID;
//import java.util.concurrent.ForkJoinPool;
//
//public class OtpEventListener implements PermissionBlocker {
//    private static final Logger LOGGER = LoggerFactory.getLogger(OtpEventListener.class);
//    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
//
//    private static final Map<McPlayerSecurityProto.YubikeyResponse.Status, Component> STATUS_MESSAGES = Map.ofEntries(
//            Map.entry(McPlayerSecurityProto.YubikeyResponse.Status.OK, MINI_MESSAGE.deserialize("<light_purple>Successfully authenticated! Redirecting you...")),
//            Map.entry(McPlayerSecurityProto.YubikeyResponse.Status.BAD_OTP, MINI_MESSAGE.deserialize("<red>Invalid OTP! Please try again.")),
//            Map.entry(McPlayerSecurityProto.YubikeyResponse.Status.REPLAYED_OTP, MINI_MESSAGE.deserialize("<red>OTP has already been used! Please try again.")),
//            Map.entry(McPlayerSecurityProto.YubikeyResponse.Status.BAD_SIGNATURE, MINI_MESSAGE.deserialize("<red>Internal error occurred (sig)!")),
//            Map.entry(McPlayerSecurityProto.YubikeyResponse.Status.MISSING_PARAMETER, MINI_MESSAGE.deserialize("<red>Internal error occurred (param)!")),
//            Map.entry(McPlayerSecurityProto.YubikeyResponse.Status.NO_SUCH_CLIENT, MINI_MESSAGE.deserialize("<red>Internal error occurred (client)!")),
//            Map.entry(McPlayerSecurityProto.YubikeyResponse.Status.OPERATION_NOT_ALLOWED, MINI_MESSAGE.deserialize("<red>Internal error occurred (op)!")),
//            Map.entry(McPlayerSecurityProto.YubikeyResponse.Status.BACKEND_ERROR, MINI_MESSAGE.deserialize("<red>Internal error occurred (backend)!")),
//            Map.entry(McPlayerSecurityProto.YubikeyResponse.Status.NOT_ENOUGH_ANSWERS, MINI_MESSAGE.deserialize("<red>Internal error occurred (ans)!")),
//            Map.entry(McPlayerSecurityProto.YubikeyResponse.Status.REPLAYED_REQUEST, MINI_MESSAGE.deserialize("<red>Internal error occurred (req)!")),
//            Map.entry(McPlayerSecurityProto.YubikeyResponse.Status.KEY_NOT_LINKED, MINI_MESSAGE.deserialize("<red>The key used is not linked to your account!")),
//            Map.entry(McPlayerSecurityProto.YubikeyResponse.Status.SECURITY_ERROR, MINI_MESSAGE.deserialize("<red>!")),
//            Map.entry(McPlayerSecurityProto.YubikeyResponse.Status.UNRECOGNIZED, MINI_MESSAGE.deserialize("<red>Internal error occurred (unrec)!"))
//    );
//
//    private final McPlayerSecurityGrpc.McPlayerSecurityFutureStub mcPlayerSecurityService;
//    private final ServerManager serverManager;
//
//    private final Set<UUID> restrictedPlayers = new HashSet<>();
//
//    public OtpEventListener(ServerManager serverManager) {
//        this.mcPlayerSecurityService = GrpcStubCollection.getPlayerSecurityService().orElse(null);
//        this.serverManager = serverManager;
//    }
//
//    @Subscribe
//    public void onDisconnect(DisconnectEvent event) {
//        this.restrictedPlayers.remove(event.getPlayer().getUniqueId());
//    }
//
//    @Subscribe
//    public void onChat(PlayerChatEvent event) {
//        Player player = event.getPlayer();
//        UUID issuerId = player.getUniqueId();
//
//        if (!this.restrictedPlayers.contains(issuerId)) return;
//
//        String otp = event.getMessage();
//
//        if (otp.length() < 32 || otp.length() > 48) {
//            player.sendMessage(STATUS_MESSAGES.get(McPlayerSecurityProto.YubikeyResponse.Status.BAD_OTP));
//            return;
//        }
//
//        ListenableFuture<McPlayerSecurityProto.YubikeyResponse> yubikeyResponseFuture = this.mcPlayerSecurityService.verifyYubikey(
//                McPlayerSecurityProto.YubikeyRequest.newBuilder()
//                        .setIssuerId(issuerId.toString())
//                        .setOtp(otp)
//                        .build());
//
//        Futures.addCallback(yubikeyResponseFuture, FunctionalFutureCallback.create(
//                response -> {
//                    player.sendMessage(STATUS_MESSAGES.get(response.getStatus()));
//
//                    if (response.getStatus() == McPlayerSecurityProto.YubikeyResponse.Status.OK) {
//                        this.restrictedPlayers.remove(issuerId);
//                        this.serverManager.sendToLobbyServer(player);
//                    }
//                },
//                throwable -> {
//                    player.sendMessage(MINI_MESSAGE.deserialize("<red>Internal error occurred (throw)!"));
//                    LOGGER.error("Error while verifying OTP", throwable);
//                }
//        ), ForkJoinPool.commonPool());
//    }
//
//    public Set<UUID> getRestrictedPlayers() {
//        return this.restrictedPlayers;
//    }
//
//    @Override
//    public boolean isBlocked(UUID playerId, String permission) {
//        return this.restrictedPlayers.contains(playerId);
//    }
//}
