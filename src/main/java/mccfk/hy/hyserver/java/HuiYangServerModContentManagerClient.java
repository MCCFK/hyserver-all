package mccfk.hy.hyserver.java;

import mccfk.hy.hyserver.java.client.overlay.GroundItemOverlay;
import mccfk.hy.hyserver.java.client.renderer.GroundItemRenderer;
import mccfk.hy.hyserver.java.command.client.HyClientCommandRegistrar;
import mccfk.hy.hyserver.java.config.ConfigDirectoryManager;
import mccfk.hy.hyserver.java.entity.HyEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * 客户端入口类
 */
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class HuiYangServerModContentManagerClient {
    
    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // 客户端设置时执行
        Minecraft mc = Minecraft.getInstance();
        
        // 注册地面物品实体渲染器
        net.minecraft.client.renderer.entity.EntityRenderers.register(HyEntities.GROUND_ITEM.get(), GroundItemRenderer::new);
        
        // 检查并显示许可证（阻塞直到用户操作）
        boolean agreed = mccfk.hy.hyserver.java.license.ClientLicenseManager.checkAndShowLicense();
        if (!agreed) {
            // 用户拒绝，抛出异常阻止游戏加载
            throw new RuntimeException("HYServer License Refused - Game will not start");
        }
        
        // 初始化客户端配置目录
        ConfigDirectoryManager.initClientConfig();
        
        // 加载客户端配置
        mccfk.hy.hyserver.java.config.ClientConfigManager.loadConfig();
    }
    
    @SubscribeEvent
    static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        // 注册地面物品信息覆盖层
        event.registerAbove(VanillaGuiLayers.BOSS_OVERLAY, 
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("hyserver", "df_item_e_overlay"),
            GroundItemOverlay.OVERLAY);
    }
    
    @SubscribeEvent
    static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        // 注册客户端指令
        HyClientCommandRegistrar.register(event.getDispatcher());
    }

    
    @SubscribeEvent
    static void onClientLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        // 玩家登录时检测运行环境
        Minecraft mc = Minecraft.getInstance();
        
        if (mc.player != null) {
            boolean isSingleplayer = mc.getCurrentServer() == null;
            boolean isLAN = mc.hasSingleplayerServer();
            
            if (isSingleplayer || isLAN) {
                // 单人游戏或局域网环境，显示警告
                String envType = isSingleplayer ? "单人游戏" : "局域网联机";
                
                // 动态生成不可用和可用指令列表
                java.util.List<String> unavailableCommands = new java.util.ArrayList<>();
                java.util.List<String> availableCommands = new java.util.ArrayList<>();
                
                // 检查服务端指令
                // 服务端指令列表已禁用
                /*
                for (java.util.Map.Entry<String, mccfk.hy.hyserver.java.command.CommandLevelManager.CommandLevel> entry : 
                     mccfk.hy.hyserver.java.command.CommandLevelManager.getServerCommands().entrySet()) {
                    if (!mccfk.hy.hyserver.java.command.CommandLevelManager.canExecute(entry.getValue())) {
                        unavailableCommands.add(entry.getKey());
                    } else {
                        availableCommands.add(entry.getKey());
                    }
                }
                */
                
                // 检查客户端指令
                for (java.util.Map.Entry<String, mccfk.hy.hyserver.java.command.CommandLevelManager.CommandLevel> entry : 
                     mccfk.hy.hyserver.java.command.CommandLevelManager.getClientCommands().entrySet()) {
                    if (!mccfk.hy.hyserver.java.command.CommandLevelManager.canExecute(entry.getValue())) {
                        unavailableCommands.add(entry.getKey());
                    } else {
                        availableCommands.add(entry.getKey());
                    }
                }
                
                // 显示警告信息
                mc.player.sendSystemMessage(Component.literal("§c========== §e警告 §c=========="));
                mc.player.sendSystemMessage(Component.literal("§c游戏在" + envType + "环境运行"));
                
                if (!unavailableCommands.isEmpty()) {
                    mc.player.sendSystemMessage(Component.literal("§c部分指令无法使用："));
                    for (String cmd : unavailableCommands) {
                        mc.player.sendSystemMessage(Component.literal("§7- " + cmd));
                    }
                }
                
                if (!availableCommands.isEmpty()) {
                    mc.player.sendSystemMessage(Component.literal("§e可用指令："));
                    for (String cmd : availableCommands) {
                        mc.player.sendSystemMessage(Component.literal("§a- " + cmd));
                    }
                }
                
                mc.player.sendSystemMessage(Component.literal("§6==================================="));
            }
        }
    }
}
