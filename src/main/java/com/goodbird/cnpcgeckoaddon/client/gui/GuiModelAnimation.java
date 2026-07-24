package com.goodbird.cnpcgeckoaddon.client.gui;

import com.goodbird.cnpcgeckoaddon.data.CustomModelDataProvider;
import com.goodbird.cnpcgeckoaddon.data.ICustomModelData;
import com.goodbird.cnpcgeckoaddon.utils.AnimationFileUtil;
import com.goodbird.cnpcgeckoaddon.utils.FloatTextFieldUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.select.GuiSoundSelection;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.entity.EntityNPCInterface;
import org.lwjgl.input.Mouse;
import software.bernie.geckolib3.resource.GeckoLibCache;

import java.io.IOException;

public class GuiModelAnimation extends SubGuiInterface implements ITextfieldListener, ISubGuiListener {

    private static final int ROW_HEIGHT = 22;
    private static final int HEADER_HEIGHT = 22;
    private static final int VISIBLE_ROWS = 5;
    private static final int PANEL_WIDTH = 300;

    private int openSection = 1;
    private int panelY;
    private int attackScroll = 0, hurtScroll = 0, deathScroll = 0;
    private int maxAttackScroll = 0, maxHurtScroll = 0, maxDeathScroll = 0;
    private int pendingSoundIndex = -1;
    private boolean pendingSoundIsAttack = true;

    public GuiModelAnimation(GuiScreen parent, EntityNPCInterface npc){
        this.npc = npc;
        closeOnEsc = true;
        this.parent = parent;
    }

    @Override
    public void initGui() {
        super.initGui();
        if (openSection == 0) openSection = 1;
        int y = guiTop + 8;

        addSelectionBlock(1, y, "cnpcgeckoaddon.gui.anim_file", getModelData(npc).getAnimFile());
        addSelectionBlock(2, y += 23, "cnpcgeckoaddon.gui.idle_anim", getModelData(npc).getIdleAnim());
        addSelectionBlock(3, y += 23, "cnpcgeckoaddon.gui.walk_anim", getModelData(npc).getWalkAnim());

        panelY = y + 24;

        int tabBtnW = 50;
        int tabBtnH = 20;
        int tabGap = 4;
        int tabStartX = guiLeft - 87 + PANEL_WIDTH + 6;
        int tabStartY = panelY + 4 + HEADER_HEIGHT;

        addButton(new TabButton(5, tabStartX, tabStartY, tabBtnW, tabBtnH, getTabLabel(5), openSection == 1));
        addButton(new TabButton(6, tabStartX, tabStartY + (tabBtnH + tabGap), tabBtnW, tabBtnH, getTabLabel(6), openSection == 2));
        addButton(new TabButton(7, tabStartX, tabStartY + 2 * (tabBtnH + tabGap), tabBtnW, tabBtnH, getTabLabel(7), openSection == 3));

        createRows();

        addButton(new GuiNpcButton(670, width - 22, 2, 20, 20, "X"));
    }

    private String getTabLabel(int tabId) {
        int sec = tabId - 4;
        return I18n.format(sec == 1 ? "cnpcgeckoaddon.gui.attack" : sec == 2 ? "cnpcgeckoaddon.gui.hurt" : "cnpcgeckoaddon.gui.death");
    }

    private static String soundLabel(String snd) {
        return (snd != null && !snd.isEmpty()) ? "\u266B" : "\u266A";
    }

    private void createRows() {
        switch (openSection) {
            case 1: createAttackRows(); break;
            case 2: createHurtRows(); break;
            case 3: createDeathRows(); break;
        }
    }

