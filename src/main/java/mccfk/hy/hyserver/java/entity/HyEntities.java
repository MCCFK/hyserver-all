package mccfk.hy.hyserver.java.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class HyEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = 
        DeferredRegister.create(Registries.ENTITY_TYPE, "hyserver");

    public static final DeferredHolder<EntityType<?>, EntityType<GroundItemEntity>> GROUND_ITEM =
        ENTITY_TYPES.register("df_item_e", () -> EntityType.Builder.<GroundItemEntity>of(
                GroundItemEntity::new, MobCategory.MISC)
            .sized(0.5F, 0.25F)
            .clientTrackingRange(6)
            .updateInterval(20)
            .build(ResourceLocation.fromNamespaceAndPath("hyserver", "df_item_e").toString()));
}
