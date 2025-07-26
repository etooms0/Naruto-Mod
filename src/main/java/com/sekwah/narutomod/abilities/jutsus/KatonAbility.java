package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.registries.NarutoRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;

import java.util.List;

public class KatonAbility extends Ability implements Ability.Cooldown, Ability.Channeled {

    private static final int POINT_COST = 1;
    private static final long DEFAULT_COMBO = 1121L;
    private static final int DURATION_TICKS = 5 * 20;
    private static final int COOLDOWN_TICKS = 12 * 20;
    private static final int DAMAGE_INTERVAL = 4;
    private static final float RANGE = 12.0F;
    private static final int PARTICLES_PER_TICK = 15;
    private static final double CONSUME_INTERVAL = 2;
    private static final float DAMAGE_PER_HIT = 1.5F;
    private static final double GROUND_BURN_INTERVAL = 5;

    @Override
    public ActivationType activationType() {
        return ActivationType.CHANNELED;
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
    public boolean handleCost(Player player, INinjaData data, int charge) {
        String id = NarutoRegistries.ABILITIES
                .getResourceKey(this)
                .map(r -> r.location().getPath())
                .orElse("");
        if (!data.getSlotData().isEquipped(id)) {
            player.displayClientMessage(
                    Component.literal("this ability is not in your deck")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return false;
        }
        return true;
    }

    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    public int getUseDuration(ItemStack stack) {
        return DURATION_TICKS;
    }

    @Override
    public void performServer(Player player, INinjaData data, int useCount) {
        handleChannelling(player, data, useCount);
    }

    @Override
    public void handleChannelling(Player player, INinjaData data, int ticksChanneled) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        // consommation de chakra
        if (ticksChanneled % CONSUME_INTERVAL == 0) {
            data.useChakra(1, 1);
            if (data.getChakra() <= 0) {
                player.stopUsingItem();
                return;
            }
        }

        Vec3 origin = player.getEyePosition(1.0F);
        Vec3 dir = player.getLookAngle().normalize();
        double progress = ticksChanneled / (double) DURATION_TICKS;
        Vec3 jetBase = origin.add(dir.scale(0.5 + progress * RANGE));

        // son de feu et résistance au démarrage
        if (ticksChanneled == 0) {
            serverLevel.playSound(
                    null, origin.x, origin.y, origin.z,
                    SoundEvents.FIRE_AMBIENT,
                    SoundSource.PLAYERS, 2.0F, 1.0F
            );
            if (player instanceof ServerPlayer sp) {
                sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE,
                        DURATION_TICKS, 0, false, false
                ));
            }
        }

        // particules massives
        for (int i = 0; i < PARTICLES_PER_TICK; i++) {
            double spread = 0.2;
            Vec3 offset = dir.add(
                    (serverLevel.random.nextDouble() - 0.5) * spread,
                    (serverLevel.random.nextDouble() - 0.5) * spread,
                    (serverLevel.random.nextDouble() - 0.5) * spread
            ).normalize();

            Vec3 p = jetBase.add(offset.scale(serverLevel.random.nextDouble() * 0.5));

            serverLevel.sendParticles(
                    ParticleTypes.FLAME,
                    p.x, p.y, p.z,
                    1, offset.x * 0.3, offset.y * 0.3, offset.z * 0.3, 0.02
            );
            serverLevel.sendParticles(
                    ParticleTypes.LAVA,
                    p.x, p.y, p.z,
                    1, 0, -0.05, 0, 0.01
            );
            serverLevel.sendParticles(
                    ParticleTypes.SMOKE,
                    p.x, p.y, p.z,
                    1, offset.x * 0.1, offset.y * 0.1, offset.z * 0.1, 0.005
            );
        }

        // brûle le sol
        if (ticksChanneled % GROUND_BURN_INTERVAL == 0) {
            BlockPos flamePos = new BlockPos((int) jetBase.x, (int) jetBase.y, (int) jetBase.z);
            BlockPos ground = flamePos.below();

            if (serverLevel.isEmptyBlock(flamePos)
                    && !serverLevel.isEmptyBlock(ground)) {
                serverLevel.setBlock(flamePos, Blocks.FIRE.defaultBlockState(), 3);
            }
        }

        // dégâts & ignition
        if (ticksChanneled % DAMAGE_INTERVAL == 0) {
            Vec3 hitPoint = origin.add(dir.scale(progress * RANGE));
            AABB zone = new AABB(hitPoint, hitPoint).inflate(1.0);
            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
                    LivingEntity.class, zone,
                    e -> e != player && e.isAlive()
            );

            for (LivingEntity target : targets) {
                // dégâts manuels
                float newHp = Math.max(target.getHealth() - DAMAGE_PER_HIT, 0);
                target.setHealth(newHp);
                if (newHp <= 0) {
                    target.remove(RemovalReason.KILLED);
                }
                target.setSecondsOnFire(8);

                // recul
                Vec3 kb = target.position()
                        .subtract(player.position())
                        .normalize()
                        .scale(0.3);
                target.push(kb.x, 0.2, kb.z);
            }
        }
    }

    @Override
    public boolean canActivateBelowMinCharge() {
        return false;
    }
}