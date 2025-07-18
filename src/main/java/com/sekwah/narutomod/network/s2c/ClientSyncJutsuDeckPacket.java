package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.client.ClientNinjaData;
import com.sekwah.narutomod.registries.NarutoRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ClientSyncJutsuDeckPacket {

    private final List<ResourceLocation> jutsuIds;

    public ClientSyncJutsuDeckPacket(List<ResourceLocation> jutsuIds) {
        this.jutsuIds = jutsuIds;
    }

    public static void encode(ClientSyncJutsuDeckPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.jutsuIds.size());
        for (ResourceLocation id : msg.jutsuIds) {
            buf.writeResourceLocation(id);
        }
    }

    public static ClientSyncJutsuDeckPacket decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<ResourceLocation> jutsuIds = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            jutsuIds.add(buf.readResourceLocation());
        }
        return new ClientSyncJutsuDeckPacket(jutsuIds);
    }

    public static class Handler {
        @OnlyIn(Dist.CLIENT)
        public static void handle(ClientSyncJutsuDeckPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                List<Ability> abilities = new ArrayList<>();
                for (ResourceLocation id : msg.jutsuIds) {
                    Ability ability = NarutoRegistries.ABILITIES.getValue(id);
                    if (ability != null) {
                        abilities.add(ability);
                    }
                }

                // ⚠️ On suppose ici que ClientNinjaData est une classe statique d’accès global
                ClientNinjaData.getJutsuDeck().setAbilities(abilities);
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
