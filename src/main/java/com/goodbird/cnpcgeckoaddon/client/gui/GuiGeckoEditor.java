package com.goodbird.cnpcgeckoaddon.client.gui;

import com.goodbird.cnpcgeckoaddon.data.CustomModelData;
import com.goodbird.cnpcgeckoaddon.mixin.IDataDisplay;
import com.goodbird.cnpcgeckoaddon.utils.NpcTextureUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import noppes.npcs.client.gui.select.GuiSoundSelection;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiButtonYesNo;
import noppes.npcs.shared.client.gui.components.GuiLabel;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.ITextfieldListener;
import noppes.npcs.shared.client.util.AssetsFinder;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.loading.object.BakedAnimations;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** 原版控件的模型编辑台，资源浏览与设置共用右侧区域。 */
public class GuiGeckoEditor extends GuiNPCInterface implements ITextfieldListener {
    private static final int ANIM_ROW_H = 24;
    private static final int GROUP_ROW_H = 22;
    private static final int BROWSE_ROW_H = 20;

    private int tab;
    private int section;
    private final int[] offsets = new int[4];
    private int leftWidth, contentX, contentY, contentWidth, bottom, listTop, visibleRows;
    private GeckoPreview preview;
    private float rotation, zoom = 1;
    private boolean rotating, draggingScroll;
    private String message = "";
    private int browseTarget;
    private List<String> options = List.of();
    private List<String> filtered = List.of();
    private List<String> groups = List.of();
    private boolean groupView;
    private String browseNamespace = "";
    private String browsePath = "";
    private int browseOffset;
    private String candidate = "";
    private String query = "";
    private boolean normalizedDisplay;
    private final List<GuiTextFieldNop> animationFields = new ArrayList<>();
    private final List<String> animationValues = new ArrayList<>();
    private final List<Consumer<String>> animationSetters = new ArrayList<>();

    public GuiGeckoEditor(EntityNPCInterface npc, int tab) {
        super(npc);
        this.tab = tab;
        drawDefaultBackground = false;
    }

    public static String text(String key) {
        return Component.translatable("cnpcgeckoaddon.gui." + key).getString();
    }

    public CustomModelData getModelData(EntityNPCInterface npc) {
        return ((IDataDisplay)npc.display).getCustomModelData();
    }

    private CustomModelData data() {
        return getModelData(npc);
    }

    @Override
    public void init() {
        imageWidth = Math.min(width - 12, 680);
        imageHeight = Math.min(height - 12, 390);
        super.init();
        if (!normalizedDisplay) {
            normalizedDisplay = true;
            // 皮肤只保留资源贴图；CNPC 的附加材质发光对 Gecko 模型无效，关闭它避免旧配置继续生效。
            npc.display.skinType = 0;
            npc.textureLocation = null;
            npc.display.setOverlayGlowing(false);
        }
        if (preview == null) preview = new GeckoPreview(npc);
        leftWidth = Math.max(92, imageWidth / 3);
        contentX = guiLeft + leftWidth + 12;
        contentY = guiTop + 52;
        contentWidth = imageWidth - leftWidth - 22;
        bottom = guiTop + imageHeight - 30;
        animationFields.clear();
        animationValues.clear();
        animationSetters.clear();
        headerLabels.clear();

        int tabWidth = (contentWidth - 4) / 3;
        String[] tabs = {"model_material", "animations", "outer_extras"};
        for (int i = 0; i < tabs.length; i++) {
            GuiButtonNop button = new GuiButtonNop(this, 800 + i, contentX + i * (tabWidth + 2), guiTop + 26,
                    i == 2 ? contentWidth - 2 * (tabWidth + 2) : tabWidth, 20, text(tabs[i]));
            button.active = i != tab;
            addButton(button);
        }
        addButton(new GuiButtonNop(this, 810, guiLeft + imageWidth - 80, guiTop + imageHeight - 24, 72, 20,
                browseTarget == 0 ? "gui.done" : "gui.back"));
        addButton(new GuiButtonNop(this, 811, guiLeft + 8, guiTop + imageHeight - 24, leftWidth - 4, 20, text("reset_view")));
        if (browseTarget != 0) createBrowser();
        else if (tab == 0) createModelPage();
        else if (tab == 1) createAnimationPage();
        else createExtrasPage();
    }

    private GuiTextFieldNop field(int id, int x, int y, int w, String value) {
        GuiTextFieldNop field = new GuiTextFieldNop(id, this, x, y, w, 18, value);
        addTextField(field);
        return field;
    }

    private void resourceField(int id, int y, String label, String value) {
        addLabel(new GuiLabel(id, text(label), contentX, y, 0x404040));
        field(id, contentX, y + 11, contentWidth - 36, value);
        addButton(new GuiButtonNop(this, id, contentX + contentWidth - 32, y + 10, 32, 20, "..."));
    }

