package mccfk.hy.hyserver.java.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import mccfk.hy.hyserver.java.HuiYangServerModContentManager;
import mccfk.hy.hyserver.java.block.ModBlocks;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, HuiYangServerModContentManager.MODID);

    // 安山机械物品
    public static final DeferredHolder<Item, Item> ANDESITE_MACHINE = ITEMS.register("andesite_machine",
            () -> new BlockItem(ModBlocks.ANDESITE_MACHINE.get(), new Item.Properties()));

    // 黄铜机械物品
    public static final DeferredHolder<Item, Item> BRASS_MACHINE = ITEMS.register("brass_machine",
            () -> new BlockItem(ModBlocks.BRASS_MACHINE.get(), new Item.Properties()));

    // 铜机械物品
    public static final DeferredHolder<Item, Item> COPPER_MACHINE = ITEMS.register("copper_machine",
            () -> new BlockItem(ModBlocks.COPPER_MACHINE.get(), new Item.Properties()));

    // 红石机械物品
    public static final DeferredHolder<Item, Item> REDSTONE_MACHINE = ITEMS.register("redstone_machine",
            () -> new BlockItem(ModBlocks.REDSTONE_MACHINE.get(), new Item.Properties()));

    // 无限岩浆源物品
    public static final DeferredHolder<Item, Item> INFINITE_LAVA = ITEMS.register("infinite_lava",
            () -> new BlockItem(ModBlocks.INFINITE_LAVA.get(), new Item.Properties()));

    // 无限水源物品
    public static final DeferredHolder<Item, Item> INFINITE_WATER = ITEMS.register("infinite_water",
            () -> new BlockItem(ModBlocks.INFINITE_WATER.get(), new Item.Properties()));

    // 维度钥匙
    public static final DeferredHolder<Item, Item> DIMENSION_KEY = ITEMS.register("dimension_key",
            () -> new DimensionKeyItem(new Item.Properties().stacksTo(1)));

    // 太阳能板物品 - 等级0（基础）
    public static final DeferredHolder<Item, Item> SOLAR_PANEL = ITEMS.register("solar_panel",
            () -> new BlockItem(ModBlocks.SOLAR_PANEL.get(), new Item.Properties()));

    // 1级压缩太阳能板物品
    public static final DeferredHolder<Item, Item> SOLAR_PANEL_1 = ITEMS.register("solar_panel_1",
            () -> new BlockItem(ModBlocks.SOLAR_PANEL_1.get(), new Item.Properties()));

    // 2级压缩太阳能板物品
    public static final DeferredHolder<Item, Item> SOLAR_PANEL_2 = ITEMS.register("solar_panel_2",
            () -> new BlockItem(ModBlocks.SOLAR_PANEL_2.get(), new Item.Properties()));

    // 3级压缩太阳能板物品
    public static final DeferredHolder<Item, Item> SOLAR_PANEL_3 = ITEMS.register("solar_panel_3",
            () -> new BlockItem(ModBlocks.SOLAR_PANEL_3.get(), new Item.Properties()));

    // 榴莲处理刀 - 无限耐久斧子，只能通过机械动力机械手使用
    public static final DeferredHolder<Item, Item> DURIAN_PROCESSING_KNIFE = ITEMS.register("durian_processing_knife",
            DurianProcessingKnifeItem::new);

    // 临时DF物品
    public static final DeferredHolder<Item, Item> TEMP_DF = ITEMS.register("temp_df",
            () -> new Item(new Item.Properties()));

    // 三角洲行动物品 - 高价值物资
    // 6级(红色)
    public static final DeferredHolder<Item, Item> AFRICAN_STAR = ITEMS.register("african_star",
            () -> new Item(new Item.Properties()));
    
    public static final DeferredHolder<Item, Item> OCEAN_TEAR = ITEMS.register("ocean_tear",
            () -> new Item(new Item.Properties()));
    
    public static final DeferredHolder<Item, Item> EXPERIMENTAL_DATA = ITEMS.register("experimental_data",
            () -> new Item(new Item.Properties()));
    
    public static final DeferredHolder<Item, Item> CARBON_FIBER_PLATE = ITEMS.register("carbon_fiber_plate",
            () -> new Item(new Item.Properties()));
    
    public static final DeferredHolder<Item, Item> GOLD_BAR = ITEMS.register("gold_bar",
            () -> new Item(new Item.Properties()));

    // 5级(金色)
    public static final DeferredHolder<Item, Item> DESIGN_BLUEPRINT = ITEMS.register("design_blueprint",
            () -> new Item(new Item.Properties()));
    
    public static final DeferredHolder<Item, Item> GOLD_COIN = ITEMS.register("gold_coin",
            () -> new Item(new Item.Properties()));

    // 4级(紫色)
    public static final DeferredHolder<Item, Item> IMPRESSIONIST_PAINTING = ITEMS.register("impressionist_painting",
            () -> new Item(new Item.Properties()));
    
    public static final DeferredHolder<Item, Item> ASARA_CLASSIFIED_DOCS = ITEMS.register("asara_classified_docs",
            () -> new Item(new Item.Properties()));

    // 3级(蓝色)
    public static final DeferredHolder<Item, Item> SUGAR_TRIANGLE = ITEMS.register("sugar_triangle",
            () -> new Item(new Item.Properties()));
    
    public static final DeferredHolder<Item, Item> DANCING_GIRL = ITEMS.register("dancing_girl",
            () -> new Item(new Item.Properties()));
    
    public static final DeferredHolder<Item, Item> SILVER_COIN = ITEMS.register("silver_coin",
            () -> new Item(new Item.Properties()));

    // 2级(绿色)
    public static final DeferredHolder<Item, Item> COPPER_COIN = ITEMS.register("copper_coin",
            () -> new Item(new Item.Properties()));

    // 1级(白色)
    public static final DeferredHolder<Item, Item> LITTLE_JOKER = ITEMS.register("little_joker",
            () -> new Item(new Item.Properties()));

    // 纸条物品 - 潜行右键打开界面输入内容，显示为物品名
    public static final DeferredHolder<Item, Item> NOTE_PAPER = ITEMS.register("note_paper",
            () -> new NotePaperItem(new Item.Properties()));

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
