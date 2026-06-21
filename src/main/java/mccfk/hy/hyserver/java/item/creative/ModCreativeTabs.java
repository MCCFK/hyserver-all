package mccfk.hy.hyserver.java.item.creative;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import mccfk.hy.hyserver.java.HuiYangServerModContentManager;
import mccfk.hy.hyserver.java.item.ModItems;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, HuiYangServerModContentManager.MODID);

    // hyserver 创造模式标签页
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> HYSERVER_TAB = CREATIVE_MODE_TABS.register("hyserver",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("cfk.itemGroup.hyserver"))
                    .icon(() -> ModItems.ANDESITE_MACHINE.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.ANDESITE_MACHINE.get());
                        output.accept(ModItems.BRASS_MACHINE.get());
                        output.accept(ModItems.COPPER_MACHINE.get());
                        output.accept(ModItems.REDSTONE_MACHINE.get());
                        output.accept(ModItems.INFINITE_LAVA.get());
                        output.accept(ModItems.INFINITE_WATER.get());
                        output.accept(ModItems.DIMENSION_KEY.get());
                        output.accept(ModItems.SOLAR_PANEL.get());
                        output.accept(ModItems.SOLAR_PANEL_1.get());
                        output.accept(ModItems.SOLAR_PANEL_2.get());
                        output.accept(ModItems.SOLAR_PANEL_3.get());
                        output.accept(ModItems.DURIAN_PROCESSING_KNIFE.get());
                        output.accept(ModItems.TEMP_DF.get());
                        
                        // 三角洲行动物品 - 按价值等级排序
                        // 6级(红色) - 最珍贵
                        output.accept(ModItems.AFRICAN_STAR.get());
                        output.accept(ModItems.OCEAN_TEAR.get());
                        output.accept(ModItems.EXPERIMENTAL_DATA.get());
                        output.accept(ModItems.CARBON_FIBER_PLATE.get());
                        output.accept(ModItems.GOLD_BAR.get());
                        
                        // 5级(金色)
                        output.accept(ModItems.DESIGN_BLUEPRINT.get());
                        output.accept(ModItems.GOLD_COIN.get());
                        
                        // 4级(紫色)
                        output.accept(ModItems.IMPRESSIONIST_PAINTING.get());
                        output.accept(ModItems.ASARA_CLASSIFIED_DOCS.get());
                        
                        // 3级(蓝色)
                        output.accept(ModItems.SUGAR_TRIANGLE.get());
                        output.accept(ModItems.DANCING_GIRL.get());
                        output.accept(ModItems.SILVER_COIN.get());
                        
                        // 2级(绿色)
                        output.accept(ModItems.COPPER_COIN.get());
                        
                        // 1级(白色)
                        output.accept(ModItems.LITTLE_JOKER.get());
                        
                        // 纸条物品
                        output.accept(ModItems.NOTE_PAPER.get());
                    })
                    .build());

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
