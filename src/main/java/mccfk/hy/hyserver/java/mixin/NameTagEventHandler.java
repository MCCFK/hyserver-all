package mccfk.hy.hyserver.java.event;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 头顶名字 Mixin — 使用事件系统代替 Mixin 注入
 * 服务端通过 setCustomName() 同步格式化名，客户端读取 getCustomName() 作为头顶显示
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class NameTagEventHandler {

    @SubscribeEvent
    public static void onPlayerNameFormat(PlayerEvent.NameFormat event) {
        AbstractClientPlayer player = (AbstractClientPlayer) event.getEntity();
        Component customName = player.getCustomName();
        if (customName != null) {
            event.setDisplayname(customName);
        }
    }
}
