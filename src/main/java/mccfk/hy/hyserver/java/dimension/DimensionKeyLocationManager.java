package mccfk.hy.hyserver.java.dimension;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import mccfk.hy.hyserver.java.HuiYangServerModContentManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 维度钥匙位置管理器 - 记录玩家从外部进入私人维度前的位置
 */
public class DimensionKeyLocationManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DimensionKeyLocationManager.class);
    private static final String DIMENSION_KEY_LOCATIONS_FILE = "dimension_key_locations.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    // 玩家UUID -> 外部位置信息
    private Map<UUID, ExternalLocationData> externalLocations = new ConcurrentHashMap<>();
    
    public static class ExternalLocationData {
        public double x, y, z;
        public float yaw, pitch;
        public String dimension;
        
        public ExternalLocationData(double x, double y, double z, float yaw, float pitch, String dimension) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.dimension = dimension;
        }
    }
    
    /**
     * 加载数据
     */
    public void load(MinecraftServer server) {
        Path dataDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("hyserver");
        Path filePath = dataDir.resolve(DIMENSION_KEY_LOCATIONS_FILE);
        
        if (!Files.exists(filePath)) {
            save(server);
            return;
        }
        
        try {
            String json = Files.readString(filePath);
            JsonObject jsonObject = GSON.fromJson(json, JsonObject.class);
            
            if (jsonObject.has("externalLocations")) {
                JsonObject locations = jsonObject.getAsJsonObject("externalLocations");
                for (Map.Entry<String, com.google.gson.JsonElement> entry : locations.entrySet()) {
                    try {
                        UUID uuid = UUID.fromString(entry.getKey());
                        JsonObject locData = entry.getValue().getAsJsonObject();
                        
                        double x = locData.get("x").getAsDouble();
                        double y = locData.get("y").getAsDouble();
                        double z = locData.get("z").getAsDouble();
                        float yaw = locData.get("yaw").getAsFloat();
                        float pitch = locData.get("pitch").getAsFloat();
                        String dimension = locData.get("dimension").getAsString();
                        
                        externalLocations.put(uuid, new ExternalLocationData(x, y, z, yaw, pitch, dimension));
                    } catch (Exception e) {
                        LOGGER.warn("\n\n§e[HYSERVER.WARN]\n§e[维度钥匙位置] 加载玩家位置数据失败: {}", e.getMessage());
                    }
                }
            }
            
            LOGGER.info("\n\n§a[HYSERVER.INFO]\n§a[维度钥匙位置] 已加载 {} 个玩家的外部位置数据", externalLocations.size());
        } catch (Exception e) {
            LOGGER.error("\n\n§c[HYSERVER.ERROR]\n§c[维度钥匙位置] 加载数据失败: {}", e.getMessage());
        }
    }
    
    /**
     * 保存数据
     */
    public void save(MinecraftServer server) {
        Path dataDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("hyserver");
        Path filePath = dataDir.resolve(DIMENSION_KEY_LOCATIONS_FILE);
        
        try {
            Files.createDirectories(dataDir);
            
            JsonObject root = new JsonObject();
            JsonObject locations = new JsonObject();
            
            for (Map.Entry<UUID, ExternalLocationData> entry : externalLocations.entrySet()) {
                JsonObject locData = new JsonObject();
                ExternalLocationData data = entry.getValue();
                
                locData.addProperty("x", data.x);
                locData.addProperty("y", data.y);
                locData.addProperty("z", data.z);
                locData.addProperty("yaw", data.yaw);
                locData.addProperty("pitch", data.pitch);
                locData.addProperty("dimension", data.dimension);
                
                locations.add(entry.getKey().toString(), locData);
            }
            
            root.add("externalLocations", locations);
            
            String json = GSON.toJson(root);
            Files.writeString(filePath, json);
        } catch (Exception e) {
            LOGGER.error("\n\n§c[HYSERVER.ERROR]\n§c[维度钥匙位置] 保存数据失败: {}", e.getMessage());
        }
    }
    
    /**
     * 保存玩家当前外部位置
     */
    public void saveExternalLocation(ServerPlayer player) {
        externalLocations.put(player.getUUID(), new ExternalLocationData(
            player.getX(),
            player.getY(),
            player.getZ(),
            player.getYRot(),
            player.getXRot(),
            player.level().dimension().location().toString()
        ));
        
        // 异步保存到文件
        MinecraftServer server = player.getServer();
        if (server != null) {
            save(server);
        }
        
        LOGGER.info("\n\n§a[HYSERVER.INFO]\n§a[维度钥匙位置] 已保存玩家 {} 的外部位置: {}, {}, {} in {}", 
            player.getName().getString(), player.getX(), player.getY(), player.getZ(), 
            player.level().dimension().location().getPath());
    }
    
    /**
     * 获取玩家的外部位置
     */
    public ExternalLocationData getExternalLocation(UUID playerUUID) {
        return externalLocations.get(playerUUID);
    }
    
    /**
     * 清除玩家的外部位置
     */
    public void clearExternalLocation(UUID playerUUID) {
        externalLocations.remove(playerUUID);
        
        // 注意：这里不自动保存，调用者应该在合适的时机调用 save()
    }
}
