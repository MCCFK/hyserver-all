package mccfk.hy.hyserver.java.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 客户端事件实时监控窗口（类似调试日志）
 * 捕获每个tick的所有事件：界面、物品、数值、网络包等
 */
public class GameInfoMonitorWindow {
    
    private JFrame mainFrame;
    private JTextArea infoTextArea;
    private JScrollPane scrollPane;
    private AtomicBoolean isPaused = new AtomicBoolean(false);
    private AtomicBoolean isAutoScroll = new AtomicBoolean(true);
    private Thread updateThread;
    private ConcurrentLinkedQueue<String> eventQueue = new ConcurrentLinkedQueue<>();
    private List<JCheckBox> checkBoxes = new ArrayList<>();
    
    // 过滤选项
    private boolean showGuiEvents = true;      // GUI事件
    private boolean showItemEvents = true;     // 物品事件
    private boolean showStatEvents = true;     // 数值变化
    private boolean showNetworkEvents = true;  // 网络包
    private boolean showInputEvents = true;    // 输入事件
    private boolean showEntityEvents = true;   // 实体事件
    private boolean showWorldEvents = true;    // 世界事件
    
    // 上次状态（用于检测变化）
    private double lastHealth = -1;
    private int lastFood = -1;
    private int lastExpLevel = -1;
    private float lastExpProgress = -1;
    private String lastHeldItem = "";
    private net.minecraft.client.gui.screens.Screen lastScreen = null;
    
