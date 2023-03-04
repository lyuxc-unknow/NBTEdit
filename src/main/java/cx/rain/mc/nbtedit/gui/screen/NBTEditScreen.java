package cx.rain.mc.nbtedit.gui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import cx.rain.mc.nbtedit.NBTEdit;
import cx.rain.mc.nbtedit.gui.NBTEditGui;
import cx.rain.mc.nbtedit.nbt.NBTTree;
import cx.rain.mc.nbtedit.utility.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

public class NBTEditScreen extends Screen {
    protected final boolean isEntity;

    protected UUID entityUuid;
    protected int entityId;
    protected boolean isSelf;

    protected BlockPos blockPos;

    protected NBTEditGui gui;

    public NBTEditScreen(UUID uuid, int id, CompoundTag tag, boolean self) {
        super(Component.translatable(Constants.GUI_TITLE_NBTEDIT_ENTITY, uuid));
        minecraft = Minecraft.getInstance();

        isEntity = true;
        entityUuid = uuid;
        entityId = id;
        isSelf = self;

        gui = new NBTEditGui(NBTTree.root(tag));
    }

    public NBTEditScreen(BlockPos pos, CompoundTag tag) {
        super(Component.translatable(Constants.GUI_TITLE_NBTEDIT_BLOCK_ENTITY,
                pos.getX(), pos.getY(), pos.getZ()));
        minecraft = Minecraft.getInstance();

        isEntity = false;
        blockPos = pos;

        gui = new NBTEditGui(NBTTree.root(tag));
    }

    @Override
    protected void init() {
        super.init();

        clearWidgets();

        gui.init(width, height, height - 35);

        addRenderableWidget(Button.builder(Component.translatable(Constants.GUI_BUTTON_SAVE), this::onSaveClicked)
                .pos(width / 4 - 100, height - 27)
                .size(200, 20)
                .build());

        addRenderableWidget(Button.builder(Component.translatable(Constants.GUI_BUTTON_QUIT), this::onQuitClicked)
                .pos(width * 3 / 4 - 100, height - 27)
                .size(200, 20)
                .build());
    }

//    @Override
//    public void onClose() {
//        super.onClose();
//    }

    @Override
    public void tick() {
        if (!getMinecraft().player.isAlive()) {
            doClose();
        } else {
            gui.tick(false);
            gui.update(true);
        }

        super.tick();
    }

    @Override
    public void render(PoseStack stack, int mouseX, int mouseY, float partialTick) {
        renderBackground(stack);
        gui.render(stack, mouseX, mouseY, partialTick);
        drawCenteredString(stack, getMinecraft().font, title, this.width / 2, 5, 16777215);

        if (gui.hasWindow()) {
            super.render(stack, mouseX, mouseY, partialTick);
        } else {
            super.render(stack, -1, -1, partialTick);
        }
    }

//    @Override
//    public boolean isPauseScreen() {
//        return false;
//    }

    // <editor-fold desc="Properties and accessors.">

    public boolean isEntity() {
        return isEntity;
    }

    public boolean isBlockEntity() {
        return !isEntity;
    }

    public Entity getEntity() {
        if (!isEntity()) {
            throw new UnsupportedOperationException("Cannot get Entity by an BlockEntity!");
        }

        return getMinecraft().level.getEntity(entityId);
    }

    public BlockPos getBlockPos() {
        if (!isBlockEntity()) {
            throw new UnsupportedOperationException("Cannot get block position of an Entity!");
        }

        return blockPos;
    }

    // </editor-fold>

    // <editor-fold desc="Button handler.">

    private void onSaveClicked(Button button) {
        doSave();
        doClose();
    }

    private void onQuitClicked(Button button) {
        doClose();
    }

    private void doSave() {
        if (isEntity) {
            NBTEdit.getInstance().getNetworkManager().saveEditing(getEntity(), gui.getTree().toCompound(), isSelf);
        } else {
            NBTEdit.getInstance().getNetworkManager().saveEditing(getBlockPos(), gui.getTree().toCompound());
        }
    }

    private void doClose() {
        getMinecraft().setScreen(null);
        getMinecraft().cursorEntered();
    }

    // </editor-fold>

    // <editor-fold desc="Input processing.">

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        super.mouseScrolled(mouseX, mouseY, delta);

        var offset = (int) delta;
        if (offset != 0) {
            gui.shiftY((offset >= 1) ? 6 : -6);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        var result = gui.onKeyPress(keyCode, scanCode, modifiers);
        return result || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        var result = gui.onMouseClicked(Mth.floor(mouseX), Mth.floor(mouseY), button);
        return result || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        var result = gui.onCharTyped(codePoint, modifiers);
        return result || super.charTyped(codePoint, modifiers);
    }

    // </editor-fold>
}
