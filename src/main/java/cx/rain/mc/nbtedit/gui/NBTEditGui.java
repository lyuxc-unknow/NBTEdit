package cx.rain.mc.nbtedit.gui;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import cx.rain.mc.nbtedit.NBTEdit;
import cx.rain.mc.nbtedit.gui.component.window.EditValueSubWindow;
import cx.rain.mc.nbtedit.gui.component.NBTNodeComponent;
import cx.rain.mc.nbtedit.gui.component.button.NBTOperatorButton;
import cx.rain.mc.nbtedit.gui.component.button.SaveLoadSlotButton;
import cx.rain.mc.nbtedit.gui.component.window.ISubWindowHolder;
import cx.rain.mc.nbtedit.gui.component.window.SubWindowComponent;
import cx.rain.mc.nbtedit.nbt.NBTTree;
import cx.rain.mc.nbtedit.nbt.NBTHelper;
import cx.rain.mc.nbtedit.utility.Constants;
import cx.rain.mc.nbtedit.utility.SortHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.List;

public class NBTEditGui extends Gui implements ISubWindowHolder {
    protected int focusedSaveSlotIndex = -1;

    protected SaveLoadSlotButton[] saves = new SaveLoadSlotButton[7];
    protected List<NBTNodeComponent> nodes = new ArrayList<>();

    protected int width;
    protected int height;
    protected int bottom;

    protected int x;
    protected int y;

    protected int heightDiff;
    protected int heightOffset;

    protected int yClick = -1;

//    protected EditValueSubWindow subWindow = null;

    public NBTEditGui(NBTTree treeIn) {
        super(Minecraft.getInstance(), Minecraft.getInstance().getItemRenderer());

        tree = treeIn;

        addButtons();
        addSaveSlotButtons();
    }

    // <editor-fold desc="Properties and accessors.">
    private final int START_X = 10;
    private final int START_Y = 30;

    private final int X_GAP = 10;
    private final int Y_GAP = getMinecraft().font.lineHeight + 2;

    protected Minecraft getMinecraft() {
        return minecraft;
    }

    private int getHeightDifference() {
        return getContentHeight() - (bottom - START_Y + 2);
    }

    private int getContentHeight() {
        return Y_GAP * nodes.size();
    }

    // </editor-fold>

    // <editor-fold desc="Initializations.">

    public void init(int widthIn, int heightIn, int bottomIn) {
        width = widthIn;
        height = heightIn;
        bottom = bottomIn;

        yClick = -1;
        update(false);
    }

    // </editor-fold>

    // <editor-fold desc="Updates.">

    public void update(NBTTree.Node<?> node) {
        var parent = node.getParent();
        Collections.sort(parent.getChildren(), SortHelper.get());
        update(true);
    }

    public void updateFromRoot(NBTTree.Node<?> node, boolean circular) {
        Collections.sort(node.getChildren(), SortHelper.get());
        if (circular) {
            for (var child : node.getChildren()) {
                updateFromRoot(child, true);
            }
        }
    }

    public void update(boolean isShiftToFocused) {
        y = START_Y;
        nodes.clear();
        addNodes(tree.getRoot(), START_X);
        if (focused != null) {
            if (!checkValidFocus(focused)) {
                setFocused(null);
            }
        }
        if (focusedSaveSlotIndex != -1) {
            saves[focusedSaveSlotIndex].startEditing();
        }
        heightDiff = getHeightDifference();
        if (heightDiff <= 0) {
            heightOffset = 0;
        } else {
            if (heightOffset < -heightDiff) {
                heightOffset = -heightDiff;
            }
            if (heightOffset > 0) {
                heightOffset = 0;
            }

            for (var node : nodes) {
                node.shiftY(heightOffset);
            }

            if (isShiftToFocused && focused != null) {
                shiftToFocus(focused);
            }
        }

        updateFromRoot(getTree().getRoot(), true);
    }

    @Override
    public void tick(boolean pause) {
        if (hasWindow()) {
            for (var window : getWindows()) {
                window.tick();
            }
        }

        for (var button : getButtons()) {
            if (button.isActive()) {
            }
        }
    }

    // </editor-fold>

    // <editor-fold desc="Buttons.">

