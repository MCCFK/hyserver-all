package mccfk.hy.hyserver.java.announce;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 服务端→客户端公告数据包
 * payload: "pageNum1|content1||pageNum2|content2||..."
 */
public class AnnouncementPacket implements CustomPacketPayload {
    public static final Type<AnnouncementPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("hyserver", "announcement")
    );

    public static final StreamCodec<FriendlyByteBuf, AnnouncementPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            p -> p.payload,
            AnnouncementPacket::new
        );

    private final String payload;

    public AnnouncementPacket(String payload) {
        this.payload = payload;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public String getPayload() {
        return payload;
    }

    /**
     * 客户端处理：打开公告窗口
     */
    public static void handle(AnnouncementPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            AnnouncementWindow.show(packet.payload);
        });
    }
}
