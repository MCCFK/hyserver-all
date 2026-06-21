package mccfk.hy.hyserver.java.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 纸条编辑界面
 * 允许玩家输入任意文本作为纸条内容
 */
public class NotePaperEditScreen extends Screen {
    private final ItemStack noteStack;
    private EditBox contentEditBox;
    private Button confirmButton;
    private Button cancelButton;
    
    public NotePaperEditScreen(ItemStack noteStack) {
        super(Component.literal("编辑纸条"));
        this.noteStack = noteStack;
    }
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        // 获取当前内容
        String currentContent = mccfk.hy.hyserver.java.item.NotePaperItem.getContent(noteStack);
        if (currentContent == null) {
            currentContent = "";
        }
        
        // 创建文本输入框
        this.contentEditBox = new EditBox(
            this.font,
            centerX - 100,
            centerY - 30,
            200,
            20,
            Component.literal("内容")
        );
        this.contentEditBox.setValue(currentContent);
        this.contentEditBox.setMaxLength(2147483647); // 限制最大长度
        this.addWidget(this.contentEditBox);
        this.setInitialFocus(this.contentEditBox);
        
        // 确认按钮
        this.confirmButton = Button.builder(
            Component.literal("确认"),
            btn -> onConfirm()
        ).bounds(centerX - 105, centerY + 10, 95, 20).build();
        this.addRenderableWidget(this.confirmButton);
        
        // 取消按钮
        this.cancelButton = Button.builder(
            Component.literal("取消"),
            btn -> onClose()
        ).bounds(centerX + 10, centerY + 10, 95, 20).build();
        this.addRenderableWidget(this.cancelButton);
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 渲染背景
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        
        // 标题
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        
        // 说明文字
        guiGraphics.drawCenteredString(this.font, "输入纸条内容（最多2147483647字符）", this.width / 2, 40, 0xAAAAAA);
        
        // 先渲染按钮（保持清晰）
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // 最后单独渲染输入框（确保不受背景模糊影响）
        this.contentEditBox.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    /**
     * 确认按钮点击事件
     */
    private void onConfirm() {
        String content = this.contentEditBox.getValue().trim();
        
        // 发送网络包到服务器更新物品数据
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
            new mccfk.hy.hyserver.java.network.UpdateNotePaperPacket(content)
        );
        
        // 关闭界面
        this.onClose();
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC键
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
