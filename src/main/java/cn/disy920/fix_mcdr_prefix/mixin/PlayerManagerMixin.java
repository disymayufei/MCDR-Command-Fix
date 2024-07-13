package cn.disy920.fix_mcdr_prefix.mixin;

import com.llamalad7.mixinextras.sugar.Local;
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
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
    @ModifyArg(
            method = "broadcast(Lnet/minecraft/network/message/SignedMessage;Ljava/util/function/Predicate;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/network/message/MessageType$Parameters;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/MinecraftServer;logChatMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageType$Parameters;Ljava/lang/String;)V"
            )
    )
    private MessageType.Parameters redirectedServerLog(MessageType.Parameters params, @Local(argsOnly = true) ServerPlayerEntity sender) {
        if (params.type().getKey().get() == MessageType.CHAT && sender != null) {
            return MessageType.params(MessageType.CHAT, sender.getRegistryManager(), sender.getName());
        }
        return params;
    }
}
