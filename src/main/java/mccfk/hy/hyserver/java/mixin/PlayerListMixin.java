package mccfk.hy.hyserver.java.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 禁用原版玩家加入/退出消息
 */
@Mixin(PlayerList.class)
public class PlayerListMixin {
    
    @Inject(method = "broadcastSystemMessage", at = @At("HEAD"), cancellable = true, remap = false)
    private void onCancelJoinLeaveMessage(net.minecraft.network.chat.Component p_11265_, boolean p_11266_, CallbackInfo ci) {
        String message = p_11265_.getString();
        // 取消包含"joined"或"left"的消息（原版加入/退出提示）
        if (message.contains("multiplayer.player.joined") || message.contains("multiplayer.player.left")) {
            ci.cancel();
        }
    }
}
