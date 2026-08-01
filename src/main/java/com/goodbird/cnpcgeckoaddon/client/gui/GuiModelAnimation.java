package com.goodbird.cnpcgeckoaddon.client.gui;

import com.goodbird.cnpcgeckoaddon.data.CustomModelData;
import com.goodbird.cnpcgeckoaddon.mixin.IDataDisplay;
import com.goodbird.cnpcgeckoaddon.utils.FloatTextFieldUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import noppes.npcs.client.gui.select.GuiSoundSelection;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiLabel;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.IGuiInterface;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;
import com.goodbird.cnpcgeckoaddon.utils.AnimationFileUtil;
import software.bernie.geckolib.cache.GeckoLibCache;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GuiModelAnimation extends GuiNPCInterface implements ITextfieldListener {

    private static final int ROW_HEIGHT = 22;
    private static final int HEADER_HEIGHT = 22;
    private static final int VISIBLE_ROWS = 5;
    private static final int PANEL_WIDTH = 328;
    private static final int OFFSCREEN = -9999;

    private int openSection = 1;
    private int panelY;
    private int attackScroll = 0, hurtScroll = 0, deathScroll = 0;
    private int maxAttackScroll = 0, maxHurtScroll = 0, maxDeathScroll = 0;

    private List<Row> rows = new ArrayList<>();
    private GuiButtonNop addButton;

    private static class Row {
        int index;
        GuiTextFieldNop nameField;
        GuiButtonNop selectBtn;
        GuiTextFieldNop weightField;
        GuiTextFieldNop extraField;
        GuiButtonNop deleteBtn;
        GuiButtonNop soundBtn;
    }

    @Override
    public void init() {
        if (openSection == 0) openSection = 1;
        super.init();

        addButton(new GuiButtonNop(this, 670, width - 22, 2, 20, 20, "X"));

        int y = guiTop + 4;

        addSelectionBlock(1, y, "cnpcgeckoaddon.gui.animfile", getModelData(npc).getAnimFile());
        addSelectionBlock(2, y += 23, "cnpcgeckoaddon.gui.idle", getModelData(npc).getIdleAnim());
        addSelectionBlock(3, y += 23, "cnpcgeckoaddon.gui.walk", getModelData(npc).getWalkAnim());

        panelY = y + 20;
        rows.clear();

        int tabBtnW = 50;
        int tabBtnH = 20;
        int tabGap = 4;
        int tabStartX = guiLeft - 85 + PANEL_WIDTH + 6;
        int tabStartY = panelY + 4 + HEADER_HEIGHT;

        addButton(new TabButton(this, 5, tabStartX, tabStartY, tabBtnW, tabBtnH, getTabLabel(5), openSection == 1));
        addButton(new TabButton(this, 6, tabStartX, tabStartY + (tabBtnH + tabGap), tabBtnW, tabBtnH, getTabLabel(6), openSection == 2));
        addButton(new TabButton(this, 7, tabStartX, tabStartY + 2 * (tabBtnH + tabGap), tabBtnW, tabBtnH, getTabLabel(7), openSection == 3));

        switch (openSection) {
            case 1: createAttackSection(); break;
            case 2: createHurtSection(); break;
            case 3: createDeathSection(); break;
        }
    }

    private String getTabLabel(int tabId) {
        int sec = tabId - 4;
        return Component.translatable(sec == 1 ? "cnpcgeckoaddon.gui.attack" : sec == 2 ? "cnpcgeckoaddon.gui.hurt" : "cnpcgeckoaddon.gui.death").getString();
    }

    private void createAttackSection() {
        int count = getModelData(npc).getAttackCount();
        maxAttackScroll = Math.max(0, count * ROW_HEIGHT - VISIBLE_ROWS * ROW_HEIGHT);
        if (attackScroll > maxAttackScroll) attackScroll = maxAttackScroll;

        for (int i = 0; i < count; i++) {
            Row row = new Row();
            row.index = i;
            int rowY = panelY + 4 + HEADER_HEIGHT + i * ROW_HEIGHT;

            row.nameField = new GuiTextFieldNop(100 + i, this, guiLeft - 35, rowY, 76, 18, getModelData(npc).getAttackAnimNames()[i]);
            addTextField(row.nameField);

            row.selectBtn = new GuiButtonNop(this, 400 + i, guiLeft + 44, rowY, 36, 18, "mco.template.button.select");
            addButton(row.selectBtn);

            GuiTextFieldNop wf = new GuiTextFieldNop(200 + i, this, guiLeft + 83, rowY, 26, 18, "" + getModelData(npc).getAttackWeights()[i]);
            wf.setNumbersOnly();
            wf.setMinMaxDefault(0, 100, 1);
            addTextField(wf);
            row.weightField = wf;

            row.extraField = new GuiTextFieldNop(300 + i, this, guiLeft + 112, rowY, 48, 18, String.format("%.2f", getModelData(npc).getAttackFrames()[i]));
            addTextField(row.extraField);

            row.deleteBtn = new GuiButtonNop(this, 500 + i, guiLeft + 163, rowY, 18, 18, "X");
            addButton(row.deleteBtn);

            String snd = getModelData(npc).getAttackSoundNames()[i];
            String sndLabel = (snd != null && !snd.isEmpty()) ? "\u266B" : "\u266A";
            row.soundBtn = new GuiButtonNop(this, 700 + i, guiLeft + 184, rowY, 36, 18, sndLabel);
            addButton(row.soundBtn);

            rows.add(row);
        }

        updatePositions(count, attackScroll);

        int addBtnX = guiLeft - 85 + PANEL_WIDTH / 2 - 40;
        addButton = new GuiButtonNop(this, 50, addBtnX, panelY + 4 + HEADER_HEIGHT + VISIBLE_ROWS * ROW_HEIGHT + 4, 80, 20,
                Component.translatable("cnpcgeckoaddon.gui.add").getString());
        addButton(addButton);
    }

    private void createHurtSection() {
        int count = getModelData(npc).getHurtAnimCount();
        maxHurtScroll = Math.max(0, count * ROW_HEIGHT - VISIBLE_ROWS * ROW_HEIGHT);
        if (hurtScroll > maxHurtScroll) hurtScroll = maxHurtScroll;

        for (int i = 0; i < count; i++) {
            Row row = new Row();
            row.index = i;
            int rowY = panelY + 4 + HEADER_HEIGHT + i * ROW_HEIGHT;

            row.nameField = new GuiTextFieldNop(1000 + i, this, guiLeft - 35, rowY, 124, 18, getModelData(npc).getHurtAnimNames()[i]);
            addTextField(row.nameField);

            row.selectBtn = new GuiButtonNop(this, 1200 + i, guiLeft + 92, rowY, 36, 18, "mco.template.button.select");
            addButton(row.selectBtn);

            GuiTextFieldNop wf = new GuiTextFieldNop(1100 + i, this, guiLeft + 131, rowY, 36, 18, "" + getModelData(npc).getHurtWeights()[i]);
            wf.setNumbersOnly();
            wf.setMinMaxDefault(0, 100, 1);
            addTextField(wf);
            row.weightField = wf;

            row.extraField = null;

            row.deleteBtn = new GuiButtonNop(this, 1300 + i, guiLeft + 170, rowY, 18, 18, "X");
            addButton(row.deleteBtn);

            String snd = getModelData(npc).getHurtSoundNames()[i];
            String sndLabel = (snd != null && !snd.isEmpty()) ? "\u266B" : "\u266A";
            row.soundBtn = new GuiButtonNop(this, 1500 + i, guiLeft + 191, rowY, 36, 18, sndLabel);
            addButton(row.soundBtn);

            rows.add(row);
        }

        updatePositions(count, hurtScroll);

        int addBtnX = guiLeft - 85 + PANEL_WIDTH / 2 - 40;
        addButton = new GuiButtonNop(this, 51, addBtnX, panelY + 4 + HEADER_HEIGHT + VISIBLE_ROWS * ROW_HEIGHT + 4, 80, 20,
                Component.translatable("cnpcgeckoaddon.gui.add").getString());
        addButton(addButton);
    }

    private void createDeathSection() {
        int count = getModelData(npc).getDeathAnimCount();
        maxDeathScroll = Math.max(0, count * ROW_HEIGHT - VISIBLE_ROWS * ROW_HEIGHT);
        if (deathScroll > maxDeathScroll) deathScroll = maxDeathScroll;

        for (int i = 0; i < count; i++) {
            Row row = new Row();
            row.index = i;
            int rowY = panelY + 4 + HEADER_HEIGHT + i * ROW_HEIGHT;

            row.nameField = new GuiTextFieldNop(2000 + i, this, guiLeft - 35, rowY, 42, 20, getModelData(npc).getDeathAnimNames()[i]);
            addTextField(row.nameField);

            row.selectBtn = new GuiButtonNop(this, 2300 + i, guiLeft + 10, rowY, 40, 20, "mco.template.button.select");
            addButton(row.selectBtn);

            GuiTextFieldNop wf = new GuiTextFieldNop(2100 + i, this, guiLeft + 53, rowY, 30, 20, "" + getModelData(npc).getDeathWeights()[i]);
            wf.setNumbersOnly();
            wf.setMinMaxDefault(0, 100, 1);
            addTextField(wf);
            row.weightField = wf;

            row.extraField = new GuiTextFieldNop(2200 + i, this, guiLeft + 86, rowY, 35, 20, String.format("%.1f", getModelData(npc).getDeathHealthThresholds()[i]));
            addTextField(row.extraField);

            GuiTextFieldNop df = new GuiTextFieldNop(2500 + i, this, guiLeft + 124, rowY, 35, 20, String.format("%.1f", getModelData(npc).getDeathAnimDurations()[i]));
            addTextField(df);

            row.deleteBtn = new GuiButtonNop(this, 2400 + i, guiLeft + 163, rowY, 18, 20, "X");
            addButton(row.deleteBtn);

            rows.add(row);
        }

        updatePositions(count, deathScroll);

        int addBtnX = guiLeft - 85 + PANEL_WIDTH / 2 - 40;
        int addBtnY = panelY + 4 + HEADER_HEIGHT + VISIBLE_ROWS * ROW_HEIGHT + 4;
        addButton = new GuiButtonNop(this, 52, addBtnX, addBtnY, 80, 20,
                Component.translatable("cnpcgeckoaddon.gui.add").getString());
        addButton(addButton);
    }

    private void updatePositions(int count, int scrollOffset) {
        int contentTop = panelY + 4 + HEADER_HEIGHT;
        int contentBottom = panelY + 4 + HEADER_HEIGHT + VISIBLE_ROWS * ROW_HEIGHT;
        for (int i = 0; i < count; i++) {
            int rowY = panelY + 4 + HEADER_HEIGHT + i * ROW_HEIGHT - scrollOffset;
            boolean visible = rowY + ROW_HEIGHT > contentTop && rowY < contentBottom;
            int widgetY = visible ? rowY : OFFSCREEN;
            Row row = rows.get(i);
            row.nameField.setY(widgetY);
            row.selectBtn.setY(widgetY);
            row.weightField.setY(widgetY);
            if (row.extraField != null) row.extraField.setY(widgetY);
            row.deleteBtn.setY(widgetY);
            if (row.soundBtn != null) row.soundBtn.setY(widgetY);
        }
    }

    public CustomModelData getModelData(EntityNPCInterface npc) {
        return ((IDataDisplay) npc.display).getCustomModelData();
    }

    public void addSelectionBlock(int id, int y, String labelKey, String value) {
        addLabel(new GuiLabel(id, Component.translatable(labelKey).getString(), guiLeft - 85, y + 5, 0xffffff));
        addTextField(new GuiTextFieldNop(id, this, guiLeft - 40, y, 200, 20, value));
        addButton(new GuiButtonNop(this, id, guiLeft + 163, y, 80, 20, "mco.template.button.select"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int panelH = 4 + HEADER_HEIGHT + VISIBLE_ROWS * ROW_HEIGHT + 28;
        int panelX = guiLeft - 85;

        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + panelH, 0xFF333333);
        graphics.fill(panelX - 1, panelY - 1, panelX + PANEL_WIDTH + 1, panelY, 0xFFFFFFFF);
        graphics.fill(panelX - 1, panelY + panelH, panelX + PANEL_WIDTH + 1, panelY + panelH + 1, 0xFFFFFFFF);
        graphics.fill(panelX - 1, panelY - 1, panelX, panelY + panelH + 1, 0xFFFFFFFF);
        graphics.fill(panelX + PANEL_WIDTH, panelY - 1, panelX + PANEL_WIDTH + 1, panelY + panelH + 1, 0xFFFFFFFF);

        super.render(graphics, mouseX, mouseY, partialTicks);

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

        graphics.drawString(font, Component.translatable(sectionLabelKey).getString(), panelX + 4, colHeaderY, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("cnpcgeckoaddon.gui.col_name").getString(), guiLeft - 35, colHeaderY, 0x888888);
        if (openSection == 1) {
            graphics.drawString(font, Component.translatable("cnpcgeckoaddon.gui.col_weight").getString(), guiLeft + 83, colHeaderY, 0x888888);
            graphics.drawString(font, Component.translatable("cnpcgeckoaddon.gui.col_frame").getString(), guiLeft + 112, colHeaderY, 0x888888);
        } else if (openSection == 2) {
            graphics.drawString(font, Component.translatable("cnpcgeckoaddon.gui.col_weight").getString(), guiLeft + 131, colHeaderY, 0x888888);
        } else if (openSection == 3) {
            graphics.drawString(font, Component.translatable("cnpcgeckoaddon.gui.col_weight").getString(), guiLeft + 53, colHeaderY, 0x888888);
            graphics.drawString(font, Component.translatable("cnpcgeckoaddon.gui.col_hp").getString(), guiLeft + 86, colHeaderY, 0x888888);
            graphics.drawString(font, Component.translatable("cnpcgeckoaddon.gui.death_duration").getString(), guiLeft + 124, colHeaderY, 0x888888);
        }

        int contentTop = panelY + 4 + HEADER_HEIGHT;
        int contentH = VISIBLE_ROWS * ROW_HEIGHT;

        for (int i = 0; i < count; i++) {
            int rowY = panelY + 4 + HEADER_HEIGHT + i * ROW_HEIGHT - scrollOffset;
            if (rowY + ROW_HEIGHT > contentTop && rowY < contentTop + contentH) {
                graphics.drawString(font, labelPrefix + (i + 1) + ":", panelX + 4, rowY + 5, 0xFFFFFF);
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
            graphics.fill(sbX, contentTop, sbX + 5, contentTop + contentH, 0x55FFFFFF);
            float ratio = (float) scrollOffset / maxScroll;
            int totalH = count * ROW_HEIGHT;
            int thumbH = Math.max(8, (int) (contentH * (float) contentH / totalH));
            int thumbY = contentTop + (int) (ratio * (contentH - thumbH));
            graphics.fill(sbX, thumbY, sbX + 5, thumbY + thumbH, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (openSection == 0) return super.mouseScrolled(mouseX, mouseY, delta);
        int contentX = guiLeft - 85;
        int contentY = panelY + 4 + HEADER_HEIGHT;
        int contentH = VISIBLE_ROWS * ROW_HEIGHT;
        if (mouseX >= contentX && mouseX <= contentX + PANEL_WIDTH
                && mouseY >= contentY && mouseY <= contentY + contentH) {
            int step = (int) delta * ROW_HEIGHT;
            switch (openSection) {
                case 1:
                    attackScroll = Math.max(0, Math.min(attackScroll - step, maxAttackScroll));
                    updatePositions(getModelData(npc).getAttackCount(), attackScroll);
                    break;
                case 2:
                    hurtScroll = Math.max(0, Math.min(hurtScroll - step, maxHurtScroll));
                    updatePositions(getModelData(npc).getHurtAnimCount(), hurtScroll);
                    break;
                case 3:
                    deathScroll = Math.max(0, Math.min(deathScroll - step, maxDeathScroll));
                    updatePositions(getModelData(npc).getDeathAnimCount(), deathScroll);
                    break;
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (openSection == 0) return super.mouseClicked(mouseX, mouseY, button);
        int sbX = guiLeft - 85 + PANEL_WIDTH - 5;
        int sbTop = panelY + 4 + HEADER_HEIGHT;
        int sbH = VISIBLE_ROWS * ROW_HEIGHT;
        int maxScroll;
        int count;
        switch (openSection) {
            case 1: maxScroll = maxAttackScroll; count = getModelData(npc).getAttackCount(); break;
            case 2: maxScroll = maxHurtScroll; count = getModelData(npc).getHurtAnimCount(); break;
            default: maxScroll = maxDeathScroll; count = getModelData(npc).getDeathAnimCount(); break;
        }
        if (maxScroll > 0 && mouseX >= sbX && mouseX <= sbX + 5 && mouseY >= sbTop && mouseY <= sbTop + sbH) {
            float ratio = (float) (mouseY - sbTop) / sbH;
            int newScroll = (int) (ratio * maxScroll);
            switch (openSection) {
                case 1:
                    attackScroll = Math.max(0, Math.min(newScroll, maxAttackScroll));
                    updatePositions(count, attackScroll);
                    break;
                case 2:
                    hurtScroll = Math.max(0, Math.min(newScroll, maxHurtScroll));
                    updatePositions(count, hurtScroll);
                    break;
                case 3:
                    deathScroll = Math.max(0, Math.min(newScroll, maxDeathScroll));
                    updatePositions(count, deathScroll);
                    break;
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void buttonEvent(GuiButtonNop button) {
        if (button.id == 670) {
            close();
            return;
        }

        if (button.id == 1) {
            setSubGui(new GuiStringSelection(this, "Selecting geckolib animation file:",
                    AnimationFileUtil.getAnimationFileList(),
                    (name) -> getModelData(npc).setAnimFile(name)));
            return;
        }
        if (button.id == 2) {
            setSubGui(new GuiStringSelection(this, "Selecting geckolib idle animation:",
                    AnimationFileUtil.getAnimationList(getModelData(npc).getAnimFile()),
                    (name) -> getModelData(npc).setIdleAnim(name)));
            return;
        }
        if (button.id == 3) {
            setSubGui(new GuiStringSelection(this, "Selecting geckolib walk animation:",
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
            init();
            return;
        }

        if (button.id == 50) {
            getModelData(npc).addAttack();
            init();
            return;
        }
        if (button.id == 51) {
            getModelData(npc).addHurtAnim();
            init();
            return;
        }
        if (button.id == 52) {
            getModelData(npc).addDeathAnim();
            init();
            return;
        }

        if (button.id >= 400 && button.id < 400 + CustomModelData.MAX_ATTACKS) {
            int idx = button.id - 400;
            if (idx < getModelData(npc).getAttackCount()) {
                setSubGui(new GuiStringSelection(this, "Selecting geckolib attack animation:",
                        AnimationFileUtil.getAnimationList(getModelData(npc).getAnimFile()),
                        (name) -> {
                            getModelData(npc).getAttackAnimNames()[idx] = name;
                            GuiTextFieldNop field = getTextField(100 + idx);
                            if (field != null) field.setValue(name);
                        }));
            }
            return;
        }

        if (button.id >= 500 && button.id < 500 + CustomModelData.MAX_ATTACKS) {
            int idx = button.id - 500;
            if (idx < getModelData(npc).getAttackCount()) {
                getModelData(npc).removeAttack(idx);
                init();
            }
            return;
        }

        if (button.id >= 700 && button.id < 700 + CustomModelData.MAX_ATTACKS) {
            int idx = button.id - 700;
            if (idx < getModelData(npc).getAttackCount()) {
                String[] sounds = getModelData(npc).getAttackSoundNames();
                if (sounds[idx] == null) sounds[idx] = "";
                openSoundPicker(sounds[idx],
                    (result) -> sounds[idx] = result == null ? "" : result);
            }
            return;
        }

        if (button.id >= 1200 && button.id < 1200 + CustomModelData.MAX_HURTS) {
            int idx = button.id - 1200;
            if (idx < getModelData(npc).getHurtAnimCount()) {
                setSubGui(new GuiStringSelection(this, "Selecting geckolib hurt animation:",
                        AnimationFileUtil.getAnimationList(getModelData(npc).getAnimFile()),
                        (name) -> {
                            getModelData(npc).getHurtAnimNames()[idx] = name;
                            GuiTextFieldNop field = getTextField(1000 + idx);
                            if (field != null) field.setValue(name);
                        }));
            }
            return;
        }

        if (button.id >= 1300 && button.id < 1300 + CustomModelData.MAX_HURTS) {
            int idx = button.id - 1300;
            if (idx < getModelData(npc).getHurtAnimCount()) {
                getModelData(npc).removeHurtAnim(idx);
                init();
            }
            return;
        }

        if (button.id >= 1500 && button.id < 1500 + CustomModelData.MAX_HURTS) {
            int idx = button.id - 1500;
            if (idx < getModelData(npc).getHurtAnimCount()) {
                String[] sounds = getModelData(npc).getHurtSoundNames();
                if (sounds[idx] == null) sounds[idx] = "";
                openSoundPicker(sounds[idx],
                    (result) -> sounds[idx] = result == null ? "" : result);
            }
            return;
        }

        if (button.id >= 2300 && button.id < 2300 + CustomModelData.MAX_DEATHS) {
            int idx = button.id - 2300;
            if (idx < getModelData(npc).getDeathAnimCount()) {
                setSubGui(new GuiStringSelection(this, "Selecting geckolib death animation:",
                        AnimationFileUtil.getAnimationList(getModelData(npc).getAnimFile()),
                        (name) -> {
                            getModelData(npc).getDeathAnimNames()[idx] = name;
                            GuiTextFieldNop field = getTextField(2000 + idx);
                            if (field != null) field.setValue(name);
                        }));
            }
            return;
        }

        if (button.id >= 2400 && button.id < 2400 + CustomModelData.MAX_DEATHS) {
            int idx = button.id - 2400;
            if (idx < getModelData(npc).getDeathAnimCount()) {
                getModelData(npc).removeDeathAnim(idx);
                init();
            }
            return;
        }
    }

    @Override
    public void unFocused(GuiTextFieldNop textfield) {
        if (textfield.id == 1) {
            if (isValidAnimFile(textfield.getValue()))
                getModelData(npc).setAnimFile(textfield.getValue());
            else
                textfield.setValue(getModelData(npc).getAnimFile());
            return;
        }
        if (textfield.id == 2) {
            if (isValidAnimation(textfield.getValue()))
                getModelData(npc).setIdleAnim(textfield.getValue());
            else
                textfield.setValue(getModelData(npc).getIdleAnim());
            return;
        }
        if (textfield.id == 3) {
            if (isValidAnimation(textfield.getValue()))
                getModelData(npc).setWalkAnim(textfield.getValue());
            else
                textfield.setValue(getModelData(npc).getWalkAnim());
            return;
        }

        if (textfield.id >= 100 && textfield.id < 100 + CustomModelData.MAX_ATTACKS) {
            int idx = textfield.id - 100;
            if (idx < getModelData(npc).getAttackCount()) {
                if (isValidAnimation(textfield.getValue()))
                    getModelData(npc).getAttackAnimNames()[idx] = textfield.getValue();
                else
                    textfield.setValue(getModelData(npc).getAttackAnimNames()[idx]);
            }
            return;
        }
        if (textfield.id >= 200 && textfield.id < 200 + CustomModelData.MAX_ATTACKS) {
            int idx = textfield.id - 200;
            if (idx < getModelData(npc).getAttackCount()) {
                FloatTextFieldUtils.performFloatChecks(0, 100, 1, textfield);
                getModelData(npc).getAttackWeights()[idx] = (int) FloatTextFieldUtils.getFloat(textfield);
            }
            return;
        }
        if (textfield.id >= 300 && textfield.id < 300 + CustomModelData.MAX_ATTACKS) {
            int idx = textfield.id - 300;
            if (idx < getModelData(npc).getAttackCount()) {
                FloatTextFieldUtils.performFloatChecks(0, 1000, 0, textfield);
                getModelData(npc).getAttackFrames()[idx] = FloatTextFieldUtils.getFloat(textfield);
            }
            return;
        }

        if (textfield.id >= 1000 && textfield.id < 1000 + CustomModelData.MAX_HURTS) {
            int idx = textfield.id - 1000;
            if (idx < getModelData(npc).getHurtAnimCount()) {
                if (isValidAnimation(textfield.getValue()))
                    getModelData(npc).getHurtAnimNames()[idx] = textfield.getValue();
                else
                    textfield.setValue(getModelData(npc).getHurtAnimNames()[idx]);
            }
            return;
        }
        if (textfield.id >= 1100 && textfield.id < 1100 + CustomModelData.MAX_HURTS) {
            int idx = textfield.id - 1100;
            if (idx < getModelData(npc).getHurtAnimCount()) {
                FloatTextFieldUtils.performFloatChecks(0, 100, 1, textfield);
                getModelData(npc).getHurtWeights()[idx] = (int) FloatTextFieldUtils.getFloat(textfield);
            }
            return;
        }

        if (textfield.id >= 2000 && textfield.id < 2000 + CustomModelData.MAX_DEATHS) {
            int idx = textfield.id - 2000;
            if (idx < getModelData(npc).getDeathAnimCount()) {
                if (isValidAnimation(textfield.getValue()))
                    getModelData(npc).getDeathAnimNames()[idx] = textfield.getValue();
                else
                    textfield.setValue(getModelData(npc).getDeathAnimNames()[idx]);
            }
            return;
        }
        if (textfield.id >= 2100 && textfield.id < 2100 + CustomModelData.MAX_DEATHS) {
            int idx = textfield.id - 2100;
            if (idx < getModelData(npc).getDeathAnimCount()) {
                FloatTextFieldUtils.performFloatChecks(0, 100, 1, textfield);
                getModelData(npc).getDeathWeights()[idx] = (int) FloatTextFieldUtils.getFloat(textfield);
            }
            return;
        }
        if (textfield.id >= 2200 && textfield.id < 2200 + CustomModelData.MAX_DEATHS) {
            int idx = textfield.id - 2200;
            if (idx < getModelData(npc).getDeathAnimCount()) {
                FloatTextFieldUtils.performFloatChecks(0.1f, 100, 100f, textfield);
                getModelData(npc).getDeathHealthThresholds()[idx] = FloatTextFieldUtils.getFloat(textfield);
            }
            return;
        }
        if (textfield.id >= 2500 && textfield.id < 2500 + CustomModelData.MAX_DEATHS) {
            int idx = textfield.id - 2500;
            if (idx < getModelData(npc).getDeathAnimCount()) {
                FloatTextFieldUtils.performFloatChecks(0.1f, 9999f, 5f, textfield);
                getModelData(npc).getDeathAnimDurations()[idx] = FloatTextFieldUtils.getFloat(textfield);
            }
            return;
        }
    }

    public boolean isValidAnimFile(String name) {
        if (name == null || name.isEmpty()) return false;
        return GeckoLibCache.getBakedAnimations().containsKey(new ResourceLocation(name));
    }

    public boolean isValidAnimation(String name) {
        if (name == null || name.isEmpty()) return true;
        return AnimationFileUtil.getAnimationList(getModelData(npc).getAnimFile()).contains(name);
    }

    private void openSoundPicker(String currentSound, Consumer<String> callback) {
        // Old saves can hold a null/invalid sound string; CNPC's picker would throw on it
        String safe = currentSound;
        if (safe != null && !safe.isEmpty()) {
            try {
                new ResourceLocation(safe);
            } catch (Exception e) {
                safe = "";
            }
        } else {
            safe = "";
        }
        setSubGui(new GuiSoundSelectionWrapper(safe, callback));
    }

    private static class GuiSoundSelectionWrapper extends GuiSoundSelection {
        private final Consumer<String> callback;

        public GuiSoundSelectionWrapper(String currentSound, Consumer<String> callback) {
            super(currentSound);
            this.callback = callback;
        }

        @Override
        public void close() {
            if (selectedResource != null && callback != null) {
                callback.accept(selectedResource.toString());
            }
            super.close();
        }
    }

    private static class TabButton extends GuiButtonNop {
        public boolean selected;

        TabButton(IGuiInterface gui, int id, int x, int y, int w, int h, String label, boolean selected) {
            super(gui, id, x, y, w, h, label);
            this.selected = selected;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            if (selected) {
                boolean old = this.active;
                this.active = false;
                super.renderWidget(graphics, mouseX, mouseY, partialTicks);
                this.active = old;
            } else {
                super.renderWidget(graphics, mouseX, mouseY, partialTicks);
            }
        }
    }
}