    private void createAttackRows() {
        int count = getModelData(npc).getAttackCount();
        int arrLen = getModelData(npc).getAttackAnimNames().length;
        maxAttackScroll = Math.max(0, count - VISIBLE_ROWS);
        if (attackScroll > maxAttackScroll) attackScroll = maxAttackScroll;

        int visibleCount = Math.min(VISIBLE_ROWS, count);
        for (int slot = 0; slot < visibleCount; slot++) {
            int idx = attackScroll + slot;
            if (idx < 0 || idx >= count || idx >= arrLen) continue;
            int rowY = panelY + 4 + HEADER_HEIGHT + slot * ROW_HEIGHT;
            addTextField(new GuiNpcTextField(100 + slot, this, fontRenderer, guiLeft - 35, rowY, 70, 20, getModelData(npc).getAttackAnimNames()[idx]));
            addButton(new GuiNpcButton(400 + slot, guiLeft + 38, rowY, 36, 20, I18n.format("cnpcgeckoaddon.gui.select")));
            GuiNpcTextField wf = new GuiNpcTextField(200 + slot, this, fontRenderer, guiLeft + 76, rowY, 28, 20, "" + getModelData(npc).getAttackWeights()[idx]);
            wf.setNumbersOnly();
            addTextField(wf);
            addTextField(new GuiNpcTextField(300 + slot, this, fontRenderer, guiLeft + 106, rowY, 45, 20, String.format("%.2f", getModelData(npc).getAttackFrames()[idx])));
            addButton(new GuiNpcButton(500 + slot, guiLeft + 154, rowY, 18, 20, "X"));
            addButton(new GuiNpcButton(700 + slot, guiLeft + 174, rowY, 28, 20, soundLabel(getModelData(npc).getAttackSoundNames()[idx])));
        }

        int addBtnX = guiLeft - 87 + PANEL_WIDTH / 2 - 40;
        addButton(new GuiNpcButton(50, addBtnX, panelY + 4 + HEADER_HEIGHT + VISIBLE_ROWS * ROW_HEIGHT + 4, 80, 20, I18n.format("cnpcgeckoaddon.gui.add")));
    }

    private void createHurtRows() {
        int count = getModelData(npc).getHurtAnimCount();
        int arrLen = getModelData(npc).getHurtAnimNames().length;
        maxHurtScroll = Math.max(0, count - VISIBLE_ROWS);
        if (hurtScroll > maxHurtScroll) hurtScroll = maxHurtScroll;

        int visibleCount = Math.min(VISIBLE_ROWS, count);
        for (int slot = 0; slot < visibleCount; slot++) {
            int idx = hurtScroll + slot;
            if (idx < 0 || idx >= count || idx >= arrLen) continue;
            int rowY = panelY + 4 + HEADER_HEIGHT + slot * ROW_HEIGHT;
            addTextField(new GuiNpcTextField(1000 + slot, this, fontRenderer, guiLeft - 35, rowY, 85, 20, getModelData(npc).getHurtAnimNames()[idx]));
            addButton(new GuiNpcButton(1200 + slot, guiLeft + 53, rowY, 36, 20, I18n.format("cnpcgeckoaddon.gui.select")));
            GuiNpcTextField wf = new GuiNpcTextField(1100 + slot, this, fontRenderer, guiLeft + 91, rowY, 40, 20, "" + getModelData(npc).getHurtWeights()[idx]);
            wf.setNumbersOnly();
            addTextField(wf);
            addButton(new GuiNpcButton(1300 + slot, guiLeft + 154, rowY, 18, 20, "X"));
            addButton(new GuiNpcButton(1500 + slot, guiLeft + 174, rowY, 28, 20, soundLabel(getModelData(npc).getHurtSoundNames()[idx])));
        }

        int addBtnX = guiLeft - 87 + PANEL_WIDTH / 2 - 40;
        addButton(new GuiNpcButton(51, addBtnX, panelY + 4 + HEADER_HEIGHT + VISIBLE_ROWS * ROW_HEIGHT + 4, 80, 20, I18n.format("cnpcgeckoaddon.gui.add")));
    }

    private void createDeathRows() {
        int count = getModelData(npc).getDeathAnimCount();
        int arrLen = getModelData(npc).getDeathAnimNames().length;
        maxDeathScroll = Math.max(0, count - VISIBLE_ROWS);
        if (deathScroll > maxDeathScroll) deathScroll = maxDeathScroll;

        int visibleCount = Math.min(VISIBLE_ROWS, count);
        for (int slot = 0; slot < visibleCount; slot++) {
            int idx = deathScroll + slot;
            if (idx < 0 || idx >= count || idx >= arrLen) continue;
            int rowY = panelY + 4 + HEADER_HEIGHT + slot * ROW_HEIGHT;
            addTextField(new GuiNpcTextField(2000 + slot, this, fontRenderer, guiLeft - 35, rowY, 60, 20, getModelData(npc).getDeathAnimNames()[idx]));
            addButton(new GuiNpcButton(2300 + slot, guiLeft + 29, rowY, 40, 20, I18n.format("cnpcgeckoaddon.gui.select")));
            GuiNpcTextField wf = new GuiNpcTextField(2100 + slot, this, fontRenderer, guiLeft + 73, rowY, 30, 20, "" + getModelData(npc).getDeathWeights()[idx]);
            wf.setNumbersOnly();
            addTextField(wf);
            addTextField(new GuiNpcTextField(2200 + slot, this, fontRenderer, guiLeft + 107, rowY, 28, 20, String.format("%.1f", getModelData(npc).getDeathHealthThresholds()[idx])));
            addTextField(new GuiNpcTextField(2500 + slot, this, fontRenderer, guiLeft + 139, rowY, 35, 20, String.format("%.1f", getModelData(npc).getDeathAnimDurations()[idx])));
            addButton(new GuiNpcButton(2400 + slot, guiLeft + 178, rowY, 18, 20, "X"));
        }

        int addBtnX = guiLeft - 87 + PANEL_WIDTH / 2 - 40;
        addButton(new GuiNpcButton(52, addBtnX, panelY + 4 + HEADER_HEIGHT + VISIBLE_ROWS * ROW_HEIGHT + 4, 80, 20, I18n.format("cnpcgeckoaddon.gui.add")));
    }