    private void createModelPage() {
        int y = contentY;
        resourceField(10, y, "outer_model", data().getModel());
        resourceField(11, y + 40, "skin", npc.display.getSkinTexture());
        resourceField(12, y + 80, "overlay", npc.display.getOverlayTexture());
        addLabel(new GuiLabel(15, font.plainSubstrByWidth(text("skin_only"), contentWidth), contentX, y + 113, 0x777777));
        addLabel(new GuiLabel(16, font.plainSubstrByWidth(text("overlay_hint"), contentWidth), contentX, y + 126, 0x777777));
    }

    private void createAnimationPage() {
        resourceField(20, contentY, "animfile", data().getAnimFile());
        int w = (contentWidth - 6) / 4;
        String[] sections = {"base", "attack", "hurt", "death"};
        for (int i = 0; i < 4; i++) {
            GuiButtonNop button = new GuiButtonNop(this, 820 + i, contentX + i * (w + 2), contentY + 35,
                    i == 3 ? contentWidth - 3 * (w + 2) : w, 20, text(sections[i]));
            button.active = i != section;
            addButton(button);
        }
        if (section == 0) {
            resourceField(21, contentY + 64, "idle", data().getIdleAnim());
            resourceField(22, contentY + 100, "walk", data().getWalkAnim());
            animationFields.add(getTextField(21));
            animationValues.add(data().getIdleAnim());
            animationSetters.add(data()::setIdleAnim);
            animationFields.add(getTextField(22));
            animationValues.add(data().getWalkAnim());
            animationSetters.add(data()::setWalkAnim);
            addLabel(new GuiLabel(23, font.plainSubstrByWidth(text("hover_preview"), contentWidth), contentX,
                    contentY + 136, 0x777777));
            return;
        }
        createAnimationTable();
    }

    private void createAnimationTable() {
        listTop = contentY + 64;
        int rowH = ANIM_ROW_H;
        visibleRows = Math.max(1, (bottom - 24 - listTop) / rowH);
        offsets[section] = Mth.clamp(offsets[section], 0, Math.max(0, count() - visibleRows));

        int idxW = 12;
        int delW = 14;
        int weightW = 22;
        int frameW = 28;
        int soundW = 24;
        int secW = 26;
        int gap = 2;
        if (contentWidth < 240) {
            weightW = 20;
            frameW = 24;
            soundW = 18;
            secW = 22;
        }
        int fixed = idxW + delW + weightW + frameW + soundW + gap * 4 + (section == 3 ? secW + gap : 0);
        int nameW = Math.max(40, contentWidth - fixed);
        int nameX = contentX + idxW;
        int nameFieldW = Math.max(22, nameW - 20);
        int browseX = nameX + nameFieldW + 1;
        int weightX = nameX + nameW + gap;
        int frameX = weightX + weightW + gap;
        int secX = frameX + frameW + gap;
        int soundX = (section == 3 ? secX + secW : frameX + frameW) + gap;
        int delX = contentX + contentWidth - delW;

        int headerY = listTop - 11;
        graphicsLabel("#", contentX + 2, headerY);
        graphicsLabel(text("col_name"), nameX, headerY);
        graphicsLabel(text("short_weight"), weightX, headerY);
        graphicsLabel(text(section == 1 ? "short_frame" : section == 3 ? "short_hp" : "short_weight"), frameX, headerY);
        if (section == 3) graphicsLabel(text("seconds"), secX, headerY);
        graphicsLabel(text("short_sound"), soundX, headerY);

        for (int row = 0; row < Math.min(visibleRows, count() - offsets[section]); row++) {
            int index = offsets[section] + row;
            int y = listTop + row * rowH;
            addLabel(new GuiLabel(100 + index, "" + (index + 1), contentX + 2, y + 7, 0x404040));
            animationFields.add(field(100 + index, nameX, y + 3, nameFieldW, names()[index]));
            animationValues.add(names()[index]);
            animationSetters.add(value -> names()[index] = value);
            addButton(new GuiButtonNop(this, 300 + index, browseX, y + 2, 18, 18, "…"));
            field(200 + index, weightX, y + 3, weightW, "" + weights()[index]).setNumbersOnly().setMinMaxDefault(0, 100, 1);
            if (section == 1 || section == 3) {
                field(400 + index, frameX, y + 3, frameW,
                        "" + (section == 1 ? data().getAttackFrames()[index] : data().getDeathHealthThresholds()[index]));
            }
            if (section == 3) {
                field(500 + index, secX, y + 3, secW, "" + data().getDeathAnimDurations()[index]);
            }
            String sound = sounds()[index];
            addButton(new GuiButtonNop(this, 600 + index, soundX, y + 2, soundW, 18,
                    text("short_sound") + (sound == null || sound.isEmpty() ? "" : "*")));
            addButton(new GuiButtonNop(this, 700 + index, delX, y + 2, delW, 18, "×"));
        }
        addButton(new GuiButtonNop(this, 830, contentX, bottom - 21, 70, 20, "+ " + text("add")));
        getButton(830).active = count() < 20;
        addLabel(new GuiLabel(830, count() + " / 20", contentX + 76, bottom - 15, 0x404040));
    }

