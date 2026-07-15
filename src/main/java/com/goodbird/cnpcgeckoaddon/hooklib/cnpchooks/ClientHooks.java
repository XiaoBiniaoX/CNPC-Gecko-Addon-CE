package com.goodbird.cnpcgeckoaddon.hooklib.cnpchooks;

import com.goodbird.cnpcgeckoaddon.client.gui.GuiModelAnimation;
import com.goodbird.cnpcgeckoaddon.client.gui.GuiStringSelection;
import com.goodbird.cnpcgeckoaddon.client.gui.SubGuiModelExtras;
import com.goodbird.cnpcgeckoaddon.data.CustomModelDataProvider;
import com.goodbird.cnpcgeckoaddon.entity.EntityCustomModel;
import com.goodbird.cnpcgeckoaddon.hooklib.asm.Hook;
import com.goodbird.cnpcgeckoaddon.hooklib.asm.ReturnCondition;
import com.goodbird.cnpcgeckoaddon.tile.TileEntityCustomModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.CustomItems;
import noppes.npcs.blocks.tiles.TileScripted;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.model.GuiCreationEntities;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.renderer.RenderCustomNpc;
import noppes.npcs.client.renderer.blocks.BlockScriptedRenderer;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.items.ItemScripted;
import org.lwjgl.opengl.GL11;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.resource.GeckoLibCache;

