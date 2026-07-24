package com.goodbird.cnpcgeckoaddon.hooklib.cnpchooks;

import com.goodbird.cnpcgeckoaddon.CNPCGeckoAddon;
import com.goodbird.cnpcgeckoaddon.data.CustomModelDataProvider;
import com.goodbird.cnpcgeckoaddon.data.ICustomModelData;
import com.goodbird.cnpcgeckoaddon.entity.EntityCustomModel;
import com.goodbird.cnpcgeckoaddon.hooklib.asm.Hook;
import com.goodbird.cnpcgeckoaddon.hooklib.asm.ReturnCondition;
import com.goodbird.cnpcgeckoaddon.network.CPacketSyncTileManualAnim;
import com.goodbird.cnpcgeckoaddon.network.NetworkWrapper;
import com.goodbird.cnpcgeckoaddon.network.PacketSyncAnimation;
import com.goodbird.cnpcgeckoaddon.tile.TileEntityCustomModel;
import com.goodbird.cnpcgeckoaddon.utils.NpcTextureUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import noppes.npcs.Server;
import noppes.npcs.constants.EnumPacketClient;
import noppes.npcs.api.block.IBlockScripted;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.wrapper.BlockScriptedWrapper;
import noppes.npcs.api.wrapper.ItemScriptedWrapper;
import noppes.npcs.api.wrapper.NPCWrapper;
import noppes.npcs.api.wrapper.WrapperNpcAPI;
import noppes.npcs.blocks.tiles.TileScripted;
import noppes.npcs.client.EntityUtil;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataDisplay;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.manager.AnimationData;

public class CommonHooks {
    @Hook(injectOnExit = true)
    public static void writeToNBT(DataDisplay data, NBTTagCompound nbttagcompound) {
        ICustomModelData modeldata = data.npc.getCapability(CustomModelDataProvider.DATA_CAP, null);
        if(modeldata!=null)
            modeldata.writeToNBT(nbttagcompound);
    }

    @Hook(injectOnExit = true)
    public static void readToNBT(DataDisplay data, NBTTagCompound nbttagcompound){
        ICustomModelData modeldata = data.npc.getCapability(CustomModelDataProvider.DATA_CAP, null);
        if(modeldata!=null) {
            modeldata.readFromNBT(nbttagcompound);
            if (data.npc instanceof EntityCustomNpc) {
                Entity entity = ((EntityCustomNpc) data.npc).modelData.getEntity(data.npc);
                if (entity instanceof EntityCustomModel) {
                    EntityUtil.Copy(data.npc, (EntityLivingBase) entity);
                }
            }
        }
    }

    @Hook(createMethod = true, returnCondition = ReturnCondition.ALWAYS)
    public static AnimationBuilder createAnimBuilder(WrapperNpcAPI api){
        return new AnimationBuilder();
    }

    private static ICustomModelData getModelData(EntityNPCInterface npc){
        return npc.getCapability(CustomModelDataProvider.DATA_CAP, null);
    }

    @Hook(createMethod = true, returnCondition = ReturnCondition.ALWAYS)
    public static void setGeckoModel(NPCWrapper<EntityNPCInterface> npc, String model) {
        getModelData(npc.getMCEntity()).setModel(model);
        npc.updateClient();
    }

    @Hook(createMethod = true, returnCondition = ReturnCondition.ALWAYS)
    public static void setGeckoTexture(NPCWrapper<EntityNPCInterface> npc, String texture) {
        npc.getMCEntity().display.setSkinTexture(texture);
        npc.updateClient();
    }

    @Hook(createMethod = true, returnCondition = ReturnCondition.ALWAYS)
    public static void setGeckoAnimationFile(NPCWrapper<EntityNPCInterface> npc, String animation) {
        getModelData(npc.getMCEntity()).setAnimFile(animation);
        npc.updateClient();
    }

    @Hook(createMethod = true, returnCondition = ReturnCondition.ALWAYS)
    public static void setGeckoIdleAnimation(NPCWrapper<EntityNPCInterface> npc, String animation) {
        getModelData(npc.getMCEntity()).setIdleAnim(animation);
        npc.updateClient();
    }