    private void updateScroll(int newScroll) {
        int count;
        int maxScroll;
        switch (openSection) {
            case 1:
                count = getModelData(npc).getAttackCount();
                maxAttackScroll = Math.max(0, count - VISIBLE_ROWS);
                maxScroll = maxAttackScroll;
                newScroll = Math.max(0, Math.min(newScroll, maxScroll));
                if (newScroll == attackScroll) return;
                attackScroll = newScroll;
                break;
            case 2:
                count = getModelData(npc).getHurtAnimCount();
                maxHurtScroll = Math.max(0, count - VISIBLE_ROWS);
                maxScroll = maxHurtScroll;
                newScroll = Math.max(0, Math.min(newScroll, maxScroll));
                if (newScroll == hurtScroll) return;
                hurtScroll = newScroll;
                break;
            default:
                count = getModelData(npc).getDeathAnimCount();
                maxDeathScroll = Math.max(0, count - VISIBLE_ROWS);
                maxScroll = maxDeathScroll;
                newScroll = Math.max(0, Math.min(newScroll, maxScroll));
                if (newScroll == deathScroll) return;
                deathScroll = newScroll;
                break;
        }

        int visibleCount = Math.min(VISIBLE_ROWS, count);
        for (int slot = 0; slot < visibleCount; slot++) {
            int idx = newScroll + slot;
            updateRowWidgets(slot, idx);
        }
    }

    private void updateRowWidgets(int slot, int logicalIdx) {
        int count;
        int arrLen;
        switch (openSection) {
            case 1: {
                count = getModelData(npc).getAttackCount();
                arrLen = getModelData(npc).getAttackAnimNames().length;
                if (logicalIdx < 0 || logicalIdx >= count || logicalIdx >= arrLen) return;
                GuiNpcTextField nameField = getTextField(100 + slot);
                if (nameField != null) nameField.setText(getModelData(npc).getAttackAnimNames()[logicalIdx]);
                GuiNpcTextField weightField = getTextField(200 + slot);
                if (weightField != null) weightField.setText("" + getModelData(npc).getAttackWeights()[logicalIdx]);
                GuiNpcTextField frameField = getTextField(300 + slot);
                if (frameField != null) frameField.setText(String.format("%.2f", getModelData(npc).getAttackFrames()[logicalIdx]));
                GuiNpcButton soundBtn = getButton(700 + slot);
                if (soundBtn != null) soundBtn.displayString = soundLabel(getModelData(npc).getAttackSoundNames()[logicalIdx]);
                break;
            }
            case 2: {
                count = getModelData(npc).getHurtAnimCount();
                arrLen = getModelData(npc).getHurtAnimNames().length;
                if (logicalIdx < 0 || logicalIdx >= count || logicalIdx >= arrLen) return;
                GuiNpcTextField nameField = getTextField(1000 + slot);
                if (nameField != null) nameField.setText(getModelData(npc).getHurtAnimNames()[logicalIdx]);
                GuiNpcTextField weightField = getTextField(1100 + slot);
                if (weightField != null) weightField.setText("" + getModelData(npc).getHurtWeights()[logicalIdx]);
                GuiNpcButton soundBtn = getButton(1500 + slot);
                if (soundBtn != null) soundBtn.displayString = soundLabel(getModelData(npc).getHurtSoundNames()[logicalIdx]);
                break;
            }
            case 3: {
                count = getModelData(npc).getDeathAnimCount();
                arrLen = getModelData(npc).getDeathAnimNames().length;
                if (logicalIdx < 0 || logicalIdx >= count || logicalIdx >= arrLen) return;
                GuiNpcTextField nameField = getTextField(2000 + slot);
                if (nameField != null) nameField.setText(getModelData(npc).getDeathAnimNames()[logicalIdx]);
                GuiNpcTextField weightField = getTextField(2100 + slot);
                if (weightField != null) weightField.setText("" + getModelData(npc).getDeathWeights()[logicalIdx]);
                GuiNpcTextField hpField = getTextField(2200 + slot);
                if (hpField != null) hpField.setText(String.format("%.1f", getModelData(npc).getDeathHealthThresholds()[logicalIdx]));
                GuiNpcTextField durField = getTextField(2500 + slot);
                if (durField != null) durField.setText(String.format("%.1f", getModelData(npc).getDeathAnimDurations()[logicalIdx]));
                break;
            }
        }
    }

