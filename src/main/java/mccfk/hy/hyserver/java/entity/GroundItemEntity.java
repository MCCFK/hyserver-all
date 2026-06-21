package mccfk.hy.hyserver.java.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 地面物品实体 - 贴在地面上的物品掉落物
 * 玩家右键可以拾取
 */
public class GroundItemEntity extends Entity {
    private static final EntityDataAccessor<ItemStack> ITEM = SynchedEntityData.defineId(GroundItemEntity.class, EntityDataSerializers.ITEM_STACK);
    // 移除age和MAX_AGE,不再自动消失

    public GroundItemEntity(EntityType<? extends GroundItemEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = false; // 不阻挡方块
        this.noPhysics = true; // 无物理碰撞
        this.setNoGravity(true); // 不受重力影响
    }

    public GroundItemEntity(Level level, double x, double y, double z, ItemStack stack) {
        this(HyEntities.GROUND_ITEM.get(), level);
        // 保存初始位置
        this.setPos(x, y, z);
        this.initialY = y;
        this.setItem(stack);
    }
    
    private double initialY = 0; // 记录初始Y坐标

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ITEM, ItemStack.EMPTY);
    }

    @Override
    public void tick() {
        super.tick();
        
        if (!this.level().isClientSide()) {
            // 检查物品是否为空
            if (this.getItem().isEmpty()) {
                this.discard();
                return;
            }
            
            // 确保实体精确贴在地面上(只执行一次)
            if (this.tickCount == 1) {
                this.snapToGround();
            }
        }
    }

    private void snapToGround() {
        // 保持初始Y坐标，不往上移动
        this.setPos(this.getX(), this.initialY, this.getZ());
        // 清除所有速度
        this.setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        
        ItemStack itemStack = this.getItem();
        if (itemStack.isEmpty()) {
            return InteractionResult.PASS;
        }
        
        // 将物品给予玩家
        if (!player.addItem(itemStack)) {
            // 如果玩家背包满了,在地上生成普通掉落物
            this.spawnAsRegularDrop();
        } else {
            // 播放拾取音效
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), 
                SoundEvents.ITEM_PICKUP, SoundSource.AMBIENT, 0.2F, 
                ((this.random.nextFloat() - this.random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
        }
        
        this.discard();
        return InteractionResult.CONSUME;
    }

    private void spawnAsRegularDrop() {
        if (this.level() instanceof ServerLevel serverLevel) {
            ItemStack itemStack = this.getItem();
            // 使用正确的API创建物品实体
            ItemEntity itemEntity = new ItemEntity(serverLevel, this.getX(), this.getY(), this.getZ(), itemStack);
            serverLevel.addFreshEntity(itemEntity);
        }
    }

    public ItemStack getItem() {
        return this.entityData.get(ITEM);
    }

    public void setItem(ItemStack stack) {
        this.entityData.set(ITEM, stack.copy());
        this.updateBoundingBox(stack);
    }

    private void updateBoundingBox(ItemStack stack) {
        if (!stack.isEmpty()) {
            // 设置较小的碰撞箱,贴在地面上
            float size = 0.25F;
            this.setBoundingBox(new AABB(
                this.getX() - size, this.getY(), this.getZ() - size,
                this.getX() + size, this.getY() + 0.25, this.getZ() + size
            ));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        // 保存物品数据
        ItemStack itemStack = this.getItem();
        if (!itemStack.isEmpty()) {
            tag.put("Item", itemStack.save(this.registryAccess()));
        }
        // 保存初始Y坐标
        tag.putDouble("InitialY", this.initialY);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // 加载物品数据
        if (tag.contains("Item")) {
            CompoundTag itemTag = tag.getCompound("Item");
            ItemStack itemStack = ItemStack.parseOptional(this.registryAccess(), itemTag);
            this.setItem(itemStack);
        }
        // 加载初始Y坐标
        if (tag.contains("InitialY")) {
            this.initialY = tag.getDouble("InitialY");
        }
    }

    @Override
    public boolean isPickable() {
        return this.isAlive();
    }

    @Override
    public ItemStack getPickResult() {
        return this.getItem();
    }
    
    @Override
    public boolean isPushable() {
        return false; // 不能被推动
    }
    
    @Override
    public boolean isPushedByFluid() {
        return false; // 不被流体推动
    }
}