    @Hook(createMethod = true, returnCondition = ReturnCondition.ALWAYS)
    public static void setGeckoWalkAnimation(NPCWrapper<EntityNPCInterface> npc, String animation) {
        getModelData(npc.getMCEntity()).setWalkAnim(animation);
        npc.updateClient();
    }

    @Hook(createMethod = true, returnCondition = ReturnCondition.ALWAYS)
    public static void syncAnimationsFor(NPCWrapper<EntityNPCInterface> wrapper, IPlayer player, AnimationBuilder builder) {
        NetworkWrapper.sendToPlayer(new PacketSyncAnimation(wrapper.getMCEntity(), builder), player.getMCEntity());
    }
    @Hook(createMethod = true, returnCondition = ReturnCondition.ALWAYS)
    public static void syncAnimationsForAll(NPCWrapper<EntityNPCInterface> wrapper, AnimationBuilder builder) {
        NetworkWrapper.sendToAll(new PacketSyncAnimation(wrapper.getMCEntity(), builder));
    }

    @Hook(injectOnExit = true)
    public static void onUpdate(EntityCustomNpc npc) {
        ICustomModelData data = npc.getCapability(CustomModelDataProvider.DATA_CAP, null);
        if(data==null) return;
        Entity entity = npc.modelData.getEntity(npc);
        if (!(entity instanceof EntityCustomModel)) return;
        EntityCustomModel modelEntity = (EntityCustomModel) entity;
        if (data.getHeight() != modelEntity.height || data.getWidth() != modelEntity.width) {
            modelEntity.setSize(data.getWidth(), data.getHeight());
            npc.updateHitbox();
        }
        if (npc.world.isRemote) {
            modelEntity.detectAnimationEvents();
        } else {
            if (modelEntity.currentAttackAnim != null && modelEntity.attackingTarget != null && !modelEntity.attackDamageDealt && modelEntity.currentAttackFrame > 0) {
                int elapsedTicks = npc.ticksExisted - modelEntity.attackAnimStartTick;
                int targetTicks = (int)(modelEntity.currentAttackFrame * 20.0f);
                if (elapsedTicks >= targetTicks && targetTicks > 0) {
                    modelEntity.delayedAttackPending = false;
                    if (modelEntity.attackingTarget.isEntityAlive()) {
                        modelEntity.frameAttackInProgress = true;
                        npc.attackEntityAsMob(modelEntity.attackingTarget);
                        modelEntity.frameAttackInProgress = false;
                    }
                    modelEntity.attackDamageDealt = true;
                    playGeckoSound(npc, modelEntity.currentAttackSound);
                    modelEntity.resetAttackState();
                }
            }
            int animTimeout = 200;
            if (modelEntity.currentAttackAnim != null && (npc.ticksExisted - modelEntity.attackAnimStartTick > animTimeout)) {
                if (!modelEntity.attackDamageDealt && modelEntity.attackingTarget != null && modelEntity.attackingTarget.isEntityAlive()) {
                    modelEntity.frameAttackInProgress = true;
                    npc.attackEntityAsMob(modelEntity.attackingTarget);
                    modelEntity.frameAttackInProgress = false;
                    playGeckoSound(npc, modelEntity.currentAttackSound);
                }
                modelEntity.resetAttackState();
            }
        }
    }

    /** Same path as CNPC DataAdvanced.playSound (string id via PLAY_SOUND packet). */
    public static void playGeckoSound(Entity entity, String soundId) {
        if (soundId == null || soundId.isEmpty()) return;
        if (entity.world == null || entity.world.isRemote) return;
        BlockPos pos = entity.getPosition();
        Server.sendRangedData(entity, 16, EnumPacketClient.PLAY_SOUND,
                soundId, pos.getX(), pos.getY(), pos.getZ(), 1.0f, 1.0f);
    }

