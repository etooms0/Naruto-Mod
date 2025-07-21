package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class ByakuganAbility extends Ability implements Ability.Cooldown {

    private static final int RADIUS          = 35;
    private static final int DURATION_TICKS  = 30 * 20;    // 30 secondes
    private static final int CHAKRA_COST     = 25;
    private static final int COOLDOWN_TICKS  = 20 * 20;    // 20 secondes

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public int getPointCost() {
        return 3;
    }

    @Override
    public long defaultCombo() {
        return 111;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (ninjaData.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(
                    Component.literal("Not enough chakra for Byakugan!")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, CHAKRA_COST);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        Level level = player.level();

        // ➊ Applique Night Vision au joueur
        player.addEffect(new MobEffectInstance(
                MobEffects.NIGHT_VISION,
                DURATION_TICKS,
                0,      // amplifier
                false,  // ambient
                false   // showParticles
        ));

        // ➋ Recherche des cibles
        AABB box = player.getBoundingBox().inflate(RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                box,
                e -> !(e instanceof ServerPlayer)
        );

        // ➌ Applique le Glow et affiche la santé
        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(
                    MobEffects.GLOWING,
                    DURATION_TICKS,
                    0,
                    false,
                    false
            ));

            float hp    = target.getHealth();
            float maxHp = target.getMaxHealth();
            String hpText = String.format("%.1f/%.1f", hp, maxHp);
            player.sendSystemMessage(
                    Component.literal(target.getName().getString() + ": ")
                            .append(Component.literal(hpText)
                                    .withStyle(ChatFormatting.GREEN))
            );
        }

        // ➍ Son d’activation vanilla
        level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.PLAYERS,
                1.0f,
                1.0f
        );

        // ➎ Planifier la fin des effets Glow
        ninjaData.scheduleDelayedTickEvent(p -> {
            for (LivingEntity t : targets) {
                t.removeEffect(MobEffects.GLOWING);
            }
        }, DURATION_TICKS);
    }

    @Override
    public int getCooldown() {
        return COOLDOWN_TICKS;
    }
}