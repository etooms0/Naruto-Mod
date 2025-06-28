package com.sekwah.narutomod.abilities.jutsus;

import com.mojang.authlib.GameProfile;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import com.sekwah.narutomod.entity.SubstitutionLogEntity;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.jutsuprojectile.FireballJutsuEntity;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.SyncCloneProfilePacket;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;


public class ShadowCloneAbility extends Ability implements Ability.Cooldown {

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 1332;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if(ninjaData.getChakra() < 30) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra", Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(30, 30);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        ninjaData.scheduleDelayedTickEvent((delayedPlayer) -> {

            System.out.println("[+] - Shadow Cone Jutsu launched !");
            ItemStack heldItem = player.getMainHandItem();
            boolean hasBow = heldItem.getItem() instanceof net.minecraft.world.item.BowItem;

            for (int i = 0; i < 3; i++) {
                Vec3 offsetPos = player.position();
                if (hasBow) {
                    double angle = (2 * Math.PI / 3) * i; // répartit les clones en cercle
                    double radius = 2.5D;
                    double offsetX = Math.cos(angle) * radius;
                    double offsetZ = Math.sin(angle) * radius;
                    offsetPos = offsetPos.add(offsetX, 0, offsetZ);
                }

                spawnCloneAt(player, offsetPos, ninjaData);
            }

            System.out.println("[+] - Fini !");
        }, 10);
    }

    @Override
    public int getCooldown() {
        return 3 * 20;
    }

    public void spawnCloneAt(Player player, Vec3 pos, INinjaData ninjaData) {
        ninjaData.setInvisibleTicks(5);
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 5, 0, false, false));

        // ⚠️ Utiliser le GameProfile complet avec textures depuis le ServerPlayer
        GameProfile originalProfile = ((ServerPlayer) player).getGameProfile();
        GameProfile fullProfile = new GameProfile(player.getUUID(), player.getGameProfile().getName());

        // ✅ Copier les propriétés (notamment "textures")
        if (!originalProfile.getProperties().isEmpty()) {
            fullProfile.getProperties().putAll(originalProfile.getProperties());
        }

        // ✅ Crée le clone avec le GameProfile complet
        ShadowCloneEntity clone = new ShadowCloneEntity(NarutoEntities.SHADOW_CLONE.get(), player.level(), fullProfile);
        clone.setPos(pos.add(0, 1, 0));
        clone.setOwner(player);

        // ⚔️ Hérite la cible du joueur si elle existe
        if (player.getLastHurtMob() != null) {
            clone.setTarget(player.getLastHurtMob());
            System.out.println("[DEBUG] - Cible transmise au clone : " + player.getLastHurtMob().getName().getString());
        }

        // ✅ Ajoute le clone dans le monde
        player.level().addFreshEntity(clone);

        // ✅ Envoie le GameProfile au client (seulement côté serveur)
        if (!player.level().isClientSide) {
            PacketHandler.NARUTO_CHANNEL.send(
                    PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> clone),
                    new SyncCloneProfilePacket(clone.getId(), fullProfile)
            );
        }

    }







}