    @Hook(returnCondition = ReturnCondition.ON_TRUE, targetMethod = "attackEntityAsMob")
    public static boolean onAttackEntityAsMob(EntityNPCInterface npc, Entity target) {
        if (!(npc instanceof EntityCustomNpc) || target == null) {
            return false;
        }
        ICustomModelData data = npc.getCapability(CustomModelDataProvider.DATA_CAP, null);
        if (data == null) {
            return false;
        }
        Entity entity = ((EntityCustomNpc)npc).modelData.getEntity(npc);
        if (!(entity instanceof EntityCustomModel)) {
            return false;
        }
        EntityCustomModel em = (EntityCustomModel) entity;
        if (em.frameAttackInProgress) {
            return false;
        }
        if (em.delayedAttackPending) {
            return true;
        }
        em.attackCount = data.getAttackCount();
        em.attackAnimNames = data.getAttackAnimNames().clone();
        em.attackWeights = data.getAttackWeights().clone();
        em.attackFrames = data.getAttackFrames().clone();
        em.attackSoundNames = data.getAttackSoundNames().clone();
        if (em.attackCount <= 0 && !data.getMeleeAttackAnim().isEmpty()) {
            em.attackCount = 1;
            em.attackAnimNames = new String[]{data.getMeleeAttackAnim()};
            em.attackWeights = new int[]{1};
            em.attackFrames = new float[]{0f};
            em.attackSoundNames = new String[]{""};
        }
        if (em.attackAnimNames == null || em.attackCount <= 0) {
            return false;
        }

        String selectedAnim = null;
        float frame = 0;
        String selectedSound = null;
        int totalWeight = 0;
        for (int i = 0; i < em.attackCount; i++) {
            if (em.attackAnimNames[i] != null && !em.attackAnimNames[i].isEmpty()) {
                totalWeight += Math.max(em.attackWeights[i], 0);
            }
        }
        if (totalWeight > 0) {
            int rand = npc.getRNG().nextInt(totalWeight);
            int cumulative = 0;
            for (int i = 0; i < em.attackCount; i++) {
                if (em.attackAnimNames[i] != null && !em.attackAnimNames[i].isEmpty()) {
                    cumulative += Math.max(em.attackWeights[i], 0);
                    if (rand < cumulative) {
                        selectedAnim = em.attackAnimNames[i];
                        frame = em.attackFrames[i];
                        selectedSound = (em.attackSoundNames != null && i < em.attackSoundNames.length)
                                ? em.attackSoundNames[i] : null;
                        break;
                    }
                }
            }
        }
        if (selectedAnim == null || selectedAnim.isEmpty()) {
            return false;
        }

        em.currentAttackAnim = selectedAnim;
        em.currentAttackFrame = frame;
        em.attackingTarget = target;
        em.attackAnimStartTick = npc.ticksExisted;
        em.attackDamageDealt = false;
        em.currentAttackSound = selectedSound;

        if (!npc.world.isRemote) {
            NetworkWrapper.sendToAll(new PacketSyncAnimation(npc, new AnimationBuilder().playOnce(selectedAnim)));
        }

        if (frame > 0) {
            em.delayedAttackPending = true;
            return true;
        } else {
            em.attackDamageDealt = true;
            playGeckoSound(npc, em.currentAttackSound);
            return false;
        }
    }

