package com.goodbird.cnpcgeckoaddon.client.gui;

import com.goodbird.cnpcgeckoaddon.data.CustomModelDataProvider;
import com.goodbird.cnpcgeckoaddon.data.ICustomModelData;
import com.goodbird.cnpcgeckoaddon.utils.NpcTextureUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import noppes.npcs.client.gui.select.GuiSoundSelection;
import noppes.npcs.client.gui.util.*;
import noppes.npcs.entity.EntityNPCInterface;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import software.bernie.geckolib3.file.AnimationFile;
import software.bernie.geckolib3.resource.GeckoLibCache;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** The 1.20.1 editor layout, adapted to CNPC's 1.12.2 screen lifecycle. */
public class GuiGeckoEditor extends SubGuiInterface implements ITextfieldListener {
    private int tab, section, modelOffset;
    private final int[] offsets = new int[4];
    private int leftWidth, contentX, contentY, contentWidth, bottom, listTop, visibleRows;
    private int browseTarget, browseOffset, directoryOffset, directoryWidth, fileX, fileWidth;
    private float rotation, zoom = 1;
    private int dragX;
    private boolean rotating, draggingScroll, draggingDirectories, normalizedDisplay, openingSound;
    private String message = "", candidate = "", query = "", browseNamespace = "", browsePath = "";
    private List<String> options = Collections.emptyList(), filtered = Collections.emptyList();
    private List<Directory> directories = Collections.emptyList(), allDirectories = Collections.emptyList();
    private final Set<String> expanded = new HashSet<>();
    private final Map<String, ModLogo> logos = new HashMap<>();
    private final List<GuiNpcTextField> fields = new ArrayList<>();
    private final Map<Integer, String> animationValues = new HashMap<>();
    private final List<Header> headers = new ArrayList<>();
    private GeckoPreview preview;

    public GuiGeckoEditor(GuiScreen parent, EntityNPCInterface npc, int tab) {
        this.parent = parent;
        this.npc = npc;
        this.tab = tab == 1 ? 1 : 0;
        closeOnEsc = true;
        drawDefaultBackground = false;
    }

    public static String text(String key) { return I18n.format("cnpcgeckoaddon.gui." + key); }
    public ICustomModelData getModelData(EntityNPCInterface npc) { return npc.getCapability(CustomModelDataProvider.DATA_CAP, null); }
    private ICustomModelData data() { return getModelData(npc); }

    @Override public void initGui() {
        // Commit while old field IDs still refer to the old section/offset.
        GuiNpcTextField.unfocus();
        xSize = Math.min(width - 12, 680);
        ySize = Math.min(height - 12, 390);
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        if (!normalizedDisplay) {
            normalizedDisplay = true;
            npc.display.skinType = 0;
            npc.textureLocation = null;
        }
        if (preview == null) preview = new GeckoPreview(npc);
        leftWidth = Math.max(92, xSize / 3);
        contentX = guiLeft + leftWidth + 12;
        contentY = guiTop + 52;
        contentWidth = xSize - leftWidth - 22;
        bottom = guiTop + ySize - 30;
        fields.clear(); animationValues.clear(); headers.clear();
        int tw = (contentWidth - 2) / 2;
        button(800, contentX, guiTop + 26, tw, 20, text("model_material")).enabled = tab != 0;
        button(801, contentX + tw + 2, guiTop + 26, contentWidth - tw - 2, 20, text("animations")).enabled = tab != 1;
        button(810, guiLeft + xSize - 80, guiTop + ySize - 24, 72, 20, browseTarget == 0 ? "gui.done" : "gui.back");
        button(811, guiLeft + 8, guiTop + ySize - 24, leftWidth - 4, 20, text("reset_view"));
        if (browseTarget != 0) createBrowser();
        else if (tab == 0) createModelPage();
        else createAnimationPage();
    }

