package mccfk.hy.hyserver.java.chat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 聊天格式化工具
 */
public class ChatFormatter {
    
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\{([^}]+)\\}");
    
    /**
     * 将模板字符串解析为 Component，支持占位符替换
     * 参考 AeroChat 的 renderTemplate 方法
     */
    public static MutableComponent formatTemplate(
            String template,
            String dimension,
            String title,
            String guild,
            String playerName,
            String message,
            String playerColor
    ) {
        if (template == null) {
            template = "[{dimension}] {title} {guild} {player}{say}{message}";
        }
        
        // 替换占位符
        template = template.replace("{dimension}", dimension != null ? dimension : "")
                          .replace("{title}", title != null ? title : "")
                          .replace("{guild}", guild != null ? guild : "")
                          .replace("{player}", playerName != null ? playerName : "")
                          .replace("{message}", message != null ? message : "")
                          .replace("{say}", "说：");
        
        // 解析为 Component（保留颜色代码）
        return parseColorCodes(template);
    }
    
    /**
     * 解析颜色代码字符串为 Component
     * 支持 &0-&f 颜色代码、&k-&o 格式代码、&#RRGGBB 十六进制颜色
     * 支持 &<#颜色1;#颜色2;...> 渐变效果
     * 可用于聊天、铁砧、指令、材质包等所有场景
     */
    public static MutableComponent parseColorCodes(String text) {
        MutableComponent result = Component.empty();
        TextColor currentColor = null;
        boolean bold = false, italic = false, underline = false, strikethrough = false, obfuscated = false;
        
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            
            // 解析到 &
            if (c == '&') {
                // 查看后一个字符
                if (i + 1 >= text.length()) {
                    // 后面没有字符，跳过这个 & 的解析，继续后面的
                    MutableComponent part = Component.literal("&");
                    Style style = Style.EMPTY
                            .withColor(currentColor)
                            .withBold(bold)
                            .withItalic(italic)
                            .withUnderlined(underline)
                            .withStrikethrough(strikethrough)
                            .withObfuscated(obfuscated);
                    part.withStyle(style);
                    result.append(part);
                    i++;
                    continue;
                }
                
                char nextChar = text.charAt(i + 1);
                
                // 检查是否为有效颜色字符、状态字符、# 或 <
                boolean isValidColorCode = (nextChar >= '0' && nextChar <= '9') || 
                                          (nextChar >= 'a' && nextChar <= 'f') || 
                                          (nextChar >= 'A' && nextChar <= 'F');
                boolean isValidFormatCode = (nextChar == 'k' || nextChar == 'K' ||
                                            nextChar == 'l' || nextChar == 'L' ||
                                            nextChar == 'm' || nextChar == 'M' ||
                                            nextChar == 'n' || nextChar == 'N' ||
                                            nextChar == 'o' || nextChar == 'O' ||
                                            nextChar == 'r' || nextChar == 'R');
                boolean isHashOrBracket = (nextChar == '#' || nextChar == '<');
                
                if (!isValidColorCode && !isValidFormatCode && !isHashOrBracket) {
                    // 否 -> 跳过这个 & 的解析，继续后面的
                    MutableComponent part = Component.literal("&");
                    Style style = Style.EMPTY
                            .withColor(currentColor)
                            .withBold(bold)
                            .withItalic(italic)
                            .withUnderlined(underline)
                            .withStrikethrough(strikethrough)
                            .withObfuscated(obfuscated);
                    part.withStyle(style);
                    result.append(part);
                    i++;
                    continue;
                }
                
                // 是 -> 继续判断具体类型
                if (isValidColorCode) {
                    // 颜色组 -> 只替换颜色，不影响状态
                    switch (Character.toLowerCase(nextChar)) {
                        case '0' -> currentColor = TextColor.fromLegacyFormat(ChatFormatting.BLACK);
                        case '1' -> currentColor = TextColor.fromLegacyFormat(ChatFormatting.DARK_BLUE);
                        case '2' -> currentColor = TextColor.fromLegacyFormat(ChatFormatting.DARK_GREEN);
                        case '3' -> currentColor = TextColor.fromLegacyFormat(ChatFormatting.DARK_AQUA);
                        case '4' -> currentColor = TextColor.fromLegacyFormat(ChatFormatting.DARK_RED);
                        case '5' -> currentColor = TextColor.fromLegacyFormat(ChatFormatting.DARK_PURPLE);
                        case '6' -> currentColor = TextColor.fromLegacyFormat(ChatFormatting.GOLD);
                        case '7' -> currentColor = TextColor.fromLegacyFormat(ChatFormatting.GRAY);
                        case '8' -> currentColor = TextColor.fromLegacyFormat(ChatFormatting.DARK_GRAY);
                        case '9' -> currentColor = TextColor.fromLegacyFormat(ChatFormatting.BLUE);
                        case 'a' -> currentColor = TextColor.fromLegacyFormat(ChatFormatting.GREEN);
                        case 'b' -> currentColor = TextColor.fromLegacyFormat(ChatFormatting.AQUA);
                        case 'c' -> currentColor = TextColor.fromLegacyFormat(ChatFormatting.RED);
                        case 'd' -> currentColor = TextColor.fromLegacyFormat(ChatFormatting.LIGHT_PURPLE);
                        case 'e' -> currentColor = TextColor.fromLegacyFormat(ChatFormatting.YELLOW);
                        case 'f' -> currentColor = TextColor.fromLegacyFormat(ChatFormatting.WHITE);
                    }
                    i += 2;
                    continue;
                } else if (isValidFormatCode) {
                    // 状态组或重置组
                    switch (Character.toLowerCase(nextChar)) {
                        case 'r' -> {
                            // 重置组 -> 重置所有（颜色+状态）
                            currentColor = null;
                            bold = false;
                            italic = false;
                            underline = false;
                            strikethrough = false;
                            obfuscated = false;
                        }
                        case 'l' -> bold = true;           // 状态组 -> 粗体
                        case 'o' -> italic = true;         // 状态组 -> 斜体
                        case 'n' -> underline = true;      // 状态组 -> 下划线
                        case 'm' -> strikethrough = true;  // 状态组 -> 删除线
                        case 'k' -> obfuscated = true;     // 状态组 -> 混淆
                    }
                    i += 2;
                    continue;
                } else if (nextChar == '#') {
                    // 解析为 # -> 检测后6位是否为有效 #XXXXXX 颜色格式
                    if (i + 7 < text.length()) {
                        String hex = text.substring(i + 2, i + 8);
                        try {
                            int rgb = Integer.parseInt(hex, 16);
                            // 是 -> 应用效果...
                            currentColor = TextColor.fromRgb(rgb);
                            i += 8; // 跳过 &#RRGGBB
                            continue;
                        } catch (NumberFormatException e) {
                            // 否 -> 跳过这个 & 的解析，继续后面的
                            MutableComponent part = Component.literal("&");
                            Style style = Style.EMPTY
                                    .withColor(currentColor)
                                    .withBold(bold)
                                    .withItalic(italic)
                                    .withUnderlined(underline)
                                    .withStrikethrough(strikethrough)
                                    .withObfuscated(obfuscated);
                            part.withStyle(style);
                            result.append(part);
                            i++;
                            continue;
                        }
                    } else {
                        // 长度不足，跳过这个 & 的解析，继续后面的
                        MutableComponent part = Component.literal("&");
                        Style style = Style.EMPTY
                                .withColor(currentColor)
                                .withBold(bold)
                                .withItalic(italic)
                                .withUnderlined(underline)
                                .withStrikethrough(strikethrough)
                                .withObfuscated(obfuscated);
                        part.withStyle(style);
                        result.append(part);
                        i++;
                        continue;
                    }
                } else if (nextChar == '<') {
                    // 解析为 < -> 检测后面是否为有效结构 #XXXXXX;#XXXXXX...
                    // （<>闭合且内部为单个或多个#XXXXXX，每两个#XXXXXX之间需要有一个;分割）
                    int closeBracket = text.indexOf('>', i + 2);
                    if (closeBracket != -1) {
                        String gradientDef = text.substring(i + 2, closeBracket);
                        String[] colorHexes = gradientDef.split(";");
                        
                        // 验证每个颜色是否为有效的 #XXXXXX 格式
                        java.util.List<TextColor> gradientColors = new java.util.ArrayList<>();
                        boolean allValid = true;
                        
                        for (String hex : colorHexes) {
                            hex = hex.trim();
                            if (hex.startsWith("#") && hex.length() == 7) {
                                try {
                                    int rgb = Integer.parseInt(hex.substring(1), 16);
                                    gradientColors.add(TextColor.fromRgb(rgb));
                                } catch (NumberFormatException e) {
                                    allValid = false;
                                    break;
                                }
                            } else {
                                allValid = false;
                                break;
                            }
                        }
                        
                        if (allValid && !gradientColors.isEmpty()) {
                            // 是 -> 解析渐变并应用...
                            int gradientTextStart = closeBracket + 1;
                            
                            // 查找渐变文本的结束位置
                            int gradientEnd = text.length();
                            for (int k = gradientTextStart; k < text.length(); k++) {
                                if (text.charAt(k) == '&' && k + 1 < text.length()) {
                                    char checkChar = text.charAt(k + 1);
                                    boolean isCheckValid = (checkChar >= '0' && checkChar <= '9') || 
                                                          (checkChar >= 'a' && checkChar <= 'f') || 
                                                          (checkChar >= 'A' && checkChar <= 'F') ||
                                                          checkChar == '#' || checkChar == '<' ||
                                                          checkChar == 'k' || checkChar == 'K' ||
                                                          checkChar == 'l' || checkChar == 'L' ||
                                                          checkChar == 'm' || checkChar == 'M' ||
                                                          checkChar == 'n' || checkChar == 'N' ||
                                                          checkChar == 'o' || checkChar == 'O' ||
                                                          checkChar == 'r' || checkChar == 'R';
                                    if (isCheckValid) {
                                        gradientEnd = k;
                                        break;
                                    }
                                }
                            }
                            
                            String textToGradient = text.substring(gradientTextStart, gradientEnd);
                            
                            // 为每个字符分配渐变色
                            for (int j = 0; j < textToGradient.length(); j++) {
                                char ch = textToGradient.charAt(j);
                                
                                TextColor charColor;
                                if (gradientColors.size() == 1) {
                                    charColor = gradientColors.get(0);
                                } else {
                                    float ratio = textToGradient.length() > 1 
                                        ? (float) j / (textToGradient.length() - 1) 
                                        : 0.0f;
                                    
                                    float segment = ratio * (gradientColors.size() - 1);
                                    int segmentIndex = (int) segment;
                                    float segmentRatio = segment - segmentIndex;
                                    
                                    TextColor startColor = gradientColors.get(segmentIndex);
                                    TextColor endColor = gradientColors.get(Math.min(segmentIndex + 1, gradientColors.size() - 1));
                                    
                                    int startRgb = startColor != null ? startColor.getValue() : 0;
                                    int endRgb = endColor != null ? endColor.getValue() : 0;
                                    
                                    int startR = (startRgb >> 16) & 0xFF;
                                    int startG = (startRgb >> 8) & 0xFF;
                                    int startB = startRgb & 0xFF;
                                    
                                    int endR = (endRgb >> 16) & 0xFF;
                                    int endG = (endRgb >> 8) & 0xFF;
                                    int endB = endRgb & 0xFF;
                                    
                                    int r = (int) (startR + (endR - startR) * segmentRatio);
                                    int g = (int) (startG + (endG - startG) * segmentRatio);
                                    int b = (int) (startB + (endB - startB) * segmentRatio);
                                    
                                    charColor = TextColor.fromRgb((r << 16) | (g << 8) | b);
                                }
                                
                                MutableComponent charComponent = Component.literal(String.valueOf(ch));
                                Style charStyle = Style.EMPTY
                                        .withColor(charColor)
                                        .withBold(bold)
                                        .withItalic(italic)
                                        .withUnderlined(underline)
                                        .withStrikethrough(strikethrough)
                                        .withObfuscated(obfuscated);
                                charComponent.withStyle(charStyle);
                                result.append(charComponent);
                            }
                            
                            i = gradientEnd;
                            continue;
                        } else {
                            // 否 -> 跳过这个 & 的解析，继续后面的
                            MutableComponent part = Component.literal("&");
                            Style style = Style.EMPTY
                                    .withColor(currentColor)
                                    .withBold(bold)
                                    .withItalic(italic)
                                    .withUnderlined(underline)
                                    .withStrikethrough(strikethrough)
                                    .withObfuscated(obfuscated);
                            part.withStyle(style);
                            result.append(part);
                            i++;
                            continue;
                        }
                    } else {
                        // 没有闭合的 >，跳过这个 & 的解析，继续后面的
                        MutableComponent part = Component.literal("&");
                        Style style = Style.EMPTY
                                .withColor(currentColor)
                                .withBold(bold)
                                .withItalic(italic)
                                .withUnderlined(underline)
                                .withStrikethrough(strikethrough)
                                .withObfuscated(obfuscated);
                        part.withStyle(style);
                        result.append(part);
                        i++;
                        continue;
                    }
                }
            }
            
            // 普通字符
            StringBuilder currentText = new StringBuilder();
            while (i < text.length()) {
                char ch = text.charAt(i);
                if (ch == '&' && i + 1 < text.length()) {
                    char nextCh = text.charAt(i + 1);
                    boolean isValidCode = (nextCh >= '0' && nextCh <= '9') || 
                                         (nextCh >= 'a' && nextCh <= 'f') || 
                                         (nextCh >= 'A' && nextCh <= 'F') ||
                                         nextCh == '#' || nextCh == '<' ||
                                         nextCh == 'k' || nextCh == 'K' ||
                                         nextCh == 'l' || nextCh == 'L' ||
                                         nextCh == 'm' || nextCh == 'M' ||
                                         nextCh == 'n' || nextCh == 'N' ||
                                         nextCh == 'o' || nextCh == 'O' ||
                                         nextCh == 'r' || nextCh == 'R';
                    if (isValidCode) break;
                }
                currentText.append(ch);
                i++;
            }
            
            if (!currentText.isEmpty()) {
                MutableComponent part = Component.literal(currentText.toString());
                Style style = Style.EMPTY
                        .withColor(currentColor)
                        .withBold(bold)
                        .withItalic(italic)
                        .withUnderlined(underline)
                        .withStrikethrough(strikethrough)
                        .withObfuscated(obfuscated);
                part.withStyle(style);
                result.append(part);
            }
        }
        
        return result;
    }
    
    /**
     * 将 Component 附加点击和悬停事件
     * 参考 AeroChat 保留原版交互功能的逻辑
     */
    public static MutableComponent applyStyle(MutableComponent component, Style originalStyle) {
        if (originalStyle != null) {
            component.withStyle(originalStyle);
        }
        return component;
    }
}
