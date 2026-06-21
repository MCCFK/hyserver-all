package mccfk.hy.hyserver.java.announce;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 客户端独立公告窗口（Swing JFrame，始终置顶）
 */
public class AnnouncementWindow extends JFrame {

    private final Map<Integer, String> pages = new LinkedHashMap<>();
    private final java.util.List<Integer> pageNumbers;
    private int currentPageIdx;
    private final JTextPane textPane;
    private final JPanel navPanel;

    public AnnouncementWindow() {
        setTitle("服务器公告");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setAlwaysOnTop(true);
        setSize(500, 400);
        setLocationRelativeTo(null);

        textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setContentType("text/html");
        // 设置 HTMLEditorKit 和自定义 CSS
        HTMLEditorKit kit = new HTMLEditorKit();
        StyleSheet css = kit.getStyleSheet();
        css.addRule("body { font-family: 'SansSerif', sans-serif; font-size: 14px; background-color: #1E1E1E; color: #FFFFFF; text-align: center; padding: 10px; }");
        css.addRule("b { font-weight: bold; }");
        css.addRule("i { font-style: italic; }");
        textPane.setEditorKit(kit);
        textPane.setBackground(new Color(30, 30, 30));
        textPane.setCaretColor(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        navPanel = new JPanel();
        navPanel.setBackground(new Color(40, 40, 40));
        navPanel.setBorder(new EmptyBorder(5, 10, 5, 10));
        navPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 8, 5));
        add(navPanel, BorderLayout.SOUTH);

