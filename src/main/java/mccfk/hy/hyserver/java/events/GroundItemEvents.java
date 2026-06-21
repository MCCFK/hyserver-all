package mccfk.hy.hyserver.java.events;

import mccfk.hy.hyserver.java.entity.GroundItemEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 地面物品事件处理器
 */
@EventBusSubscriber
public class GroundItemEvents {
    
    /**
     * 地面物品标签 - 带有此标签的物品掉落时会变成地面物品实体
     */
    public static final TagKey<Item> GROUND_ITEM_TAG = TagKey.create(
        Registries.ITEM, 
        ResourceLocation.fromNamespaceAndPath("hyserver", "df_item_e")
    );
    
    /**
     * DF物品标签 - 带有此标签的物品也会生成自定义地面掉落物实体
     */
    public static final TagKey<Item> DF_ITEM_TAG = TagKey.create(
        Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath("hyserver", "df_item")
    );
    
    /**
     * 检查物品是否应该转换为地面物品实体
     */
    private static boolean shouldConvertToGroundItem(ItemStack itemStack) {
        return itemStack.is(GROUND_ITEM_TAG) || itemStack.is(DF_ITEM_TAG);
    }
    
    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        // 当玩家丢弃物品时检查标签
        if (shouldConvertToGroundItem(event.getEntity().getItem())) {
            // 取消原版掉落物生成
            event.setCanceled(true);
            
            // 创建地面物品实体
            var level = event.getEntity().level();
            var entity = event.getEntity();
            var itemStack = entity.getItem();
            var player = event.getPlayer();
            
            if (!level.isClientSide()) {
                // 使用玩家实际位置，但Y坐标调整为地面高度
                double x = player.getX();
                double y = player.getY(); // 玩家脚部Y坐标
                double z = player.getZ();
                
                GroundItemEntity groundItem = new GroundItemEntity(
                    level, 
                    x, 
                    y, 
                    z, 
                    itemStack
                );
                
                level.addFreshEntity(groundItem);
            }
        }
    }
    
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        // 拦截所有新生成的 ItemEntity,检查是否应该转换为地面物品
        if (event.getEntity() instanceof ItemEntity itemEntity && !itemEntity.getItem().isEmpty()) {
            ItemStack itemStack = itemEntity.getItem();
            
            // 检查物品是否带有地面物品标签或DF物品标签
            if (shouldConvertToGroundItem(itemStack)) {
                Level level = event.getEntity().level();
                
                if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
                    // 取消原版掉落物生成
                    event.setCanceled(true);
                    
                    // 创建地面物品实体
                    GroundItemEntity groundItem = new GroundItemEntity(
                        serverLevel,
                        itemEntity.getX(),
                        itemEntity.getY(),
                        itemEntity.getZ(),
                        itemStack.copy()
                    );
                    
                    // 不继承动量,直接静止在地面上
                    // groundItem.setDeltaMovement(itemEntity.getDeltaMovement());
                    
                    serverLevel.addFreshEntity(groundItem);
                }
            }
        }
    }
    
    /**
     * 工具方法:将普通物品掉落转换为地面物品
     */
    public static void convertToGroundItem(ServerLevel level, double x, double y, double z, ItemStack itemStack) {
        if (shouldConvertToGroundItem(itemStack)) {
            GroundItemEntity groundItem = new GroundItemEntity(level, x, y, z, itemStack);
            level.addFreshEntity(groundItem);
        } else {
            // 如果不是地面物品标签,正常生成掉落物
            ItemEntity itemEntity = new ItemEntity(level, x, y, z, itemStack);
            level.addFreshEntity(itemEntity);
        }
    }
}
