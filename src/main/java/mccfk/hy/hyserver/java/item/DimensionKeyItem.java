package mccfk.hy.hyserver.java.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 维度钥匙物品
 * - 首次右键：打开GUI选择维度（1-20）
 * - 选择后：存储到NBT worldID
 * - 在非d_*维度右键：记录当前位置并传送到绑定的维度
 * - 在d_*维度右键：返回之前记录的外部位置
 */
public class DimensionKeyItem extends Item {
    private static final Logger LOGGER = LoggerFactory.getLogger(DimensionKeyItem.class);
    private static final String WORLD_ID_TAG = "worldID";
    
    public DimensionKeyItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        // 显示绑定的维度ID
        String worldId = getWorldId(stack);
        if (worldId != null) {
            tooltipComponents.add(Component.literal("§7绑定维度: §ed_" + worldId));
        } else {
            tooltipComponents.add(Component.literal("§7未绑定维度"));
            tooltipComponents.add(Component.literal("§e右键选择维度"));
        }
    }
    
    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        
//        LOGGER.info("[维度钥匙] use方法被调用 - 客户端: {}, 玩家: {}, 手: {}", level.isClientSide(), player.getName().getString(), hand);
        
        // 客户端：检查是否绑定，未绑定则发送请求到服务器
        if (level.isClientSide()) {
            String worldId = getWorldId(itemStack);
            LOGGER.info("[维度钥匙] 客户端当前worldId: {}", worldId);
            
            if (worldId == null) {
                // 未绑定维度，发送请求到服务器打开GUI
                LOGGER.info("[维度钥匙] 客户端发送RequestOpenDimensionSelectorPacket");
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                    new mccfk.hy.hyserver.java.network.RequestOpenDimensionSelectorPacket()
                );
                LOGGER.info("[维度钥匙] 客户端请求已发送");
            } else {
                // 已绑定维度，客户端不做处理，等待服务器传送
                LOGGER.info("[维度钥匙] 客户端已绑定，等待服务器处理");
            }
            
            return InteractionResultHolder.pass(itemStack);
        }
        
        // 服务器端处理
        if (!(player instanceof ServerPlayer serverPlayer)) {
            LOGGER.warn("\n\n§e[HYSERVER.WARN]\n§e[维度钥匙] 玩家不是ServerPlayer");
            return InteractionResultHolder.fail(itemStack);
        }
        
        String worldId = getWorldId(itemStack);
        LOGGER.info("\n\n§a[HYSERVER.INFO]\n§a[维度钥匙] 服务器端当前worldId: {}", worldId);
        
        if (worldId == null) {
            // 未绑定维度，等待客户端请求（不主动打开GUI）
            LOGGER.info("\n\n§a[HYSERVER.INFO]\n§a[维度钥匙] 服务器端未绑定，等待客户端请求");
            return InteractionResultHolder.success(itemStack);
        } else {
            // 已绑定维度，根据当前所在维度决定行为
            String currentDimension = serverPlayer.level().dimension().location().getPath();
            LOGGER.info("\n\n§a[HYSERVER.INFO]\n§a[维度钥匙] 当前维度: {}, 绑定维度: d_{}", currentDimension, worldId);
            
            // 判断是否在私人维度（d_*格式）
            boolean isInPrivateDimension = currentDimension.startsWith("d_");
            
            if (isInPrivateDimension) {
                // 在私人维度内，返回外部位置
                LOGGER.info("\n\n§a[HYSERVER.INFO]\n§a[维度钥匙] 在私人维度内，尝试返回外部位置");
                teleportToExternalLocation(serverPlayer, itemStack);
            } else {
                // 在外部维度，记录位置并传送到私人维度
                LOGGER.info("\n\n§a[HYSERVER.INFO]\n§a[维度钥匙] 在外部维度，记录位置并传送到私人维度 d_{}", worldId);
                saveAndTeleportToDimension(serverPlayer, itemStack, worldId);
            }
            
            return InteractionResultHolder.success(itemStack);
        }
    }
    
    /**
     * 获取物品NBT中的世界ID
     */
    private String getWorldId(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        if (tag.contains(WORLD_ID_TAG)) {
            return tag.getString(WORLD_ID_TAG);
        }
        return null;
    }
    
    /**
     * 设置世界ID到物品NBT
     */
    private void setWorldId(ItemStack stack, String worldId) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        tag.putString(WORLD_ID_TAG, worldId);
        stack.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
    }
    
    /**
     * 保存当前位置并传送到指定维度
     */
    private void saveAndTeleportToDimension(ServerPlayer player, ItemStack keyStack, String worldId) {
        // 获取位置管理器
        mccfk.hy.hyserver.java.dimension.DimensionKeyLocationManager locationManager = 
            mccfk.hy.hyserver.java.HuiYangServerModContentManager.getDimensionKeyLocationManager();
        
        if (locationManager != null) {
            // 保存当前位置到永久文件
            locationManager.saveExternalLocation(player);
        }
        
        // 然后传送到私人维度
        teleportToDimension(player, keyStack, worldId);
    }
    
    /**
     * 传送到之前记录的外部位置
     */
    private void teleportToExternalLocation(ServerPlayer player, ItemStack keyStack) {
        // 获取位置管理器
        mccfk.hy.hyserver.java.dimension.DimensionKeyLocationManager locationManager = 
            mccfk.hy.hyserver.java.HuiYangServerModContentManager.getDimensionKeyLocationManager();
        
        if (locationManager == null) {
            player.sendSystemMessage(Component.literal("§c位置管理器未初始化！"));
            return;
        }
        
        // 获取保存的外部位置
        mccfk.hy.hyserver.java.dimension.DimensionKeyLocationManager.ExternalLocationData externalLoc = 
            locationManager.getExternalLocation(player.getUUID());
        
        if (externalLoc == null) {
            player.sendSystemMessage(Component.literal("§c未找到记录的外部位置！"));
            return;
        }
        
        // 获取目标维度
        ServerLevel targetLevel = player.getServer().getLevel(
            net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,
                net.minecraft.resources.ResourceLocation.parse(externalLoc.dimension)
            )
        );
        
        if (targetLevel == null) {
            player.sendSystemMessage(Component.literal("§c目标维度不存在：" + externalLoc.dimension));
            locationManager.clearExternalLocation(player.getUUID());
            return;
        }
        
        // 检查位置安全性并调整
        double x = externalLoc.x;
        double y = externalLoc.y;
        double z = externalLoc.z;
        
        // 确保y坐标在合法范围内
        if (y < targetLevel.getMinBuildHeight()) {
            y = targetLevel.getMinBuildHeight() + 1;
        }
        if (y > targetLevel.getMaxBuildHeight()) {
            y = targetLevel.getMaxBuildHeight() - 1;
        }
        
        // 传送玩家
        player.teleportTo(targetLevel, x, y, z, externalLoc.yaw, externalLoc.pitch);
        
        // 清除已使用的外部位置记录并保存
        locationManager.clearExternalLocation(player.getUUID());
        if (player.getServer() != null) {
            locationManager.save(player.getServer());
        }
        
        player.sendSystemMessage(Component.literal("§a已返回到外部位置"));
        
        LOGGER.info("\n\n§a[HYSERVER.INFO]\n§a[维度钥匙] 玩家 {} 已返回到外部位置: {}, {}, {} in {}", 
            player.getName().getString(), x, y, z, externalLoc.dimension);
    }
    
    /**
     * 传送到指定维度
     */
    private void teleportToDimension(ServerPlayer player, ItemStack keyStack, String worldId) {
        ServerLevel targetDimension = mccfk.hy.hyserver.java.dimension.PrivateDimensionManager.getDimension(
            player.getServer(), 
            worldId
        );
        
        if (targetDimension == null) {
            player.sendSystemMessage(Component.literal("§c维度 d_" + worldId + " 不存在！"));
            return;
        }
        
        // 计算安全出生点（y=1，基岩上方）
        double x = 0.0;
        double y = 1.0;
        double z = 0.0;
        
        // 传送玩家
        player.teleportTo(targetDimension, x, y, z, player.getYRot(), player.getXRot());
        
        player.sendSystemMessage(Component.literal("§a已传送到维度 d_" + worldId));
        

    }
}