    private void graphicsLabel(String value, int x, int y) {
        // 表头在 render 阶段绘制，避免占用原版控件 id。
        headerLabels.add(new HeaderLabel(value, x, y));
    }

    private record HeaderLabel(String value, int x, int y) {
    }

    private final List<HeaderLabel> headerLabels = new ArrayList<>();

    private int count() {
        return switch (section) {
            case 1 -> data().getAttackCount();
            case 2 -> data().getHurtAnimCount();
            default -> data().getDeathAnimCount();
        };
    }

    private String[] names() {
        return switch (section) {
            case 1 -> data().getAttackAnimNames();
            case 2 -> data().getHurtAnimNames();
            default -> data().getDeathAnimNames();
        };
    }

    private int[] weights() {
        return switch (section) {
            case 1 -> data().getAttackWeights();
            case 2 -> data().getHurtWeights();
            default -> data().getDeathWeights();
        };
    }

    private String[] sounds() {
        return section == 1 ? data().getAttackSoundNames() : data().getHurtSoundNames();
    }

    private void createExtrasPage() {
        int y = contentY;
        sectionLabel("section_bone", y);
        y += 16;
        y = labeledField(30, y, "head_bone", data().getHeadBoneName());
        y += 4;
        sectionLabel("section_motion", y);
        y += 16;
        y = labeledField(31, y, "transition_length", "" + data().getTransitionLengthTicks());
        getTextField(31).setNumbersOnly().setMinMaxDefault(0, Integer.MAX_VALUE, data().getTransitionLengthTicks());
        y += 4;
        sectionLabel("section_hitbox", y);
        y += 16;
        y = labeledField(32, y, "hitbox_width", "" + data().getWidth());
        y = labeledField(33, y, "hitbox_height", "" + data().getHeight());
        y += 4;
        sectionLabel("section_hurt", y);
        y += 16;
        addLabel(new GuiLabel(34, font.plainSubstrByWidth(text("hurt_tint"), contentWidth - 52), contentX, y + 5, 0x404040));
        addButton(new GuiButtonYesNo(this, 34, contentX + contentWidth - 48, y, 48, 20, data().isHurtTintEnabled()));
    }

    private void sectionLabel(String key, int y) {
        addLabel(new GuiLabel(40 + y, "§7" + text(key), contentX, y, 0x555555));
    }

    private int labeledField(int id, int y, String label, String value) {
        int fieldWidth = Math.min(86, contentWidth / 3);
        addLabel(new GuiLabel(id, font.plainSubstrByWidth(text(label), contentWidth - fieldWidth - 4), contentX, y + 5, 0x404040));
        field(id, contentX + contentWidth - fieldWidth, y, fieldWidth, value);
        return y + 24;
    }

    private BakedAnimations animationFile() {
        ResourceLocation id = ResourceLocation.tryParse(data().getAnimFile());
        return id == null ? null : GeckoLibCache.getBakedAnimations().get(id);
    }

    public boolean isValidAnimFile(String name) {
        ResourceLocation id = ResourceLocation.tryParse(name);
        return id != null && GeckoLibCache.getBakedAnimations().containsKey(id);
    }

    public boolean isValidAnimation(String name) {
        BakedAnimations file = animationFile();
        return name.isEmpty() || file != null && file.animations().containsKey(name);
    }

    private boolean validResource(int target, String value) {
        if (target == 21 || target == 22 || target >= 100 && target < 120) return isValidAnimation(value);
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (target == 10) return id != null && GeckoLibCache.getBakedModels().containsKey(id);
        if (target == 20) return isValidAnimFile(value);
        if (target == 12) return value.isEmpty() || id != null;
        return id != null;
    }

    private void openBrowser(int target) {
        GuiTextFieldNop.unfocus();
        candidate = getTextField(target).getValue();
        browseTarget = target;
        message = "";
        query = "";
        browseOffset = 0;
        if (target == 10) {
            options = GeckoLibCache.getBakedModels().keySet().stream().map(Object::toString).sorted().toList();
            groupView = true;
            browseNamespace = "";
            browsePath = "";
        } else if (target == 20) {
            options = GeckoLibCache.getBakedAnimations().keySet().stream().map(Object::toString).sorted().toList();
            groupView = true;
            browseNamespace = "";
            browsePath = "";
        } else if (target == 11 || target == 12) {
            options = AssetsFinder.find("textures", ".png").stream().map(Object::toString).distinct().sorted().toList();
            groupView = false;
            browseNamespace = "customnpcs";
            browsePath = target == 11 ? CnpclinesCompat.skinRoot() : CnpclinesCompat.overlayRoot();
        } else {
            BakedAnimations file = animationFile();
            options = file == null ? List.of() : file.animations().keySet().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
            groupView = false;
            browseNamespace = "";
            browsePath = "";
        }
        updateFiltered();
        init();
    }