    protected List<Button> buttons = new ArrayList<>();

    protected NBTOperatorButton[] addButtons = new NBTOperatorButton[11];
    protected NBTOperatorButton editButton;
    protected NBTOperatorButton deleteButton;
    protected NBTOperatorButton copyButton;
    protected NBTOperatorButton cutButton;
    protected NBTOperatorButton pasteButton;

    protected List<Button> getButtons() {
        return buttons;
    }

    protected void addButton(Button button) {
        buttons.add(button);
    }

    private void addButtons() {
        int xLoc = 18;
        int yLoc = 4;

        // Todo: qyl27: Copy/Paste function.
        copyButton = new NBTOperatorButton(14, xLoc, yLoc, this, this::onCopyButtonClick,
                componentSupplier -> componentSupplier.get().append(Component.translatable(Constants.GUI_NARRATION_BUTTON_COPY))); // Copy Button.
        addButton(copyButton);

        xLoc += 15;
        cutButton = new NBTOperatorButton(15, xLoc, yLoc, this, this::onCutButtonClick,
                componentSupplier -> componentSupplier.get().append(Component.translatable(Constants.GUI_NARRATION_BUTTON_CUT))); // Cut Button.
        addButton(cutButton);

        xLoc += 15;
        pasteButton = new NBTOperatorButton(16, xLoc, yLoc, this, this::onPasteButtonClick,
                componentSupplier -> componentSupplier.get().append(Component.translatable(Constants.GUI_NARRATION_BUTTON_PASTE))); // Paste Button.
        addButton(pasteButton);

        xLoc += 45;
        editButton = new NBTOperatorButton(12, xLoc, yLoc, this, this::onEditButtonClick,
                componentSupplier -> componentSupplier.get().append(Component.translatable(Constants.GUI_NARRATION_BUTTON_EDIT))); // Edit Button.
        addButton(editButton);

        xLoc += 15;
        deleteButton = new NBTOperatorButton(13, xLoc, yLoc, this, this::onDeleteButtonClick,
                componentSupplier -> componentSupplier.get().append(Component.translatable(Constants.GUI_NARRATION_BUTTON_DELETE))); // Delete Button.
        addButton(deleteButton);

        // Add nbt type buttons.
        xLoc = 18;
        yLoc = 17;
        for (var i = 1; i < 12; i++) {
            var button = new NBTOperatorButton(i, xLoc, yLoc, this, this::onAddButtonsClick,
                    componentSupplier -> componentSupplier.get().append(Component.translatable(Constants.GUI_NARRATION_BUTTON_ADD)));
            addButtons[i - 1] = button;
            addButton(button);
            xLoc += 9;
        }

        updateButtons();
    }

    protected void onEditButtonClick(Button button) {
        if (button instanceof NBTOperatorButton operatorButton) {
            if (operatorButton.getButtonId() == 12) {  // 但愿人没事。
                doEditSelected();
            }
        }
    }

    protected void onDeleteButtonClick(Button button) {
        if (button instanceof NBTOperatorButton operatorButton) {
            if (operatorButton.getButtonId() == 13) {
                deleteSelected();
            }
        }
    }

    protected void onCopyButtonClick(Button button) {
        if (button instanceof NBTOperatorButton nbtOperator) {
            if (nbtOperator.getButtonId() == 14) {
                copySelected();
            }
        }
    }

    protected void onCutButtonClick(Button button) {
        if (button instanceof NBTOperatorButton nbtOperator) {
            if (nbtOperator.getButtonId() == 15) {
                copySelected();
                deleteSelected();
            }
        }
    }

    protected void onPasteButtonClick(Button button) {
        if (button instanceof NBTOperatorButton nbtOperator) {
            if (nbtOperator.getButtonId() == 16) {
                paste();
            }
        }
    }

