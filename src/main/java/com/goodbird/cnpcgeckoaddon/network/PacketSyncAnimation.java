package com.goodbird.cnpcgeckoaddon.network;

import com.goodbird.cnpcgeckoaddon.entity.EntityCustomModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import noppes.npcs.entity.EntityCustomNpc;
import software.bernie.geckolib.core.animation.RawAnimation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class PacketSyncAnimation {
    /**
     * 动画未播放成功的缓存：包到达时客户端实体可能还没加载（实体未注册/模型实体未创建），
     * 先存到这里，等实体 tick 时补放。key=实体id，value=待放动画。
     */
    public static final Map<Integer, PendingAnim> PENDING = new ConcurrentHashMap<>();

    public static class PendingAnim {
        public final String animName;
        public final boolean instant;
        public int tries = 0;

        public PendingAnim(String animName, boolean instant) {
            this.animName = animName;
            this.instant = instant;
        }
    }

    private int id;
    private String animName;
    private boolean instant = false;

    public PacketSyncAnimation(int entityId, String animName, boolean instant) {
        this.id = entityId;
        this.animName = animName;
        this.instant = instant;
    }

    public PacketSyncAnimation(){

    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(id);
        buf.writeBoolean(instant);
        buf.writeBoolean(animName != null);
        if (animName != null) {
            buf.writeUtf(animName);
        }
    }

    public static PacketSyncAnimation decode(FriendlyByteBuf buf) {
        int id = buf.readInt();
        boolean instant = buf.readBoolean();
        boolean hasAnim = buf.readBoolean();
        String animName = hasAnim ? buf.readUtf() : null;
        return new PacketSyncAnimation(id, animName, instant);
    }

    public static void handle(PacketSyncAnimation packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        System.out.println("[GeckoDBG] PacketSyncAnimation received: id=" + packet.id + " anim=" + packet.animName + " instant=" + packet.instant);
        context.enqueueWork(() -> {
            // 注意：不能直接用 Minecraft.getInstance().level——该字段类型是 ClientLevel（带 @OnlyIn(CLIENT)），
            // 专用服务器加载本类验证字节码时会触发 RuntimeDistCleaner 崩溃（"Attempted to load class ... ClientLevel for invalid dist"）。
            // 改用 player.level()：LocalPlayer 与 Level 均无 @OnlyIn 注解，服务器可安全加载。
            // player 在加入/离开世界窗口时为 null（旧存档崩溃修复），必须先判空。
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                // 玩家还没进世界：先缓存，等进入后由实体 tick 补放
                PENDING.put(packet.id, new PendingAnim(packet.animName, packet.instant));
                return;
            }
            Level level = player.level();
            Entity entity = level.getEntity(packet.id);
            if (!(entity instanceof EntityCustomNpc npc)) { System.out.println("[GeckoDBG]   -> client entity not found/not customnpc: " + (entity == null ? "null" : entity.getClass().getName())); PENDING.put(packet.id, new PendingAnim(packet.animName, packet.instant)); return; }
            if (npc.modelData == null) { System.out.println("[GeckoDBG]   -> modelData null"); return; }
            if (!(npc.modelData.getEntity(npc) instanceof EntityCustomModel entityCustomModel)) { System.out.println("[GeckoDBG]   -> getEntity()=" + npc.modelData.getEntity(npc) + " not EntityCustomModel"); PENDING.put(packet.id, new PendingAnim(packet.animName, packet.instant)); return; }
            PENDING.remove(packet.id);
            entityCustomModel.manualAnimName = packet.animName;
            entityCustomModel.manualAnimInstant = packet.instant;
            System.out.println("[GeckoDBG]   -> manualAnimName set to " + packet.animName);
        });
        context.setPacketHandled(true);
    }
}
