package mccfk.hy.hyserver.java;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.minecraft.server.level.ServerPlayer;

import mccfk.hy.hyserver.java.command.HyServerCommands;
import mccfk.hy.hyserver.java.config.ConfigDirectoryManager;
import mccfk.hy.hyserver.java.config.ChatConfig;
import mccfk.hy.hyserver.java.permission.PermissionManager;
import mccfk.hy.hyserver.java.tab.TitleManager;
import mccfk.hy.hyserver.java.tab.TabListManager;
import mccfk.hy.hyserver.java.chat.ChatFormatter;
import mccfk.hy.hyserver.java.home.HomeManager;
import mccfk.hy.hyserver.java.tpa.TpaManager;
import mccfk.hy.hyserver.java.tpa.BackLocationManager;
import mccfk.hy.hyserver.java.block.ModBlocks;
import mccfk.hy.hyserver.java.item.ModItems;
import mccfk.hy.hyserver.java.item.creative.ModCreativeTabs;
import mccfk.hy.hyserver.java.block.entity.ModBlockEntities;
import mccfk.hy.hyserver.java.menu.ModMenus;
import mccfk.hy.hyserver.java.banitem.BanItemManager;
import mccfk.hy.hyserver.java.entity.HyEntities;

import mccfk.hy.hyserver.java.command.tpa.HyTpaCommand;
import mccfk.hy.hyserver.java.command.tpa.HyTpaHereCommand;
import mccfk.hy.hyserver.java.command.tpa.HyTpAcceptCommand;
import mccfk.hy.hyserver.java.command.tpa.HyTpDenyCommand;
import mccfk.hy.hyserver.java.command.tpa.HyTpCancelCommand;
import mccfk.hy.hyserver.java.command.tpa.HyRtpCommand;
import mccfk.hy.hyserver.java.command.tpa.HyBackCommand;
import mccfk.hy.hyserver.java.command.home.HyHomeCommand;
import mccfk.hy.hyserver.java.command.home.HySetHomeCommand;
import mccfk.hy.hyserver.java.command.home.HyDHomeCommand;
import mccfk.hy.hyserver.java.command.HyShowItemCommand;

import java.io.File;
import java.nio.file.Path;

// 模组ID必须与 META-INF/neoforge.mods.toml 文件中的值匹配
@Mod(HuiYangServerModContentManager.MODID)
public class HuiYangServerModContentManager {
    public static final String MODID = "hyserver";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static HuiYangServerModContentManager instance;
    private final PermissionManager permissionManager = new PermissionManager();
    private final TitleManager titleManager = new TitleManager();
    private final TabListManager tabListManager = new TabListManager();
    private final HomeManager homeManager = new HomeManager();
    private final TpaManager tpaManager = new TpaManager();
    private final BackLocationManager backLocationManager = new BackLocationManager();
    private final mccfk.hy.hyserver.java.dimension.DimensionKeyLocationManager dimensionKeyLocationManager = new mccfk.hy.hyserver.java.dimension.DimensionKeyLocationManager();
    private HyServerCommands hyServerCommands;
    private int tabUpdateCounter = 0;
    private int tickCounter = 0;
    private long lastSaveTime = 0;
    private boolean saveInProgress = false;
    
    public HuiYangServerModContentManager(IEventBus modEventBus, ModContainer modContainer) {
        instance = this;
        
        // 注册方块、物品、方块实体、创造模式标签页和菜单
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModMenus.register(modEventBus);
        HyEntities.ENTITY_TYPES.register(modEventBus);
        
        // 注册方块能力
        modEventBus.addListener(this::registerCapabilities);
        
        // 注册配置文件
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, ChatConfig.SPEC);
        
        // 注册事件监听器
        NeoForge.EVENT_BUS.register(this);
        