import java.util.Vector;
@SideOnly(Side.CLIENT)
public class ClientHooks {
    @Hook(returnCondition = ReturnCondition.ON_TRUE)
    @SideOnly(Side.CLIENT)
    public static boolean renderModel(RenderCustomNpc thiss, EntityCustomNpc npc, float par2, float par3, float par4, float par5, float par6, float par7) {
        if(npc.modelData.getEntity(npc) instanceof IAnimatable){
            GL11.glRotatef(180, 1,0,0);
            GL11.glTranslated(0, -1.5,0);
            renderGeoModel(npc, npc.rotationYaw, Minecraft.getMinecraft().getRenderPartialTicks());
            return true;
        }
        return false;
    }
    @SideOnly(Side.CLIENT)
    private static void renderGeoModel(EntityCustomNpc npc, float rot, float partial)
    {
        //npc.modelData.getEntity(npc).renderYawOffset = npc.modelData.getEntity(npc).prevRenderYawOffset = 0;
        if (!npc.isInvisible())
        {
            Minecraft.getMinecraft().getRenderManager().renderEntity(npc.modelData.getEntity(npc), 0,0,0,rot,partial,false);
        }
        else if (!npc.isInvisibleToPlayer(Minecraft.getMinecraft().player))
        {
            GL11.glPushMatrix();
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.15F);
            GL11.glDepthMask(false);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.003921569F);
            Minecraft.getMinecraft().getRenderManager().renderEntity(npc.modelData.getEntity(npc), 0,0,0,rot,partial,false);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
            GL11.glPopMatrix();
            GL11.glDepthMask(true);
        }
    }

    @Hook(injectOnExit = true)
    @SideOnly(Side.CLIENT)
    public static void initGui(GuiCreationEntities gui) {
        EntityNPCInterface npc = gui.npc;
        if (npc instanceof EntityCustomNpc && ((EntityCustomNpc) npc).modelData.getEntity(npc) instanceof EntityCustomModel) {
            EntityCustomModel customModel = (EntityCustomModel) ((EntityCustomNpc) npc).modelData.getEntity(npc);
            Vector<String> list = new Vector<>();
            for (ResourceLocation resLoc : GeckoLibCache.getInstance().getGeoModels().keySet()) {
                list.add(resLoc.toString());
            }
            gui.addLabel(new GuiNpcLabel(212, I18n.format("cnpcgeckoaddon.gui.model"), gui.guiLeft + 124, gui.guiTop + 26, 0xffffff));
            gui.addButton(new GuiNpcButton(202, gui.guiLeft + 160, gui.guiTop + 20, 150, 20, customModel.modelResLoc.getResourcePath()));
            gui.addLabel(new GuiNpcLabel(213, I18n.format("cnpcgeckoaddon.gui.model_animation"), gui.guiLeft + 124, gui.guiTop + 46, 0xffffff));
            gui.addButton(new GuiNpcButton(212, gui.guiLeft + 210, gui.guiTop + 40, 100, 20, "cnpcgeckoaddon.gui.select"));
            gui.addButton(new GuiNpcButton(213, gui.guiLeft + 124, gui.guiTop + 60, 187, 20, "cnpcgeckoaddon.gui.extras"));
        }
    }

    @Hook(injectOnExit = true)
    @SideOnly(Side.CLIENT)
    public static void actionPerformed(GuiCreationEntities gui, GuiButton btn) {
        if (btn.id == 202) {
            Vector<String> list = new Vector<>();
            for (ResourceLocation resLoc : GeckoLibCache.getInstance().getGeoModels().keySet()) {
                list.add(resLoc.toString());
            }
            NoppesUtil.openGUI(Minecraft.getMinecraft().player, new GuiStringSelection(gui, I18n.format("cnpcgeckoaddon.gui.select_model"), list, name -> {
                gui.npc.getCapability(CustomModelDataProvider.DATA_CAP, null).setModel(name);
                gui.getButton(202).setDisplayText(name);
            }));
        }
        if (btn.id == 212) {
            NoppesUtil.openGUI(Minecraft.getMinecraft().player, new GuiModelAnimation(gui, gui.npc));
        }
        if(btn.id == 213){
            NoppesUtil.openGUI(Minecraft.getMinecraft().player, new SubGuiModelExtras(gui, gui.npc));
        }
    }

    @Hook(targetMethod = "render", returnCondition = ReturnCondition.ON_TRUE)
    @SideOnly(Side.CLIENT)
    public static boolean customGeckoModelRendering(BlockScriptedRenderer renderer, TileEntity te, double x, double y, double z, float partialTicks, int blockDamage, float alpha) {
        if(overrideModel()) return false;
        if(!(te instanceof TileScripted)) return false;
        TileScripted tileScripted = (TileScripted) te;
        if(!(tileScripted.renderTile instanceof TileEntityCustomModel)) return false;
        GL11.glPushMatrix();
        GL11.glTranslated(x,y,z);
        GL11.glTranslated(0.5,0.5, 0.5);
        GL11.glRotatef((float)tileScripted.rotationY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef((float)tileScripted.rotationX, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef((float)tileScripted.rotationZ, 0.0F, 0.0F, 1.0F);
        GL11.glScalef(tileScripted.scaleX, tileScripted.scaleY, tileScripted.scaleZ);
        GL11.glTranslated(-0.5,-0.5/tileScripted.scaleY, -0.5);
        GL11.glTranslated(-x,-y,-z);
        TileEntityRendererDispatcher.instance.render(tileScripted.renderTile, x, y, z, partialTicks);
        GL11.glPopMatrix();
        return true;
    }

    private static boolean overrideModel() {
        ItemStack held = Minecraft.getMinecraft().player.getHeldItemMainhand();
        if (held == null) {
            return false;
        } else {
            return held.getItem() == CustomItems.wand || held.getItem() == CustomItems.scripter;
        }
    }

    // ---- ItemScripted rendering (ported from 1.20.1) ----

    private static TileEntityCustomModel cachedTile = null;
    private static String cachedConfigKey = "";
    private static String lastPlayedAnim = "";
    private static long lastPlayedTick = 0;

    @Hook(targetMethod = "renderItem", returnCondition = ReturnCondition.ON_TRUE)
    @SideOnly(Side.CLIENT)
    public static boolean renderGeckoItem(RenderItem renderer, ItemStack stack, net.minecraft.client.renderer.block.model.IBakedModel model) {
        if (!(stack.getItem() instanceof ItemScripted)) return false;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey("geckoData", 10)) return false;
        NBTTagCompound geckoData = tag.getCompoundTag("geckoData");
        String modelStr = geckoData.getString("model");
        if (modelStr.isEmpty()) return false;

        World world = Minecraft.getMinecraft().world;
        if (world == null) return false;

        String animFile = geckoData.getString("animFile");
        String idleAnim = geckoData.getString("idleAnim");
        String texStr = geckoData.getString("texture");

        String configKey = modelStr + "|" + texStr + "|" + animFile + "|" + idleAnim;

        if (cachedTile == null || !configKey.equals(cachedConfigKey)) {
            cachedTile = new TileEntityCustomModel();
            cachedTile.setWorld(world);
            cachedConfigKey = configKey;
        }

        cachedTile.modelResLoc = new ResourceLocation(modelStr);
        cachedTile.animResLoc = animFile.isEmpty()
            ? new ResourceLocation("cnpcgeckoaddon", "animations/none.animations.json")
            : new ResourceLocation(animFile);
        cachedTile.idleAnimName = idleAnim;

        if (!texStr.isEmpty()) {
            cachedTile.textureResLoc = new ResourceLocation(texStr);
        } else {
            cachedTile.textureResLoc = new ResourceLocation("cnpcgeckoaddon", "textures/model/alphabet.png");
        }

        String playAnim = geckoData.getString("playAnim");
        if (!playAnim.isEmpty()) {
            long playTick = geckoData.getLong("playAnimTick");
            if (!playAnim.equals(lastPlayedAnim) || playTick != lastPlayedTick) {
                cachedTile.manualAnim = new AnimationBuilder().playOnce(playAnim);
                lastPlayedAnim = playAnim;
                lastPlayedTick = playTick;
            }
        }

        GL11.glPushMatrix();
        GL11.glTranslated(-0.5, -0.5, -0.5);

        float ox = geckoData.getFloat("displayOffsetX");
        float oy = geckoData.getFloat("displayOffsetY");
        if (ox != 0 || oy != 0) {
            GL11.glTranslatef(ox / 16.0f, -oy / 16.0f, 0);
        }
        float sx = geckoData.getFloat("itemDisplayScaleX");
        float sy = geckoData.getFloat("itemDisplayScaleY");
        float sz = geckoData.getFloat("itemDisplayScaleZ");
        if (sx == 0) sx = 1;
        if (sy == 0) sy = 1;
        if (sz == 0) sz = 1;
        GL11.glScalef(sx, sy, sz);

        float scale = geckoData.getFloat("modelScale");
        if (scale == 0) scale = 1;
        GL11.glScalef(scale, scale, scale);

        float rotX = geckoData.getFloat("rotationX");
        float rotY = geckoData.getFloat("rotationY");
        float rotZ = geckoData.getFloat("rotationZ");
        if (rotX != 0) GL11.glRotatef(rotX, 1, 0, 0);
        if (rotY != 0) GL11.glRotatef(rotY, 0, 1, 0);
        if (rotZ != 0) GL11.glRotatef(rotZ, 0, 0, 1);

        TileEntityRendererDispatcher.instance.render(cachedTile, 0, 0, 0, Minecraft.getMinecraft().getRenderPartialTicks());
        GL11.glPopMatrix();

        return true;
    }
}
