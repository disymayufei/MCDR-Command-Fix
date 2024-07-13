package cn.disy920.fix_mcdr_prefix.mixin;

import net.minecraft.command.EntityDataObject;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityDataObject.class)
public class EntityDataObjectMixin {
    @Redirect(
            method = "feedbackQuery",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;getDisplayName()Lnet/minecraft/text/Text;"
            )
    )
    private Text redirectDisplayName(Entity instance) {
        return instance.getName();
    }
}