    protected void onAddButtonsClick(Button button) {
        if (button instanceof NBTOperatorButton operatorButton) {
            if (operatorButton.getButtonId() >= 0 && operatorButton.getButtonId() <= 11) {
                if (getFocused() != null) {
                    getFocused().setShowChildren(true);
                    var children = getFocused().getChildren();
                    String type = button.getMessage().getString();

                    if (getFocused().getTag() instanceof ListTag) {
                        var tag = NBTHelper.newTag((operatorButton.getButtonId()));
                        if (tag != null) {
                            var newChild = getFocused().newChild("", tag);
                            setFocused(newChild);
                        }
                    } else {
                        var typeId = operatorButton.getButtonId();
                        setFocused(insertOnFocus(newName(getFocused(), typeId), NBTHelper.newTag(typeId)));
                    }
                    update(true);
                }
            }
        }
    }

    public void doEditSelected() {
        var base = getFocused().getTag();
        var parent = getFocused().getParent().getTag();
        var editor = new EditValueSubWindow(this, getFocused(), !(parent instanceof ListTag),
                !(base instanceof CompoundTag || base instanceof ListTag));
        editor.init((width - EditValueSubWindow.WIDTH) / 2, (height - EditValueSubWindow.HEIGHT) / 2);
        addWindow(editor);
    }

    private void copySelected() {
        var node = getFocused();
        if (node != null) {
            if (node.getTag() instanceof ListTag) {
                var list = new ListTag();
//                focused.newChild("", list);
                // Todo: qyl27: clipboard.
//                NBTEdit.getInstance().getClientManager().setClipboard(new NamedNBT(node.getName(), list));
            } else if (node.getTag() instanceof CompoundTag) {
                CompoundTag compound = new CompoundTag();
//                focused.newChild(compound);
                // Todo: qyl27: clipboard.
//                NBTEdit.getInstance().getClientManager().setClipboard(new NamedNBT(node.getName(), compound));
            } else {
                // Todo: qyl27: clipboard.
//                NBTEdit.getInstance().getClientManager().setClipboard(focused.copy());
            }

            setFocused(focused);
        }
    }

    private void paste() {
        // Todo: qyl27: paste.
//        if (focused != null) {
//            if (NBTEdit.getInstance().getClientManager().getClipboard() != null) {
//                focused.setShowChildren(true);
//
//                var namedNBT = NBTEdit.getInstance().getClientManager().getClipboard().copy();
//                if (focused.getTag() instanceof ListTag) {
//                    namedNBT.setName("");
//                    focused.newChild(namedNBT);
//                    tree.addChildrenToTree(node);
//                    tree.sort(node);
//                    setFocused(node);
//                } else {
//                    String name = namedNBT.getName();
//                    List<NBTTree.Node<?>> children = focused.getChildren();
//                    if (!isNameValid(name, children)) {
//                        for (int i = 1; i <= children.size() + 1; ++i) {
//                            String n = name + "(" + i + ")";
//                            if (isNameValid(n, children)) {
//                                namedNBT.setName(n);
//                                break;
//                            }
//                        }
//                    }
//                    NBTTree.Node<?> node = insertNode(namedNBT);
//                    tree.addChildrenToTree(node);
//                    tree.sort(node);
//                    setFocused(node);
//                }
//
//                update(true);
//            }
//        }
    }

    public void deleteSelected() {
        var prevFocused = getFocused();
        if (prevFocused != null) {
            var parent = prevFocused.getParent();
            parent.removeChild(prevFocused);
            shiftFocus(true);
            if (focused == prevFocused) {
                setFocused(null);
            }
            update(false);
        }
    }

    private void updateButtons() {
        var nodeToFocus = getFocused();
        if (nodeToFocus == null) {
            inactiveAllButtons();
        } else if (nodeToFocus.getTag() instanceof CompoundTag) {
            activeAllButtons();
            editButton.active = nodeToFocus.hasParent() && !(nodeToFocus.getParent().getTag() instanceof ListTag);
            deleteButton.active = nodeToFocus != tree.getRoot();
            cutButton.active = nodeToFocus != tree.getRoot();
            pasteButton.active = NBTEdit.getInstance().getClientManager().getClipboard() != null;
        } else if (nodeToFocus.getTag() instanceof ListTag) {
            if (nodeToFocus.hasChild()) {
                var elementType = nodeToFocus.getChildren().get(0).getTag().getId();
                inactiveAllButtons();

                addButtons[elementType - 1].active = true;
                editButton.active = !(nodeToFocus.getParent().getTag() instanceof ListTag);
                deleteButton.active = true;
                copyButton.active = true;
                cutButton.active = true;
                pasteButton.active = NBTEdit.getInstance().getClientManager().getClipboard() != null
                        && NBTEdit.getInstance().getClientManager().getClipboard().getTag().getId() == elementType;
            } else {
                activeAllButtons();
                editButton.active = !(nodeToFocus.getParent().getTag() instanceof ListTag);
                pasteButton.active = NBTEdit.getInstance().getClientManager().getClipboard() != null;
            }
        } else {
            inactiveAllButtons();

            editButton.active = true;
            deleteButton.active = true;
            copyButton.active = true;
            cutButton.active = true;
        }
    }

