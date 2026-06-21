package mccfk.hy.hyserver.java.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.ChatTrustLevel;
import net.minecraft.network.chat.PlayerChatMessage;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@OnlyIn(Dist.CLIENT)
@Mixin(ChatComponent.class)
public class ChatComponentMixin {
    @ModifyExpressionValue(
            method = {"addMessageToDisplayQueue", "addMessageToQueue"},
            at = @At(value = "CONSTANT", args = "intValue=100")
    )
    private int changeMaxHistory(int original) {
        return 2147483647;
    }
}
