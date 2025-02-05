package cx.rain.mc.nbtedit.gui.component;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class ScrollableViewport extends AbstractComposedComponent {

    private final int scrollBarWidth;

    private int scrollXOffset = 0;
    private int scrollYOffset = 0;
    private ScrollBar verticalScrollBar = null;
    private ScrollBar horizontalScrollBar = null;

    private int contentWidth = 0;
    private int contentHeight = 0;

    public ScrollableViewport(int x, int y, int width, int height, int scrollBarWidth) {
        super(x, y, width, height, Component.empty());

        this.scrollBarWidth = scrollBarWidth;
    }

    @Override
    public void update() {
        super.update();
        createChildren();
    }

    @Override
    protected void createChildren() {
        contentWidth = 0;
        contentHeight = 0;

        for (var c : getChildren()) {
            var cw = c.getX() + c.getWidth();
            var ch = c.getY() + c.getHeight();
            if (contentWidth < cw) {
                contentWidth = cw;
            }

            if (contentHeight < ch) {
                contentHeight = ch;
            }
        }

        if (shouldShowVerticalBar()) {
            scrollYOffset = Mth.clamp(scrollYOffset, 0, Math.max(contentHeight - getHeight(), 0));
            verticalScrollBar = new ScrollBar(getX() + getWidth() - getScrollBarWidth(), getY(),
                    getScrollBarWidth(), getHeight(),
                    offset -> this.onScroll(0, offset), contentHeight);
            verticalScrollBar.setScrollAmount(scrollYOffset);
        }

        if (shouldShowHorizontalBar()) {
            scrollXOffset = Mth.clamp(scrollXOffset, 0, Math.max(contentWidth - getWidth(), 0));
            horizontalScrollBar = new ScrollBar(getX(), getY() + getHeight() - getScrollBarWidth(),
                    getWidth() - (shouldShowVerticalBar() ? getScrollBarWidth() : 0), getScrollBarWidth(),
                    offset -> this.onScroll(offset, 0), contentWidth, true);
            horizontalScrollBar.setScrollAmount(scrollXOffset);
        }
    }

    public boolean shouldShowVerticalBar() {
        return contentHeight > getHeight();
    }

    public boolean shouldShowHorizontalBar() {
        return contentWidth > getWidth();
    }

    public void onScroll(int deltaX, int deltaY) {
        if (deltaY != 0) {
            scrollYOffset += deltaY;
            scrollYOffset = Mth.clamp(scrollYOffset, 0, Math.max(contentHeight - getHeight(), 0));
        }

        if (deltaX != 0) {
            scrollXOffset += deltaX;
            scrollXOffset = Mth.clamp(scrollXOffset, 0, Math.max(contentWidth - getWidth(), 0));
        }
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        var maskedMouseX = mouseX > getX() && mouseX < (getX() + getWidth()) ? mouseX - getX() : -1;
        var maskedMouseY = mouseY > getY() && mouseY < (getY() + getHeight()) ? mouseY - getY() : -1;

        guiGraphics.enableScissor(getX(), getY(), getX() + getWidth(), getY() + getHeight());
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(getX() - getScrollXOffset(), getY() - getScrollYOffset(), 0.0);

        super.renderWidget(guiGraphics, maskedMouseX, maskedMouseY, partialTick);

        guiGraphics.pose().popPose();
        guiGraphics.disableScissor();

        if (shouldShowVerticalBar()) {
            verticalScrollBar.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        if (shouldShowHorizontalBar()) {
            horizontalScrollBar.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }

    public int getScrollBarWidth() {
        return scrollBarWidth;
    }

    public double getScrollXOffset() {
        return scrollXOffset;
    }

    public void setScrollXOffset(int value) {
        this.scrollXOffset = value;
    }

    public double getScrollYOffset() {
        return scrollYOffset;
    }

    public void setScrollYOffset(int value) {
        this.scrollYOffset = value;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (shouldShowVerticalBar() && verticalScrollBar.isMouseOver(mouseX, mouseY)) {
            return verticalScrollBar.mouseClicked(mouseX, mouseY, button);
        }

        if (shouldShowHorizontalBar() && horizontalScrollBar.isMouseOver(mouseX, mouseY)) {
            return horizontalScrollBar.mouseClicked(mouseX, mouseY, button);
        }

        return super.mouseClicked(mouseX - getX() + getScrollXOffset(), mouseY - getY() + getScrollYOffset(), button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (shouldShowVerticalBar() && verticalScrollBar.isMouseOver(mouseX, mouseY)) {
            return verticalScrollBar.mouseReleased(mouseX, mouseY, button);
        }

        if (shouldShowHorizontalBar() && horizontalScrollBar.isMouseOver(mouseX, mouseY)) {
            return horizontalScrollBar.mouseReleased(mouseX, mouseY, button);
        }

        return super.mouseReleased(mouseX - getX() + getScrollXOffset(), mouseY - getY() + getScrollYOffset(), button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (shouldShowVerticalBar() && verticalScrollBar.isDragging() && dragY != 0) {
            return verticalScrollBar.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        if (shouldShowHorizontalBar() && horizontalScrollBar.isDragging() && dragX != 0) {
            return horizontalScrollBar.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        return super.mouseDragged(mouseX - getX() + getScrollXOffset(), mouseY - getY() + getScrollYOffset(), button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (shouldShowVerticalBar() && scrollY != 0) {
            return verticalScrollBar.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        if (shouldShowHorizontalBar() && scrollX != 0) {
            return horizontalScrollBar.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        return super.mouseScrolled(mouseX - getX() + getScrollXOffset(), mouseY - getY() + getScrollYOffset(), scrollX, scrollY);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (shouldShowVerticalBar() && verticalScrollBar.isHoveredOrFocused()) {
            return verticalScrollBar.keyReleased(keyCode, scanCode, modifiers);
        }

        if (shouldShowHorizontalBar() && verticalScrollBar.isHoveredOrFocused()) {
            return horizontalScrollBar.keyReleased(keyCode, scanCode, modifiers);
        }

        return super.keyReleased(keyCode, scanCode, modifiers);
    }
}
