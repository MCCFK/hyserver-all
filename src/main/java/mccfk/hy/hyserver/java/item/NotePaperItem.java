package mccfk.hy.hyserver.java.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 纸条物品
 * - 潜行右键：打开GUI输入内容
 * - 内容显示为物品名
 * - 可以一直修改
 */
public class NotePaperItem extends Item {
    private static final String CONTENT_TAG = "content";
    
    public NotePaperItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        String content = getContent(stack);
        if (content != null && !content.isEmpty()) {
            tooltipComponents.add(Component.literal("§7内容: §f" + content));
        } else {
            tooltipComponents.add(Component.literal("§7空白纸条"));
        }
        tooltipComponents.add(Component.literal("§e潜行+右键编辑内容"));
    }
    
    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        
        // 仅在客户端且潜行时打开编辑界面
        if (level.isClientSide() && player.isShiftKeyDown()) {
            // 使用反射打开纸条编辑界面（避免客户端类加载问题）
            try {
                Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
                java.lang.reflect.Method getInstanceMethod = minecraftClass.getMethod("getInstance");
                Object minecraft = getInstanceMethod.invoke(null);
                java.lang.reflect.Method setScreenMethod = minecraftClass.getMethod("setScreen", Class.forName("net.minecraft.client.gui.screens.Screen"));
                
                Class<?> screenClass = Class.forName("mccfk.hy.hyserver.java.gui.NotePaperEditScreen");
                java.lang.reflect.Constructor<?> constructor = screenClass.getConstructor(ItemStack.class);
                Object screen = constructor.newInstance(itemStack);
                
                setScreenMethod.invoke(minecraft, screen);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return InteractionResultHolder.success(itemStack);
        }
        
        return InteractionResultHolder.pass(itemStack);
    }
    
    /**
     * 获取纸条内容
     */
    public static String getContent(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        if (tag.contains(CONTENT_TAG)) {
            return tag.getString(CONTENT_TAG);
        }
        return null;
    }
    
    /**
     * 设置纸条内容
     */
    public static void setContent(ItemStack stack, String content) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        if (content == null || content.isEmpty()) {
            tag.remove(CONTENT_TAG);
        } else {
            tag.putString(CONTENT_TAG, content);
            // 添加唯一标识符，确保不同内容的纸条被视为完全不同的物品
            tag.putInt("content_hash", content.hashCode());
        }
        stack.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        
        // 更新显示名称
        if (content != null && !content.isEmpty()) {
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(content));
        } else {
            stack.remove(DataComponents.CUSTOM_NAME);
        }
    }
}
