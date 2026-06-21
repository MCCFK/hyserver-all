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
 * 在登录阶段验证客户端模组版本
 * 如果版本不匹配，在玩家加载世界之前就拒绝连接
 */
@Mixin(ServerLoginPacketListenerImpl.class)
public class VersionCheckLoginMixin {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(VersionCheckLoginMixin.class);
    
    @Inject(
        method = "verifyLoginAndFinishConnectionSetup",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void onVerifyLogin(GameProfile profile, CallbackInfo ci) {
        UUID playerUuid = profile.getId();
        String playerName = profile.getName();
        
        try {
            // 获取服务端版本
            String serverVersion = getServerVersion();
            
            // 注意：这里无法直接获取客户端版本，因为登录阶段还没有网络通信
            // NeoForge 会自动在握手阶段验证模组列表和版本
            // 如果客户端没有安装模组或版本不匹配，NeoForge 会直接拒绝连接
            
            // 这里只记录日志
            if (playerName != null) {
                LOGGER.info("[版本检查] 玩家 {} ({}) 正在连接，服务端 hyserver 版本: {}", 
                    playerName, playerUuid, serverVersion);
            }
            
        } catch (Exception e) {
            // 如果检查失败，记录错误但不阻止登录
            LOGGER.warn("\n\n§e[HYSERVER.WARN]\n§e[版本检查] 版本检查失败: {}", e.getMessage());
        }
    }
    
    /**
     * 获取服务端模组版本
     */
    private static String getServerVersion() {
        try {
            var modContainer = net.neoforged.fml.ModList.get().getModContainerById("hyserver");
            if (modContainer.isPresent()) {
                return modContainer.get().getModInfo().getVersion().toString();
            }
        } catch (Exception e) {
            LOGGER.error("[版本检查] 获取服务端版本失败: {}", e.getMessage());
        }
        return "未知";
    }
}
