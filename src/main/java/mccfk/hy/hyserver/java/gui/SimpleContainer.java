package mccfk.hy.hyserver.java.gui;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 简单的容器实现
 */
public class SimpleContainer implements Container {
    private final NonNullList<ItemStack> items;
    
    public SimpleContainer(int size) {
        this.items = NonNullList.withSize(size, ItemStack.EMPTY);
    }
    
    @Override
    public int getContainerSize() {
        return items.size();
    }
    
    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public @NotNull ItemStack getItem(int index) {
        return items.get(index);
    }
    
    @Override
    public @NotNull ItemStack removeItem(int index, int amount) {
        ItemStack stack = items.get(index);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return stack.split(amount);
    }
    
    @Override
    public @NotNull ItemStack removeItemNoUpdate(int index) {
        return items.set(index, ItemStack.EMPTY);
    }
    
    @Override
    public void setItem(int index, @NotNull ItemStack stack) {
        items.set(index, stack);
    }
    
    @Override
    public void setChanged() {
        // 不需要处理
    }
    
    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }
    
    @Override
    public void clearContent() {
        items.clear();
    }
}
