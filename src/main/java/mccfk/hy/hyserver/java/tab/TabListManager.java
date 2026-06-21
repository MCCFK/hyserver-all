package mccfk.hy.hyserver.java.tab;

import mccfk.hy.hyserver.java.chat.ChatFormatter;
import mccfk.hy.hyserver.java.config.ServerNameConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.lang.management.ManagementFactory;
import java.util.List;

/**
 * Tab 列表管理器 - 管理 Tab 列表的头部和底部信息
 */
public class TabListManager {
    
    private long serverStartTime = 0;
    private ServerNameConfig serverNameConfig = new ServerNameConfig();
    
    /**
     * 初始化服务器启动时间
     */
    public void init(MinecraftServer server) {
        serverStartTime = System.currentTimeMillis();
        serverNameConfig.load(server);
    }
    
    /**
     * 更新所有玩家的 Tab Header/Footer
     */
    public void updateTabList(MinecraftServer server) {
        if (server == null) return;
        
        List<ServerPlayer> players = server.getPlayerList().getPlayers();

        for (ServerPlayer player : players) {
            Component header = buildHeader(server, player);
            Component footer = buildFooter(server, player);

            ClientboundTabListPacket packet = new ClientboundTabListPacket(header, footer);
            player.connection.send(packet);
        }
    }
    
    /**
     * 构建 Header
     * 格式：
     * 灰羊服务器
     * {玩家名}
     */
    private Component buildHeader(MinecraftServer server, ServerPlayer player) {
        StringBuilder sb = new StringBuilder();
        
        // 服务器名称（从配置文件读取）
        sb.append(serverNameConfig.getServerName()).append("\n");

        String playerName = player.getName().getString();
        sb.append("&r&f").append(playerName);
        
        sb.append("\n&r&m&<#ff0000;#ffff00;#00ff00;#00ffff;#0000ff;#ff00ff>                                                                ");

        return ChatFormatter.parseColorCodes(sb.toString());
    }
    
    /**
     * 构建 Footer
     * 格式：
     * --------------------------------
     * 人数: X | TPS: XX.X | Ping: XXms | 内存: XXX/XXXMB (XX.X%)
     * 服务器运行时间: X天 X时 X分 X秒
     * 当前时间: YYYY-MM-DD HH:MM:SS
     */
    private Component buildFooter(MinecraftServer server, ServerPlayer player) {
        StringBuilder sb = new StringBuilder();
        
        // 分隔线
        sb.append("&r&m&<#ff0000;#ffff00;#00ff00;#00ffff;#0000ff;#ff00ff>                                                                \n");
        
        // 服务器信息
        int playerCount = server.getPlayerCount();
        int maxPlayers = server.getMaxPlayers();
        double tps = getTPS(server);
        int ping = getPlayerPing(player);
        long memoryUsed = getMemoryUsed();
        long memoryMax = getMemoryMax();
        double memoryPercent = (memoryUsed * 100.0) / memoryMax;
        String uptime = getUptime();
        String currentTime = getCurrentTime();
        
        sb.append("&r&7人数: &r&f").append(playerCount).append("&r&7/&r&f").append(maxPlayers);
        sb.append(" &r&7| &r&7TPS: &r&f").append(tps >= 0 ? String.format("%.1f", tps) : "--.--");
        sb.append(" &r&7| &r&7Ping: &r&f").append(ping).append("ms");
        sb.append(" &r&7| &r&7内存: &r&f").append(memoryUsed).append("&r&7/&r&f").append(memoryMax).append("MB");
        sb.append(" &r&7(&r&f").append(String.format("%.1f", memoryPercent)).append("%&r&7)");
        sb.append("\n&r&7服务器运行时间: &r&f").append(uptime);
        sb.append("\n&r&7当前时间: &r&f").append(currentTime);
        
        return ChatFormatter.parseColorCodes(sb.toString());
    }
    
    /**
     * 获取 TPS（每秒刻数）
     * 使用反射获取 MinecraftServer 的 recentTps 字段
     */
    private double getTPS(MinecraftServer server) {
        try {
            // 通过反射获取 recentTps 字段
            java.lang.reflect.Field recentTpsField = server.getClass().getField("recentTps");
            double[] recentTps = (double[]) recentTpsField.get(server);
            
            if (recentTps != null && recentTps.length > 0) {
                return recentTps[0]; // 返回1分钟平均TPS
            }
        } catch (Exception e) {
            // 如果反射失败，返回 -1 表示无效值
        }
        return -1.0;
    }
    
    /**
     * 获取玩家 Ping（延迟，毫秒）
     */
    private int getPlayerPing(ServerPlayer player) {
        return player.connection.latency();
    }
    
    /**
     * 获取已使用的内存（MB）
     */
    private long getMemoryUsed() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
    }
    
    /**
     * 获取最大内存（MB）
     */
    private long getMemoryMax() {
        return Runtime.getRuntime().maxMemory() / 1024 / 1024;
    }
    
    /**
     * 获取开服时间（天 时 分 秒）
     */
    private String getUptime() {
        long elapsed = System.currentTimeMillis() - serverStartTime;
        long seconds = elapsed / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        seconds = seconds % 60;
        minutes = minutes % 60;
        hours = hours % 24;
        
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("天 ");
        if (hours > 0 || days > 0) sb.append(hours).append("时 ");
        sb.append(minutes).append("分 ").append(seconds).append("秒");
        
        return sb.toString();
    }
    
    /**
     * 获取当前时间（YYYY-MM-DD HH:MM:SS）
     */
    private String getCurrentTime() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        return String.format("%04d-%02d-%02d %02d:%02d:%02d", 
            now.getYear(), now.getMonthValue(), now.getDayOfMonth(),
            now.getHour(), now.getMinute(), now.getSecond());
    }
}
