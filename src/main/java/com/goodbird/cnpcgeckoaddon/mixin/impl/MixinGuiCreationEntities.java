package com.goodbird.cnpcgeckoaddon.mixin.impl;

import com.goodbird.cnpcgeckoaddon.client.gui.GeckoPreview;
import com.goodbird.cnpcgeckoaddon.client.gui.GuiGeckoEditor;
import com.goodbird.cnpcgeckoaddon.data.CustomModelData;
import com.goodbird.cnpcgeckoaddon.entity.EntityCustomModel;
import com.goodbird.cnpcgeckoaddon.mixin.IDataDisplay;
import com.goodbird.cnpcgeckoaddon.utils.NpcTextureUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import noppes.npcs.client.gui.model.GuiCreationEntities;
import noppes.npcs.client.gui.model.GuiCreationScreenInterface;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

@Mixin(GuiCreationEntities.class)
public class MixinGuiCreationEntities extends GuiCreationScreenInterface {

    @Unique
    private GeckoPreview gecko$preview;

    public MixinGuiCreationEntities() {
        super(null);
    }

    @Inject(method = "init",at = @At("TAIL"))
    public void init(CallbackInfo ci){
        if(npc instanceof EntityCustomNpc customNpc && customNpc.modelData.getEntity(npc) instanceof EntityCustomModel) {
            this.addButton(new GuiButtonNop(this, 202, guiLeft + 126, guiTop + 25, imageWidth - 150, 20,
                    GuiGeckoEditor.text("editor"), b -> setSubGui(new GuiGeckoEditor(npc, 0))));
        }
    }

    @Override
    public void drawNpc(GuiGraphics graphics, LivingEntity entity, int x, int y, float zoomed, int rotation) {
        if(wrapper.subgui != null) return;
        if (!(playerdata.getEntity(npc) instanceof EntityCustomModel)) {
            super.drawNpc(graphics, entity, x, y, zoomed, rotation);
            return;
        }
        if (gecko$preview == null) gecko$preview = new GeckoPreview(npc);
        CustomModelData data = ((IDataDisplay)npc.display).getCustomModelData();
        int px = guiLeft + 126, py = guiTop + 51, pw = imageWidth - 134, ph = 152;
        graphics.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, 0xff777777);
        graphics.fill(px, py, px + pw, py + ph, 0xff252525);
        int viewH = ph - 46;
        gecko$preview.render(graphics, px, py, pw, viewH, data.getModel(), data.getAnimFile(),
                NpcTextureUtils.getNpcTexture(npc), npc.display.getOverlayTexture().isEmpty() ? null : ResourceLocation.tryParse(npc.display.getOverlayTexture()),
                data.getIdleAnim(), rotation, 1);
        drawSizeBoxes(graphics, px, py + viewH, pw, ph - viewH, data, gecko$preview);
    }

    /** A 为静态模型包围盒，B 为碰撞箱配置；同一比例下比较宽×高，单位为格。 */
    private void drawSizeBoxes(GuiGraphics graphics, int x, int y, int w, int h,
                               CustomModelData data, GeckoPreview preview) {
        float modelW = Math.max(0.01f, preview.modelWidth());
        float modelH = Math.max(0.01f, preview.modelHeight());
        float boxW = Math.max(0.01f, data.getWidth());
        float boxH = Math.max(0.01f, data.getHeight());
        float maxDim = Math.max(Math.max(modelW, modelH), Math.max(boxW, boxH));
        float scale = Math.min((h - 27) / maxDim, (w / 2f - 12) / maxDim);

        int boxAH = Math.max(2, Math.round(modelH * scale));
        int boxAW = Math.max(2, Math.round(modelW * scale));
        int boxBH = Math.max(2, Math.round(boxH * scale));
        int boxBW = Math.max(2, Math.round(boxW * scale));

        int leftX = x + 6, rightX = x + w / 2 + 6;
        int baseY = y + h - 4;

        graphics.fill(leftX, baseY - boxAH, leftX + boxAW, baseY, 0x66ffffff);
        graphics.fill(leftX, baseY - boxAH, leftX + boxAW, baseY - boxAH + 1, 0xffffffff);
        graphics.fill(leftX, baseY - 1, leftX + boxAW, baseY, 0xffffffff);
        graphics.fill(leftX, baseY - boxAH, leftX + 1, baseY, 0xffffffff);
        graphics.fill(leftX + boxAW - 1, baseY - boxAH, leftX + boxAW, baseY, 0xffffffff);
        graphics.drawString(font, font.plainSubstrByWidth("A " + GuiGeckoEditor.text("model_bounds"), w / 2 - 12),
                leftX, y + 2, 0xffffff, false);
        graphics.drawString(font, String.format(Locale.ROOT, "%.2f×%.2f ", modelW, modelH) + GuiGeckoEditor.text("blocks"),
                leftX, y + 13, 0xffffff, false);

        graphics.fill(rightX, baseY - boxBH, rightX + boxBW, baseY, 0x6655ff55);
        graphics.fill(rightX, baseY - boxBH, rightX + boxBW, baseY - boxBH + 1, 0xff55ff55);
        graphics.fill(rightX, baseY - 1, rightX + boxBW, baseY, 0xff55ff55);
        graphics.fill(rightX, baseY - boxBH, rightX + 1, baseY, 0xff55ff55);
        graphics.fill(rightX + boxBW - 1, baseY - boxBH, rightX + boxBW, baseY, 0xff55ff55);
        graphics.drawString(font, font.plainSubstrByWidth("B " + GuiGeckoEditor.text("configured_hitbox"), w / 2 - 12),
                rightX, y + 2, 0xaaffaa, false);
        graphics.drawString(font, String.format(Locale.ROOT, "%.2f×%.2f ", boxW, boxH) + GuiGeckoEditor.text("blocks"),
                rightX, y + 13, 0xaaffaa, false);
        if (wrapper.mouseX >= x && wrapper.mouseX < x + w && wrapper.mouseY >= y && wrapper.mouseY < y + h) {
            String help = GuiGeckoEditor.text(wrapper.mouseX < x + w / 2 ? "model_bounds_help" : "hitbox_size_help");
            graphics.renderTooltip(font, font.split(net.minecraft.network.chat.Component.literal(help), 240), wrapper.mouseX, wrapper.mouseY);
        }
    }
}
