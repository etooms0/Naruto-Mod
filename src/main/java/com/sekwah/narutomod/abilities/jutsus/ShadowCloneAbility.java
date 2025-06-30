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

            System.out.println("[+] - Shadow Clone Jutsu launched !");
            ItemStack heldItem = player.getMainHandItem();
            boolean hasBow = heldItem.getItem() instanceof net.minecraft.world.item.BowItem;

            Vec3 playerPos = player.position();
            int cloneCount = 3;

            if (hasBow) {
                // cercle à 3 clones
                double radius = 2.5D;
                for (int i = 0; i < cloneCount; i++) {
                    double angle = (2 * Math.PI / cloneCount) * i;
                    double dx    = Math.cos(angle) * radius;
                    double dz    = Math.sin(angle) * radius;
                    Vec3 offset  = playerPos.add(dx, 0, dz);
                    spawnCloneAt(player, offset, ninjaData);
                }
            } else {
                // ligne à 3 clones devant le joueur
                Vec3 look    = player.getLookAngle();
                Vec3 forward = new Vec3(look.x, 0, look.z).normalize();
                double spacing   = 2.5D;                  // espace entre clones
                double totalSpan = spacing * (cloneCount-1);
                Vec3 startShift  = forward.scale(-totalSpan/2);

                for (int i = 0; i < cloneCount; i++) {
                    Vec3 offset = playerPos
                            .add(startShift)                   // recule pour centrer
                            .add(forward.scale(spacing * i));  // avance pour chaque clone
                    spawnCloneAt(player, offset, ninjaData);
                }
            }

            System.out.println("[+] - Fini !");
        }, 10);
    }

    @Override
    public int getCooldown() {
        return 3 * 20;
    }

    public void spawnCloneAt(Player player, Vec3 pos, INinjaData ninjaData) {
        // Invisibilité courte pour le joueur
        ninjaData.setInvisibleTicks(5);
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 5, 0, false, false));

        // Récupère le GameProfile complet pour copier les textures
        GameProfile originalProfile = ((ServerPlayer) player).getGameProfile();
        GameProfile fullProfile = new GameProfile(player.getUUID(), player.getGameProfile().getName());
        if (!originalProfile.getProperties().isEmpty()) {
            fullProfile.getProperties().putAll(originalProfile.getProperties());
        }

        // Crée et positionne le clone
        ShadowCloneEntity clone = new ShadowCloneEntity(
                NarutoEntities.SHADOW_CLONE.get(),
                player.level(),
                fullProfile
        );
        clone.setPos(pos.add(0, 1, 0));
        clone.setOwner(player);

        // Transmet la cible si nécessaire
        if (player.getLastHurtMob() != null) {
            clone.setTarget(player.getLastHurtMob());
        }

        // Ajoute le clone dans le monde
        player.level().addFreshEntity(clone);

        // Synchronise le GameProfile et affiche des particules (serveur uniquement)
        if (!player.level().isClientSide) {
            // Envoi du profil au client
            PacketHandler.NARUTO_CHANNEL.send(
                    PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> clone),
                    new SyncCloneProfilePacket(clone.getId(), fullProfile)
            );

            // Spawn de particules en cercle autour du clone
            ServerLevel world = (ServerLevel) player.level();
            Vec3 center = clone.position().add(0, 0.5, 0);
            int count = 30;
            double radius = 1.2;
            for (int i = 0; i < count; i++) {
                double angle = 2 * Math.PI * i / count;
                double x = center.x + Math.cos(angle) * radius;
                double y = center.y + world.random.nextDouble() * 0.6;
                double z = center.z + Math.sin(angle) * radius;
                world.sendParticles(
                        ParticleTypes.CLOUD,  // ou END_ROD, SOUL, etc.
                        x, y, z,
                        1,      // 1 particule à chaque point
                        0, 0, 0,// pas de motion
                        0.0     // vitesse aléatoire = 0
                );
            }
        }
    }







}