    private void updateFiltered() {
        String q = query.trim().toLowerCase(Locale.ROOT);
        if (groupView) {
            List<String> namespaces = options.stream().map(this::namespaceOf).filter(value -> !value.isEmpty())
                    .distinct().sorted(Comparator.comparingInt(this::namespacePriority).thenComparing(String::compareTo)).toList();
            List<String> result = new ArrayList<>();
            result.add("");
            result.addAll(namespaces);
            if (!q.isEmpty()) {
                result.removeIf(ns -> !namespaceLabel(ns).toLowerCase(Locale.ROOT).contains(q) && !ns.contains(q));
            }
            groups = result;
            browseOffset = Mth.clamp(browseOffset, 0, Math.max(0, groups.size() - visibleRows));
        } else {
            List<String> result = new ArrayList<>();
            for (String value : options) {
                if (!browseNamespace.isEmpty() && !namespaceOf(value).equals(browseNamespace)) continue;
                if (!browsePath.isEmpty() && !pathOf(value).startsWith(browsePath)) continue;
                if (!q.isEmpty() && !value.toLowerCase(Locale.ROOT).contains(q)) continue;
                result.add(value);
            }
            filtered = result;
            browseOffset = Mth.clamp(browseOffset, 0, Math.max(0, filtered.size() - visibleRows));
        }
    }

    private void createBrowser() {
        addLabel(new GuiLabel(900, text("search"), contentX, contentY, 0x404040));
        GuiTextFieldNop search = field(901, contentX + 36, contentY - 4, Math.max(40, contentWidth - 100), query);
        search.setResponder(value -> {
            query = value;
            browseOffset = 0;
            updateFiltered();
        });
        addButton(new GuiButtonNop(this, 850, contentX + contentWidth - 58, contentY - 5, 58, 20,
                text(groupView ? "all" : "mods")));
        listTop = contentY + 26;
        int rowHeight = groupView ? GROUP_ROW_H : BROWSE_ROW_H;
        visibleRows = Math.max(1, (bottom - 70 - listTop) / rowHeight);
        updateFiltered();
        addLabel(new GuiLabel(902, text("value"), contentX, bottom - 65, 0x404040));
        GuiTextFieldNop input = field(902, contentX, bottom - 54, contentWidth, candidate);
        input.setResponder(value -> candidate = value);
        addButton(new GuiButtonNop(this, 840, contentX, bottom - 26, (contentWidth - 4) / 2, 20, text("apply")));
        addButton(new GuiButtonNop(this, 841, contentX + (contentWidth + 4) / 2, bottom - 26, (contentWidth - 4) / 2, 20, "gui.cancel"));
    }

    @Override
    public void buttonEvent(GuiButtonNop button) {
        GuiTextFieldNop.unfocus();
        int id = button.id;
        if (id == 810) { onClose(); return; }
        if (id == 811) { rotation = 0; zoom = 1; return; }
        if (id >= 800 && id <= 802) {
            browseTarget = 0;
            tab = id - 800;
            message = "";
        } else if (id >= 820 && id <= 823) {
            section = id - 820;
            message = "";
        } else if (id == 840) {
            if (!validResource(browseTarget, candidate)) { message = text("invalid_resource"); return; }
            applyResource(browseTarget, candidate);
            browseTarget = 0;
            message = "";
        } else if (id == 841) {
            browseTarget = 0;
            message = "";
        } else if (id == 850) {
            groupView = !groupView;
            browseNamespace = "";
            browsePath = "";
            browseOffset = 0;
            updateFiltered();
        } else if (id == 34) {
            data().setHurtTintEnabled(((GuiButtonYesNo)button).getBoolean());
        } else if (id == 830) {
            if (section == 1) data().addAttack();
            else if (section == 2) data().addHurtAnim();
            else data().addDeathAnim();
            offsets[section] = Math.max(0, count() - visibleRows);
        } else if (id >= 700 && id < 720) {
            int index = id - 700;
            if (section == 1) data().removeAttack(index);
            else if (section == 2) data().removeHurtAnim(index);
            else data().removeDeathAnim(index);
        } else if (id >= 600 && id < 620) {
            int index = id - 600;
            String sound = sounds()[index];
            String safe = sound != null && ResourceLocation.tryParse(sound) != null ? sound : "";
            setSubGui(new SoundPicker(safe, value -> sounds()[index] = value));
            return;
        } else if (id >= 300 && id < 320) {
            openBrowser(100 + id - 300);
            return;
        } else if (id == 10 || id == 11 || id == 12 || id == 20 || id == 21 || id == 22) {
            openBrowser(id);
            return;
        }
        init();
    }

