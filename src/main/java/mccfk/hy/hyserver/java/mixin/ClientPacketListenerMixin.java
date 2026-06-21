package mccfk.hy.hyserver.java.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * ClientPacketListener Mixin - 禁用不安全服务器警告 toast
 * 
 * 源码位置: net/minecraft/client/multiplayer/ClientPacketListener.java
 * 目标代码: handleLogin() 方法第 555-559 行
 */
@OnlyIn(Dist.CLIENT)
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    
    @Shadow(remap = false)
    private boolean seenInsecureChatWarning;
    
    /**
     * 拦截 enforcesSecureChat() 调用,使其返回 true
     * 这样第 555 行的条件判断就会失败,跳过 toast 显示
     * 
     * 原逻辑:
     * if (this.serverData != null && !this.seenInsecureChatWarning && !this.enforcesSecureChat()) {
     *     // 显示 toast
     * }
     * 
     * 修改后: enforcesSecureChat() 返回 true,条件 !true = false,整个 if 被跳过
     */
    @Redirect(
        method = "handleLogin",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;enforcesSecureChat()Z"
        ),
        remap = false
    )
    private boolean hideUnsecureServerToast(ClientPacketListener instance) {
        // 标记为已看到警告,并且返回 true 让条件判断失败
        this.seenInsecureChatWarning = true;
        return true;
    }
}
