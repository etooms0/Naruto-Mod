package com.sekwah.narutomod.network.c2s;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.network.s2c.ClientSyncJutsuDeckPacket;
import com.sekwah.narutomod.registries.NarutoRegistries;
import com.sekwah.narutomod.NarutoMod; // à adapter selon ton mod principal
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ServerUpdateJutsuDeckPacket {

    private final ResourceLocation jutsuId;
    private final boolean add;

    public ServerUpdateJutsuDeckPacket(ResourceLocation jutsuId, boolean add) {
        this.jutsuId = jutsuId;
        this.add = add;
    }

    public static void encode(ServerUpdateJutsuDeckPacket msg, FriendlyByteBuf buf) {
        buf.writeResourceLocation(msg.jutsuId);
        buf.writeBoolean(msg.add);
    }

    public static ServerUpdateJutsuDeckPacket decode(FriendlyByteBuf buf) {
        ResourceLocation jutsuId = buf.readResourceLocation();
        boolean add = buf.readBoolean();
        return new ServerUpdateJutsuDeckPacket(jutsuId, add);
    }

    public static class Handler {
        public static void handle(ServerUpdateJutsuDeckPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
                    Ability ability = NarutoRegistries.ABILITIES.getValue(msg.jutsuId);
                    if (ability == null) return;

                    if (msg.add) {
                        if (ninjaData.getJutsuDeck().canAddAbility(ability, 5)) {
                            ninjaData.getJutsuDeck().addAbility(ability);
                        }
                    } else {
                        ninjaData.getJutsuDeck().removeAbility(ability);
                    }

                    ninjaData.setDirty();

                    // Optionnel : envoi d’un message de confirmation au joueur
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal(
                                    (msg.add ? "Ajouté " : "Retiré ") + ability.getTranslationKey(ninjaData)
                            ).withStyle(net.minecraft.ChatFormatting.GREEN),
                            true
                    );
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }


}
