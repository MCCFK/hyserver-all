package mccfk.hy.hyserver.java.item;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.core.component.DataComponents;

/**
 * 榴莲处理刀 - 无限耐久的斧子工具
 * 只能通过机械动力机械手使用，玩家不能直接使用
 */
public class DurianProcessingKnifeItem extends AxeItem {
    
    public DurianProcessingKnifeItem() {
        super(
            Tiers.IRON,
            new Item.Properties()
                .stacksTo(1)
                .durability(-1) // 无限耐久
                .component(DataComponents.MAX_DAMAGE, Integer.MAX_VALUE)
                .component(DataComponents.UNBREAKABLE, new net.minecraft.world.item.component.Unbreakable(true))
        );
    }
    
    /**
     * 禁止玩家直接使用此物品
     */
    public boolean isCorrectToolForDrops(net.minecraft.world.level.block.state.BlockState state) {
        return false; // 像剑一样无法挖掘方块
    }
}