    private void activeAllButtons() {
        for (var button : addButtons) {
            button.active = true;
        }

        editButton.active = true;
        deleteButton.active = true;
        copyButton.active = true;
        cutButton.active = true;
        pasteButton.active = true;
    }

    private void inactiveAllButtons() {
        for (var button : addButtons) {
            button.active = false;
        }

        editButton.active = false;
        deleteButton.active = false;
        copyButton.active = false;
        cutButton.active = false;
        pasteButton.active = false;
    }

    private void addSaveSlotButtons() {
        var saveStates = NBTEdit.getInstance().getClientManager().getClipboardSaves();
        for (int i = 0; i < 7; ++i) {
            saves[i] = new SaveLoadSlotButton(saveStates.getClipboard(i), width - 24, 31 + i * 25, i + 1, this::onSaveSlotClicked);
        }
    }

    private void onSaveSlotClicked(Button button) {
        // Todo: qyl27: S/L function.
//        if (button instanceof SaveLoadSlotButton saveButton) {
//            if (saveButton.getSave().tag.isEmpty()) { //Copy into save slot
//                var node = (focused == null) ? tree.getRoot() : focused;
//                var base = node.getTag();
//                var name = node.getName();
//                if (base instanceof ListTag) {
//                    var list = new ListTag();
//                    node.newChild(list);
//                    saveButton.getSave().tag.put(name, list);
//                } else if (base instanceof CompoundTag) {
//                    var compound = new CompoundTag();
//                    node.newChild(compound);
//                    saveButton.getSave().tag.put(name, compound);
//                } else {
//                    saveButton.getSave().tag.put(name, base.copy());
//                }
//                saveButton.saved();
//                NBTEdit.getInstance().getClientManager().getClipboardSaves().save();
//                getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
//            } else { //Paste into
//                var nbtMap = saveButton.getSave().tag.tags;
//                if (nbtMap.isEmpty()) {
//                    // Todo: AS: Logging.
//                } else {
//                    if (focused == null) {
//                        setFocused(tree.getRoot());
//                    }
//                    var firstEntry = nbtMap.entrySet().iterator().next();
//                    assert firstEntry != null;
//                    var name = firstEntry.getKey();
//                    var nbt = firstEntry.getValue().copy();
//                    if (focused == tree.getRoot() && nbt instanceof CompoundTag && name.equals("ROOT")) {
//                        setFocused(null);
//                        tree = NBTTree.root((CompoundTag) nbt);
//                        update(false);
//                        getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
//                    } else if (canAddToParentByType(focused.getTag(), nbt)) {
//                        focused.setShowChildren(true);
//                        for (var it = focused.getChildren().iterator(); it.hasNext(); ) { //Replace object with same name
//                            if (it.next().getName().equals(name)) {
//                                it.remove();
//                                break;
//                            }
//                        }
//
//                        var node = insertNode(new NamedNBT(name, nbt));
//                        tree.addChildrenToTree(node);
//                        tree.sort(node);
//                        setFocused(node);
//                        update(true);
//                        getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
//                    }
//                }
//            }
//        }
    }
    
    // </editor-fold>

    // <editor-fold desc="Focusing.">

    private void addNodes(NBTTree.Node<?> root, int startX) {
        nodes.add(new NBTNodeComponent(startX, y,
                Component.literal(NBTHelper.getNBTNameSpecial(root)), this, root));

        startX += X_GAP;
        y += Y_GAP;

        if (root.shouldShowChildren()) {
            for (var child : root.getChildren()) {
                addNodes(child, startX);
            }
        }
    }

