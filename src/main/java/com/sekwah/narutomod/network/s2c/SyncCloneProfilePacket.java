package com.sekwah.narutomod.network.s2c;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SyncCloneProfilePacket {
    private int entityId;
    private String playerName;
    private List<Property> properties;

    public SyncCloneProfilePacket(int entityId, GameProfile profile) {
        this.entityId = entityId;
        this.playerName = profile.getName();
        this.properties = new ArrayList<>(profile.getProperties().get("textures"));
    }

    public SyncCloneProfilePacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.playerName = buf.readUtf(32767);
        int propsSize = buf.readInt();
        this.properties = new ArrayList<>();
        for (int i = 0; i < propsSize; i++) {
            String name = buf.readUtf(32767);
            String value = buf.readUtf(32767);
            boolean hasSignature = buf.readBoolean();
            String signature = hasSignature ? buf.readUtf(32767) : null;
            this.properties.add(new Property(name, value, signature));
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeUtf(playerName);
        buf.writeInt(properties.size());
        for (Property prop : properties) {
            buf.writeUtf(prop.getName());
            buf.writeUtf(prop.getValue());
            if (prop.getSignature() != null) {
                buf.writeBoolean(true);
                buf.writeUtf(prop.getSignature());
            } else {
                buf.writeBoolean(false);
            }
        }
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            Level level = Minecraft.getInstance().level;
            if (level == null) return;

            Entity entity = level.getEntity(entityId);
            if (!(entity instanceof ShadowCloneEntity clone)) return;

            GameProfile profile = new GameProfile(null, playerName);
            for (Property prop : properties) {
                profile.getProperties().put(prop.getName(), prop);
            }
            clone.setGameProfile(profile);
        });
        context.setPacketHandled(true);
    }
}