    @Hook(injectOnExit = true)
    public static void Copy(EntityUtil cl, EntityLivingBase copied, EntityLivingBase entity) {
        if (entity instanceof EntityCustomModel && copied instanceof EntityNPCInterface) {
            EntityCustomModel modelEntity = (EntityCustomModel) entity;
            EntityNPCInterface npc = (EntityNPCInterface) copied;
            ICustomModelData data = npc.getCapability(CustomModelDataProvider.DATA_CAP, null);



            modelEntity.textureResLoc = NpcTextureUtils.getNpcTexture((EntityNPCInterface) copied);
            modelEntity.modelResLoc = new ResourceLocation(data.getModel());
            modelEntity.animResLoc = new ResourceLocation(data.getAnimFile());
            modelEntity.idleAnimName = data.getIdleAnim();
            modelEntity.walkAnimName = data.getWalkAnim();
            modelEntity.npcRef = npc;

            if (data.getAttackCount() > 0) {
                modelEntity.attackCount = data.getAttackCount();
                modelEntity.attackAnimNames = data.getAttackAnimNames().clone();
                modelEntity.attackWeights = data.getAttackWeights().clone();
                modelEntity.attackFrames = data.getAttackFrames().clone();
                modelEntity.attackSoundNames = data.getAttackSoundNames().clone();
            } else if (!data.getMeleeAttackAnim().isEmpty()) {
                modelEntity.attackCount = 1;
                modelEntity.attackAnimNames = new String[]{data.getMeleeAttackAnim()};
                modelEntity.attackWeights = new int[]{1};
                modelEntity.attackFrames = new float[]{0f};
                modelEntity.attackSoundNames = new String[]{""};
            } else {
                modelEntity.attackCount = 0;
                modelEntity.attackAnimNames = new String[0];
                modelEntity.attackWeights = new int[0];
                modelEntity.attackFrames = new float[0];
                modelEntity.attackSoundNames = new String[0];
            }

            if (data.getHurtAnimCount() > 0) {
                modelEntity.hurtAnimCount = data.getHurtAnimCount();
                modelEntity.hurtAnimNames = data.getHurtAnimNames().clone();
                modelEntity.hurtWeights = data.getHurtWeights().clone();
                modelEntity.hurtSoundNames = data.getHurtSoundNames().clone();
            } else if (!data.getHurtAnim().isEmpty()) {
                modelEntity.hurtAnimCount = 1;
                modelEntity.hurtAnimNames = new String[]{data.getHurtAnim()};
                modelEntity.hurtWeights = new int[]{1};
                modelEntity.hurtSoundNames = new String[]{""};
            } else {
                modelEntity.hurtAnimCount = 0;
                modelEntity.hurtAnimNames = new String[0];
                modelEntity.hurtWeights = new int[0];
                modelEntity.hurtSoundNames = new String[0];
            }

            if (data.getDeathAnimCount() > 0) {
                modelEntity.deathAnimCount = data.getDeathAnimCount();
                modelEntity.deathAnimNames = data.getDeathAnimNames().clone();
                modelEntity.deathWeights = data.getDeathWeights().clone();
                modelEntity.deathHealthThresholds = data.getDeathHealthThresholds().clone();
                modelEntity.deathAnimDurations = data.getDeathAnimDurations().clone();
            } else {
                modelEntity.deathAnimCount = 0;
                modelEntity.deathAnimNames = new String[0];
                modelEntity.deathWeights = new int[0];
                modelEntity.deathHealthThresholds = new float[0];
                modelEntity.deathAnimDurations = new float[0];
            }

            if(!data.isHurtTintEnabled()){
                modelEntity.hurtTime = npc.hurtTime = 0;
                modelEntity.deathTime = npc.deathTime = 0;
            }

            if(npc.inventory.getLeftHand()!=null) {
                modelEntity.leftHeldItem = npc.inventory.getLeftHand().getMCItemStack();
            }
            modelEntity.headBoneName = data.getHeadBoneName();
            modelEntity.transitionLengthTicks = data.getTransitionLengthTicks();
            AnimationData animationData = modelEntity.getFactory().getOrCreateAnimationData(modelEntity.getUniqueID().hashCode());
            for(AnimationController controller : animationData.getAnimationControllers().values()){
                controller.transitionLengthTicks = data.getTransitionLengthTicks();
            }
            if(data.getHeight()!=modelEntity.height || data.getWidth() != modelEntity.width){
                modelEntity.setSize(data.getWidth(), data.getHeight());
                npc.updateHitbox();
            }
        }
    }

    @Hook(injectOnExit = true)
    public static void setDisplayNBT(TileScripted tile, NBTTagCompound compound) {
        if(compound.hasKey("renderTileTag")){
            tile.renderTile = new TileEntityCustomModel();
            NBTTagCompound saveTag = compound.getCompoundTag("renderTileTag");
            if(saveTag.hasKey("dimID")){
                tile.renderTile.setWorld(CNPCGeckoAddon.proxy.getWorldById(saveTag.getInteger("dimID")));
            }else{
                tile.renderTile.setWorld(CNPCGeckoAddon.proxy.getWorldById(0));
            }
            tile.renderTile.readFromNBT(saveTag);
        }
    }