    private void setFocused(NBTTree.Node<?> toFocus) {
        updateButtons();

        focused = toFocus;
        if (focused != null && focusedSaveSlotIndex != -1) {
            stopEditingSlot();
        }
    }

    public void stopEditingSlot() {
        saves[focusedSaveSlotIndex].stopEditing();
        NBTEdit.getInstance().getClientManager().getClipboardSaves().save();
        focusedSaveSlotIndex = -1;
    }

    private void shiftToFocus(NBTTree.Node<?> focused) {
        var index = getIndexOf(focused);
        if (index != -1) {
            var component = nodes.get(index);
            shiftY((bottom + START_Y + 1) / 2 - (component.getY() + component.getHeight()));
        }
    }

    private void shiftFocus(boolean upward) {
        int index = getIndexOf(focused);
        if (index != -1) {
            index += (upward) ? -1 : 1;
            if (index >= 0 && index < nodes.size()) {
                setFocused(nodes.get(index).getNode());
                shiftY((upward) ? Y_GAP : -Y_GAP);
            }
        }
    }

    private int getIndexOf(NBTTree.Node<?> focused) {
        for (var i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).getNode() == focused) {
                return i;
            }
        }
        return -1;
    }

    public void shiftY(int offsetY) {
        if (heightDiff <= 0 || hasWindow()) {
            return;
        }

        int difference = heightOffset + offsetY;
        if (difference > 0) {
            difference = 0;
        }

        if (difference < -heightDiff) {
            difference = -heightDiff;
        }

        for (var node : nodes) {
            node.shiftY(difference - heightOffset);
        }

        heightOffset = difference;
    }

    private boolean checkValidFocus(NBTTree.Node<?> focused) {
        for (var node : nodes) { // Check all nodes.
            if (node.getNode() == focused) {
                setFocused(focused);
                return true;
            }
        }
        return focused.hasParent() && checkValidFocus(focused.getParent());
    }

    // </editor-fold>

    // <editor-fold desc="NBT tree.">

    protected NBTTree tree;
    protected NBTTree.Node<?> focused;

    public NBTTree getTree() {
        return tree;
    }

    public NBTTree.Node<?> getFocused() {
        return focused;
    }

    // </editor-fold>

    // <editor-fold desc="Helper methods.">

    private NBTTree.Node<?> insertOnFocus(String name, Tag tag) {
        // Todo: qyl27: more check for it?
        if (isNameValid(name, getFocused())) {
            return getFocused().newChild(name, tag);
        }

        return getFocused().newChild("", tag);
    }

    private boolean isNameValid(String name, NBTTree.Node<?> parent) {
        for (var node : parent.getChildren()) {
            if (node.getName().equals(name)) {
                return false;
            }
        }
        return true;
    }

    private String newName(NBTTree.Node<?> parent, byte typeId) {
        var typeName = NBTHelper.getNameByButton(typeId);
        if (!parent.hasChild()) {
            return typeName + "1";
        }
        for (int i = 1; i <= parent.getChildren().size() + 1; ++i) {
            String name = typeName + i;
            if (isNameValid(name, parent)) {
                return name;
            }
        }
        return typeName + "INF";
    }

    private boolean canAddToParentByType(Tag parent, Tag child) {
        if (parent instanceof CompoundTag) {
            return true;
        }

        if (parent instanceof ListTag list) {
            return list.size() == 0 || list.getElementType() == child.getId();
        }
        return false;
    }

    // </editor-fold>

    // Getter start.