    private GuiNpcButton button(int id, int x, int y, int w, int h, String label) {
        GuiNpcButton button = new GuiNpcButton(id, x, y, w, h, label);
        addButton(button);
        return button;
    }
    private GuiNpcTextField field(int id, int x, int y, int w, String value) {
        GuiNpcTextField field = new GuiNpcTextField(id, this, fontRenderer, x, y, w, 18, value);
        field.setMaxStringLength(512);
        addTextField(field); fields.add(field);
        return field;
    }
    private void label(int id, String text, int x, int y) { addLabel(new GuiNpcLabel(id, text, x, y, 0x404040)); }
    private void resourceField(int id, int y, String label, String value) {
        label(id, text(label), contentX, y);
        field(id, contentX, y + 11, contentWidth - 46, value);
        button(id, contentX + contentWidth - 42, y + 10, 32, 20, "...");
    }
    private void createModelPage() {
        listTop = contentY;
        visibleRows = Math.max(1, (bottom - listTop) / 34);
        modelOffset = MathHelper.clamp(modelOffset, 0, Math.max(0, 9 - visibleRows));
        String[] labels = {"outer_model", "skin", "overlay", "overlay_glow", "head_bone", "transition_length", "hitbox_width", "hitbox_height", "hurt_tint"};
        String[] values = {data().getModel(), npc.display.getSkinTexture(), npc.display.getOverlayTexture(), "", data().getHeadBoneName(), "" + data().getTransitionLengthTicks(), "" + data().getWidth(), "" + data().getHeight()};
        for (int row = 0; row < Math.min(visibleRows, 9 - modelOffset); row++) {
            int index = modelOffset + row, y = listTop + row * 34;
            if (index < 3) resourceField(10 + index, y, labels[index], values[index]);
            else if (index == 3 || index == 8) {
                int id = index == 3 ? 35 : 34;
                label(id, fontRenderer.trimStringToWidth(text(labels[index]), contentWidth - 64), contentX, y + 8);
                addButton(new GuiNpcButtonYesNo(id, contentX + contentWidth - 58, y + 2, 48, 20,
                        index == 3 ? data().isOverlayGlowing() : data().isHurtTintEnabled()));
            } else {
                int id = 30 + index - 4;
                label(id, text(labels[index]), contentX, y);
                GuiNpcTextField input = field(id, contentX, y + 11, contentWidth - 10, values[index]);
                if (id == 31) { input.setNumbersOnly(); input.setMinMaxDefault(0, Integer.MAX_VALUE, data().getTransitionLengthTicks()); }
            }
        }
    }
    private void createAnimationPage() {
        resourceField(20, contentY, "animfile", data().getAnimFile());
        int w = (contentWidth - 6) / 4;
        String[] sections = {"base", "attack", "hurt", "death"};
        for (int i = 0; i < 4; i++) button(820 + i, contentX + i * (w + 2), contentY + 35,
                i == 3 ? contentWidth - 3 * (w + 2) : w, 20, text(sections[i])).enabled = section != i;
        if (section == 0) {
            resourceField(21, contentY + 64, "idle", data().getIdleAnim());
            resourceField(22, contentY + 100, "walk", data().getWalkAnim());
            animationValues.put(21, data().getIdleAnim()); animationValues.put(22, data().getWalkAnim());
            label(23, fontRenderer.trimStringToWidth(text("hover_preview"), contentWidth), contentX, contentY + 136);
            return;
        }
        listTop = contentY + 76;
        visibleRows = Math.max(1, (bottom - 24 - listTop) / 24);
        offsets[section] = MathHelper.clamp(offsets[section], 0, Math.max(0, count() - visibleRows));
        int weightW = contentWidth < 240 ? 20 : 22, frameW = section == 2 ? 0 : contentWidth < 240 ? 24 : 28;
        int soundW = contentWidth < 240 ? 18 : 24, secW = contentWidth < 240 ? 22 : 26;
        int nameW = contentWidth - 10 - (12 + 14 + weightW + frameW + soundW + 8 + (section == 3 ? secW + 2 : 0));
        int nameX = contentX + 12, nameFieldW = Math.max(22, nameW - 20), weightX = nameX + nameW + 2;
        int frameX = weightX + weightW + 2, secX = frameX + frameW + 2;
        int soundX = (section == 3 ? secX + secW : frameX + frameW) + 2, delX = contentX + contentWidth - 24;
        headers.add(new Header("#", contentX + 2, listTop - 13));
        headers.add(new Header(text("col_name"), nameX, listTop - 13));
        headers.add(new Header(text("short_weight"), weightX, listTop - 13));
        if (section != 2) headers.add(new Header(text(section == 1 ? "short_frame" : "short_hp"), frameX, listTop - 13));
        if (section == 3) headers.add(new Header(text("seconds"), secX, listTop - 13));
        headers.add(new Header(text("short_sound"), soundX, listTop - 13));
        for (int row = 0; row < Math.min(visibleRows, count() - offsets[section]); row++) {
            int index = offsets[section] + row, y = listTop + row * 24;
            label(100 + index, "" + (index + 1), contentX + 2, y + 7);
            field(100 + index, nameX, y + 3, nameFieldW, names()[index]);
            animationValues.put(100 + index, names()[index]);
            button(300 + index, nameX + nameFieldW + 1, y + 2, 18, 18, "...");
            GuiNpcTextField weight = field(200 + index, weightX, y + 3, weightW, "" + weights()[index]);
            weight.setNumbersOnly(); weight.setMinMaxDefault(0, 100, 1);
            if (section != 2) field(400 + index, frameX, y + 3, frameW, "" + (section == 1 ? data().getAttackFrames()[index] : data().getDeathHealthThresholds()[index]));
            if (section == 3) field(500 + index, secX, y + 3, secW, "" + data().getDeathAnimDurations()[index]);
            String sound = sounds()[index];
            button(600 + index, soundX, y + 2, soundW, 18, text("short_sound") + (sound == null || sound.isEmpty() ? "" : "*"));
            button(700 + index, delX, y + 2, 14, 18, "x");
        }
        button(830, contentX, bottom - 21, 70, 20, "+ " + text("add")).enabled = count() < 20;
        label(830, count() + " / 20", contentX + 76, bottom - 15);
    }
    private int count() { return section == 1 ? data().getAttackCount() : section == 2 ? data().getHurtAnimCount() : data().getDeathAnimCount(); }
    private String[] names() { return section == 1 ? data().getAttackAnimNames() : section == 2 ? data().getHurtAnimNames() : data().getDeathAnimNames(); }
    private int[] weights() { return section == 1 ? data().getAttackWeights() : section == 2 ? data().getHurtWeights() : data().getDeathWeights(); }
    private String[] sounds() { return section == 1 ? data().getAttackSoundNames() : data().getHurtSoundNames(); }
    private AnimationFile animationFile() { return GeckoLibCache.getInstance().getAnimations().get(EditorResources.parse(data().getAnimFile())); }
    public boolean isValidAnimFile(String value) { return GeckoLibCache.getInstance().getAnimations().containsKey(EditorResources.parse(value)); }
    public boolean isValidAnimation(String value) { return value.isEmpty() || animationFile() != null && animationFile().getAnimation(value) != null; }
    private boolean validResource(int target, String value) {
        if (target == 21 || target == 22 || target >= 100 && target < 120) return isValidAnimation(value);
        ResourceLocation id = EditorResources.parse(value);
        if (target == 10) return GeckoLibCache.getInstance().getGeoModels().containsKey(id);
        if (target == 20) return isValidAnimFile(value);
        return target == 12 && value.isEmpty() || id != null;
    }
    private void openBrowser(int target) {
        GuiNpcTextField.unfocus();
        candidate = getTextField(target).getText(); browseTarget = target;
        message = ""; query = ""; browseOffset = 0; directoryOffset = 0;
        expanded.clear(); browseNamespace = ""; browsePath = "";
        if (target == 10) options = GeckoLibCache.getInstance().getGeoModels().keySet().stream().map(Object::toString).sorted().collect(Collectors.toList());
        else if (target == 20) options = GeckoLibCache.getInstance().getAnimations().keySet().stream().map(Object::toString).sorted().collect(Collectors.toList());
        else if (target == 11 || target == 12) {
            options = EditorResources.textures(); browseNamespace = "customnpcs";
            browsePath = target == 11 ? "textures/entity/humanmale/" : EditorResources.overlayRoot();
        } else options = animationFile() == null ? Collections.emptyList() : animationFile().getAllAnimations().stream().map(a -> a.animationName).sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toList());
        List<Directory> tree = new ArrayList<>(); tree.add(new Directory("", "", 0, false));
        if (target < 21) {
            List<String> spaces = options.stream().map(this::namespaceOf).distinct().sorted(Comparator.comparingInt(this::priority).thenComparing(String::compareTo)).collect(Collectors.toList());
            for (String space : spaces) {
                Set<String> paths = new TreeSet<>(), parents = new HashSet<>();
                for (String value : options) {
                    if (!namespaceOf(value).equals(space)) continue;
                    String path = pathOf(value);
                    for (int slash = path.indexOf('/'); slash >= 0; slash = path.indexOf('/', slash + 1)) paths.add(path.substring(0, slash + 1));
                }
                for (String path : paths) parents.add(path.substring(0, path.lastIndexOf('/', path.length() - 2) + 1));
                tree.add(new Directory(space, "", 0, !paths.isEmpty()));
                for (String path : paths) tree.add(new Directory(space, path, (int)path.chars().filter(c -> c == '/').count(), parents.contains(path)));
            }
        }
        allDirectories = tree;
        if (!browseNamespace.isEmpty()) {
            expanded.add(browseNamespace + ":");
            for (int slash = browsePath.indexOf('/'); slash >= 0; slash = browsePath.indexOf('/', slash + 1)) expanded.add(browseNamespace + ":" + browsePath.substring(0, slash + 1));
        }
        initGui();
    }
    private void updateFiltered() {
        String q = query.trim().toLowerCase(Locale.ROOT);
        List<Directory> tree = new ArrayList<>(); Set<String> visible = new HashSet<>();
        for (Directory dir : allDirectories) {
            String parent = dir.namespace + ":" + dir.path.substring(0, dir.path.lastIndexOf('/', dir.path.length() - 2) + 1);
            if (!dir.path.isEmpty() && (!visible.contains(parent) || !expanded.contains(parent))) continue;
            tree.add(dir); visible.add(dir.key());
        }
        directories = tree;
        directoryOffset = MathHelper.clamp(directoryOffset, 0, Math.max(0, tree.size() - visibleRows));
        filtered = options.stream().filter(v -> browseNamespace.isEmpty() || namespaceOf(v).equals(browseNamespace))
                .filter(v -> browsePath.isEmpty() || pathOf(v).startsWith(browsePath))
                .filter(v -> q.isEmpty() || v.toLowerCase(Locale.ROOT).contains(q)).collect(Collectors.toList());
        browseOffset = MathHelper.clamp(browseOffset, 0, Math.max(0, filtered.size() - visibleRows));
    }
    private void createBrowser() {
        directoryWidth = Math.min(154, contentWidth * 2 / 5); fileX = contentX + directoryWidth + 6; fileWidth = contentWidth - directoryWidth - 6;
        label(900, text("search"), contentX, contentY);
        field(901, contentX + 36, contentY - 4, contentWidth - 36, query);
        listTop = contentY + 36; visibleRows = Math.max(1, (bottom - 70 - listTop) / 22); updateFiltered();
        label(902, text("value"), contentX, bottom - 65);
        field(902, contentX, bottom - 54, contentWidth, candidate);
        button(840, contentX, bottom - 26, (contentWidth - 4) / 2, 20, text("apply"));
        button(841, contentX + (contentWidth + 4) / 2, bottom - 26, (contentWidth - 4) / 2, 20, "gui.cancel");
    }
    @Override public void buttonEvent(GuiButton button) {
        GuiNpcTextField.unfocus();
        int id = button.id;
        if (id == 810) { close(); return; }
        if (id == 811) { rotation = 0; zoom = 1; return; }
        if (id == 800 || id == 801) { browseTarget = 0; tab = id - 800; message = ""; }
        else if (id >= 820 && id <= 823) { section = id - 820; message = ""; }
        else if (id == 840) {
            if (!validResource(browseTarget, candidate)) { message = text("invalid_resource"); return; }
            applyResource(browseTarget, candidate); browseTarget = 0; message = "";
        } else if (id == 841) { browseTarget = 0; message = ""; }
        else if (id == 34) data().setEnableHurtTint(((GuiNpcButtonYesNo)button).getBoolean());
        else if (id == 35) { data().setOverlayGlowing(((GuiNpcButtonYesNo)button).getBoolean()); npc.updateClient = true; }
        else if (id == 830) {
            if (section == 1) data().addAttack(); else if (section == 2) data().addHurtAnim(); else data().addDeathAnim();
            offsets[section] = Math.max(0, count() - visibleRows);
        } else if (id >= 700 && id < 720) {
            if (section == 1) data().removeAttack(id - 700); else if (section == 2) data().removeHurtAnim(id - 700); else data().removeDeathAnim(id - 700);
        } else if (id >= 600 && id < 620) {
            final String[] values = sounds(); final int index = id - 600;
            String safe = EditorResources.parse(values[index]) == null ? "" : values[index];
            openingSound = true;
            mc.displayGuiScreen(new SoundPicker(safe, value -> values[index] = value, this));
            openingSound = false;
            return;
        } else if (id >= 300 && id < 320) { openBrowser(id - 200); return; }
        else if (id == 10 || id == 11 || id == 12 || id == 20 || id == 21 || id == 22) { openBrowser(id); return; }
        initGui();
    }
    private void applyResource(int target, String value) {
        switch (target) {
            case 10: data().setModel(value); break;
            case 11: npc.display.skinType = 0; npc.display.setSkinTexture(value); npc.textureLocation = null; break;
            case 12: npc.display.setOverlayTexture(value); break;
            case 20: data().setAnimFile(value); break;
            case 21: data().setIdleAnim(value); break;
            case 22: data().setWalkAnim(value); break;
            default: names()[target - 100] = value;
        }
    }
    @Override public void unFocused(GuiNpcTextField field) {
        int id = field.getId(); String value = field.getText().trim();
        if (id == 901) { query = value; browseOffset = 0; updateFiltered(); return; }
        if (id == 902) { candidate = value; return; }
        if (id == 10 || id == 11 || id == 12 || id == 20 || animationValues.containsKey(id)) {
            if (validResource(id, value)) { applyResource(id, value); message = ""; if (animationValues.containsKey(id)) animationValues.put(id, value); }
            else {
                message = text(animationValues.containsKey(id) ? "invalid_animation" : "invalid_resource");
                field.setText(id == 10 ? data().getModel() : id == 11 ? npc.display.getSkinTexture() : id == 12 ? npc.display.getOverlayTexture() : id == 20 ? data().getAnimFile() : animationValues.get(id));
            }
        } else if (id == 30) data().setHeadBoneName(value);
        else if (id == 31) data().setTransitionLengthTicks(field.getInteger());
        else if (id == 32) data().setWidth(number(field, 0, Float.MAX_VALUE, data().getWidth()));
        else if (id == 33) data().setHeight(number(field, 0, Float.MAX_VALUE, data().getHeight()));
        else if (id >= 200 && id < 220) weights()[id - 200] = field.getInteger();
        else if (id >= 400 && id < 420) {
            if (section == 1) data().getAttackFrames()[id - 400] = number(field, 0, 1000, 0);
            else data().getDeathHealthThresholds()[id - 400] = number(field, 0.1F, 100, 100);
        } else if (id >= 500 && id < 520) data().getDeathAnimDurations()[id - 500] = number(field, 0.1F, 9999, 5);
    }
    private float number(GuiNpcTextField field, float min, float max, float fallback) {
        float value = fallback;
        try { float parsed = Float.parseFloat(field.getText().replace(',', '.')); if (Float.isFinite(parsed)) value = MathHelper.clamp(parsed, min, max); }
        catch (NumberFormatException ignored) { }
        field.setText(Float.toString(value)); return value;
    }
    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, width, height, 0xd0101010);
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xffc6c6c6);
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + 2, 0xffffffff);
        drawRect(guiLeft, guiTop, guiLeft + 2, guiTop + ySize, 0xffffffff);
        drawRect(guiLeft, guiTop + ySize - 2, guiLeft + xSize, guiTop + ySize, 0xff555555);
        drawRect(guiLeft + xSize - 2, guiTop, guiLeft + xSize, guiTop + ySize, 0xff555555);
        fontRenderer.drawString(text("editor"), guiLeft + 9, guiTop + 9, 0x404040);
        int px = guiLeft + 8, py = guiTop + 26, pw = leftWidth - 4, ph = bottom - py;
        drawRect(px - 1, py - 1, px + pw + 1, py + ph + 1, 0xff555555);
        drawRect(px, py, px + pw, py + ph, 0xff252525);
        String model = data().getModel(), animFile = data().getAnimFile(), animation = data().getIdleAnim();
        ResourceLocation texture = NpcTextureUtils.getNpcTexture(npc), overlay = EditorResources.parse(npc.display.getOverlayTexture());
        for (GuiNpcTextField field : fields) if (browseTarget == 0 && animationValues.containsKey(field.getId()) && over(field, mouseX, mouseY) && isValidAnimation(field.getText())) animation = field.getText();
        if (browseTarget != 0 && validResource(browseTarget, candidate)) {
            if (browseTarget == 10) model = candidate;
            else if (browseTarget == 20) {
                animFile = candidate;
                AnimationFile file = GeckoLibCache.getInstance().getAnimations().get(EditorResources.parse(animFile));
                if (file.getAnimation(animation) == null) animation = file.getAllAnimations().stream().map(a -> a.animationName).sorted().findFirst().orElse("");
            } else if (browseTarget == 11) texture = EditorResources.parse(candidate);
            else if (browseTarget == 12) overlay = EditorResources.parse(candidate);
            else animation = candidate;
        }
        int swatchHeight = browseTarget == 11 || browseTarget == 12 ? Math.min(leftWidth - 20, 64) + 8 : 0;
        preview.render(px, py, pw, ph - 17 - swatchHeight, npc, model, animFile, texture, overlay, animation, rotation, zoom);
        fontRenderer.drawString(fontRenderer.trimStringToWidth(animation.isEmpty() ? text("preview") : animation, pw - 12), px + 6, py + ph - 13, 0xcccccc);
        if (browseTarget != 0) renderBrowser(mouseX, mouseY);
        else if (tab == 0) scrollBar(contentX + contentWidth - 6, 9, modelOffset, 34);
        else if (section != 0) {
            for (int row = 0; row < Math.min(visibleRows, count() - offsets[section]); row++) drawRect(contentX, listTop + row * 24, contentX + contentWidth, listTop + row * 24 + 23, (row & 1) == 0 ? 0xffb6b6b6 : 0xffc0c0c0);
            for (Header header : headers) fontRenderer.drawString(header.value, header.x, header.y, 0x555555);
            scrollBar(contentX + contentWidth - 6, count(), offsets[section], 24);
        }
        if (!message.isEmpty()) fontRenderer.drawString(fontRenderer.trimStringToWidth(message, contentWidth - 80), contentX, guiTop + ySize - 17, 0xaa2222);
        super.drawScreen(mouseX, mouseY, partialTicks);
        String tooltip = "";
        for (GuiNpcTextField field : fields) {
            if (!over(field, mouseX, mouseY)) continue;
            int id = field.getId();
            if (animationValues.containsKey(id)) tooltip = field.getText().isEmpty() ? text("hover_preview") : field.getText();
            else if (id >= 200 && id < 220) tooltip = text("weight_help");
            else if (id >= 400 && id < 420) tooltip = text(section == 1 ? "frame_help" : "hp_help");
            else if (id >= 500 && id < 520) tooltip = text("duration_help");
            else if (id == 11) tooltip = text("skin_only");
            else if (id == 12) tooltip = text("overlay_hint");
            else if (id == 32 || id == 33) tooltip = text("hitbox_help");
        }
        for (GuiButton b : buttonList) if (mouseX >= b.x && mouseX < b.x + b.width && mouseY >= b.y && mouseY < b.y + b.height) {
            if (b.id == 35) tooltip = text("overlay_glow_hint");
            else if (b.id >= 700 && b.id < 720) tooltip = text("delete_help");
            else if (b.id >= 600 && b.id < 620) tooltip = sounds()[b.id - 600];
        }
        if (browseTarget != 0 && mouseY >= listTop && mouseY < listTop + visibleRows * 22) {
            int row = (mouseY - listTop) / 22;
            if (mouseX >= contentX && mouseX < contentX + directoryWidth - 10 && directoryOffset + row < directories.size()) {
                Directory dir = directories.get(directoryOffset + row); tooltip = namespaceLabel(dir.namespace) + ": " + dir.path;
            } else if (mouseX >= fileX && mouseX < fileX + fileWidth - 10 && browseOffset + row < filtered.size()) tooltip = filtered.get(browseOffset + row);
        }
        if (mouseX >= px && mouseX < px + pw && mouseY >= py && mouseY < py + ph) tooltip = text("view_help") + "\n" + text("model_bounds_help") + "\n" + text("hitbox_size_help");
        if (tooltip != null && !tooltip.isEmpty()) drawHoveringText(fontRenderer.listFormattedStringToWidth(tooltip, Math.min(260, width - 20)), mouseX, mouseY);
    }
    private boolean over(GuiNpcTextField field, int x, int y) { return x >= field.x && x < field.x + field.width && y >= field.y && y < field.y + field.height; }
    private void renderBrowser(int mx, int my) {
        drawRect(contentX, listTop, contentX + directoryWidth, listTop + visibleRows * 22, 0xff202020);
        drawRect(fileX, listTop, fileX + fileWidth, listTop + visibleRows * 22, 0xff202020);
        fontRenderer.drawString(text("directories"), contentX, listTop - 12, 0x666666);
        fontRenderer.drawString(fontRenderer.trimStringToWidth(namespaceLabel(browseNamespace) + ": " + browsePath, fileWidth - 8), fileX, listTop - 12, 0x666666);
        for (int row = 0; row < Math.min(visibleRows, directories.size() - directoryOffset); row++) {
            Directory dir = directories.get(directoryOffset + row); int y = listTop + row * 22;
            boolean selected = dir.namespace.equals(browseNamespace) && dir.path.equals(browsePath);
            if (selected || mx >= contentX && mx < contentX + directoryWidth - 10 && my >= y && my < y + 22) drawRect(contentX + 1, y + 1, contentX + directoryWidth - 10, y + 21, selected ? 0xff555555 : 0xff383838);
            int x = contentX + 3 + Math.min(dir.depth, 3) * 8;
            if (dir.children) fontRenderer.drawString(expanded.contains(dir.key()) ? "-" : "+", x, y + 7, 0xaaaaaa);
            x += 9; String name;
            if (dir.path.isEmpty()) { drawModIcon(dir.namespace, x, y + 3); x += 19; name = namespaceLabel(dir.namespace); }
            else { drawRect(x, y + 6, x + 8, y + 14, 0xffc6a256); x += 11; name = dir.path.substring(dir.path.lastIndexOf('/', dir.path.length() - 2) + 1, dir.path.length() - 1); }
            fontRenderer.drawString(fontRenderer.trimStringToWidth(name, Math.max(0, contentX + directoryWidth - 12 - x)), x, y + 7, 0xffffff);
        }
        for (int row = 0; row < Math.min(visibleRows, filtered.size() - browseOffset); row++) {
            String value = filtered.get(browseOffset + row); int y = listTop + row * 22;
            if (value.equals(candidate) || mx >= fileX && mx < fileX + fileWidth - 10 && my >= y && my < y + 22) drawRect(fileX + 1, y + 1, fileX + fileWidth - 10, y + 21, value.equals(candidate) ? 0xff555555 : 0xff383838);
            String label = browseNamespace.isEmpty() ? value : pathOf(value).substring(browsePath.length());
            fontRenderer.drawString(fontRenderer.trimStringToWidth(label, fileWidth - 18), fileX + 4, y + 7, 0xffffff);
        }
        if (filtered.isEmpty()) fontRenderer.drawString(fontRenderer.trimStringToWidth(text("no_results"), fileWidth - 18), fileX + 4, listTop + 7, 0xaaaaaa);
        scrollBar(contentX + directoryWidth - 6, directories.size(), directoryOffset, 22);
        scrollBar(fileX + fileWidth - 6, filtered.size(), browseOffset, 22);
        if (browseTarget == 11 || browseTarget == 12) {
            ResourceLocation texture = EditorResources.parse(candidate);
            if (texture != null) {
                int size = Math.min(leftWidth - 20, 64), x = guiLeft + 8 + (leftWidth - 4 - size) / 2, y = bottom - 23 - size;
                drawRect(x - 1, y - 1, x + size + 1, y + size + 1, 0xff777777);
                image(texture, x, y, size, size);
            }
        }
    }
    private void image(ResourceLocation texture, int x, int y, int w, int h) {
        GlStateManager.color(1, 1, 1, 1); GlStateManager.enableBlend(); GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        mc.getTextureManager().bindTexture(texture);
        drawModalRectWithCustomSizedTexture(x, y, 0, 0, w, h, w, h);
        GlStateManager.disableBlend();
    }
    private void drawModIcon(String namespace, int x, int y) {
        if (!logos.containsKey(namespace)) {
            ModLogo logo = null; ModContainer mod = Loader.instance().getIndexedModList().get(namespace);
            if (mod != null && mod.getMetadata() != null && mod.getMetadata().logoFile != null && !mod.getMetadata().logoFile.isEmpty()) {
                String path = mod.getMetadata().logoFile; BufferedImage image = null;
                try {
                    if (mod.getSource().isDirectory()) image = ImageIO.read(new File(mod.getSource(), path));
                    else try (ZipFile zip = new ZipFile(mod.getSource())) {
                        ZipEntry entry = zip.getEntry(path);
                        if (entry != null) try (InputStream stream = zip.getInputStream(entry)) { image = ImageIO.read(stream); }
                    }
                    if (image != null) logo = new ModLogo(mc.getTextureManager().getDynamicTextureLocation("gecko_modlogo", new DynamicTexture(image)), image.getWidth(), image.getHeight());
                } catch (IOException ignored) { }
            }
            logos.put(namespace, logo);
        }
        ModLogo logo = logos.get(namespace);
        if (logo != null) {
            float scale = 16F / Math.max(logo.width, logo.height); int w = Math.max(1, Math.round(logo.width * scale)), h = Math.max(1, Math.round(logo.height * scale));
            image(logo.texture, x + (16 - w) / 2, y + (16 - h) / 2, w, h);
        } else {
            drawRect(x, y, x + 16, y + 16, 0xff3a3a3a);
            fontRenderer.drawString(namespace.isEmpty() ? "=" : namespace.substring(0, 1).toUpperCase(Locale.ROOT), x + 5, y + 4, 0xffffff);
        }
    }
    private String namespaceLabel(String ns) { return ns.isEmpty() ? text("all") : EditorResources.displayName(ns); }
    private int priority(String ns) { return ns.equals("cnpcgeckoaddon") ? 0 : ns.equals("customnpcs") ? 1 : ns.equals("minecraft") ? 2 : 3; }
    private String namespaceOf(String value) { ResourceLocation id = EditorResources.parse(value); return id == null ? "" : id.getNamespace(); }
    private String pathOf(String value) { ResourceLocation id = EditorResources.parse(value); return id == null ? value : id.getPath(); }
    private void scrollBar(int x, int count, int offset, int rowH) {
        if (count <= visibleRows) return;
        int h = visibleRows * rowH, thumb = Math.max(10, h * visibleRows / count), y = listTop + (h - thumb) * offset / (count - visibleRows);
        drawRect(x, listTop, x + 6, listTop + h, 0xff555555); drawRect(x + 1, y, x + 5, y + thumb, 0xffc6c6c6);
    }
    @Override public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int delta = Mouse.getEventDWheel(); if (delta == 0) return;
        int x = Mouse.getEventX() * width / mc.displayWidth, y = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        if (x >= guiLeft + 8 && x < guiLeft + leftWidth + 4 && y >= guiTop + 26 && y < bottom) { zoom = MathHelper.clamp(zoom * (delta > 0 ? 1.12F : 1 / 1.12F), 0.1F, 8); return; }
        int rh = browseTarget != 0 ? 22 : tab == 0 ? 34 : 24;
        if (x < contentX || x >= contentX + contentWidth || y < listTop || y >= listTop + visibleRows * rh || browseTarget == 0 && tab == 1 && section == 0) return;
        GuiNpcTextField.unfocus(); int step = delta > 0 ? -1 : 1;
        if (browseTarget != 0) {
            if (x < contentX + directoryWidth) directoryOffset = MathHelper.clamp(directoryOffset + step, 0, Math.max(0, directories.size() - visibleRows));
            else browseOffset = MathHelper.clamp(browseOffset + step, 0, Math.max(0, filtered.size() - visibleRows));
        } else { if (tab == 0) modelOffset = MathHelper.clamp(modelOffset + step, 0, Math.max(0, 9 - visibleRows));
            else offsets[section] = MathHelper.clamp(offsets[section] + step, 0, Math.max(0, count() - visibleRows)); initGui(); }
    }
    @Override public void mouseClicked(int x, int y, int button) {
        if (button == 0 && x >= guiLeft + 8 && x < guiLeft + leftWidth + 4 && y >= guiTop + 26 && y < bottom) { GuiNpcTextField.unfocus(); rotating = true; dragX = x; return; }
        int rh = browseTarget != 0 ? 22 : tab == 0 ? 34 : 24;
        if (button == 0 && (browseTarget != 0 || tab == 0 || section != 0) && y >= listTop && y < listTop + visibleRows * rh) {
            boolean dirBar = browseTarget != 0 && x >= contentX + directoryWidth - 6 && x < contentX + directoryWidth;
            if (dirBar || x >= contentX + contentWidth - 6 && x < contentX + contentWidth) {
                draggingScroll = true; draggingDirectories = dirBar; scrollTo(y, rh); return;
            }
            if (browseTarget != 0) {
                int row = (y - listTop) / rh;
                if (x >= contentX && x < contentX + directoryWidth - 10) {
                    if (directoryOffset + row < directories.size()) {
                        GuiNpcTextField.unfocus(); Directory dir = directories.get(directoryOffset + row);
                        if (dir.children && x < contentX + 12 + Math.min(dir.depth, 3) * 8) { if (!expanded.remove(dir.key())) expanded.add(dir.key()); }
                        else { browseNamespace = dir.namespace; browsePath = dir.path; browseOffset = 0; expanded.add(dir.key()); }
                        updateFiltered();
                    } return;
                }
                if (x >= fileX && x < fileX + fileWidth - 10) {
                    if (browseOffset + row < filtered.size()) { GuiNpcTextField.unfocus(); candidate = filtered.get(browseOffset + row); getTextField(902).setText(candidate); message = ""; } return;
                }
            }
        }
        super.mouseClicked(x, y, button);
    }
    private void scrollTo(int y, int rh) {
        GuiNpcTextField.unfocus();
        int max = Math.max(0, (browseTarget != 0 ? draggingDirectories ? directories.size() : filtered.size() : tab == 0 ? 9 : count()) - visibleRows);
        int offset = MathHelper.clamp(Math.round((y - listTop) / (float)(visibleRows * rh) * max), 0, max);
        if (browseTarget != 0) { if (draggingDirectories) directoryOffset = offset; else browseOffset = offset; }
        else { if (tab == 0) modelOffset = offset; else offsets[section] = offset; initGui(); }
    }
    @Override protected void mouseClickMove(int x, int y, int button, long elapsed) {
        if (rotating) { rotation += (x - dragX) * 2; dragX = x; }
        else if (draggingScroll) scrollTo(y, browseTarget != 0 ? 22 : tab == 0 ? 34 : 24);
        else super.mouseClickMove(x, y, button, elapsed);
    }
    @Override public void mouseReleased(int x, int y, int button) { rotating = false; draggingScroll = false; draggingDirectories = false; super.mouseReleased(x, y, button); }
    @Override public void keyTyped(char c, int key) {
        if (browseTarget != 0 && (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER)) { buttonEvent(getButton(840)); return; }
        super.keyTyped(c, key);
        if (browseTarget != 0) {
            String next = getTextField(901).getText();
            if (!next.equals(query)) { query = next; browseOffset = 0; updateFiltered(); }
            candidate = getTextField(902).getText().trim();
        }
    }
    @Override public void close() {
        GuiNpcTextField.unfocus();
        if (browseTarget != 0) { browseTarget = 0; message = ""; initGui(); return; }
        if (parent instanceof GuiNPCInterface && ((GuiNPCInterface)parent).getSubGui() == this) super.close();
        else mc.displayGuiScreen(parent);
    }
    @Override public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
        if (openingSound) return;
        for (ModLogo logo : logos.values()) if (logo != null) mc.getTextureManager().deleteTexture(logo.texture);
        logos.clear();
    }
    private static final class Directory {
        final String namespace, path; final int depth; final boolean children;
        Directory(String namespace, String path, int depth, boolean children) { this.namespace = namespace; this.path = path; this.depth = depth; this.children = children; }
        String key() { return namespace + ":" + path; }
    }
    private static final class Header {
        final String value; final int x, y;
        Header(String value, int x, int y) { this.value = value; this.x = x; this.y = y; }
    }
    private static final class ModLogo {
        final ResourceLocation texture; final int width, height;
        ModLogo(ResourceLocation texture, int width, int height) { this.texture = texture; this.width = width; this.height = height; }
    }
    private static final class SoundPicker extends GuiSoundSelection {
        private final Consumer<String> action; private final GuiScreen returnScreen;
        SoundPicker(String sound, Consumer<String> action, GuiScreen returnScreen) {
            super(sound); this.action = action; this.returnScreen = returnScreen; closeOnEsc = true; drawDefaultBackground = true;
        }
        @Override public void close() { if (selectedResource != null) action.accept(selectedResource.toString()); Minecraft.getMinecraft().displayGuiScreen(returnScreen); }
    }
}