    @Hook(injectOnExit = true)
    public static void getDisplayNBT(TileScripted tile, NBTTagCompound compound) {
        if(tile.renderTile!=null) {
            NBTTagCompound saveTag = new NBTTagCompound();
            tile.renderTile.writeToNBT(saveTag);
            if(tile.renderTile.getWorld()!= null && tile.renderTile.getWorld().provider != null)
                saveTag.setInteger("dimID", tile.renderTile.getWorld().provider.getDimension());
            compound.setTag("renderTileTag", saveTag);
        }
    }

    private static TileEntityCustomModel getOrCreateTECM(IBlockScripted scriptedBlock){
        TileScripted tile = (TileScripted) scriptedBlock.getMCTileEntity();
        if(!(tile.renderTile instanceof TileEntityCustomModel)){
            tile.renderTile = new TileEntityCustomModel(tile);
        }
        return (TileEntityCustomModel) tile.renderTile;
    }

    @Hook(createMethod = true, returnCondition = ReturnCondition.ALWAYS)
    public static void setGeckoModel(BlockScriptedWrapper scriptedBlock, String model) {
        TileEntityCustomModel geckoTile = getOrCreateTECM(scriptedBlock);
        geckoTile.modelResLoc = new ResourceLocation(model);
        ((TileScripted) scriptedBlock.getMCTileEntity()).needsClientUpdate = true;
    }

    @Hook(createMethod = true, returnCondition = ReturnCondition.ALWAYS)
    public static void setGeckoTexture(BlockScriptedWrapper scriptedBlock, String texture) {
        TileEntityCustomModel geckoTile = getOrCreateTECM(scriptedBlock);
        geckoTile.textureResLoc = new ResourceLocation(texture);
        ((TileScripted) scriptedBlock.getMCTileEntity()).needsClientUpdate = true;
    }

    @Hook(createMethod = true, returnCondition = ReturnCondition.ALWAYS)
    public static void setGeckoAnimationFile(BlockScriptedWrapper scriptedBlock, String animation) {
        TileEntityCustomModel geckoTile = getOrCreateTECM(scriptedBlock);
        geckoTile.animResLoc = new ResourceLocation(animation);
        ((TileScripted) scriptedBlock.getMCTileEntity()).needsClientUpdate = true;
    }

    @Hook(createMethod = true, returnCondition = ReturnCondition.ALWAYS)
    public static void setGeckoIdleAnimation(BlockScriptedWrapper scriptedBlock, String animation) {
        TileEntityCustomModel geckoTile = getOrCreateTECM(scriptedBlock);
        geckoTile.idleAnimName = animation;
        ((TileScripted) scriptedBlock.getMCTileEntity()).needsClientUpdate = true;
    }

    @Hook(createMethod = true, returnCondition = ReturnCondition.ALWAYS)
    public static void syncAnimationsFor(BlockScriptedWrapper scriptedBlock, IPlayer player, AnimationBuilder builder) {
        NetworkWrapper.sendToPlayer(new CPacketSyncTileManualAnim(scriptedBlock.getMCTileEntity(), builder), player.getMCEntity());
    }

    @Hook(createMethod = true, returnCondition = ReturnCondition.ALWAYS)
    public static void syncAnimationsForAll(BlockScriptedWrapper scriptedBlock, AnimationBuilder builder) {
        NetworkWrapper.sendToAll(new CPacketSyncTileManualAnim(scriptedBlock.getMCTileEntity(), builder));
    }

    // ---- Block methods matching 1.20.1 naming (String-based) ----

    @Hook(createMethod = true, returnCondition = ReturnCondition.ALWAYS)
    public static void syncAnimForPlayer(BlockScriptedWrapper scriptedBlock, String animName, IPlayer player) {
        NetworkWrapper.sendToPlayer(new CPacketSyncTileManualAnim(scriptedBlock.getMCTileEntity(), new AnimationBuilder().playOnce(animName)), player.getMCEntity());
    }