    /**
     * 显示游戏信息监控窗口
     */
    public static void showWindow() {
        SwingUtilities.invokeLater(() -> {
            try {
                new GameInfoMonitorWindow().createAndShowWindow();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    /**
     * 创建并显示窗口
     */
    private void createAndShowWindow() {
        // 创建主窗口
        mainFrame = new JFrame("客户端事件实时监控");
        mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        mainFrame.setSize(900, 650);
        mainFrame.setLocationRelativeTo(null);
        
        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 标题
        JLabel titleLabel = new JLabel("客户端事件实时监控 (调试日志)", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        
        // 信息显示区域（类似CMD）
        infoTextArea = new JTextArea();
        // 使用支持中文的等宽字体
        Font chineseFont = new Font("Monospaced", Font.PLAIN, 13);
        infoTextArea.setFont(chineseFont);
        infoTextArea.setEditable(false); // 不可编辑，但可选择复制
        infoTextArea.setBackground(Color.BLACK);
        infoTextArea.setForeground(Color.GREEN);
        infoTextArea.setLineWrap(false);
        
        scrollPane = new JScrollPane(infoTextArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        // 控制面板
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBorder(BorderFactory.createTitledBorder("事件过滤"));
        
        // 复选框面板
        JPanel checkBoxPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        
        JCheckBox guiCheck = new JCheckBox("GUI事件", true);
        JCheckBox itemCheck = new JCheckBox("物品事件", true);
        JCheckBox statCheck = new JCheckBox("数值变化", true);
        JCheckBox networkCheck = new JCheckBox("网络包", true);
        JCheckBox inputCheck = new JCheckBox("输入事件", true);
        JCheckBox entityCheck = new JCheckBox("实体事件", true);
        JCheckBox worldCheck = new JCheckBox("世界事件", true);
        
        guiCheck.addActionListener(e -> showGuiEvents = guiCheck.isSelected());
        itemCheck.addActionListener(e -> showItemEvents = itemCheck.isSelected());
        statCheck.addActionListener(e -> showStatEvents = statCheck.isSelected());
        networkCheck.addActionListener(e -> showNetworkEvents = networkCheck.isSelected());
        inputCheck.addActionListener(e -> showInputEvents = inputCheck.isSelected());
        entityCheck.addActionListener(e -> showEntityEvents = entityCheck.isSelected());
        worldCheck.addActionListener(e -> showWorldEvents = worldCheck.isSelected());
        
        checkBoxes.add(guiCheck);
        checkBoxes.add(itemCheck);
        checkBoxes.add(statCheck);
        checkBoxes.add(networkCheck);
        checkBoxes.add(inputCheck);
        checkBoxes.add(entityCheck);
        checkBoxes.add(worldCheck);
        
        checkBoxPanel.add(guiCheck);
        checkBoxPanel.add(itemCheck);
        checkBoxPanel.add(statCheck);
        checkBoxPanel.add(networkCheck);
        checkBoxPanel.add(inputCheck);
        checkBoxPanel.add(entityCheck);
        checkBoxPanel.add(worldCheck);
        
        controlPanel.add(checkBoxPanel);
        controlPanel.add(Box.createVerticalStrut(10));
        
        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        
        JButton pauseButton = new JButton("暂停/继续");
        pauseButton.addActionListener(e -> {
            isPaused.set(!isPaused.get());
            pauseButton.setText(isPaused.get() ? "继续" : "暂停");
        });
        
        JButton autoScrollButton = new JButton("自动滚动: 开");
        autoScrollButton.addActionListener(e -> {
            isAutoScroll.set(!isAutoScroll.get());
            autoScrollButton.setText(isAutoScroll.get() ? "自动滚动: 开" : "自动滚动: 关");
        });
        
        JButton copyButton = new JButton("复制全部");
        copyButton.addActionListener(e -> {
            String text = infoTextArea.getText();
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                new StringSelection(text), null);
            JOptionPane.showMessageDialog(mainFrame, "已复制到剪贴板", "提示", JOptionPane.INFORMATION_MESSAGE);
        });
        
        JButton clearButton = new JButton("清空");
        clearButton.addActionListener(e -> {
            infoTextArea.setText("");
            eventQueue.clear();
        });
        
        JButton closeButton = new JButton("关闭窗口");
        closeButton.addActionListener(e -> {
            stopUpdateThread();
            mainFrame.dispose();
        });
        
        buttonPanel.add(pauseButton);
        buttonPanel.add(autoScrollButton);
        buttonPanel.add(copyButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(closeButton);
        
        controlPanel.add(buttonPanel);
        mainPanel.add(controlPanel, BorderLayout.SOUTH);
        
        mainFrame.add(mainPanel);
        mainFrame.setVisible(true);
        
        // 始终置顶
        mainFrame.setAlwaysOnTop(true);
        
        // 启动更新线程
        startUpdateThread();
    }
    
    /**
     * 启动信息更新线程
     */
    private void startUpdateThread() {
        updateThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (!isPaused.get()) {
                        updateGameInfo();
                    }
                    Thread.sleep(50); // 每50ms检测一次（20次/秒）
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        updateThread.setDaemon(true);
        updateThread.start();
    }
    
    /**
     * 停止更新线程
     */
    private void stopUpdateThread() {
        if (updateThread != null) {
            updateThread.interrupt();
        }
    }
    
    /**
     * 更新游戏信息（检测变化并记录事件）
     */
    private void updateGameInfo() {
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc == null || mc.player == null) {
                return;
            }
            
            String timestamp = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
            
            // 1. 检测GUI变化
            if (showGuiEvents) {
                detectGuiChanges(mc, timestamp);
            }
            
            // 2. 检测物品变化
            if (showItemEvents) {
                detectItemChanges(mc, timestamp);
            }
            
            // 3. 检测数值变化
            if (showStatEvents) {
                detectStatChanges(mc, timestamp);
            }
            
            // 4. 检测网络包（需要Mixin支持，这里显示连接状态）
            if (showNetworkEvents) {
                detectNetworkEvents(mc, timestamp);
            }
            
            // 5. 检测实体事件
            if (showEntityEvents) {
                detectEntityEvents(mc, timestamp);
            }
            
            // 6. 检测世界事件
            if (showWorldEvents) {
                detectWorldEvents(mc, timestamp);
            }
            
            // 处理事件队列
            processEventQueue();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 检测GUI变化
     */
    private void detectGuiChanges(net.minecraft.client.Minecraft mc, String timestamp) {
        net.minecraft.client.gui.screens.Screen currentScreen = mc.screen;
        
        if (currentScreen != lastScreen) {
            if (lastScreen == null && currentScreen != null) {
                // 打开了新界面
                String screenName = currentScreen.getClass().getSimpleName();
                addEvent(timestamp, "[GUI]", String.format("打开界面: %s", screenName));
            } else if (lastScreen != null && currentScreen == null) {
                // 关闭了界面
                String screenName = lastScreen.getClass().getSimpleName();
                addEvent(timestamp, "[GUI]", String.format("关闭界面: %s", screenName));
            } else {
                // 切换界面
                String oldName = lastScreen.getClass().getSimpleName();
                String newName = currentScreen.getClass().getSimpleName();
                addEvent(timestamp, "[GUI]", String.format("切换界面: %s -> %s", oldName, newName));
            }
            lastScreen = currentScreen;
        }
    }
    
    /**
     * 检测物品变化
     */
    private void detectItemChanges(net.minecraft.client.Minecraft mc, String timestamp) {
        var heldItem = mc.player.getMainHandItem();
        String currentItemName = heldItem.getHoverName().getString();
        String currentItemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(heldItem.getItem()).toString();
        int currentCount = heldItem.getCount();
        
        String currentItemStr = String.format("%s (ID: %s, 数量: %d)", currentItemName, currentItemId, currentCount);
        
        if (!currentItemStr.equals(lastHeldItem)) {
            if (lastHeldItem.isEmpty()) {
                addEvent(timestamp, "[ITEM]", String.format("手持物品: %s", currentItemStr));
            } else {
                addEvent(timestamp, "[ITEM]", String.format("切换物品: %s -> %s", lastHeldItem, currentItemStr));
            }
            lastHeldItem = currentItemStr;
        }
    }
    
    /**
     * 检测数值变化
     */
    private void detectStatChanges(net.minecraft.client.Minecraft mc, String timestamp) {
        double currentHealth = mc.player.getHealth();
        int currentFood = mc.player.getFoodData().getFoodLevel();
        int currentExpLevel = mc.player.experienceLevel;
        float currentExpProgress = mc.player.experienceProgress;
        
        // 生命值变化
        if (currentHealth != lastHealth && lastHealth >= 0) {
            String changeType = currentHealth > lastHealth ? "恢复" : "受伤";
            addEvent(timestamp, "[STAT]", String.format("生命值%s: %.1f -> %.1f", changeType, lastHealth, currentHealth));
        }
        lastHealth = currentHealth;
        
        // 饥饿值变化
        if (currentFood != lastFood && lastFood >= 0) {
            String changeType = currentFood > lastFood ? "恢复" : "消耗";
            addEvent(timestamp, "[STAT]", String.format("饥饿值%s: %d -> %d", changeType, lastFood, currentFood));
        }
        lastFood = currentFood;
        
        // 经验等级变化
        if (currentExpLevel != lastExpLevel && lastExpLevel >= 0) {
            addEvent(timestamp, "[STAT]", String.format("经验等级提升: %d -> %d", lastExpLevel, currentExpLevel));
        }
        lastExpLevel = currentExpLevel;
        
        // 经验进度变化（超过阈值才记录）
        if (Math.abs(currentExpProgress - lastExpProgress) > 0.1f && lastExpProgress >= 0) {
            addEvent(timestamp, "[STAT]", String.format("经验进度: %.0f%% -> %.0f%%", lastExpProgress * 100, currentExpProgress * 100));
        }
        lastExpProgress = currentExpProgress;
    }
    
    /**
     * 检测网络事件
     */
    private void detectNetworkEvents(net.minecraft.client.Minecraft mc, String timestamp) {
        if (mc.getConnection() != null) {
            var playerInfo = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            if (playerInfo != null) {
                int latency = playerInfo.getLatency();
                if (latency > 1000) {
                    addEvent(timestamp, "[NET]", String.format("高延迟警告: %d ms", latency));
                }
            }
        }
    }
    
    /**
     * 检测实体事件
     */
    private void detectEntityEvents(net.minecraft.client.Minecraft mc, String timestamp) {
        // 检测附近实体数量变化（简化版）
        // 可以在这里添加更多实体事件检测
    }
    
    /**
     * 检测世界事件
     */
    private void detectWorldEvents(net.minecraft.client.Minecraft mc, String timestamp) {
        var blockPos = mc.player.blockPosition();
        var blockState = mc.player.level().getBlockState(blockPos);
        String blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).toString();
        
        // 检测方块变化（脚下）
        // 这里可以添加更复杂的世界事件检测
    }
    
    /**
     * 添加事件到队列
     */
    private void addEvent(String timestamp, String type, String message) {
        String event = String.format("[%s] %s %s", timestamp, type, message);
        eventQueue.offer(event);
    }
    
    /**
     * 处理事件队列并显示
     */
    private void processEventQueue() {
        StringBuilder displayText = new StringBuilder();
        
        // 从队列中取出所有事件
        String event;
        while ((event = eventQueue.poll()) != null) {
            displayText.append(event).append("\n");
        }
        
        if (displayText.length() > 0) {
            SwingUtilities.invokeLater(() -> {
                infoTextArea.append(displayText.toString());
                
                // 自动滚动到底部
                if (isAutoScroll.get()) {
                    infoTextArea.setCaretPosition(infoTextArea.getDocument().getLength());
                }
            });
        }
    }
}
