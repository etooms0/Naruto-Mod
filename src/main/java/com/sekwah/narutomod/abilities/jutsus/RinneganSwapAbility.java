package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class RinneganSwapAbility extends Ability implements Ability.Cooldown {

    @Override
    public int getPointCost() {
        return 100;
    }

    @Override
    public long defaultCombo() {
        return 222; // Ex: C + B + V
    }

    @Override
    public int getCooldown() {
        return  20 * 45;
    }

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    private LivingEntity getLookedAtEntity(ServerPlayer player, double maxDistance) {
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getLookAngle().scale(maxDistance);
        Vec3 targetPos = eyePos.add(lookVec);

        for (Entity entity : player.level().getEntities(player, player.getBoundingBox().expandTowards(lookVec).inflate(1.0))) {
            if (entity instanceof LivingEntity livingEntity &&
                    entity != player && // évite soi-même
                    entity.getBoundingBox().intersects(eyePos, targetPos)) {
                return livingEntity;
            }
        }
        return null;
    }


    @Override
    public void performServer(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        Entity target = getLookedAtEntity(serverPlayer, 50.0);
        if (target == null) {
            return;
        }

        if (target == player) {
            return;
        }

        swapPositions(serverPlayer, target);
    }


    private void swapPositions(ServerPlayer player, Entity target) {
        Vec3 playerPos = player.position();
        Vec3 targetPos = target.position();

        if (!player.level().isClientSide) {
            player.teleportTo(targetPos.x, targetPos.y, targetPos.z);
            target.teleportTo(playerPos.x, playerPos.y, playerPos.z);

            target.setPos(playerPos.x, playerPos.y, playerPos.z);
            player.setPos(targetPos.x, targetPos.y, targetPos.z);

        }
    }



    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        int chakraCost = 30;
        if (ninjaData.getChakra() < chakraCost) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false; // Pas assez de chakra
        }
        ninjaData.useChakra(chakraCost, 200); // Consomme le chakra et applique un délai de récupération
        return true;
    }


}