        pageNumbers = new java.util.ArrayList<>();
    }

    /**
     * 静态入口：解析payload并显示窗口
     */
    public static void show(String payload) {
        SwingUtilities.invokeLater(() -> {
            AnnouncementWindow w = new AnnouncementWindow();
            w.parsePayload(payload);
            if (w.pages.isEmpty()) return;
            w.pageNumbers.addAll(w.pages.keySet());
            w.currentPageIdx = 0;
            w.refresh();
            w.setVisible(true);
        });
    }

    private void parsePayload(String payload) {
        // payload: "pageNum1|content1||pageNum2|content2||..."
        if (payload == null || payload.isEmpty()) return;
        String[] entries = payload.split("\\|\\|");
        for (String entry : entries) {
            int sep = entry.indexOf('|');
            if (sep <= 0) continue;
            try {
                int num = Integer.parseInt(entry.substring(0, sep));
                String content = entry.substring(sep + 1);
                pages.put(num, content);
            } catch (NumberFormatException ignored) {}
        }
    }

    private void refresh() {
        if (pages.isEmpty() || currentPageIdx < 0 || currentPageIdx >= pageNumbers.size()) return;

        int pageNum = pageNumbers.get(currentPageIdx);
        String rawContent = pages.get(pageNum);
        String html = convertToHtml(rawContent);
        textPane.setText(html);

        // 更新导航
        navPanel.removeAll();
        buildNavigation();
        navPanel.revalidate();
        navPanel.repaint();
    }

    private void buildNavigation() {
        JButton closeBtn = new JButton("关闭");
        closeBtn.setFocusPainted(false);
        closeBtn.addActionListener(e -> dispose());
        navPanel.add(closeBtn);

        int total = pageNumbers.size();
        int cur = currentPageIdx;

        for (int i = 0; i < total; i++) {
            if (i > 0 && i < cur - 1 && cur < total - 1) {
                // 中间省略
            }
            // 判断是否需要显示"..."
            if (i == 1 && cur > 2) {
                JLabel ellipsis = new JLabel("...");
                ellipsis.setForeground(Color.LIGHT_GRAY);
                navPanel.add(ellipsis);
                continue;
            }
            if (i == total - 2 && cur < total - 3) {
                JLabel ellipsis = new JLabel("...");
                ellipsis.setForeground(Color.LIGHT_GRAY);
                navPanel.add(ellipsis);
                continue;
            }
            if (i > 0 && i < cur - 1 && cur < total - 1) {
                continue;
            }
            if (i > cur + 1 && i < total - 1 && cur > 0) {
                continue;
            }

            if (i > 0 && i < total - 1) {
                // 非首尾的中间页，判断是否显示省略号
                boolean skip = false;
                if (cur > 2 && i < cur - 1 && i > 0) skip = true;
                if (cur < total - 3 && i > cur + 1 && i < total - 1) skip = true;
                if (skip) {
                    if (i == 1 || i == total - 2) {
                        // 省略号已在上面处理
                    }
                    continue;
                }
            }

            int pageNum = pageNumbers.get(i);
            if (i == cur) {
                // 当前页：显示页码文本（不可点击）
                JButton curBtn = new JButton(String.valueOf(pageNum));
                curBtn.setFocusPainted(false);
                curBtn.setEnabled(false);
                curBtn.setBackground(new Color(60, 60, 60));
                curBtn.setForeground(Color.WHITE);
                navPanel.add(curBtn);
            } else {
                JButton pageBtn = new JButton(String.valueOf(pageNum));
                pageBtn.setFocusPainted(false);
                final int idx = i;
                pageBtn.addActionListener(e -> {
                    currentPageIdx = idx;
                    refresh();
                });
                navPanel.add(pageBtn);
            }

            // 添加 > 分隔符（当前页后）
            if (i == cur && cur < total - 1) {
                JLabel sep = new JLabel(">");
                sep.setForeground(Color.LIGHT_GRAY);
                navPanel.add(sep);
            }
            // 添加 < 分隔符（当前页前）
            if (i == cur - 1) {
                JLabel sep = new JLabel("<");
                sep.setForeground(Color.LIGHT_GRAY);
                navPanel.add(sep);
            }
        }
    }

    /**
     * 将 & 颜色码转为 HTML
     */
    private String convertToHtml(String text) {
        StringBuilder html = new StringBuilder("<html><body>");
        boolean bold = false, italic = false, underline = false, strike = false;
        boolean hasOpenSpan = false;
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '&' && i + 1 < text.length()) {
                char nc = text.charAt(i + 1);
                String tag = null;
                int skipLen = 2;
                switch (nc) {
                    case '0': tag = "color:#000000"; break;
                    case '1': tag = "color:#0000AA"; break;
                    case '2': tag = "color:#00AA00"; break;
                    case '3': tag = "color:#00AAAA"; break;
                    case '4': tag = "color:#AA0000"; break;
                    case '5': tag = "color:#AA00AA"; break;
                    case '6': tag = "color:#FFAA00"; break;
                    case '7': tag = "color:#AAAAAA"; break;
                    case '8': tag = "color:#555555"; break;
                    case '9': tag = "color:#5555FF"; break;
                    case 'a': case 'A': tag = "color:#55FF55"; break;
                    case 'b': case 'B': tag = "color:#55FFFF"; break;
                    case 'c': case 'C': tag = "color:#FF5555"; break;
                    case 'd': case 'D': tag = "color:#FF55FF"; break;
                    case 'e': case 'E': tag = "color:#FFFF55"; break;
                    case 'f': case 'F': tag = "color:#FFFFFF"; break;
                    case 'l': case 'L': bold = true; i += 2; continue;
                    case 'm': case 'M': strike = true; i += 2; continue;
                    case 'n': case 'N': underline = true; i += 2; continue;
                    case 'o': case 'O': italic = true; i += 2; continue;
                    case 'k': case 'K': i += 2; continue;
                    case '#':
                        if (i + 8 <= text.length()) {
                            String hex = text.substring(i + 2, i + 8);
                            try { Integer.parseInt(hex, 16); tag = "color:#" + hex; skipLen = 8; }
                            catch (NumberFormatException e) { i++; continue; }
                        } else { i++; continue; }
                        break;
                    case 'r': case 'R':
                        if (hasOpenSpan) { html.append("</span>"); hasOpenSpan = false; }
                        bold = italic = underline = strike = false;
                        i += 2; continue;
                    default: i++; continue;
                }
                if (tag != null) {
                    if (hasOpenSpan) { html.append("</span>"); }
                    StringBuilder style = new StringBuilder(tag);
                    if (bold) style.append(";font-weight:bold");
                    if (italic) style.append(";font-style:italic");
                    if (underline) style.append(";text-decoration:underline");
                    if (strike) style.append(";text-decoration:line-through");
                    html.append("<span style='").append(style).append("'>");
                    hasOpenSpan = true;
                }
                i += skipLen;
                continue;
            }
            if (c == '\n') {
                html.append("<br>");
                i++;
                continue;
            }
            switch (c) {
                case '<': html.append("&lt;"); break;
                case '>': html.append("&gt;"); break;
                case '&': html.append("&amp;"); break;
                default: html.append(c);
            }
            i++;
        }
        if (hasOpenSpan) { html.append("</span>"); }
        html.append("</body></html>");
        return html.toString();
    }
}
