package com.sekwah.narutomod.abilities.jutsus;

import com.mojang.authlib.GameProfile;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.SyncCloneProfilePacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;


public class MultipleShadowCloneAbility extends Ability implements Ability.Cooldown {

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 1332312;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if(ninjaData.getChakra() < 30) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra", Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(90, 60);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        ninjaData.scheduleDelayedTickEvent((delayedPlayer) -> {
            System.out.println("[+] - Shadow Clone Jutsu launched !");
            ItemStack heldItem = player.getMainHandItem();
            boolean hasBow = heldItem.getItem() instanceof net.minecraft.world.item.BowItem;

            Vec3 playerPos = player.position();
            int cloneCount = 10;

            if (hasBow) {
                // ton code existant en cercles...
                for (int i = 0; i < cloneCount; i++) {
                    double angle  = (2 * Math.PI / 5) * (i % 5);
                    double radius = (i < 5) ? 3.0D : 5.0D;
                    double dx = Math.cos(angle) * radius;
                    double dz = Math.sin(angle) * radius;
                    Vec3 offset = playerPos.add(dx, 0, dz);
                    spawnCloneAt(player, offset, ninjaData);
                }
            } else {
                // nouvelle logique : ligne devant le joueur
                // 1) on récupère la direction horizontale du regard
                Vec3 look    = player.getLookAngle();
                Vec3 forward = new Vec3(look.x, 0, look.z).normalize();

                // 2) espacement entre chaque clone
                double spacing    = 2.0D;
                // 3) on centre la ligne sur le joueur
                double totalSpan  = spacing * (cloneCount - 1);
                Vec3   startShift = forward.scale(-totalSpan / 2.0D);

                // 4) on spawn
                for (int i = 0; i < cloneCount; i++) {
                    Vec3 offset = playerPos
                            .add(startShift)                  // on recule pour centrer
                            .add(forward.scale(spacing * i)); // on avance pour chaque clone
                    spawnCloneAt(player, offset, ninjaData);
                }
            }

            System.out.println("[+] - Clones spawned !");
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

        // Synchronise le GameProfile sur les clients
        if (!player.level().isClientSide) {
            PacketHandler.NARUTO_CHANNEL.send(
                    PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> clone),
                    new SyncCloneProfilePacket(clone.getId(), fullProfile)
            );

            // --- Particules en cercle autour du clone ---
            ServerLevel world = (ServerLevel) player.level();
            Vec3 center = clone.position().add(0, 0.5, 0);
            int count = 30;
            double radius = 1.5;
            for (int i = 0; i < count; i++) {
                double angle = 2 * Math.PI * i / count;
                double x = center.x + Math.cos(angle) * radius;
                double y = center.y + world.random.nextDouble() * 0.6;
                double z = center.z + Math.sin(angle) * radius;
                world.sendParticles(
                        ParticleTypes.CLOUD,  // tu peux remplacer par END_ROD, SOUL, etc.
                        x, y, z,
                        1,      // 1 particule à chaque point
                        0, 0, 0,// pas de direction
                        0.0     // vitesse aléatoire = 0
                );
            }
        }
    }








}