    private void applyResource(int target, String value) {
        switch (target) {
            case 10 -> data().setModel(value);
            case 11 -> {
                npc.display.skinType = 0;
                npc.display.setSkinTexture(value);
                npc.textureLocation = null;
            }
            case 12 -> npc.display.setOverlayTexture(value);
            case 20 -> data().setAnimFile(value);
            case 21 -> data().setIdleAnim(value);
            case 22 -> data().setWalkAnim(value);
            default -> names()[target - 100] = value;
        }
    }

    @Override
    public void unFocused(GuiTextFieldNop field) {
        int id = field.id;
        if (id >= 900) return;
        String value = field.getValue().trim();
        if (id == 11) {
            if (validResource(11, value)) {
                npc.display.skinType = 0;
                npc.display.setSkinTexture(value);
                npc.textureLocation = null;
                message = "";
            } else {
                message = text("invalid_resource");
                field.setValue(npc.display.getSkinTexture());
            }
        } else if (id == 10 || id == 12 || id == 20) {
            if (validResource(id, value)) {
                applyResource(id, value);
                message = "";
            } else {
                message = text("invalid_resource");
                field.setValue(switch (id) {
                    case 10 -> data().getModel();
                    case 12 -> npc.display.getOverlayTexture();
                    default -> data().getAnimFile();
                });
            }
        } else if (animationFields.contains(field)) {
            int index = animationFields.indexOf(field);
            if (isValidAnimation(value)) {
                animationSetters.get(index).accept(value);
                animationValues.set(index, value);
                message = "";
            } else {
                field.setValue(animationValues.get(index));
                message = text("invalid_animation");
            }
        } else if (id == 30) data().setHeadBoneName(value);
        else if (id == 31) data().setTransitionLengthTicks(field.getInteger());
        else if (id == 32) data().setWidth(number(field, 0, Float.MAX_VALUE, data().getWidth()));
        else if (id == 33) data().setHeight(number(field, 0, Float.MAX_VALUE, data().getHeight()));
        else if (id >= 200 && id < 220) weights()[id - 200] = field.getInteger();
        else if (id >= 400 && id < 420) {
            if (section == 1) data().getAttackFrames()[id - 400] = number(field, 0, 1000, 0);
            else data().getDeathHealthThresholds()[id - 400] = number(field, 0.1f, 100, 100);
        } else if (id >= 500 && id < 520) data().getDeathAnimDurations()[id - 500] = number(field, 0.1f, 9999, 5);
    }

    private float number(GuiTextFieldNop field, float min, float max, float fallback) {
        float value = fallback;
        try {
            float parsed = Float.parseFloat(field.getValue().replace(',', '.'));
            if (Float.isFinite(parsed)) value = Mth.clamp(parsed, min, max);
        } catch (NumberFormatException ignored) {
            // 输入未完成时恢复原值，不让无效文本进入配置。
        }
        field.setValue(Float.toString(value));
        return value;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.fill(0, 0, width, height, 0xd0101010);
        graphics.fill(guiLeft, guiTop, guiLeft + imageWidth, guiTop + imageHeight, 0xffc6c6c6);
        graphics.fill(guiLeft, guiTop, guiLeft + imageWidth, guiTop + 2, 0xffffffff);
        graphics.fill(guiLeft, guiTop, guiLeft + 2, guiTop + imageHeight, 0xffffffff);
        graphics.fill(guiLeft, guiTop + imageHeight - 2, guiLeft + imageWidth, guiTop + imageHeight, 0xff555555);
        graphics.fill(guiLeft + imageWidth - 2, guiTop, guiLeft + imageWidth, guiTop + imageHeight, 0xff555555);
        graphics.drawString(font, text("editor"), guiLeft + 9, guiTop + 9, 0x404040, false);
        int px = guiLeft + 8, py = guiTop + 26, pw = leftWidth - 4, ph = bottom - py;
        graphics.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, 0xff555555);
        graphics.fill(px, py, px + pw, py + ph, 0xff252525);
        graphics.fill(px + 8, py + ph - 21, px + pw - 8, py + ph - 20, 0xff555555);

        String model = data().getModel(), animFile = data().getAnimFile(), animation = data().getIdleAnim();
        ResourceLocation texture = NpcTextureUtils.getNpcTexture(npc);
        ResourceLocation overlay = npc.display.getOverlayTexture().isEmpty() ? null : ResourceLocation.tryParse(npc.display.getOverlayTexture());
        if (wrapper.subgui == null && browseTarget == 0) {
            for (GuiTextFieldNop field : animationFields) {
                if (field.isMouseOver(mouseX, mouseY) && isValidAnimation(field.getValue())) animation = field.getValue();
            }
        }
        if (browseTarget != 0 && validResource(browseTarget, candidate)) {
            if (browseTarget == 10) model = candidate;
            else if (browseTarget == 20) {
                animFile = candidate;
                var file = GeckoLibCache.getBakedAnimations().get(ResourceLocation.tryParse(animFile));
                if (!file.animations().containsKey(animation)) animation = file.animations().keySet().stream().sorted().findFirst().orElse("");
            } else if (browseTarget == 11 && !candidate.isEmpty()) texture = ResourceLocation.tryParse(candidate);
            else if (browseTarget == 12) overlay = candidate.isEmpty() ? null : ResourceLocation.tryParse(candidate);
            else if (browseTarget == 21 || browseTarget == 22 || browseTarget >= 100) animation = candidate;
        }
        int swatchHeight = browseTarget == 11 || browseTarget == 12 ? Math.min(leftWidth - 20, 64) + 8 : 0;
        preview.render(graphics, px, py, pw, ph - 17 - swatchHeight, model, animFile, texture, overlay, animation, rotation, zoom);
        graphics.drawString(font, font.plainSubstrByWidth(animation.isEmpty() ? text("preview") : animation, pw - 12), px + 6, py + ph - 13, 0xcccccc, false);

