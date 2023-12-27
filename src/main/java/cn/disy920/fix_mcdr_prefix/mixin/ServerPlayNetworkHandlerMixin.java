package cn.disy920.fix_mcdr_prefix.mixin;

import net.minecraft.network.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.filter.TextStream;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
    @Unique
    private final Map<String, String> rawStringCache = new ConcurrentHashMap<>(16);

    @Unique
    private final Map<String, String> filteredStringCache = new ConcurrentHashMap<>(16);

    @Shadow
    public ServerPlayerEntity player;

    @Shadow
    @Final
    private MinecraftServer server;

    @Inject(
            method = "handleMessage",
            at = @At(value = "INVOKE", target = "Ljava/lang/String;isEmpty()Z"),
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    public void injectHandleMessage(TextStream.Message message, CallbackInfo ci, String string, String string2) {
        if (string.startsWith("!!")) {
            rawStringCache.put(Thread.currentThread().getName(), string);
            filteredStringCache.put(Thread.currentThread().getName(), string2);
        }
    }

    @Redirect(
            method = "handleMessage",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/text/Text;Ljava/util/function/Function;Lnet/minecraft/network/MessageType;Ljava/util/UUID;)V")
    )
    public void redirectBroadcast(PlayerManager playerManager, Text serverMessage, Function<ServerPlayerEntity, Text> playerMessageFactory, MessageType type, UUID sender) {
        String threadName = Thread.currentThread().getName();

        if (rawStringCache.get(threadName) != null && type == MessageType.CHAT) {
            String string = filteredStringCache.get(threadName);

            rawStringCache.remove(threadName);
            filteredStringCache.remove(threadName);

            Text text = string.isEmpty() ? null : new TranslatableText("chat.type.text", this.player.getName(), string);

            this.server.sendSystemMessage(text, sender);
            for (ServerPlayerEntity serverPlayer : this.server.getPlayerManager().getPlayerList()) {
                Text sendText = playerMessageFactory.apply(serverPlayer);
                if (sendText != null) {
                    serverPlayer.sendMessage(sendText, MessageType.CHAT, sender);
                }
            }
        }
        else {
            playerManager.broadcast(serverMessage, playerMessageFactory, type, sender);
        }
    }
}