    @Hook(createMethod = true, returnCondition = ReturnCondition.ALWAYS)
    public static void syncAnimForAll(BlockScriptedWrapper scriptedBlock, String animName) {
        NetworkWrapper.sendToAll(new CPacketSyncTileManualAnim(scriptedBlock.getMCTileEntity(), new AnimationBuilder().playOnce(animName)));
    }

    @Hook(createMethod = true, returnCondition = ReturnCondition.ALWAYS)
    public static void playAnimation(BlockScriptedWrapper scriptedBlock, String animName) {
        NetworkWrapper.sendToAll(new CPacketSyncTileManualAnim(scriptedBlock.getMCTileEntity(), new AnimationBuilder().playOnce(animName)));
    }

    // ---- ItemScriptedWrapper methods (ported from 1.20.1) ----

    private static NBTTagCompound getGeckoData(ItemScriptedWrapper item) {
        ItemStack stack = item.getMCItemStack();
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        if (!tag.hasKey("geckoData", 10)) {
            tag.setTag("geckoData", new NBTTagCompound());
        }
        return tag.getCompoundTag("geckoData");
    }

    @Hook(createMethod = true, returnCondition = ReturnCondition.ALWAYS)
    public static void setGeckoModel(ItemScriptedWrapper item, String model) {
        getGeckoData(item).setString("model", model);
        item.updateClient = true;
    }

    @Hook(createMethod = true, returnCondition = ReturnCondition.ALWAYS)
    public static void setGeckoTexture(ItemScriptedWrapper item, String texture) {
        getGeckoData(item).setString("texture", texture);
        item.updateClient = true;
    }

    @Hook(createMethod = true, returnCondition = ReturnCondition.ALWAYS)
    public static void setGeckoAnimationFile(ItemScriptedWrapper item, String animation) {
        getGeckoData(item).setString("animFile", animation);
        item.updateClient = true;
    }

    @Hook(createMethod = true, returnCondition = ReturnCondition.ALWAYS)
    public static void setGeckoIdleAnimation(ItemScriptedWrapper item, String animation) {
        getGeckoData(item).setString("idleAnim", animation);
        item.updateClient = true;
    }

    @Hook(createMethod = true, returnCondition = ReturnCondition.ALWAYS)
    public static void playAnimation(ItemScriptedWrapper item, String animName) {
        NBTTagCompound data = getGeckoData(item);
        data.setString("playAnim", animName);
        data.setLong("playAnimTick", System.currentTimeMillis());
        item.updateClient = true;
    }

    @Hook(createMethod = true, returnCondition = ReturnCondition.ALWAYS)
    public static void setGeckoItemDisplaySize(ItemScriptedWrapper item, float x, float y, float z) {
        NBTTagCompound data = getGeckoData(item);
        data.setFloat("itemDisplayScaleX", x);
        data.setFloat("itemDisplayScaleY", y);
        data.setFloat("itemDisplayScaleZ", z);
        item.updateClient = true;
    }

    @Hook(createMethod = true, returnCondition = ReturnCondition.ALWAYS)
    public static void setGeckoItemDisplayOffset(ItemScriptedWrapper item, float x, float y) {
        NBTTagCompound data = getGeckoData(item);
        data.setFloat("displayOffsetX", x);
        data.setFloat("displayOffsetY", y);
        item.updateClient = true;
    }

    @Hook(createMethod = true, returnCondition = ReturnCondition.ALWAYS)
    public static void setGeckoRotation(ItemScriptedWrapper item, float x, float y, float z) {
        NBTTagCompound data = getGeckoData(item);
        data.setFloat("rotationX", x);
        data.setFloat("rotationY", y);
        data.setFloat("rotationZ", z);
        item.updateClient = true;
    }

    @Hook(createMethod = true, returnCondition = ReturnCondition.ALWAYS)
    public static void setGeckoScale(ItemScriptedWrapper item, float scale) {
        getGeckoData(item).setFloat("modelScale", scale);
        item.updateClient = true;
    }
}