        if (browseTarget != 0) renderBrowser(graphics, mouseX, mouseY);
        else {
            for (HeaderLabel label : headerLabels) {
                graphics.drawString(font, label.value, label.x, label.y, 0x555555, false);
            }
            if (tab == 1 && section != 0) {
                renderAnimationRows(graphics, mouseX, mouseY);
                renderScrollBar(graphics, count(), offsets[section], ANIM_ROW_H);
            }
        }
        if (!message.isEmpty()) graphics.drawString(font, font.plainSubstrByWidth(message, contentWidth - 80), contentX, guiTop + imageHeight - 17, 0xaa2222, false);
        super.render(graphics, mouseX, mouseY, partialTicks);
        if (wrapper.subgui != null) return;
        String tooltip = "";
        if (browseTarget == 0 && tab == 1) {
            for (GuiTextFieldNop field : animationFields) {
                if (field.isMouseOver(mouseX, mouseY)) tooltip = field.getValue().isEmpty() ? text("hover_preview") : field.getValue();
            }
            for (GuiTextFieldNop field : wrapper.textfields.values()) {
                if (!field.isMouseOver(mouseX, mouseY)) continue;
                if (field.id >= 200 && field.id < 220) tooltip = text("weight_help");
                else if (field.id >= 400 && field.id < 420) tooltip = text(section == 1 ? "frame_help" : "hp_help");
                else if (field.id >= 500 && field.id < 520) tooltip = text("duration_help");
            }
            for (GuiButtonNop button : wrapper.npcbuttons.values()) {
                if (!button.isMouseOver(mouseX, mouseY)) continue;
                if (button.id >= 700 && button.id < 720) tooltip = text("delete_help");
                else if (button.id >= 600 && button.id < 620) {
                    String sound = sounds()[button.id - 600];
                    tooltip = sound == null || sound.isEmpty() ? text("sound") : sound;
                }
            }
        } else if (browseTarget != 0) {
            if (mouseX >= contentX && mouseX < contentX + contentWidth - 8
                    && mouseY >= listTop && mouseY < listTop + visibleRows * (groupView ? GROUP_ROW_H : BROWSE_ROW_H)) {
                int index = browseOffset + (mouseY - listTop) / (groupView ? GROUP_ROW_H : BROWSE_ROW_H);
                if (groupView && index < groups.size()) tooltip = namespaceLabel(groups.get(index));
                else if (!groupView && index < filtered.size()) tooltip = filtered.get(index);
            }
        } else if (tab == 2) {
            for (GuiTextFieldNop field : wrapper.textfields.values()) {
                if (!field.isMouseOver(mouseX, mouseY)) continue;
                if (field.id == 30) tooltip = text("head_bone");
                else if (field.id == 31) tooltip = text("transition_length");
                else if (field.id == 32 || field.id == 33) tooltip = text("hitbox_help");
            }
        }
        if (mouseX >= px && mouseX < px + pw && mouseY >= py && mouseY < py + ph) tooltip = text("view_help");
        if (!tooltip.isEmpty()) graphics.renderTooltip(font, font.split(Component.literal(tooltip), Math.min(260, width - 20)), mouseX, mouseY);
    }

    private void renderAnimationRows(GuiGraphics graphics, int mouseX, int mouseY) {
        for (int row = 0; row < Math.min(visibleRows, count() - offsets[section]); row++) {
            int y = listTop + row * ANIM_ROW_H;
            int color = (row & 1) == 0 ? 0xffb6b6b6 : 0xffc0c0c0;
            graphics.fill(contentX, y, contentX + contentWidth, y + ANIM_ROW_H - 1, color);
        }
    }

    private void renderBrowser(GuiGraphics graphics, int mouseX, int mouseY) {
        int rowHeight = groupView ? GROUP_ROW_H : BROWSE_ROW_H;
        graphics.fill(contentX, listTop, contentX + contentWidth, listTop + visibleRows * rowHeight, 0xff202020);
        String scope = groupView ? text("select_mod") : namespaceLabel(browseNamespace)
                + (browsePath.isEmpty() ? "" : " : " + browsePath);
        graphics.drawString(font, font.plainSubstrByWidth(scope, contentWidth - 8), contentX, listTop - 10, 0x666666, false);
        if (groupView) {
            for (int row = 0; row < Math.min(visibleRows, groups.size() - browseOffset); row++) {
                String namespace = groups.get(browseOffset + row);
                int y = listTop + row * rowHeight;
                boolean hover = mouseX >= contentX && mouseX < contentX + contentWidth - 8 && mouseY >= y && mouseY < y + rowHeight;
                boolean selected = namespace.equals(browseNamespace);
                if (hover || selected) graphics.fill(contentX + 1, y + 1, contentX + contentWidth - 8, y + rowHeight - 1,
                        selected ? 0xff555555 : 0xff383838);
                drawModIcon(graphics, namespace, contentX + 4, y + 3);
                graphics.drawString(font, font.plainSubstrByWidth(namespaceLabel(namespace), contentWidth - 60), contentX + 24, y + 7, 0xffffff, false);
                String count = "" + namespaceCount(namespace);
                graphics.drawString(font, count, contentX + contentWidth - 12 - font.width(count), y + 7, 0xaaaaaa, false);
            }
            if (groups.isEmpty()) graphics.drawString(font, text("no_results"), contentX + 5, listTop + 7, 0xaaaaaa, false);
            renderScrollBar(graphics, groups.size(), browseOffset, GROUP_ROW_H);
        } else {
            for (int row = 0; row < Math.min(visibleRows, filtered.size() - browseOffset); row++) {
                String option = filtered.get(browseOffset + row);
                int y = listTop + row * rowHeight;
                boolean hover = mouseX >= contentX && mouseX < contentX + contentWidth - 8 && mouseY >= y && mouseY < y + rowHeight && wrapper.subgui == null;
                if (option.equals(candidate) || hover) graphics.fill(contentX + 1, y + 1, contentX + contentWidth - 8, y + rowHeight - 1,
                        option.equals(candidate) ? 0xff555555 : 0xff383838);
                graphics.drawString(font, font.plainSubstrByWidth(label(option), contentWidth - 16), contentX + 4, y + 6, 0xffffff, false);
            }
            if (filtered.isEmpty()) graphics.drawString(font, text("no_results"), contentX + 5, listTop + 7, 0xaaaaaa, false);
            renderScrollBar(graphics, filtered.size(), browseOffset, BROWSE_ROW_H);
        }
        if (browseTarget == 11 || browseTarget == 12) {
            ResourceLocation texture = ResourceLocation.tryParse(candidate);
            if (texture != null) {
                int size = Math.min(leftWidth - 20, 64);
                int x = guiLeft + 8 + (leftWidth - 4 - size) / 2, y = bottom - 23 - size;
                graphics.fill(x - 1, y - 1, x + size + 1, y + size + 1, 0xff777777);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                graphics.blit(texture, x, y, size, size, 0, 0, 1, 1, 1, 1);
                RenderSystem.disableBlend();
            }
        }
    }

    private void drawModIcon(GuiGraphics graphics, String namespace, int x, int y) {
        ResourceLocation logo = CnpclinesCompat.logo(namespace);
        if (logo != null && namespaceExists(logo)) {
            graphics.blit(logo, x, y, 0, 0, 16, 16, 16, 16);
            return;
        }
        graphics.fill(x, y, x + 16, y + 16, 0xff3a3a3a);
        graphics.fill(x, y, x + 16, y + 1, 0xff777777);
        graphics.drawString(font, "?", x + 6, y + 4, 0xffffff, false);
    }

    private boolean namespaceExists(ResourceLocation location) {
        try {
            return Minecraft.getInstance().getResourceManager().getResource(location).isPresent();
        } catch (Exception ignored) {
            return false;
        }
    }

    private String namespaceLabel(String namespace) {
        return namespace.isEmpty() ? text("all") : CnpclinesCompat.displayName(namespace);
    }

    private int namespaceCount(String namespace) {
        if (namespace.isEmpty()) return options.size();
        int count = 0;
        for (String value : options) if (namespaceOf(value).equals(namespace)) count++;
        return count;
    }

    private int namespacePriority(String namespace) {
        if (namespace.equals("cnpcgeckoaddon")) return 0;
        if (namespace.equals("customnpcs")) return 1;
        if (namespace.equals("minecraft")) return 2;
        return 3;
    }

    private String namespaceOf(String value) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        return id == null ? "" : id.getNamespace();
    }

    private String pathOf(String value) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        return id == null ? value : id.getPath();
    }

    private String label(String value) {
        if (browseNamespace.isEmpty()) return value;
        return pathOf(value);
    }

    private void renderScrollBar(GuiGraphics graphics, int count, int offset, int rowHeight) {
        if (count <= visibleRows) return;
        int x = contentX + contentWidth - 6, h = visibleRows * rowHeight;
        graphics.fill(x, listTop, x + 6, listTop + h, 0xff555555);
        int thumb = Math.max(10, h * visibleRows / count);
        int y = listTop + (h - thumb) * offset / (count - visibleRows);
        graphics.fill(x + 1, y, x + 5, y + thumb, 0xffc6c6c6);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        if (wrapper.subgui != null) return super.mouseScrolled(x, y, delta);
        if (x >= guiLeft + 8 && x < guiLeft + leftWidth + 4 && y >= guiTop + 26 && y < bottom) {
            zoom = Mth.clamp(zoom * (float)Math.pow(1.12, delta), 0.1f, 8);
            return true;
        }
        int rowHeight = browseTarget != 0 ? (groupView ? GROUP_ROW_H : BROWSE_ROW_H) : ANIM_ROW_H;
        if (x >= contentX && x < contentX + contentWidth && y >= listTop
                && y < listTop + visibleRows * rowHeight
                && (browseTarget != 0 || tab == 1 && section != 0)) {
            GuiTextFieldNop.unfocus();
            int step = delta > 0 ? -1 : 1;
            if (browseTarget != 0) {
                int max = Math.max(0, (groupView ? groups.size() : filtered.size()) - visibleRows);
                browseOffset = Mth.clamp(browseOffset + step, 0, max);
            } else {
                offsets[section] = Mth.clamp(offsets[section] + step, 0, Math.max(0, count() - visibleRows));
                init();
            }
            return true;
        }
        return super.mouseScrolled(x, y, delta);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (wrapper.subgui != null) return super.mouseClicked(x, y, button);
        if (button == 0 && x >= guiLeft + 8 && x < guiLeft + leftWidth + 4 && y >= guiTop + 26 && y < bottom) {
            GuiTextFieldNop.unfocus();
            rotating = true;
            return true;
        }
        int rowHeight = browseTarget != 0 ? (groupView ? GROUP_ROW_H : BROWSE_ROW_H) : ANIM_ROW_H;
        if (button == 0 && (browseTarget != 0 || tab == 1 && section != 0)) {
            if (x >= contentX + contentWidth - 6 && x < contentX + contentWidth && y >= listTop && y < listTop + visibleRows * rowHeight) {
                draggingScroll = true;
                scrollTo(y, rowHeight);
                return true;
            }
            if (browseTarget != 0 && x >= contentX && x < contentX + contentWidth - 8 && y >= listTop && y < listTop + visibleRows * rowHeight) {
                int index = browseOffset + (int)(y - listTop) / rowHeight;
                if (groupView) {
                    if (index < groups.size()) enterGroup(groups.get(index));
                } else if (index < filtered.size()) {
                    candidate = filtered.get(index);
                    getTextField(902).setValue(candidate);
                    message = "";
                }
                return true;
            }
        }
        return super.mouseClicked(x, y, button);
    }

    private void enterGroup(String namespace) {
        browseNamespace = namespace;
        browsePath = namespace.equals("customnpcs") && (browseTarget == 11 || browseTarget == 12)
                ? (browseTarget == 11 ? CnpclinesCompat.skinRoot() : CnpclinesCompat.overlayRoot()) : "";
        groupView = false;
        browseOffset = 0;
        query = "";
        if (getTextField(901) != null) getTextField(901).setValue("");
        updateFiltered();
        init();
    }

    private void scrollTo(double y, int rowHeight) {
        GuiTextFieldNop.unfocus();
        int max = Math.max(0, (browseTarget != 0 ? (groupView ? groups.size() : filtered.size()) : count()) - visibleRows);
        int offset = Mth.clamp((int)Math.round((y - listTop) / (visibleRows * rowHeight) * max), 0, max);
        if (browseTarget != 0) browseOffset = offset;
        else { offsets[section] = offset; init(); }
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (wrapper.subgui != null) return super.mouseDragged(x, y, button, dx, dy);
        if (rotating) { rotation += (float)dx * 2; return true; }
        if (draggingScroll) { scrollTo(y, browseTarget != 0 ? (groupView ? GROUP_ROW_H : BROWSE_ROW_H) : ANIM_ROW_H); return true; }
        return super.mouseDragged(x, y, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        rotating = false;
        draggingScroll = false;
        return super.mouseReleased(x, y, button);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (wrapper.subgui == null && browseTarget != 0 && (key == 257 || key == 335)) {
            buttonEvent(getButton(840));
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (browseTarget != 0) {
            GuiTextFieldNop.unfocus();
            browseTarget = 0;
            message = "";
            init();
        } else super.onClose();
    }

    @Override
    public void subGuiClosed(Screen subgui) {
        init();
    }

    private static final class SoundPicker extends GuiSoundSelection {
        private final Consumer<String> action;

        SoundPicker(String current, Consumer<String> action) {
            super(current);
            this.action = action;
        }

        @Override
        public void close() {
            if (selectedResource != null) action.accept(selectedResource.toString());
            super.close();
        }
    }
}
