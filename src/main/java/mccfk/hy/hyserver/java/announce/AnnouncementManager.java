package mccfk.hy.hyserver.java.announce;

import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.network.PacketDistributor;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 服务端公告管理器
 * 从 hyserver/Announcement.txt 读取公告内容并分发给客户端
 */
public class AnnouncementManager {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Pattern ENTRY_PATTERN = Pattern.compile("i\\.(\\d+)\\.ui\\(\"((?:[^\"\\\\]|\\\\.)*)\"\\)");
    private static boolean initialized = false;
    private static Path announcePath;

    /**
     * 初始化公告文件
     */
    public static void init(MinecraftServer server) {
        if (initialized) return;
        announcePath = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
            .resolve("hyserver")
            .resolve("Announcement.txt");
        try {
            if (!Files.exists(announcePath)) {
                try (InputStream in = AnnouncementManager.class.getResourceAsStream("/hyserver/Announcement.txt")) {
                    if (in != null) {
                        Files.createDirectories(announcePath.getParent());
                        Files.copy(in, announcePath);
                        LOGGER.info("\n\n§a[HYSERVER.INFO]\n§a已创建默认公告文件");
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("\n\n§c[HYSERVER.ERROR]\n§c初始化公告文件失败", e);
        }
        initialized = true;
    }

    /**
     * 解析 Announcement.txt 文件
     * @return 页码 → 内容的映射
     */
    public static Map<Integer, String> parseAnnouncementFile(Path filePath) {
        Map<Integer, String> pages = new LinkedHashMap<>();
        if (!Files.exists(filePath)) {
            return pages;
        }

        try {
            String raw = Files.readString(filePath, StandardCharsets.UTF_8);
            Matcher m = ENTRY_PATTERN.matcher(raw);
            while (m.find()) {
                int pageNum = Integer.parseInt(m.group(1));
                String content = m.group(2);
                // 将真实 \n 替换为字面换行符
                content = content.replace("\\n", "\n");
                pages.put(pageNum, content);
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
        return pages;
    }

    /**
     * 从默认路径读取并发送给指定玩家
     */
    public static void sendToPlayer(MinecraftServer server, net.minecraft.server.level.ServerPlayer player) {
        if (announcePath == null) return;
        Map<Integer, String> pages = parseAnnouncementFile(announcePath);

        if (pages.isEmpty()) {
            return;
        }

        // 构造数据：pageNum1|content1||pageNum2|content2...
        List<String> pageData = new ArrayList<>();
        for (Map.Entry<Integer, String> e : pages.entrySet()) {
            pageData.add(e.getKey() + "|" + e.getValue());
        }
        String payload = String.join("||", pageData);

        PacketDistributor.sendToPlayer(player, new AnnouncementPacket(payload));
    }
}
