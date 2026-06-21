package mccfk.hy.hyserver.java.mixin;

import mccfk.hy.hyserver.java.chat.ChatFormatter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.network.FilteredText;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.List;

/**
 * SignBlockEntity Mixin - 在告示牌保存时解析颜色代码（服务端）
 */
@Mixin(SignBlockEntity.class)
public class SignEditScreenMixin {
    
    /**
     * 修改 updateSignText 方法中传递给 updateText 的 lambda 结果
     * 在 setMessages 之后立即解析颜色代码
     */
    @ModifyArg(
        method = "updateSignText",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/SignBlockEntity;updateText(Ljava/util/function/UnaryOperator;Z)Z",
            ordinal = 0
        ),
        index = 0,
        remap = false
    )
    private java.util.function.UnaryOperator<SignText> parseColorCodesInLambda(java.util.function.UnaryOperator<SignText> originalOperator) {
        return (signText) -> {
            // 先执行原始的 setMessages 逻辑
            SignText result = originalOperator.apply(signText);
            
            // 然后解析颜色代码
            try {
                Component[] messages = result.getMessages(false);
                boolean hasChanges = false;
                SignText newText = result;
                
                for (int i = 0; i < 4; i++) {
                    if (messages[i] != null) {
                        String lineText = messages[i].getString();
                        
                        if (lineText.contains("&")) {
                            MutableComponent parsed = ChatFormatter.parseColorCodes(lineText);
                            newText = newText.setMessage(i, parsed, parsed);
                            hasChanges = true;
                        }
                    }
                }
                
                return hasChanges ? newText : result;
            } catch (Exception e) {
                return result;
            }
        };
    }
}
