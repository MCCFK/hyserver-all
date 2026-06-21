 package mccfk.hy.hyserver.java.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;

/**
 * 网易云音乐导入助手（Swing非游戏内GUI）
 */
public class MusicImportScreen {
    
    private int currentStep = 0; // 当前步骤：0=登录, 1=搜索, 2=选择歌曲, 3=复制链接
    private JTextField urlInputField;
    private JComboBox<String> outputTypeCombo;
    private JTextField resultField; // 结果显示文本框（可选择复制）
    private String parsedId = null;
    private boolean isPlaylist = false;
    private String selectedOutputType = "指令"; // 默认选择指令
    private JLabel errorLabel;
    private JFrame mainFrame; // 保存主窗口引用
    private JCheckBox disableAutoPopupCheckbox; // 不再弹出复选框
    
    /**
     * 显示音乐导入助手窗口
     */
    public static void showMusicImportWindow() {
        SwingUtilities.invokeLater(() -> {
            try {
                new MusicImportScreen().createAndShowWindow();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    /**
     * 创建并显示窗口
     */
    private void createAndShowWindow() {
        // 打开浏览器
        openNeteaseMusic();
        
        // 创建主窗口
        mainFrame = new JFrame("网易云音乐导入助手");
        mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        mainFrame.setSize(600, 450);
        mainFrame.setLocationRelativeTo(null); // 居中显示
        mainFrame.setResizable(false);
        
        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // 标题
        JLabel titleLabel = new JLabel("网易云音乐导入助手", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLabel.setForeground(new Color(255, 170, 0));
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        
        // 内容面板
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        
        // 初始化第一步界面
        initStepPanel(contentPanel);
        
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        
        mainFrame.add(mainPanel);
        mainFrame.setVisible(true);
        
        // 保持在最上层
        mainFrame.setAlwaysOnTop(true);
    }
    
    /**
     * 初始化步骤引导面板
     */
    private void initStepPanel(JPanel contentPanel) {
        contentPanel.removeAll();
        
        // 步骤标题
        JLabel stepTitle = new JLabel("步骤 " + (currentStep + 1) + "/4", SwingConstants.CENTER);
        stepTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        stepTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(stepTitle);
        contentPanel.add(Box.createVerticalStrut(10));
        
        // 步骤内容
        JTextArea stepText = new JTextArea();
        stepText.setEditable(false);
        stepText.setFont(new Font("SansSerif", Font.PLAIN, 13));
        stepText.setLineWrap(true);
        stepText.setWrapStyleWord(true);
        stepText.setBackground(null);
        stepText.setAlignmentX(Component.CENTER_ALIGNMENT);
        stepText.setMaximumSize(new Dimension(550, 150));
        
        switch (currentStep) {
            case 0: // 登录提示
                stepText.setText("请先在浏览器中\n登录网易云音乐账号");
                break;
            case 1: // 搜索提示
                stepText.setText("在浏览器右上角搜索框\n输入歌名并回车");
                break;
            case 2: // 选择歌曲提示
                stepText.setText("选择你要的歌曲\n点击歌名（一般是蓝色的）");
                break;
            case 3: // URL输入界面
                initUrlInputPanel(contentPanel);
                return; // URL输入界面有特殊布局，直接返回
        }
        
        contentPanel.add(stepText);
        contentPanel.add(Box.createVerticalStrut(20));
        
        // 下一步按钮
        JButton nextButton = new JButton("下一步");
        nextButton.setFont(new Font("SansSerif", Font.PLAIN, 14));
        nextButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        nextButton.setMaximumSize(new Dimension(150, 35));
        nextButton.addActionListener(e -> {
            currentStep++;
            if (currentStep >= 3) {
                // 进入URL输入界面
                initStepPanel((JPanel) contentPanel.getParent().getComponent(1));
            } else {
                initStepPanel(contentPanel);
            }
        });
        
        contentPanel.add(nextButton);
        contentPanel.revalidate();
        contentPanel.repaint();
    }
    
    /**
     * 初始化URL输入面板
     */
    private void initUrlInputPanel(JPanel contentPanel) {
        contentPanel.removeAll();
        
        // 步骤标题
        JLabel stepTitle = new JLabel("步骤 4/4", SwingConstants.CENTER);
        stepTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        stepTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(stepTitle);
        contentPanel.add(Box.createVerticalStrut(10));
        
        // 说明文字
        JTextArea instructionText = new JTextArea(
            "如果浏览器网址栏出现这样的URL：\n" +
            "https://music.163.com/#/song?id=0000000...\n" +
            "请复制到下面的输入框并选择导出格式"
        );
        instructionText.setEditable(false);
        instructionText.setFont(new Font("SansSerif", Font.PLAIN, 12));
        instructionText.setLineWrap(true);
        instructionText.setWrapStyleWord(true);
        instructionText.setBackground(null);
        instructionText.setAlignmentX(Component.CENTER_ALIGNMENT);
        instructionText.setMaximumSize(new Dimension(550, 80));
        contentPanel.add(instructionText);
        contentPanel.add(Box.createVerticalStrut(10));
        
        // URL输入框
        JPanel urlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        urlInputField = new JTextField(30);
        urlInputField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        urlInputField.setMaximumSize(new Dimension(500, 30));
        urlPanel.add(urlInputField);
        contentPanel.add(urlPanel);
        contentPanel.add(Box.createVerticalStrut(5));
        
        // 错误提示标签
        errorLabel = new JLabel("", SwingConstants.CENTER);
        errorLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        errorLabel.setForeground(Color.RED);
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(errorLabel);
        contentPanel.add(Box.createVerticalStrut(5));
        
        // 输出类型选择
        JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        String[] types = {"指令", "ID"};
        outputTypeCombo = new JComboBox<>(types);
        outputTypeCombo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        outputTypeCombo.addActionListener(e -> {
            selectedOutputType = (String) outputTypeCombo.getSelectedItem();
            parseAndRefresh();
        });
        typePanel.add(new JLabel("输出类型："));
        typePanel.add(outputTypeCombo);
        contentPanel.add(typePanel);
        contentPanel.add(Box.createVerticalStrut(10));
        
        // 结果显示框（可复制）
        JPanel resultPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        resultField = new JTextField(30);
        resultField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        resultField.setEditable(false); // 不可编辑，但可选择复制
        resultField.setMaximumSize(new Dimension(500, 30));
        resultField.setToolTipText("选中文本后按 Ctrl+C 复制");
        resultPanel.add(resultField);
        contentPanel.add(resultPanel);
        contentPanel.add(Box.createVerticalStrut(10));
        
        // 不再弹出复选框
        JPanel checkboxPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        disableAutoPopupCheckbox = new JCheckBox("不再自动弹出（请谨慎选择）");
        disableAutoPopupCheckbox.setFont(new Font("SansSerif", Font.PLAIN, 12));
        disableAutoPopupCheckbox.setForeground(Color.RED);
        disableAutoPopupCheckbox.setToolTipText("勾选后，除指令外将不再自动打开此帮助界面");
        // 加载当前配置
        disableAutoPopupCheckbox.setSelected(mccfk.hy.hyserver.java.config.ClientConfigManager.isMusicHelperDisabled());
        checkboxPanel.add(disableAutoPopupCheckbox);
        contentPanel.add(checkboxPanel);
        contentPanel.add(Box.createVerticalStrut(5));
        
        // 添加输入监听器，实时解析
        urlInputField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                parseAndRefresh();
            }
            
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                parseAndRefresh();
            }
            
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                parseAndRefresh();
            }
        });
        
        // 返回游戏窗口按钮
        JButton backButton = new JButton("返回游戏窗口");
        backButton.setFont(new Font("SansSerif", Font.PLAIN, 14));
        backButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        backButton.setMaximumSize(new Dimension(150, 35));
        backButton.addActionListener(e -> {
            // 保存配置
            boolean shouldDisable = disableAutoPopupCheckbox.isSelected();
            mccfk.hy.hyserver.java.config.ClientConfigManager.setMusicHelperDisabled(shouldDisable);
            
            if (shouldDisable) {
                System.out.println("[MusicHelper] ⚠ 已禁用音乐帮助助手自动弹出");
                
                // 显示提示窗口
                showDisableConfirmDialog();
            }
            
            if (mainFrame != null) {
                mainFrame.dispose(); // 正确关闭窗口
            }
        });
        
        contentPanel.add(backButton);
        contentPanel.revalidate();
        contentPanel.repaint();
    }
    
