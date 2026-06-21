package mccfk.hy.hyserver.java.mixin;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin 注入 EntityRenderer.getNameTag() 方法
 * 支持头顶名字多行显示（将 \n 换行符转换为 Component 组件）
 * 
 * 注意：此 Mixin 当前被禁用，因为 Minecraft 1.21.1 的方法签名变化导致注入失败。
 * 头顶名字的多行显示功能暂时不可用，但 Tab 列表和聊天栏仍支持多行显示。
 */
@Mixin(EntityRenderer.class)
public class NameTagMultiLineMixin {
    // Mixin 已禁用 - 方法注入失败
}
