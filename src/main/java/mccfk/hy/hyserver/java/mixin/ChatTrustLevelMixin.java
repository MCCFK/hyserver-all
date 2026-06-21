package mccfk.hy.hyserver.java.mixin;

import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.multiplayer.chat.ChatTrustLevel;
import net.minecraft.network.chat.PlayerChatMessage;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ChatTrustLevel Mixin - 禁用聊天消息修改指示器（蓝色提示）
 */
@OnlyIn(Dist.CLIENT)
@Mixin(ChatTrustLevel.class)
public class ChatTrustLevelMixin {
    
    /**
     * 拦截 createTag 方法，始终返回 null 以禁用修改指示器
     * @param p_240632_ 玩家聊天消息
     * @param cir 回调信息
     */
    @Inject(method = "createTag", at = @At("HEAD"), cancellable = true, remap = false)
    private void hideModifiedIndicator(net.minecraft.network.chat.PlayerChatMessage p_240632_, CallbackInfoReturnable<net.minecraft.client.GuiMessageTag> cir) {
        // 始终返回 null，不显示任何信任状态指示器
        cir.setReturnValue(null);
    }
}
