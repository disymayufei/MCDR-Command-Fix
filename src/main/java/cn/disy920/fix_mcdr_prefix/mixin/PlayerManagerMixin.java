package cn.disy920.fix_mcdr_prefix.mixin;

import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {

    @Unique
    private final Map<String, ServerPlayerEntity> playerCache = new ConcurrentHashMap<>(16);

    @Inject(
            method = "broadcast(Lnet/minecraft/network/message/SignedMessage;Ljava/util/function/Predicate;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/network/message/MessageType$Parameters;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/MinecraftServer;logChatMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageType$Parameters;Ljava/lang/String;)V"
            )
    )
    private void injectBroadcast(SignedMessage message, Predicate<ServerPlayerEntity> shouldSendFiltered, ServerPlayerEntity sender, MessageType.Parameters params, CallbackInfo ci) {
        if (message.getContent().getString().startsWith("!!")) {
            playerCache.put(Thread.currentThread().getName(), sender);
        }
    }

    @Redirect(
            method = "broadcast(Lnet/minecraft/network/message/SignedMessage;Ljava/util/function/Predicate;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/network/message/MessageType$Parameters;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/MinecraftServer;logChatMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageType$Parameters;Ljava/lang/String;)V"
            )
    )
    private void redirectedServerLog(MinecraftServer server, Text message, MessageType.Parameters params, String prefix) {
        if (message.getString().startsWith("!!")) {
            ServerPlayerEntity player = playerCache.get(Thread.currentThread().getName());
            if (player != null) {
                params = MessageType.params(MessageType.CHAT, player.getWorld().getRegistryManager(), player.getName());
            }
            playerCache.remove(Thread.currentThread().getName());
        }

        server.logChatMessage(message, params, prefix);
    }
}
