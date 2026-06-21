package mccfk.hy.hyserver.java.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * 维度选择GUI界面
 * 显示20个维度按钮供玩家选择
 */
public class DimensionSelectorScreen extends Screen {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath("hyserver", "textures/gui/dimension_selector.png");
    
    private final net.minecraft.world.item.ItemStack keyStack;
    private Button[] dimensionButtons = new Button[20];
    
    /**
     * 无参构造函数（用于网络包反射调用）
     */
    public DimensionSelectorScreen() {
        this(net.minecraft.world.item.ItemStack.EMPTY);
    }
    
    public DimensionSelectorScreen(net.minecraft.world.item.ItemStack keyStack) {
        super(Component.literal("选择私人维度"));
        this.keyStack = keyStack;
    }
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        // 创建4x5网格布局（20个维度）
        int buttonWidth = 40;
        int buttonHeight = 20;
        int spacing = 5;
        int startX = centerX - (5 * buttonWidth + 4 * spacing) / 2;
        int startY = centerY - (4 * buttonHeight + 3 * spacing) / 2;
        
        for (int i = 0; i < 20; i++) {
            int row = i / 5;
            int col = i % 5;
            int x = startX + col * (buttonWidth + spacing);
            int y = startY + row * (buttonHeight + spacing);
            
            int dimensionId = i + 1;
            Button button = Button.builder(
                Component.literal("d_" + dimensionId),
                btn -> onDimensionSelected(dimensionId)
            ).bounds(x, y, buttonWidth, buttonHeight).build();
            
            dimensionButtons[i] = button;
            addRenderableWidget(button);
        }
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 渲染半透明背景
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        
        // 标题
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        
        // 说明文字
        guiGraphics.drawCenteredString(this.font, "选择一个维度绑定到钥匙", this.width / 2, 40, 0xAAAAAA);
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    /**
     * 当玩家选择维度时调用
     */
    private void onDimensionSelected(int dimensionId) {
        // 发送网络包到服务器设置NBT
        String worldId = String.valueOf(dimensionId);
        
        // 通过网络包发送到服务器（安全，无法被玩家滥用）
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
            new mccfk.hy.hyserver.java.network.SetDimensionKeyPacket(worldId)
        );
        
        // 关闭GUI
        this.onClose();
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
