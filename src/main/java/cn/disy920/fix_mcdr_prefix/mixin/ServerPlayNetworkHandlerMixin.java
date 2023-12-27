package cn.disy920.fix_mcdr_prefix.mixin;

import net.minecraft.network.MessageType;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
    @Unique
    private final Map<String, String> rawStringCache = new ConcurrentHashMap<>(16);

    @Shadow
    public ServerPlayerEntity player;

    @Shadow
    @Final
    private MinecraftServer server;

    @Inject(
            method = "method_31286",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/PlayerManager;broadcastChatMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/MessageType;Ljava/util/UUID;)V",
                    shift = At.Shift.BEFORE
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    public void injectHandleMessage(String string, CallbackInfo ci, Text text) {
        if (string.startsWith("!!")) {
            rawStringCache.put(Thread.currentThread().getName(), string);
        }
    }

    @Redirect(
            method = "method_31286",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;broadcastChatMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/MessageType;Ljava/util/UUID;)V")
    )
    public void redirectBroadcast(PlayerManager playerManager, Text message, MessageType type, UUID sender) {
        String threadName = Thread.currentThread().getName();

        if (rawStringCache.get(threadName) != null && type == MessageType.CHAT) {
            String string = rawStringCache.get(threadName);
            rawStringCache.remove(threadName);

            Text text = new TranslatableText("chat.type.text", this.player.getName(), string);

            this.server.sendSystemMessage(text, sender);
            playerManager.sendToAll(new GameMessageS2CPacket(message, type, sender));
        }
        else {
            playerManager.broadcastChatMessage(message, type, sender);
        }
    }
}
