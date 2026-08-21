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
        public final int priority;
        public int tries = 0;

        public PendingAnim(String animName, boolean instant, int priority) {
            this.animName = animName;
            this.instant = instant;
            this.priority = priority;
        }
    }

    private int id;
    private String animName;
    private boolean instant = false;
    /** 动画优先级，见 EntityCustomModel.PRIO_*。脚本 API 走默认最高优先级。 */
    private int priority = EntityCustomModel.PRIO_SCRIPT;

    public PacketSyncAnimation(int entityId, String animName, boolean instant) {
        this(entityId, animName, instant, EntityCustomModel.PRIO_SCRIPT);
    }

    public PacketSyncAnimation(int entityId, String animName, boolean instant, int priority) {
        this.id = entityId;
        this.animName = animName;
        this.instant = instant;
        this.priority = priority;
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
        // 新增字段追加在末尾：写入/读取顺序一致即可。
        // 通道协议号（NetworkWrapper.PROTOCOL）保证客户端与服务端 mod 版本一致，
        // 版本不匹配时 Forge 直接拒绝连接，不会出现半新半旧的错位解码。
        buf.writeVarInt(priority);
    }

    public static PacketSyncAnimation decode(FriendlyByteBuf buf) {
        int id = buf.readInt();
        boolean instant = buf.readBoolean();
        boolean hasAnim = buf.readBoolean();
        String animName = hasAnim ? buf.readUtf() : null;
        // 兼容读取：万一遇到未带优先级的旧包（不应发生），按最高优先级处理，
        // 保持「脚本动画一定能播」的旧行为，而不是抛异常踢掉连接。
        int priority = buf.isReadable() ? buf.readVarInt() : EntityCustomModel.PRIO_SCRIPT;
        return new PacketSyncAnimation(id, animName, instant, priority);
    }

    public static void handle(PacketSyncAnimation packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            // 注意：不能直接用 Minecraft.getInstance().level——该字段类型是 ClientLevel（带 @OnlyIn(CLIENT)），
            // 专用服务器加载本类验证字节码时会触发 RuntimeDistCleaner 崩溃（"Attempted to load class ... ClientLevel for invalid dist"）。
            // 改用 player.level()：LocalPlayer 与 Level 均无 @OnlyIn 注解，服务器可安全加载。
            // player 在加入/离开世界窗口时为 null（旧存档崩溃修复），必须先判空。
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                // 玩家还没进世界：先缓存，等进入后由实体 tick 补放
                PENDING.put(packet.id, new PendingAnim(packet.animName, packet.instant, packet.priority));
                return;
            }
            Level level = player.level();
            Entity entity = level.getEntity(packet.id);
            if (!(entity instanceof EntityCustomNpc npc)) { PENDING.put(packet.id, new PendingAnim(packet.animName, packet.instant, packet.priority)); return; }
            if (npc.modelData == null) { return; }
            if (!(npc.modelData.getEntity(npc) instanceof EntityCustomModel entityCustomModel)) { PENDING.put(packet.id, new PendingAnim(packet.animName, packet.instant, packet.priority)); return; }
            PENDING.remove(packet.id);
            // 按优先级仲裁：低优先级动画（如攻击）不会打断正在播放的脚本/死亡动画
            entityCustomModel.requestManualAnim(packet.animName, packet.instant, packet.priority);
        });
        context.setPacketHandled(true);
    }
}