//    public SaveLoadSlotButton getFocusedSaveSlotIndex() {
//        return (focusedSaveSlotIndex != -1) ? saves[focusedSaveSlotIndex] : null;
//    }
//
//    // Getter end.
//
//    // Interact start.
//
//    public void update() {
//        if (subWindow != null) {
//            subWindow.update();
//        }
//
//        if (focusedSaveSlotIndex != -1) {
//            saves[focusedSaveSlotIndex].update();
//        }
//    }
//    // Interact end.
//
//    // Nodes start.
//
//    public boolean isEditingSlot() {
//        return focusedSaveSlotIndex != -1;
//    }
//
//    // Nodes end.
//
//    // Misc start.
//
//    private boolean canPaste() {
//        return NBTEdit.getInstance().getClientManager().getClipboard() != null && focused != null
//                && canAddToParent(focused.getTag(), NBTEdit.getInstance().getClientManager().getClipboard().getTag());
//    }
//
//
//    // Misc end.

    // <editor-fold desc="Input processing.">

    public boolean onMouseClicked(int mouseX, int mouseY, int partialTick) {
        if (hasWindow()) {
            return getActiveWindow().mouseClicked(mouseX, mouseY, partialTick);
        }

        for (var button : getButtons()) {
            if (button.isMouseOver(mouseX, mouseY)) {
                button.mouseClicked(mouseX, mouseY, partialTick);
            }
        }

        var shouldUpdate = false;

        for (var node : nodes) {
            if (node.spoilerClicked(mouseX, mouseY)) { // Check hide/show children buttons
                shouldUpdate = true;
                if (node.shouldShowChildren()) {
                    heightOffset = (START_Y + 1) - (node.getY()) + heightOffset;
                }
                break;
            }
        }

        if (mouseY >= START_Y && mouseX <= width - 175) { //Check actual nodes, remove focus if nothing clicked
            NBTTree.Node<?> newFocus = null;
            for (var node : nodes) {
                if (node.isTextClicked(mouseX, mouseY)) {
                    newFocus = node.getNode();
                    break;
                }
            }
            if (focusedSaveSlotIndex != -1) {
                stopEditingSlot();
            }

            setFocused(newFocus);
        }

        if (shouldUpdate) {
            update(false);
        }

        return false;
    }

    public boolean onKeyPress(int keyCode, int scanCode, int modifiers) {
        if (hasWindow()) {
            return getActiveWindow().keyPressed(keyCode, scanCode, modifiers);
        }

        // Todo: qyl27: copy/paste shortcut.

        return false;
    }

    public boolean onCharTyped(char character, int keyId) {
        if (hasWindow()) {
            return getActiveWindow().charTyped(character, keyId);
        }

        return false;
    }

