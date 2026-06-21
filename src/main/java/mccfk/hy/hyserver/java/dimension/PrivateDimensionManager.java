package mccfk.hy.hyserver.java.dimension;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 私人维度管理器
 * 维度通过数据包自动注册（data/hyserver/dimension/D_1.json ~ D_20.json）
 * 此类仅提供维度访问接口
 */
public class PrivateDimensionManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrivateDimensionManager.class);
    private static final int TOTAL_DIMENSIONS = 20;
    
    /**
     * 初始化：验证维度是否已正确加载
     * （维度由 NeoForge 从数据包自动注册，无需手动创建）
     */
    public static void initialize(MinecraftServer server) {
        LOGGER.info("\n\n§a[HYSERVER.INFO]\n§a[私人维度] 验证预定义维度加载状态...");
        
        int loadedCount = 0;
        for (int i = 1; i <= TOTAL_DIMENSIONS; i++) {
            ServerLevel level = getDimension(server, String.valueOf(i));
            if (level != null) {
                loadedCount++;
            } else {
                LOGGER.warn("\n\n§e[HYSERVER.WARN]\n§e[私人维度] 维度 d_{} 未找到，请检查数据包配置", i);
            }
        }
        
        if (loadedCount == TOTAL_DIMENSIONS) {
            LOGGER.info("\n\n§a[HYSERVER.INFO]\n§a[私人维度] 所有 {} 个维度已成功加载", TOTAL_DIMENSIONS);
        } else {
            LOGGER.error("\n\n§c[HYSERVER.ERROR]\n§c[私人维度] 仅加载 {}/{} 个维度，可能存在配置问题", loadedCount, TOTAL_DIMENSIONS);
        }
    }
    
    /**
     * 根据世界ID获取维度对象
     * @param worldId "1" ~ "20"
     */
    public static ServerLevel getDimension(MinecraftServer server, String worldId) {
        try {
            int id = Integer.parseInt(worldId);
            if (id < 1 || id > TOTAL_DIMENSIONS) {
                LOGGER.warn("\n\n§e[HYSERVER.WARN]\n§e[私人维度] 无效的世界ID: {}", worldId);
                return null;
            }
            
            String dimensionId = "d_" + id;
            ResourceLocation dimLocation = ResourceLocation.fromNamespaceAndPath("hyserver", dimensionId);
            ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, dimLocation);
            
            return server.getLevel(dimKey);
        } catch (NumberFormatException e) {
            LOGGER.warn("\n\n§e[HYSERVER.WARN]\n§e[私人维度] 世界ID格式错误: {}", worldId);
            return null;
        } catch (Exception e) {
            LOGGER.error("\n\n§c[HYSERVER.ERROR]\n§c[私人维度] 获取维度 {} 失败", worldId, e);
            return null;
        }
    }
}