    public ICustomModelData getModelData(EntityNPCInterface npc){
        return npc.getCapability(CustomModelDataProvider.DATA_CAP, null);
    }

    public void addSelectionBlock(int id, int y, String labelKey, String value) {
        addLabel(new GuiNpcLabel(id, I18n.format(labelKey), guiLeft - 85, y + 5, 0xffffff));
        addTextField(new GuiNpcTextField(id, this, fontRenderer, guiLeft - 10, y, 200, 20, value));
        addButton(new GuiNpcButton(id, guiLeft + 193, y, 80, 20, I18n.format("cnpcgeckoaddon.gui.select")));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (hasSubGui()) {
            super.drawScreen(mouseX, mouseY, partialTicks);
            return;
        }

        drawDefaultBackground();
        int panelH = 4 + HEADER_HEIGHT + VISIBLE_ROWS * ROW_HEIGHT + 28;
        int panelX = guiLeft - 87;

        drawRect(panelX, panelY, panelX + PANEL_WIDTH, panelY + panelH, 0xFF333333);
        drawRect(panelX - 1, panelY - 1, panelX + PANEL_WIDTH + 1, panelY, 0xFFFFFFFF);
        drawRect(panelX - 1, panelY + panelH, panelX + PANEL_WIDTH + 1, panelY + panelH + 1, 0xFFFFFFFF);
        drawRect(panelX - 1, panelY - 1, panelX, panelY + panelH + 1, 0xFFFFFFFF);
        drawRect(panelX + PANEL_WIDTH, panelY - 1, panelX + PANEL_WIDTH + 1, panelY + panelH + 1, 0xFFFFFFFF);

        super.drawScreen(mouseX, mouseY, partialTicks);

        if (openSection == 0) return;

        int count;
        int scrollOffset;
        String labelPrefix;
        String sectionLabelKey;
        int colHeaderY = panelY + 4 + 5;
        switch (openSection) {
            case 1:
                count = getModelData(npc).getAttackCount(); scrollOffset = attackScroll; labelPrefix = "attack"; sectionLabelKey = "cnpcgeckoaddon.gui.attack";
                break;
            case 2:
                count = getModelData(npc).getHurtAnimCount(); scrollOffset = hurtScroll; labelPrefix = "hurt"; sectionLabelKey = "cnpcgeckoaddon.gui.hurt";
                break;
            default:
                count = getModelData(npc).getDeathAnimCount(); scrollOffset = deathScroll; labelPrefix = "death"; sectionLabelKey = "cnpcgeckoaddon.gui.death";
                break;
        }

        drawString(fontRenderer, I18n.format(sectionLabelKey), panelX + 4, colHeaderY, 0xFFFFFF);
        drawString(fontRenderer, I18n.format("cnpcgeckoaddon.gui.col_name"), guiLeft - 35, colHeaderY, 0x888888);
        if (openSection == 1) {
            drawString(fontRenderer, I18n.format("cnpcgeckoaddon.gui.col_weight"), guiLeft + 76, colHeaderY, 0x888888);
            drawString(fontRenderer, I18n.format("cnpcgeckoaddon.gui.col_frame"), guiLeft + 106, colHeaderY, 0x888888);
            drawString(fontRenderer, I18n.format("cnpcgeckoaddon.gui.col_sound"), guiLeft + 174, colHeaderY, 0x888888);
        } else if (openSection == 2) {
            drawString(fontRenderer, I18n.format("cnpcgeckoaddon.gui.col_weight"), guiLeft + 91, colHeaderY, 0x888888);
            drawString(fontRenderer, I18n.format("cnpcgeckoaddon.gui.col_sound"), guiLeft + 174, colHeaderY, 0x888888);
        } else if (openSection == 3) {
            drawString(fontRenderer, I18n.format("cnpcgeckoaddon.gui.col_weight"), guiLeft + 73, colHeaderY, 0x888888);
            drawString(fontRenderer, I18n.format("cnpcgeckoaddon.gui.col_hp") + "%", guiLeft + 107, colHeaderY, 0x888888);
            drawString(fontRenderer, I18n.format("cnpcgeckoaddon.gui.death_duration"), guiLeft + 139, colHeaderY, 0x888888);
        }

        int contentTop = panelY + 4 + HEADER_HEIGHT;
        int contentH = VISIBLE_ROWS * ROW_HEIGHT;

        int arrLen = count;
        if (openSection == 1) arrLen = getModelData(npc).getAttackAnimNames().length;
        else if (openSection == 2) arrLen = getModelData(npc).getHurtAnimNames().length;
        else if (openSection == 3) arrLen = getModelData(npc).getDeathAnimNames().length;

        for (int slot = 0; slot < VISIBLE_ROWS; slot++) {
            int idx = scrollOffset + slot;
            if (idx >= 0 && idx < count && idx < arrLen) {
                int rowY = panelY + 4 + HEADER_HEIGHT + slot * ROW_HEIGHT;
                if (rowY + ROW_HEIGHT > contentTop && rowY < contentTop + contentH) {
                    drawString(fontRenderer, labelPrefix + (idx + 1) + ":", panelX + 4, rowY + 5, 0xFFFFFF);
                }
            }
        }

        int maxScroll;
        switch (openSection) {
            case 1: maxScroll = maxAttackScroll; break;
            case 2: maxScroll = maxHurtScroll; break;
            default: maxScroll = maxDeathScroll; break;
        }

        if (maxScroll > 0) {
            int sbX = panelX + PANEL_WIDTH - 5;
            drawRect(sbX, contentTop, sbX + 5, contentTop + contentH, 0x55FFFFFF);
            float ratio = (float) scrollOffset / maxScroll;
            int totalH = count * ROW_HEIGHT;
            int thumbH = Math.max(8, (int) (contentH * (float) contentH / totalH));
            int thumbY = contentTop + (int) (ratio * (contentH - thumbH));
            drawRect(sbX, thumbY, sbX + 5, thumbY + thumbH, 0xFFFFFFFF);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        if (openSection == 0) return;
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            int contentX = guiLeft - 87;
            int contentY = panelY + 4 + HEADER_HEIGHT;
            int contentH = VISIBLE_ROWS * ROW_HEIGHT;
            if (Mouse.isInsideWindow()) {
                int mouseX = Mouse.getX() * width / Minecraft.getMinecraft().displayWidth;
                int mouseY = height - Mouse.getY() * height / Minecraft.getMinecraft().displayHeight - 1;
                if (mouseX >= contentX && mouseX <= contentX + PANEL_WIDTH
                        && mouseY >= contentY && mouseY <= contentY + contentH) {
                    int step = wheel > 0 ? 1 : -1;
                    int currentScroll, maxScroll;
                    switch (openSection) {
                        case 1: currentScroll = attackScroll; maxScroll = maxAttackScroll; break;
                        case 2: currentScroll = hurtScroll; maxScroll = maxHurtScroll; break;
                        default: currentScroll = deathScroll; maxScroll = maxDeathScroll; break;
                    }
                    updateScroll(currentScroll - step);
                }
            }
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (openSection == 0) return;
        int panelX = guiLeft - 87;
        int sbX = panelX + PANEL_WIDTH - 5;
        int sbTop = panelY + 4 + HEADER_HEIGHT;
        int sbH = VISIBLE_ROWS * ROW_HEIGHT;
        int count;
        int maxScroll;
        switch (openSection) {
            case 1: count = getModelData(npc).getAttackCount(); maxAttackScroll = Math.max(0, count - VISIBLE_ROWS); maxScroll = maxAttackScroll; break;
            case 2: count = getModelData(npc).getHurtAnimCount(); maxHurtScroll = Math.max(0, count - VISIBLE_ROWS); maxScroll = maxHurtScroll; break;
            default: count = getModelData(npc).getDeathAnimCount(); maxDeathScroll = Math.max(0, count - VISIBLE_ROWS); maxScroll = maxDeathScroll; break;
        }
        if (maxScroll > 0 && mouseX >= sbX && mouseX <= sbX + 5 && mouseY >= sbTop && mouseY <= sbTop + sbH) {
            float ratio = (float) (mouseY - sbTop) / sbH;
            int newScroll = (int) (ratio * maxScroll);
            updateScroll(newScroll);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        super.actionPerformed(button);

        if (button.id == 670) {
            close();
            return;
        }

        if (button.id == 1) {
            NoppesUtil.openGUI(this.player, new GuiStringSelection(this, I18n.format("cnpcgeckoaddon.gui.select_anim_file"),
                    AnimationFileUtil.getAnimationFileList(), (name) -> getModelData(npc).setAnimFile(name)));
            return;
        }
        if (button.id == 2) {
            NoppesUtil.openGUI(this.player, new GuiStringSelection(this, I18n.format("cnpcgeckoaddon.gui.select_idle"),
                    AnimationFileUtil.getAnimationList(getModelData(npc).getAnimFile()),
                    (name) -> getModelData(npc).setIdleAnim(name)));
            return;
        }
        if (button.id == 3) {
            NoppesUtil.openGUI(this.player, new GuiStringSelection(this, I18n.format("cnpcgeckoaddon.gui.select_walk"),
                    AnimationFileUtil.getAnimationList(getModelData(npc).getAnimFile()),
                    (name) -> getModelData(npc).setWalkAnim(name)));
            return;
        }

        if (button.id == 5 || button.id == 6 || button.id == 7) {
            int section = button.id - 4;
            if (openSection == section) {
                openSection = 0;
            } else {
                openSection = section;
            }
            initGui();
            return;
        }

        if (button.id == 50) {
            getModelData(npc).addAttack();
            initGui();
            return;
        }
        if (button.id == 51) {
            getModelData(npc).addHurtAnim();
            initGui();
            return;
        }
        if (button.id == 52) {
            getModelData(npc).addDeathAnim();
            initGui();
            return;
        }

        if (button.id >= 400 && button.id < 400 + VISIBLE_ROWS) {
            int slot = button.id - 400;
            int idx = attackScroll + slot;
            if (idx < getModelData(npc).getAttackCount()) {
                int finalIdx = idx;
                NoppesUtil.openGUI(this.player, new GuiStringSelection(this, I18n.format("cnpcgeckoaddon.gui.select_melee"),
                        AnimationFileUtil.getAnimationList(getModelData(npc).getAnimFile()),
                        (name) -> {
                            getModelData(npc).getAttackAnimNames()[finalIdx] = name;
                            GuiNpcTextField field = getTextField(100 + slot);
                            if (field != null) field.setText(name);
                        }));
            }
            return;
        }

        if (button.id >= 500 && button.id < 500 + VISIBLE_ROWS) {
            int slot = button.id - 500;
            int idx = attackScroll + slot;
            if (idx < getModelData(npc).getAttackCount()) {
                getModelData(npc).removeAttack(idx);
                initGui();
            }
            return;
        }

        if (button.id >= 700 && button.id < 700 + VISIBLE_ROWS) {
            int slot = button.id - 700;
            int idx = attackScroll + slot;
            if (idx < getModelData(npc).getAttackCount()) {
                openSoundPicker(true, idx, getModelData(npc).getAttackSoundNames()[idx]);
            }
            return;
        }

        if (button.id >= 1200 && button.id < 1200 + VISIBLE_ROWS) {
            int slot = button.id - 1200;
            int idx = hurtScroll + slot;
            if (idx < getModelData(npc).getHurtAnimCount()) {
                int finalIdx = idx;
                NoppesUtil.openGUI(this.player, new GuiStringSelection(this, I18n.format("cnpcgeckoaddon.gui.select_hurt"),
                        AnimationFileUtil.getAnimationList(getModelData(npc).getAnimFile()),
                        (name) -> {
                            getModelData(npc).getHurtAnimNames()[finalIdx] = name;
                            GuiNpcTextField field = getTextField(1000 + slot);
                            if (field != null) field.setText(name);
                        }));
            }
            return;
        }

        if (button.id >= 1300 && button.id < 1300 + VISIBLE_ROWS) {
            int slot = button.id - 1300;
            int idx = hurtScroll + slot;
            if (idx < getModelData(npc).getHurtAnimCount()) {
                getModelData(npc).removeHurtAnim(idx);
                initGui();
            }
            return;
        }

        if (button.id >= 1500 && button.id < 1500 + VISIBLE_ROWS) {
            int slot = button.id - 1500;
            int idx = hurtScroll + slot;
            if (idx < getModelData(npc).getHurtAnimCount()) {
                openSoundPicker(false, idx, getModelData(npc).getHurtSoundNames()[idx]);
            }
            return;
        }

        if (button.id >= 2300 && button.id < 2300 + VISIBLE_ROWS) {
            int slot = button.id - 2300;
            int idx = deathScroll + slot;
            if (idx < getModelData(npc).getDeathAnimCount()) {
                int finalIdx = idx;
                NoppesUtil.openGUI(this.player, new GuiStringSelection(this, I18n.format("cnpcgeckoaddon.gui.select_hurt"),
                        AnimationFileUtil.getAnimationList(getModelData(npc).getAnimFile()),
                        (name) -> {
                            getModelData(npc).getDeathAnimNames()[finalIdx] = name;
                            GuiNpcTextField field = getTextField(2000 + slot);
                            if (field != null) field.setText(name);
                        }));
            }
            return;
        }

        if (button.id >= 2400 && button.id < 2400 + VISIBLE_ROWS) {
            int slot = button.id - 2400;
            int idx = deathScroll + slot;
            if (idx < getModelData(npc).getDeathAnimCount()) {
                getModelData(npc).removeDeathAnim(idx);
                initGui();
            }
            return;
        }
    }

    public boolean isValidAnimFile(String name){
        if (name == null || name.isEmpty()) return false;
        return GeckoLibCache.getInstance().getAnimations().containsKey(new ResourceLocation(name));
    }

    public boolean isValidAnimation(String name){
        if (name == null || name.isEmpty()) return true;
        return AnimationFileUtil.getAnimationList(getModelData(npc).getAnimFile()).contains(name);
    }

    private int logicalIndexFromField(int fieldId) {
        int slot;
        if (fieldId >= 100 && fieldId < 100 + VISIBLE_ROWS) {
            slot = fieldId - 100;
            return attackScroll + slot;
        }
        if (fieldId >= 200 && fieldId < 200 + VISIBLE_ROWS) {
            slot = fieldId - 200;
            return attackScroll + slot;
        }
        if (fieldId >= 300 && fieldId < 300 + VISIBLE_ROWS) {
            slot = fieldId - 300;
            return attackScroll + slot;
        }
        if (fieldId >= 1000 && fieldId < 1000 + VISIBLE_ROWS) {
            slot = fieldId - 1000;
            return hurtScroll + slot;
        }
        if (fieldId >= 1100 && fieldId < 1100 + VISIBLE_ROWS) {
            slot = fieldId - 1100;
            return hurtScroll + slot;
        }
        if (fieldId >= 2000 && fieldId < 2000 + VISIBLE_ROWS) {
            slot = fieldId - 2000;
            return deathScroll + slot;
        }
        if (fieldId >= 2100 && fieldId < 2100 + VISIBLE_ROWS) {
            slot = fieldId - 2100;
            return deathScroll + slot;
        }
        if (fieldId >= 2200 && fieldId < 2200 + VISIBLE_ROWS) {
            slot = fieldId - 2200;
            return deathScroll + slot;
        }
        if (fieldId >= 2500 && fieldId < 2500 + VISIBLE_ROWS) {
            slot = fieldId - 2500;
            return deathScroll + slot;
        }
        return -1;
    }

    @Override
    public void unFocused(GuiNpcTextField textfield) {
        int id = textfield.getId();
        if (id == 1) {
            if (isValidAnimFile(textfield.getText()))
                getModelData(npc).setAnimFile(textfield.getText());
            else
                textfield.setText(getModelData(npc).getAnimFile());
            return;
        }
        if (id == 2) {
            if (isValidAnimation(textfield.getText()))
                getModelData(npc).setIdleAnim(textfield.getText());
            else
                textfield.setText(getModelData(npc).getIdleAnim());
            return;
        }
        if (id == 3) {
            if (isValidAnimation(textfield.getText()))
                getModelData(npc).setWalkAnim(textfield.getText());
            else
                textfield.setText(getModelData(npc).getWalkAnim());
            return;
        }

        int idx = logicalIndexFromField(id);
        if (idx < 0) return;

        if (id >= 100 && id < 100 + VISIBLE_ROWS) {
            if (idx < getModelData(npc).getAttackCount()) {
                if (isValidAnimation(textfield.getText()))
                    getModelData(npc).getAttackAnimNames()[idx] = textfield.getText();
                else
                    textfield.setText(getModelData(npc).getAttackAnimNames()[idx]);
            }
            return;
        }
        if (id >= 200 && id < 200 + VISIBLE_ROWS) {
            if (idx < getModelData(npc).getAttackCount()) {
                FloatTextFieldUtils.performFloatChecks(0, 100, 1, textfield);
                getModelData(npc).getAttackWeights()[idx] = (int) FloatTextFieldUtils.getFloat(textfield);
            }
            return;
        }
        if (id >= 300 && id < 300 + VISIBLE_ROWS) {
            if (idx < getModelData(npc).getAttackCount()) {
                FloatTextFieldUtils.performFloatChecks(0, 1000, 0, textfield);
                getModelData(npc).getAttackFrames()[idx] = FloatTextFieldUtils.getFloat(textfield);
            }
            return;
        }

        if (id >= 1000 && id < 1000 + VISIBLE_ROWS) {
            if (idx < getModelData(npc).getHurtAnimCount()) {
                if (isValidAnimation(textfield.getText()))
                    getModelData(npc).getHurtAnimNames()[idx] = textfield.getText();
                else
                    textfield.setText(getModelData(npc).getHurtAnimNames()[idx]);
            }
            return;
        }
        if (id >= 1100 && id < 1100 + VISIBLE_ROWS) {
            if (idx < getModelData(npc).getHurtAnimCount()) {
                FloatTextFieldUtils.performFloatChecks(0, 100, 1, textfield);
                getModelData(npc).getHurtWeights()[idx] = (int) FloatTextFieldUtils.getFloat(textfield);
            }
            return;
        }

        if (id >= 2000 && id < 2000 + VISIBLE_ROWS) {
            if (idx < getModelData(npc).getDeathAnimCount()) {
                if (isValidAnimation(textfield.getText()))
                    getModelData(npc).getDeathAnimNames()[idx] = textfield.getText();
                else
                    textfield.setText(getModelData(npc).getDeathAnimNames()[idx]);
            }
            return;
        }
        if (id >= 2100 && id < 2100 + VISIBLE_ROWS) {
            if (idx < getModelData(npc).getDeathAnimCount()) {
                FloatTextFieldUtils.performFloatChecks(0, 100, 1, textfield);
                getModelData(npc).getDeathWeights()[idx] = (int) FloatTextFieldUtils.getFloat(textfield);
            }
            return;
        }
        if (id >= 2200 && id < 2200 + VISIBLE_ROWS) {
            if (idx < getModelData(npc).getDeathAnimCount()) {
                FloatTextFieldUtils.performFloatChecks(0.1f, 100, 100f, textfield);
                getModelData(npc).getDeathHealthThresholds()[idx] = FloatTextFieldUtils.getFloat(textfield);
            }
            return;
        }
        if (id >= 2500 && id < 2500 + VISIBLE_ROWS) {
            if (idx < getModelData(npc).getDeathAnimCount()) {
                FloatTextFieldUtils.performFloatChecks(0.1f, 9999f, 5f, textfield);
                getModelData(npc).getDeathAnimDurations()[idx] = FloatTextFieldUtils.getFloat(textfield);
            }
            return;
        }
    }

    private void openSoundPicker(boolean isAttack, int index, String currentSound) {
        pendingSoundIsAttack = isAttack;
        pendingSoundIndex = index;
        setSubGui(new GuiSoundSelection(currentSound != null ? currentSound : ""));
    }

    @Override
    public void subGuiClosed(SubGuiInterface subGuiInterface) {
        if (subGuiInterface instanceof GuiSoundSelection && pendingSoundIndex >= 0) {
            GuiSoundSelection gss = (GuiSoundSelection) subGuiInterface;
            if (gss.selectedResource != null) {
                String result = gss.selectedResource.toString();
                if (pendingSoundIsAttack) {
                    if (pendingSoundIndex < getModelData(npc).getAttackCount()) {
                        getModelData(npc).getAttackSoundNames()[pendingSoundIndex] = result;
                    }
                } else {
                    if (pendingSoundIndex < getModelData(npc).getHurtAnimCount()) {
                        getModelData(npc).getHurtSoundNames()[pendingSoundIndex] = result;
                    }
                }
            }
            pendingSoundIndex = -1;
        }
        initGui();
    }

    @Override
    public void close() {
        super.close();
        NoppesUtil.openGUI(this.player,parent);
    }

    private static class TabButton extends GuiNpcButton {
        public boolean selected;

        TabButton(int id, int x, int y, int w, int h, String label, boolean selected) {
            super(id, x, y, w, h, label);
            this.selected = selected;
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (selected) {
                boolean old = this.enabled;
                this.enabled = false;
                super.drawButton(mc, mouseX, mouseY, partialTicks);
                this.enabled = old;
            } else {
                super.drawButton(mc, mouseX, mouseY, partialTicks);
            }
        }
    }
}
