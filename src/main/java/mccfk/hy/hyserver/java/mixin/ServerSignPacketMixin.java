package mccfk.hy.hyserver.java.mixin;

import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.stream.Stream;

/**
 * ServerGamePacketListenerImpl Mixin - 防止告示牌文本的颜色代码被移除
 */
@Mixin(ServerGamePacketListenerImpl.class)
public class ServerSignPacketMixin {
    
    /**
     * 重定向 handleSignUpdate 中的 stripFormatting 调用，保留原始文本
     */
    @Redirect(
        method = "handleSignUpdate",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/stream/Stream;map(Ljava/util/function/Function;)Ljava/util/stream/Stream;"
        ),
        remap = false
    )
    private Stream<String> preserveColorCodes(Stream<String> originalStream, java.util.function.Function<? super String, ?> mapper) {
        // 不调用 stripFormatting，直接返回原始字符串
        return originalStream;
    }
}
