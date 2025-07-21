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
import net.minecraft.world.phys.Vec3;

public class SharinganAbility extends Ability implements Ability.Cooldown {

    private static final int CHAKRA_COST       = 15;
    private static final int POINT_COST        = 1000;
    private static final long DEFAULT_COMBO    = 221L;
    private static final int COOLDOWN_TICKS    = 30 * 20;   // 30 s
    private static final int DURATION_TICKS    = 2 * 20;    // 2 s
    private static final double RANGE          = 50.0;      // look range

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return DEFAULT_COMBO;
    }

    @Override
    public int getPointCost() {
        return POINT_COST;
    }

    @Override
    public int getCooldown() {
        return COOLDOWN_TICKS;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        LivingEntity target = getLookedAtEntity(serverPlayer, RANGE);
        if (target == null) {
            serverPlayer.sendSystemMessage(
                    Component.literal("No target within Sharingan range.")
                            .withStyle(ChatFormatting.YELLOW)
            );
            return false;
        }

        if (ninjaData.getChakra() < CHAKRA_COST) {
            serverPlayer.sendSystemMessage(
                    Component.literal("Not enough chakra for Sharingan!")
                            .withStyle(ChatFormatting.RED)
            );
            return false;
        }

        ninjaData.useChakra(CHAKRA_COST, CHAKRA_COST);
        return true;
    }

    /** Ray trace to find the first living entity looked at within maxDistance */
    private LivingEntity getLookedAtEntity(ServerPlayer player, double maxDistance) {
        Vec3 eyePos  = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getViewVector(1.0F).scale(maxDistance);
        Vec3 endPos  = eyePos.add(lookVec);

        return player.level()
                .getEntities(player,
                        player.getBoundingBox()
                                .expandTowards(lookVec)
                                .inflate(1.0))
                .stream()
                .filter(e -> e instanceof LivingEntity)
                .map(e -> (LivingEntity)e)
                .filter(leb -> leb != player && leb.getBoundingBox().intersects(eyePos, endPos))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        ServerPlayer serverPlayer = (ServerPlayer) player;

        LivingEntity target = getLookedAtEntity(serverPlayer, RANGE);

        target.addEffect(new MobEffectInstance(
                MobEffects.WITHER,
                DURATION_TICKS,
                1,
                false,
                true
        ));


        target.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                DURATION_TICKS,
                8,
                false,
                true
        ));


        target.addEffect(new MobEffectInstance(
                MobEffects.BLINDNESS,
                DURATION_TICKS,
                1,
                false,
                true
        ));


        serverPlayer.level().playSound(
                null,
                serverPlayer.blockPosition(),
                SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.PLAYERS,
                1.0f,
                1.0f
        );
    }
}