//    public void onArrowKeyPress(boolean isUp) {
//        if (focused == null) {
//            shiftY((isUp) ? Y_GAP : -Y_GAP);
//        } else {
//            shiftFocus(isUp);
//        }
//    }

    // </editor-fold>

    // <editor-fold desc="Render.">
    public void render(PoseStack stack, int mouseX, int mouseY, float partialTick) {
        var prevMouseX = mouseX;
        var prevMouseY = mouseY;

        if (hasWindow()) {
            prevMouseX = -1;
            prevMouseY = -1;
        }

        for (var node : nodes) {
            if (node.shouldRender(START_Y - 1, bottom)) {
                node.render(stack, prevMouseX, prevMouseY, partialTick);
            }
        }

        renderBackground(stack);
        for (var button : buttons) {
            button.render(stack, prevMouseX, prevMouseY, partialTick);
        }

        for (var button : saves) {
            button.render(stack, prevMouseX, prevMouseY, partialTick);
        }

        renderScrollBar(stack, prevMouseX, prevMouseY);

        if (hasWindow()) {
            getActiveWindow().render(stack, mouseX, mouseY, partialTick);
        }
    }

    private void renderScrollBar(PoseStack stack, int mouseX, int mouseY) {
        if (heightDiff > 0) {
            if (InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(),
                    InputConstants.MOUSE_BUTTON_LEFT)) {
                if (yClick == -1) {
                    if (mouseX >= width - 20 && mouseX < width && mouseY >= START_Y - 1 && mouseY < bottom) {
                        yClick = mouseY;
                    }
                } else {
                    float scrollMultiplier = 1.0F;
                    int height = getHeightDifference();

                    if (height < 1) {
                        height = 1;
                    }

                    int length = (bottom - (START_Y - 1)) * (bottom - (START_Y - 1)) / getContentHeight();
                    if (length < 32) {
                        length = 32;
                    }
                    if (length > bottom - (START_Y - 1) - 8) {
                        length = bottom - (START_Y - 1) - 8;
                    }

                    scrollMultiplier /= (float) (this.bottom - (START_Y - 1) - length) / (float) height;

                    shiftY((int) ((yClick - mouseY) * scrollMultiplier));
                    yClick = mouseY;
                }
            } else {
                yClick = -1;
            }

            fill(stack, width - 20, START_Y - 1, width, bottom, Integer.MIN_VALUE);

            int length = (bottom - (START_Y - 1)) * (bottom - (START_Y - 1)) / getContentHeight();
            if (length < 32) {
                length = 32;
            }
            if (length > bottom - (START_Y - 1) - 8) {
                length = bottom - (START_Y - 1) - 8;
            }
            int y = -heightOffset * (this.bottom - (START_Y - 1) - length) / heightDiff + (START_Y - 1);

            if (y < START_Y - 1) {
                y = START_Y - 1;
            }

            fillGradient(stack, width - 20, y, width, y + length, 0x80ffffff, 0x80333333);
        }
    }

    private void renderDirtBackground(int bottom, int height) {
        var tesselator = Tesselator.getInstance();
        var bufferBuilder = tesselator.getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, BACKGROUND_LOCATION);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        var f = 32.0F;
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bufferBuilder.vertex(0.0D, height, 0.0D).uv(0.0f, (float) height / f).color(64, 64, 64, 255).endVertex();
        bufferBuilder.vertex(width, height, 0.0D).uv((float) width / f, (float) height / f).color(64, 64, 64, 255).endVertex();
        bufferBuilder.vertex(width, bottom, 0.0D).uv((float) width / f, (float) bottom / f).color(64, 64, 64, 255).endVertex();
        bufferBuilder.vertex(0.0D, bottom, 0.0D).uv(0.0f, (float) bottom / f).color(64, 64, 64, 255).endVertex();
        tesselator.end();
    }

    private void renderBackground(PoseStack stack) {
//        fillGradient(stack, 0, 0, this.width, this.height, -1072689136, -804253680);

        renderDirtBackground(0, START_Y - 1);
        renderDirtBackground(bottom, height);
    }

    // </editor-fold>

    // <editor-fold desc="Sub-window holder.">

    private final List<SubWindowComponent> subWindows = new ArrayList<>();
    private final BiMap<String, SubWindowComponent> mutexSubWindows = HashBiMap.create();
    private SubWindowComponent activeSubWindow = null;

    @Override
    public List<SubWindowComponent> getWindows() {
        var list = ImmutableList.<SubWindowComponent>builder();
        list.addAll(subWindows).addAll(mutexSubWindows.values());
        return list.build();
    }

    @Override
    public SubWindowComponent getActiveWindow() {
        return activeSubWindow;
    }

    @Override
    public void setActiveWindow(SubWindowComponent window) {
        activeSubWindow = window;
    }

    @Override
    public boolean hasWindow() {
        return getWindows().size() > 0;
    }

    @Override
    public boolean hasWindow(SubWindowComponent window) {
        return getWindows().contains(window);
    }

    @Override
    public boolean hasMutexWindow(String name) {
        return mutexSubWindows.containsKey(name);
    }

    @Override
    public void addWindow(SubWindowComponent window) {
        subWindows.add(window);
        setActiveWindow(window);
    }

    @Override
    public void addMutexWindow(String name, SubWindowComponent window) {
        if (hasMutexWindow(name)) {
            throw new RuntimeException("Check has mutex first.");
        }

        mutexSubWindows.put(name, window);
    }

    @Override
    public boolean isMutexWindow(SubWindowComponent window) {
        return mutexSubWindows.containsValue(window);
    }

    @Override
    public void closeWindow(SubWindowComponent window) {
        if (!hasWindow(window)) {
            throw new IllegalStateException("The window is not in this GUI.");
        }

        window.close();
        if (isMutexWindow(window)) {
            var name = mutexSubWindows.inverse().get(window);
            mutexSubWindows.remove(name);
        } else {
            subWindows.remove(window);
        }
    }

    @Override
    public void closeAll() {
        for (var window : subWindows) {
            window.close();
        }
    }

    @Override
    public void focus(SubWindowComponent window) {
        for (var w : getWindows()) {
           w.inactive();
        }

        window.onFocus();
        activeSubWindow = window;
    }

    // </editor-fold>
}
