package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.entity.SubstitutionLogEntity;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.jutsuprojectile.FireballJutsuEntity;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;


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
            for(int i=0; i<3; i++) {
                spawnCloneAt(player, player.position(), ninjaData);
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
        SubstitutionLogEntity log = new SubstitutionLogEntity(player.level());
        log.setPos(pos.add(0, 1, 0));
        player.level().addFreshEntity(log);
        if(player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    pos.x,
                    pos.y + (player.getBbHeight() / 2),
                    pos.z,
                    100,
                    0.5, 0.7, 0.5, 0);
        }
    }



}
