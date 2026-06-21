package mccfk.hy.hyserver.java.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * 在登录阶段拦截被封禁的玩家
 * 在玩家加载世界之前就拒绝连接，避免浪费服务器资源
 */
@Mixin(ServerLoginPacketListenerImpl.class)
public class ServerLoginPacketListenerMixin {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerLoginPacketListenerMixin.class);
    
    @Inject(
        method = "verifyLoginAndFinishConnectionSetup",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void onVerifyLogin(GameProfile profile, CallbackInfo ci) {
        // 检查玩家是否被封禁
        UUID playerUuid = profile.getId();
        
        try {
            // 调用 BanManager 检查封禁状态
            mccfk.hy.hyserver.java.ban.BanManager.BanRecord banRecord = 
                mccfk.hy.hyserver.java.ban.BanManager.checkBan(playerUuid);
            
            if (banRecord != null) {
                // 玩家被封禁，获取封禁消息
                Component banMessage = mccfk.hy.hyserver.java.ban.BanManager.getBanMessage(banRecord);
                
                // 调用 disconnect 方法断开连接
                ServerLoginPacketListenerImpl listener = (ServerLoginPacketListenerImpl) (Object) this;
                listener.disconnect(banMessage);
                
                // 记录日志
                LOGGER.info("[封禁系统] 在登录阶段阻止被封禁玩家: {} ({})", 
                    banRecord.playerName, playerUuid);
                
                // 取消原版登录流程
                ci.cancel();
            }
        } catch (Exception e) {
            // 如果检查失败（比如 BanManager 未初始化），记录错误但不阻止登录
            LOGGER.warn("\n\n§e[HYSERVER.WARN]\n§e[封禁系统] 检查封禁状态失败: {}", e.getMessage());
        }
    }
}
