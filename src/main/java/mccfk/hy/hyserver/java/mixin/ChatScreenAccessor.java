package mccfk.hy.hyserver.java.mixin;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * ChatScreen Accessor - 访问 protected 字段
 */
@OnlyIn(Dist.CLIENT)
@Mixin(ChatScreen.class)
public interface ChatScreenAccessor {
    @Accessor(value = "input", remap = false)
    net.minecraft.client.gui.components.EditBox getInput();
}
