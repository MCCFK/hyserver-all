package mccfk.hy.hyserver.java.tab;

import java.io.*;
import java.util.*;
import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import mccfk.hy.hyserver.java.HuiYangServerModContentManager;
import mccfk.hy.hyserver.java.chat.ChatFormatter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * 称号管理器
 */
public class TitleManager {
    private File titleFile;
    private Map<String, String> playerTitles = new HashMap<>(); // 玩家名 -> 称号
    private static final String TITLES_FILE = "titles.json";

    /**
     * 初始化
     */
    public void init(File configDir) {
        titleFile = new File(configDir, TITLES_FILE);
        
        loadData();
    }

    /**
     * 加载数据
     */
    private void loadData() {
        if (!titleFile.exists()) {
            saveData();
            return;
        }
        
        try (FileReader reader = new FileReader(titleFile)) {
            JsonObject json = new com.google.gson.Gson().fromJson(reader, JsonObject.class);
            
            if (json.has("playerTitles")) {
                JsonObject titles = json.getAsJsonObject("playerTitles");
                for (Map.Entry<String, JsonElement> entry : titles.entrySet()) {
                    playerTitles.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
        } catch (Exception e) {
            HuiYangServerModContentManager.LOGGER.error("加载称号数据失败：{}", e.getMessage());
        }
    }

    /**
     * 保存数据
     */
    private void saveData() {
        try {
            JsonObject json = new JsonObject();
            JsonObject titles = new JsonObject();
            
            for (Map.Entry<String, String> entry : playerTitles.entrySet()) {
                titles.addProperty(entry.getKey(), entry.getValue());
            }
            
            json.add("playerTitles", titles);
            
            try (FileWriter writer = new FileWriter(titleFile)) {
                writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(json));
            }
        } catch (Exception e) {
            HuiYangServerModContentManager.LOGGER.error("保存称号数据失败：{}", e.getMessage());
        }
    }

    /**
     * 设置玩家称号
     */
    public boolean setPlayerTitle(String playerName, String title, net.minecraft.server.MinecraftServer server) {
        if (title == null || title.isEmpty()) {
            playerTitles.remove(playerName);
        } else {
            playerTitles.put(playerName, title);
        }
        saveData();
        
        return true;
    }

    /**
     * 刷新所有玩家的 Tab 列表
     * 通过反射设置显示名并广播更新
     */
    public void refreshAllTabLists(net.minecraft.server.MinecraftServer server) {
        if (server != null) {
            net.minecraft.server.players.PlayerList playerList = server.getPlayerList();
            java.util.List<ServerPlayer> players = playerList.getPlayers();
            
            try {
                java.lang.reflect.Field displayNameField = ServerPlayer.class.getDeclaredField("tabListDisplayName");
                displayNameField.setAccessible(true);

                for (ServerPlayer player : players) {
                    net.minecraft.network.chat.Component displayName = getDisplayNameComponent(player);
                    net.minecraft.network.chat.Component headNameTag = getHeadNameTagComponent(player);

                    displayNameField.set(player, displayName);

                    // 同步头顶名字（通过 EntityData 自动发送给追踪客户端）
                    player.setCustomName(headNameTag);

                    net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket packet = 
                        new net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket(
                            java.util.EnumSet.of(net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME),
                            java.util.List.of(player)
                        );
                    playerList.broadcastAll(packet);
                }
            } catch (Exception e) {
                HuiYangServerModContentManager.LOGGER.error("更新 Tab 列表失败：{}", e.getMessage());
            }
        }
    }

    /**
     * 获取带颜色的显示名 Component（Tab列表用）
     * 格式：[维度] 称号 玩家名
     */
    private MutableComponent getDisplayNameComponent(ServerPlayer player) {
        String playerName = player.getName().getString();
        String dimension = player.level().dimension().location().getPath();

        String dimName = getDimensionDisplay(dimension);
        
        String title = getPlayerTitle(playerName);

        StringBuilder sb = new StringBuilder();
        sb.append(dimName);
        if (!title.isEmpty()) {
            sb.append(title).append("&r ");
        }
        sb.append(playerName);
        
        return ChatFormatter.parseColorCodes(sb.toString());
    }

    /**
     * 获取玩家称号
     */
    public String getPlayerTitle(String playerName) {
        return playerTitles.getOrDefault(playerName, "");
    }
    
    /**
     * 获取实体头顶名字 Component
     * 格式：身份（成员/管理员）| 称号
     *       [特殊身份]（仅特定玩家）
     *       玩家名
     */
    public MutableComponent getHeadNameTagComponent(ServerPlayer player) {
        String playerName = player.getName().getString();
        String title = getPlayerTitle(playerName);
        MinecraftServer server = player.getServer();
        
        // 根据原版OP等级判断身份
        boolean isOp = server != null && server.getPlayerList().isOp(player.getGameProfile());
        String identity = isOp ? "管理员" : "成员";
        String identityColor = isOp ? "&c" : "&b";
        
        MutableComponent component = Component.empty();
        component.append(ChatFormatter.parseColorCodes("&f[" + identityColor + identity + "&f]"));
        
        if (!title.isEmpty()) {
            // 有称号：显示称号
            component.append(ChatFormatter.parseColorCodes(" &7 &e" + title + "&r"));
        } else {
            // 无称号：显示本地化文本 "无称号"
            component.append(ChatFormatter.parseColorCodes(" &7"));
            component.append(Component.translatable("hyserver.title.none").withStyle(ChatFormatting.GRAY));
        }
        
        // 特定玩家显示特殊身份
        String specialRole = getSpecialRole(playerName);
        if (!specialRole.isEmpty()) {
            component.append(ChatFormatter.parseColorCodes(" |&6&l" + specialRole + "&r"));
        }
        
        component.append(ChatFormatter.parseColorCodes(" | &f" + playerName));
        
        return component;
    }
    
    /**
     * 获取特定玩家的特殊身份显示
     * @return 特殊身份文本（不含换行符），非特定玩家返回空字符串
     */
    private String getSpecialRole(String playerName) {
        return switch (playerName) {
            case "GQUGD" -> " &6&l服主（魔丸）&r";
            case "MC_CaoFangKuai" -> " &6&l副服主&r";
            case "HYSERVER_ADM" -> " &6&l巡查员&r";
            default -> "";
        };
    }
    
    /**
     * 获取用于聊天的称号（单行版本，不含换行符）
     * @param playerName 玩家名
     * @return 单行称号文本
     */
    public String getPlayerTitleForChat(String playerName) {
        String title = playerTitles.getOrDefault(playerName, "");
        if (title.isEmpty()) {
            // 检查是否有特殊身份
            String specialRole = getSpecialRole(playerName);
            if (!specialRole.isEmpty()) {
                return specialRole;
            }
        }
        return title;
    }
    
    /**
     * 获取维度中文名
     */
    private String getDimensionChineseName(String dimension) {
        return switch (dimension) {
            case "overworld" -> "&a主世界&r";
            case "the_nether" -> "&c下界&r";
            case "the_end" -> "&d末地&r";
            default -> dimension;
        };
    }
    
    /**
     * 获取维度显示文本（用于Tab列表和聊天栏）
     * 私人维度格式：&6私人维度{ID}
     */
    private String getDimensionDisplay(String dimension) {
        if (dimension.startsWith("d_")) {
            try {
                String idStr = dimension.substring(2);
                int id = Integer.parseInt(idStr);
                if (id >= 1 && id <= 20) {
                    return "&f[&6私人维度" + id + "&f]&r ";
                }
            } catch (NumberFormatException e) {
            }
        }
        
        // 原版维度
        return switch (dimension) {
            case "overworld" -> "&r[&a主世界&r] ";
            case "the_nether" -> "&r[&c下界&r] ";
            case "the_end" -> "&r[&d末地&r] ";
            default -> "&r[&4&o&n" + dimension + "&r] ";
        };
    }
    
    /**
     * 获取带称号的玩家显示名（Tab列表 / TabListNameFormat 事件用）
     * 格式：[维度] 称号 玩家名
     */
    public String getDisplayName(ServerPlayer player) {
        String playerName = player.getName().getString();
        String dimension = player.level().dimension().location().getPath();

        String dimName = getDimensionDisplay(dimension);
        
        String title = getPlayerTitle(playerName);

        StringBuilder sb = new StringBuilder();
        sb.append(dimName);
        if (!title.isEmpty()) {
            sb.append(title).append("&r ");
        }
        sb.append(playerName);
        
        return sb.toString();
    }
}