    /**
     * 打开网易云音乐官网
     */
    private void openNeteaseMusic() {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI("https://music.163.com/"));
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                "无法自动打开浏览器，请手动访问: https://music.163.com/",
                "提示",
                JOptionPane.WARNING_MESSAGE);
        }
    }
    
    /**
     * 解析URL并刷新显示
     */
    private void parseAndRefresh() {
        String url = urlInputField.getText();
        
        if (url == null || url.isEmpty()) {
            parsedId = null;
            errorLabel.setText("");
            resultField.setText("");
            resultField.setToolTipText("解析结果将显示在这里...");
            return;
        }
        
        // 正则匹配 id= 后面的数字
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("id=(\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(url);
        
        if (matcher.find()) {
            parsedId = matcher.group(1);
            isPlaylist = url.contains("/playlist");
            errorLabel.setText("");
            
            // 更新结果框显示
            String result;
            if ("指令".equals(selectedOutputType)) {
                result = isPlaylist ? "/netmusic get163 " + parsedId : "/netmusic get163cd " + parsedId;
            } else {
                result = parsedId;
            }
            resultField.setText(result);
        } else {
            parsedId = null;
            errorLabel.setText("格式错误：未找到有效的ID");
            resultField.setText("");
        }
    }
    
    /**
     * 显示禁用确认提示窗口
     */
    private void showDisableConfirmDialog() {
        JDialog dialog = new JDialog(mainFrame, "提示", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(450, 200);
        dialog.setLocationRelativeTo(mainFrame);
        dialog.setAlwaysOnTop(true); // 始终置顶
        dialog.setResizable(false);
        
        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // 标题
        JLabel titleLabel = new JLabel("音乐帮助助手已禁用", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setForeground(new Color(255, 0, 0));
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        
        // 内容
        JTextArea contentText = new JTextArea(
            "如需重新打开，请使用指令或访问以下路径：\n\n" +
            "配置文件位置：\n" +
            "hyclient/client/music_helper_config.json\n\n" +
            "将 \"music_helper_disabled\" 改为 false 或使用指令打开界面"
        );
        contentText.setEditable(false);
        contentText.setFont(new Font("SansSerif", Font.PLAIN, 12));
        contentText.setLineWrap(true);
        contentText.setWrapStyleWord(true);
        contentText.setBackground(null);
        mainPanel.add(contentText, BorderLayout.CENTER);
        
        // 确定按钮
        JButton okButton = new JButton("确定");
        okButton.setFont(new Font("SansSerif", Font.PLAIN, 14));
        okButton.addActionListener(e -> dialog.dispose());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(okButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(mainPanel);
        dialog.setVisible(true);
        
        // 5秒后自动关闭
        Timer timer = new Timer(5000, e -> {
            if (dialog.isVisible()) {
                dialog.dispose();
            }
        });
        timer.setRepeats(false);
        timer.start();
    }
}
