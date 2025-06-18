package com.sekwah.narutomod.network.s2c;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientAttractionPacket {
    private final int entityId;
    private final Vec3 motion;

    public ClientAttractionPacket(int entityId, Vec3 motion) {
        this.entityId = entityId;
        this.motion = motion;
    }

    public static void encode(ClientAttractionPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeDouble(msg.motion.x);
        buf.writeDouble(msg.motion.y);
        buf.writeDouble(msg.motion.z);
    }

    public static ClientAttractionPacket decode(FriendlyByteBuf buf) {
        int id = buf.readInt();
        Vec3 motion = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        return new ClientAttractionPacket(id, motion);
    }

    public static void handle(ClientAttractionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Level level = Minecraft.getInstance().level;
            if (level != null) {
                Entity entity = level.getEntity(msg.entityId);
                if (entity != null) {
                    entity.setDeltaMovement(msg.motion);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
