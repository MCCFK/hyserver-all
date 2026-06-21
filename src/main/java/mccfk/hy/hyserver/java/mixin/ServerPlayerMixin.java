package mccfk.hy.hyserver.java.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;

/**
 * ServerPlayer Mixin - 当前未使用
 * 容器检查功能已迁移到 NeoForge 事件系统 (MenuEvent.Opening)
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    // 此 Mixin 目前为空，保留以备将来使用
}