        // Tab 列表更新计数器
        tabUpdateCounter = 0;
    }
    
    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        // 注册无限岩浆方块的流体处理能力
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                mccfk.hy.hyserver.java.block.entity.ModBlockEntities.INFINITE_LAVA.get(),
                (blockEntity, context) -> blockEntity
        );
        
        // 注册无限水方块的流体处理能力
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                mccfk.hy.hyserver.java.block.entity.ModBlockEntities.INFINITE_WATER.get(),
                (blockEntity, context) -> blockEntity
        );
        
        // 注册太阳能板的能量能力
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                mccfk.hy.hyserver.java.block.entity.ModBlockEntities.SOLAR_PANEL.get(),
                (blockEntity, context) -> blockEntity.getEnergyStorage(null)
        );
    }
    
    /**
     * 注册 /hyoff 命令
     */
    private void registerHyOffCommand(com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher) {
        dispatcher.register(net.minecraft.commands.Commands.literal("hyoff")
            .requires(source -> {
                if (source.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
                    return permissionManager.getPermissionLevel(player) >= 1;
                }
                return false;
            })
            .then(net.minecraft.commands.Commands.argument("eventId", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                .executes(context -> {
                    String eventId = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "eventId");
                    net.minecraft.server.level.ServerPlayer admin = context.getSource().getPlayerOrException();
                    
                    boolean success = mccfk.hy.hyserver.java.banitem.BanItemManager.markEventAsProcessed(admin, eventId);
                    if (success) {
                        return 1;
                    } else {
                        return 0;
                    }
                })
            )
        );
    }
    
    /**
     * 获取模组实例
     */
    public static HuiYangServerModContentManager getInstance() {
        return instance;
    }
    
    /**
     * 获取当前服务器实例
     */
    public static net.minecraft.server.MinecraftServer getServer() {
        if (instance != null) {
            // 尝试从任意管理器获取服务器引用
            if (instance.homeManager != null) {
                // HomeManager 没有直接保存服务器引用，我们需要另一种方式
            }
        }
        // 返回null，调用者需要自行处理
        return null;
    }
    
    public static PermissionManager getPermissionManager() {
        return instance != null ? instance.permissionManager : null;
    }
    
    public static TitleManager getTitleManager() {
        return instance != null ? instance.titleManager : null;
    }
    
    public static HomeManager getHomeManager() {
        return instance != null ? instance.homeManager : null;
    }
    
    public static TpaManager getTpaManager() {
        return instance != null ? instance.tpaManager : null;
    }
    
    public static BackLocationManager getBackLocationManager() {
        return instance != null ? instance.backLocationManager : null;
    }
    
    public static mccfk.hy.hyserver.java.dimension.DimensionKeyLocationManager getDimensionKeyLocationManager() {
        return instance != null ? instance.dimensionKeyLocationManager : null;
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // 服务器启动时执行
        
        // 注册太阳能板的 MovementBehaviour
        com.simibubi.create.api.behaviour.movement.MovementBehaviour.REGISTRY.register(
            mccfk.hy.hyserver.java.block.ModBlocks.SOLAR_PANEL.get(),
            new mccfk.hy.hyserver.java.movement.SolarPanelMovement()
        );
        com.simibubi.create.api.behaviour.movement.MovementBehaviour.REGISTRY.register(
            mccfk.hy.hyserver.java.block.ModBlocks.SOLAR_PANEL_1.get(),
            new mccfk.hy.hyserver.java.movement.SolarPanelMovement()
        );
        com.simibubi.create.api.behaviour.movement.MovementBehaviour.REGISTRY.register(
            mccfk.hy.hyserver.java.block.ModBlocks.SOLAR_PANEL_2.get(),
            new mccfk.hy.hyserver.java.movement.SolarPanelMovement()
        );
        com.simibubi.create.api.behaviour.movement.MovementBehaviour.REGISTRY.register(
            mccfk.hy.hyserver.java.block.ModBlocks.SOLAR_PANEL_3.get(),
            new mccfk.hy.hyserver.java.movement.SolarPanelMovement()
        );
        
        // 检测环境等级
        boolean isDedicated = event.getServer().isDedicatedServer();
        int envLevel = isDedicated ? 0 : 1; // 0=专用服务器, 1=单人/局域网
        
        if (envLevel == 0) {
            // === 服务器（等级0）：启动所有功能 ===
            
            // 验证服务端许可证
            mccfk.hy.hyserver.java.license.ServerLicenseManager.checkLicenseAsync(event.getServer());
            
            // 初始化服务端配置目录
            ConfigDirectoryManager.initServerConfig(event.getServer());
            
            // 初始化公告文件（不存在时从 classpath 拷贝默认文件）
            mccfk.hy.hyserver.java.announce.AnnouncementManager.init(event.getServer());
            
            // 初始化权限管理器
            permissionManager.init(event.getServer());
            
            // 初始化称号管理器
            titleManager.init(ConfigDirectoryManager.getServerConfigDir(event.getServer()));
            
            // 初始化 Tab 列表管理器
            tabListManager.init(event.getServer());
            
            // 初始化 Home 管理器
            homeManager.init(event.getServer());
            
            // 初始化禁止物品管理器
            BanItemManager.init(event.getServer(), permissionManager);
            
            // 初始化维度钥匙位置管理器
            dimensionKeyLocationManager.load(event.getServer());
            
            // 注册所有指令
            hyServerCommands = new HyServerCommands(permissionManager, titleManager);
            hyServerCommands.register(event.getServer().getCommands().getDispatcher());
            
            HyTpaCommand.register(event.getServer().getCommands().getDispatcher(), tpaManager, backLocationManager);
            HyTpaHereCommand.register(event.getServer().getCommands().getDispatcher(), tpaManager, backLocationManager);
            HyTpAcceptCommand.register(event.getServer().getCommands().getDispatcher(), tpaManager, backLocationManager);
            HyTpDenyCommand.register(event.getServer().getCommands().getDispatcher(), tpaManager);
            HyTpCancelCommand.register(event.getServer().getCommands().getDispatcher(), tpaManager);
            HyRtpCommand.register(event.getServer().getCommands().getDispatcher(), backLocationManager, permissionManager);
            HyBackCommand.register(event.getServer().getCommands().getDispatcher(), backLocationManager);
            HyHomeCommand.register(event.getServer().getCommands().getDispatcher(), homeManager, backLocationManager);
            HySetHomeCommand.register(event.getServer().getCommands().getDispatcher(), homeManager);
            HyDHomeCommand.register(event.getServer().getCommands().getDispatcher(), homeManager);
            HyShowItemCommand.register(event.getServer().getCommands().getDispatcher());
            
            // 地面物品测试命令已禁用
            // mccfk.hy.hyserver.java.command.HyTestGroundItemCommand.register(event.getServer().getCommands().getDispatcher());
            
            // 注册维度切换指令
            mccfk.hy.hyserver.java.command.HyDimensionCommand.register(
                event.getServer().getCommands().getDispatcher(),
                permissionManager,
                backLocationManager
            );
            
            // URL ban/unban 指令已禁用
            // mccfk.hy.hyserver.java.command.ban.HyUrlBanCommand.register(event.getServer().getCommands().getDispatcher());
            // mccfk.hy.hyserver.java.command.ban.HyUrlUnbanCommand.register(event.getServer().getCommands().getDispatcher());
            
            // 注册 /hyoff 指令
            registerHyOffCommand(event.getServer().getCommands().getDispatcher());
            
            // 初始化封禁系统
            mccfk.hy.hyserver.java.ban.BanManager.init(event.getServer(), permissionManager);
            
            // 启动自动重载任务
            startAutoReload(event.getServer());
            
        } else {
            // === 单人/局域网（等级1）：仅保留基础功能 ===
            
            // 不注册任何服务端指令
            // 不初始化任何管理器（Permission、Title、Home、TabList）
            // 不启动自动重载
            // 客户端指令由 HuiYangServerModContentManagerClient 处理
        }
    }
    
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {

        // 重新注册所有指令
        if (hyServerCommands != null) {
            hyServerCommands.register(event.getDispatcher());
        }
        
        HyTpaCommand.register(event.getDispatcher(), tpaManager, backLocationManager);
        HyTpaHereCommand.register(event.getDispatcher(), tpaManager, backLocationManager);
        HyTpAcceptCommand.register(event.getDispatcher(), tpaManager, backLocationManager);
        HyTpDenyCommand.register(event.getDispatcher(), tpaManager);
        HyTpCancelCommand.register(event.getDispatcher(), tpaManager);
        HyRtpCommand.register(event.getDispatcher(), backLocationManager, permissionManager);
        HyBackCommand.register(event.getDispatcher(), backLocationManager);
        HyHomeCommand.register(event.getDispatcher(), homeManager, backLocationManager);
        HySetHomeCommand.register(event.getDispatcher(), homeManager);
        HyDHomeCommand.register(event.getDispatcher(), homeManager);
        HyShowItemCommand.register(event.getDispatcher());
        
        // 地面物品测试命令已禁用
        // mccfk.hy.hyserver.java.command.HyTestGroundItemCommand.register(event.getDispatcher());
        
        // 注册维度切换指令
        mccfk.hy.hyserver.java.command.HyDimensionCommand.register(
            event.getDispatcher(),
            permissionManager,
            backLocationManager
        );
        
        // URL ban/unban 指令已禁用
        // mccfk.hy.hyserver.java.command.ban.HyUrlBanCommand.register(event.getDispatcher());
        // mccfk.hy.hyserver.java.command.ban.HyUrlUnbanCommand.register(event.getDispatcher());
        
        // 注册 /hyoff 指令
        registerHyOffCommand(event.getDispatcher());

    }
    
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        // 玩家加入服务器时发送提示
        String playerName = event.getEntity().getName().getString();
        net.minecraft.network.chat.Component message = net.minecraft.network.chat.Component.literal("§r[§a+§r] " + playerName + " §e加入了游戏");
        
        // 向所有在线玩家发送消息
        event.getEntity().getServer().getPlayerList().broadcastSystemMessage(message, false);
        
        // 发送服务端版本到客户端
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            sendServerVersion(player);
            // 发送公告
            mccfk.hy.hyserver.java.announce.AnnouncementManager.sendToPlayer(player.getServer(), player);
        }
    }
    
    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        // 玩家退出服务器时发送提示
        String playerName = event.getEntity().getName().getString();
        net.minecraft.network.chat.Component message = net.minecraft.network.chat.Component.literal("§r[§c-§r] " + playerName + " §e退出了游戏");
        
        // 向所有在线玩家发送消息
        event.getEntity().getServer().getPlayerList().broadcastSystemMessage(message, false);
    }
    
    @SubscribeEvent
    public void onPlayerChangedDimension(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
        // 玩家切换维度时保存Back位置
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            if (player.getServer() != null && player.getServer().isDedicatedServer()) {
                backLocationManager.saveLocation(player);
            }
        }
    }
    
    @SubscribeEvent
    public void onPlayerDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        // 仅在专用服务器上记录死亡位置
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer) {
            net.minecraft.server.level.ServerPlayer player = (net.minecraft.server.level.ServerPlayer) event.getEntity();
            if (player.getServer() != null && player.getServer().isDedicatedServer()) {
                backLocationManager.saveLocation(player);
            }
        }
    }
    
    @SubscribeEvent
    public void onServerTickPre(net.neoforged.neoforge.event.tick.ServerTickEvent.Pre event) {
        net.minecraft.server.MinecraftServer server = event.getServer();
        if (server == null || !server.isDedicatedServer()) return;
        
        // 限制有未处理违规事件的玩家
        for (net.minecraft.server.level.ServerPlayer player : server.getPlayerList().getPlayers()) {
            String playerName = player.getName().getString();
            // 检查是否有未处理的事件
            if (mccfk.hy.hyserver.java.banitem.BanItemManager.hasUnprocessedEvents(playerName)) {
                // 静默传送回当前位置
                player.teleportTo(player.getX(), player.getY(), player.getZ());
                // 强制关闭所有界面
                if (player.containerMenu != player.inventoryMenu) {
                    player.closeContainer();
                }
            }
        }
    }
    
    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        net.minecraft.server.MinecraftServer server = event.getServer();
        if (server == null || !server.isDedicatedServer()) return;
        
        // 每 20 tick (1秒) 检测管理员保护
        tickCounter++;
        if (tickCounter % 20 == 0) {
            mccfk.hy.hyserver.java.protection.AdminProtection.checkAndProtect(server, permissionManager);
        }
        
        // 每 10 tick 调用禁止物品管理器
        if (tickCounter % 10 == 0) {
            BanItemManager.tick(server);
        }
        
        // 每 10 tick 更新一次 Tab 列表
        tabUpdateCounter++;
        if (tabUpdateCounter >= 10) {
            tabUpdateCounter = 0;
            tabListManager.updateTabList(server);
            titleManager.refreshAllTabLists(server);
        }
    }
    
    @SubscribeEvent
    public void onPlayerNameFormat(PlayerEvent.TabListNameFormat event) {
        // 仅在专用服务器上格式化 Tab 列表名称
        try {
            net.minecraft.server.MinecraftServer server = event.getEntity().getServer();
            if (server != null && server.isDedicatedServer() && event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
                String displayName = titleManager.getDisplayName(player);
                // 解析颜色代码
                event.setDisplayName(ChatFormatter.parseColorCodes(displayName));
            }
        } catch (Exception e) {
            LOGGER.error("[MCCFK][hyserver] Tab名称格式化失败: {}", e.getMessage());
        }
    }
    
    @SubscribeEvent
    public void onServerChat(net.neoforged.neoforge.event.ServerChatEvent event) {
        // 应用聊天格式化（专用服务器 + 单人/局域网）
        try {
            net.minecraft.server.MinecraftServer server = event.getPlayer().getServer();
            if (server == null) return;
            
            // 单人/局域网服务器：不取消事件，让原版处理
            if (!server.isDedicatedServer()) {
                return;
            }
            
            String message = event.getMessage().getString();
            
            // 检测是否为 QuickShop 等插件的价格输入（纯数字或小数）
            // QuickShop 使用聊天事件获取价格，需要放行
            if (message.matches("^[0-9]+(\\.[0-9]+)?$")) {
                // 纯数字消息，不取消事件，让其他插件处理
                return;
            }
            
            // 检测是否为命令输入（以 / 开头）
            // QuickShop 的命令交互也需要放行
            if (message.startsWith("/")) {
                return;
            }
            
            // 取消原版消息发送
            event.setCanceled(true);
            net.minecraft.server.level.ServerPlayer player = event.getPlayer();
            String playerName = player.getName().getString();
            
            // 检测并替换 [物品] 为物品组件
            net.minecraft.network.chat.Component contentMessage = event.getMessage();
            if (message.contains("[物品]")) {
                net.minecraft.world.item.ItemStack heldItem = player.getMainHandItem();
                if (!heldItem.isEmpty()) {
                    // 注册物品展示，获取UUID
                    java.util.UUID displayUuid = mccfk.hy.hyserver.java.command.ItemDisplayManager.registerDisplay(heldItem);
                    
                    // 创建物品展示组件
                    net.minecraft.network.chat.MutableComponent itemComponent = net.minecraft.network.chat.Component.empty();
                    itemComponent.append(net.minecraft.network.chat.Component.literal("["));
                    
                    net.minecraft.network.chat.MutableComponent itemNameComponent = heldItem.getHoverName().copy();
                    net.minecraft.network.chat.MutableComponent tooltip = net.minecraft.network.chat.Component.empty();
                    tooltip.append(heldItem.getHoverName());
                    
                    // 点击事件携带 UUID 参数
                    String clickCommand = "/hyshowitem " + displayUuid.toString();
                    itemNameComponent = itemNameComponent.withStyle(style -> style
                            .withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, tooltip))
                            .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, clickCommand))
                    );
                    
                    itemComponent.append(itemNameComponent);
                    itemComponent.append(net.minecraft.network.chat.Component.literal("]"));
                    
                    // 替换消息中的 [物品]
                    String newMessageStr = message.replace("[物品]", "__ITEM_PLACEHOLDER__");
                    net.minecraft.network.chat.MutableComponent newMessage = net.minecraft.network.chat.Component.empty();
                    
                    String[] parts = newMessageStr.split("__ITEM_PLACEHOLDER__", -1); // 保留空字符串
                    for (int i = 0; i < parts.length; i++) {
                        // 对每个文本部分解析颜色代码
                        if (!parts[i].isEmpty()) {
                            newMessage.append(mccfk.hy.hyserver.java.chat.ChatFormatter.parseColorCodes(parts[i]));
                        }
                        if (i < parts.length - 1) {
                            newMessage.append(itemComponent.copy());
                        }
                    }
                    
                    contentMessage = newMessage;
                }
            }
            
            // 获取玩家所在维度
            String dimension = player.level().dimension().location().getPath();
            
            // 检查是否为私人维度（d_1 ~ d_20）
            if (dimension.startsWith("d_")) {
                try {
                    String idStr = dimension.substring(2);
                    int id = Integer.parseInt(idStr);
                    if (id >= 1 && id <= 20) {
                        dimension = "&r[&6私人维度" + id + "&r]";
                    } else {
                        dimension = "&r[&4&o&n" + dimension + "&r]";
                    }
                } catch (NumberFormatException e) {
                    dimension = "&r[&4&o&n" + dimension + "&r]";
                }
            } else {
                // 原版维度
                dimension = switch (dimension) {
                    case "overworld" -> "&r[&a主世界&r]";
                    case "the_nether" -> "&r[&c下界&r]";
                    case "the_end" -> "&r[&d末地&r]";
                    default -> "&r[&4&o&n" + dimension + "&r]";
                };
            }
            
            // 获取称号（单行版本，用于聊天栏）
            String title = titleManager.getPlayerTitleForChat(playerName);
            
            // 如果配置为不显示，则清空
            if (!ChatConfig.ENABLE_TITLE.get()) {
                title = "";
            }
            if (!ChatConfig.ENABLE_DIMENSION.get()) {
                dimension = "";
            }
            
            // 构建新的聊天格式：位置组件 称号组件 玩家名（可点击） 说：内容
            net.minecraft.network.chat.MutableComponent finalMessage = net.minecraft.network.chat.Component.empty();
            
            // 位置组件
            if (!dimension.isEmpty()) {
                finalMessage.append(ChatFormatter.parseColorCodes(dimension));
                finalMessage.append(net.minecraft.network.chat.Component.literal(" "));
            }
            
            // 称号组件
            if (!title.isEmpty()) {
                finalMessage.append(ChatFormatter.parseColorCodes(title));
                finalMessage.append(net.minecraft.network.chat.Component.literal(" "));
            }
            
            // 玩家名组件（纯文本，但添加点击事件以支持私信补全）
            net.minecraft.network.chat.MutableComponent playerNameComponent = net.minecraft.network.chat.Component.literal(playerName);
            playerNameComponent = playerNameComponent.withStyle(style -> style
                    .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.SUGGEST_COMMAND, "/tell " + playerName + " "))
                    .withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, net.minecraft.network.chat.Component.literal("")))
            );
            finalMessage.append(playerNameComponent);
            
            // 说：组件
            finalMessage.append(net.minecraft.network.chat.Component.literal(" "));
            finalMessage.append(ChatFormatter.parseColorCodes("&7说：&r "));
            
            // 玩家聊天内容组件（解析&颜色代码）
            // 注意：需要先检查是否包含物品组件
            String contentText = contentMessage.getString();
            net.minecraft.network.chat.Component parsedContent;
            
            if (message.contains("[物品]") && contentText.contains("[")) {
                // 包含物品组件，需要特殊处理
                // 由于物品组件已经替换了[物品]，这里直接解析整个消息
                parsedContent = contentMessage; // 保留物品组件的点击事件
            } else {
                // 普通消息，解析颜色代码
                parsedContent = ChatFormatter.parseColorCodes(contentText);
                
                // 安全检查：如果解析结果为空，使用原始文本
                if (parsedContent.getString().isEmpty() && !contentText.isEmpty()) {
                    parsedContent = net.minecraft.network.chat.Component.literal(contentText);
                }
            }
            finalMessage.append(parsedContent);
            
            // 手动广播消息
            server.getPlayerList().broadcastSystemMessage(finalMessage, false);
        } catch (Exception e) {
            LOGGER.error("[MCCFK][hyserver] 聊天格式化失败: {}", e.getMessage());
        }
    }
    

    /**
     * 启动自动重载任务
     */
    private void startAutoReload(net.minecraft.server.MinecraftServer server) {
        Thread reloadThread = new Thread(() -> {
            LOGGER.info("[MCCFK][hyserver] 自动重载线程已启动");
            while (!server.isStopped()) {
                try {
                    // 等待10秒
                    Thread.sleep(10000);
                    // 重新加载权限配置
                    permissionManager.reload();
                    // 清理过期TPA请求
                    tpaManager.cleanupExpiredRequests();
                    // 清理过期物品展示
                    mccfk.hy.hyserver.java.command.ItemDisplayManager.cleanupExpired();
                    
                    // 清理所有玩家背包中带展示标签的物品
                    server.getPlayerList().getPlayers().forEach(player -> {
                        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                            net.minecraft.world.item.ItemStack item = player.getInventory().getItem(i);
                            if (!item.isEmpty()) {
                                net.minecraft.world.item.component.CustomData customData = item.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
                                if (customData != null && customData.copyTag().getBoolean("HyServer_DisplayItem")) {
                                    player.getInventory().setItem(i, net.minecraft.world.item.ItemStack.EMPTY);
                                }
                            }
                        }
                    });
                    
                    LOGGER.debug("[MCCFK][hyserver] 自动重载完成");
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    LOGGER.error("[MCCFK][hyserver]<FALSE>自动重载失败[END]", e);
                }
            }
            LOGGER.info("[MCCFK][hyserver] 自动重载线程已停止");
        }, "HyServer-AutoReload");
        reloadThread.setDaemon(true); // 设置为守护线程，不会阻止JVM退出
        reloadThread.start();
    }
    
    /**
     * 发送服务端版本到客户端
     */
    private void sendServerVersion(net.minecraft.server.level.ServerPlayer player) {
        try {
            String serverVersion = getServerVersion();
            mccfk.hy.hyserver.java.network.ServerVersionSyncPacket packet = 
                new mccfk.hy.hyserver.java.network.ServerVersionSyncPacket(serverVersion);
            
            // 发送数据包
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, packet);
            
            LOGGER.info("[版本检查] 已发送服务端版本 {} 给玩家 {}", serverVersion, player.getName().getString());
        } catch (Exception e) {
            LOGGER.error("[版本检查] 发送版本失败: {}", e.getMessage());
        }
    }
    
    /**
     * 获取服务端模组版本
     */
    private static String getServerVersion() {
        try {
            var modContainer = net.neoforged.fml.ModList.get().getModContainerById("hyserver");
            if (modContainer.isPresent()) {
                return modContainer.get().getModInfo().getVersion().toString();
            }
        } catch (Exception e) {
            LOGGER.error("[版本检查] 获取服务端版本失败: {}", e.getMessage());
        }
        return "未知";
    }
}
