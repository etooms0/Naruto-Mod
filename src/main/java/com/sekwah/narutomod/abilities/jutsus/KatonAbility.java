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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class KatonAbility extends Ability implements Ability.Cooldown, Ability.Channeled {

    private static final int POINT_COST         = 5;
    private static final long DEFAULT_COMBO      = 23231L;
    private static final int DURATION_TICKS     = 5 * 20;    // 5 secondes
    private static final int COOLDOWN_TICKS     = 12 * 20;   // 12 secondes
    private static final int DAMAGE_INTERVAL    = 4;         // dégâts toutes les 4 ticks
    private static final float RANGE            = 12.0F;     // portée max
    private static final int PARTICLES_PER_TICK = 5;         // nombre de particules par tick
    private static final double CONSUME_INTERVAL = 2;        // 1 chakra consommé toutes les 2 ticks
    private static final float DAMAGE_PER_HIT   = 1.5F;      // PV retirés à chaque hit

    @Override
    public ActivationType activationType() {
        return ActivationType.CHANNELED;
    }

    @Override
    public void performServer(Player player, INinjaData data, int useCount) {
        handleChannelling(player, data, useCount);
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
        // Vérifie que le jutsu est équipé dans le deck
        String id = NarutoRegistries.ABILITIES
                .getResourceKey(this)
                .map(r -> r.location().getPath())
                .orElse("");
        if (!data.getSlotData().isEquipped(id)) {
            player.displayClientMessage(
                    Component.literal("Jutsu non équipé dans votre deck")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return false;
        }
        return true;
    }

    // Démarre la canalisation
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    // Durée de canalisation
    public int getUseDuration(ItemStack stack) {
        return DURATION_TICKS;
    }

    // Tick côté serveur à chaque moment de canalisation
    @Override
    public void handleChannelling(Player player, INinjaData data, int ticksChanneled) {
        Level level = player.level();
        if (!(level instanceof ServerLevel serverLevel)) return;

        // consommation lente de chakra
        if (ticksChanneled % CONSUME_INTERVAL == 0) {
            data.useChakra(1, 1);
            if (data.getChakra() <= 0) {
                player.stopUsingItem();
                return;
            }
        }

        // position de départ et direction
        Vec3 origin = player.getEyePosition(1.0F);
        Vec3 dir    = player.getLookAngle().normalize();
        // fraction de progression de 0→1
        double progress = ticksChanneled / (double) DURATION_TICKS;
        Vec3 startPos = origin.add(dir.scale(0.5 + progress * RANGE));

        // particules en “jet de flammes”
        for (int i = 0; i < PARTICLES_PER_TICK; i++) {
            double spread = 0.1;
            Vec3 offset = dir
                    .add((serverLevel.random.nextDouble() - 0.5) * spread,
                            (serverLevel.random.nextDouble() - 0.5) * spread,
                            (serverLevel.random.nextDouble() - 0.5) * spread)
                    .normalize();

            double px = startPos.x, py = startPos.y, pz = startPos.z;
            double vx = offset.x * 0.2, vy = offset.y * 0.2, vz = offset.z * 0.2;

            serverLevel.sendParticles(
                    ParticleTypes.FLAME,
                    px, py, pz,
                    1, vx, vy, vz, 0.01
            );
            serverLevel.sendParticles(
                    ParticleTypes.SMOKE,
                    px, py, pz,
                    1, vx * 0.5, vy * 0.5, vz * 0.5, 0.005
            );
        }

        // dégâts, embrasement et recul toutes les DAMAGE_INTERVAL ticks
        if (ticksChanneled % DAMAGE_INTERVAL == 0) {
            Vec3 hitPoint = origin.add(dir.scale(progress * RANGE));
            AABB zone = new AABB(hitPoint, hitPoint).inflate(0.75);
            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
                    LivingEntity.class,
                    zone,
                    e -> e != player && e.isAlive()
            );
            for (LivingEntity target : targets) {
                float newHp = Math.max(target.getHealth() - DAMAGE_PER_HIT, 0);
                target.setHealth(newHp);
                if (newHp <= 0) {
                    target.remove(RemovalReason.KILLED);
                }
                target.setSecondsOnFire(3);
            }
        }
    }

    @Override
    public boolean canActivateBelowMinCharge() {
        return false;
    }
}