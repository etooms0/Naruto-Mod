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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class KatonAbility extends Ability implements Ability.Cooldown, Ability.Channeled {

    // Configuration constants - Enhanced for more power
    private static final int POINT_COST = 1;
    private static final long DEFAULT_COMBO = 1121L;
    private static final int DURATION_TICKS = 6 * 20; // 6 seconds
    private static final int COOLDOWN_TICKS = 15 * 20; // 15 seconds
    private static final int DAMAGE_INTERVAL = 6; // More frequent damage
    private static final float BASE_RANGE = 10.0F; // Increased base range
    private static final float MAX_RANGE = 20.0F; // Increased max range
    private static final int PARTICLES_PER_TICK = 40; // More particles for enhanced effect
    private static final double CHAKRA_CONSUME_INTERVAL = 3;
    private static final int CHAKRA_COST_PER_TICK = 2;
    private static final float BASE_DAMAGE = 3.5F; // Increased base damage
    private static final float MAX_DAMAGE = 7.0F; // Increased max damage
    private static final double GROUND_BURN_INTERVAL = 6; // More frequent ground burning
    private static final double BLOCK_MELT_CHANCE = 0.5; // Higher melt chance

    // Track damaged entities to prevent damage spam
    private final Set<Integer> damagedEntities = new HashSet<>();

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

        // Strict equipment check - blocks EVERYTHING if not equipped
        if (!data.getSlotData().isEquipped(id)) {
            player.displayClientMessage(
                    Component.literal("This jutsu is not in your deck!")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            // Stop usage immediately if it has started
            player.stopUsingItem();
            return false;
        }

        // Check minimum required chakra
        if (data.getChakra() < CHAKRA_COST_PER_TICK * 3) {
            player.displayClientMessage(
                    Component.literal("Not enough chakra!")
                            .withStyle(ChatFormatting.BLUE),
                    true
            );
            player.stopUsingItem();
            return false;
        }

        return true;
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

        // Progressive chakra consumption
        if (ticksChanneled % CHAKRA_CONSUME_INTERVAL == 0) {
            int chakraCost = calculateChakraCost(ticksChanneled);
            data.useChakra(chakraCost, chakraCost);

            if (data.getChakra() <= 0) {
                player.stopUsingItem();
                player.displayClientMessage(
                        Component.literal("Out of chakra!")
                                .withStyle(ChatFormatting.BLUE),
                        true
                );
                return;
            }
        }

        // Dynamic calculations based on progression
        double progress = Math.min(ticksChanneled / (double) DURATION_TICKS, 1.0);
        float currentRange = BASE_RANGE + (MAX_RANGE - BASE_RANGE) * (float) progress;
        float currentDamage = BASE_DAMAGE + (MAX_DAMAGE - BASE_DAMAGE) * (float) progress;

        Vec3 origin = player.getEyePosition(1.0F);
        Vec3 direction = player.getLookAngle().normalize();

        if (ticksChanneled == 0) {
            initializeFireJutsu(serverLevel, player, origin);
        }

        generateEnhancedFireParticles(serverLevel, origin, direction, currentRange, progress);

        if (ticksChanneled % GROUND_BURN_INTERVAL == 0) {
            createEnvironmentalEffects(serverLevel, origin, direction, currentRange);
        }

        if (ticksChanneled % DAMAGE_INTERVAL == 0) {
            applyEnhancedFireDamage(serverLevel, player, origin, direction, currentRange, currentDamage);
        }

        // Enhanced sound effects
        if (ticksChanneled % 15 == 0) {
            serverLevel.playSound(
                    null, origin.x, origin.y, origin.z,
                    SoundEvents.FIRECHARGE_USE,
                    SoundSource.PLAYERS, 2.0F, 0.7F + (float) progress * 0.5F
            );
        }
    }

    private int calculateChakraCost(int ticksChanneled) {
        double progressFactor = Math.min(ticksChanneled / (double) DURATION_TICKS, 1.0);
        return CHAKRA_COST_PER_TICK + (int) (progressFactor * 2);
    }

    private void initializeFireJutsu(ServerLevel serverLevel, Player player, Vec3 origin) {
        // Enhanced initialization sounds
        serverLevel.playSound(
                null, origin.x, origin.y, origin.z,
                SoundEvents.FIRE_AMBIENT,
                SoundSource.PLAYERS, 4.0F, 0.6F
        );

        serverLevel.playSound(
                null, origin.x, origin.y, origin.z,
                SoundEvents.BLAZE_SHOOT,
                SoundSource.PLAYERS, 3.0F, 1.0F
        );

        if (player instanceof ServerPlayer sp) {
            sp.addEffect(new MobEffectInstance(
                    MobEffects.FIRE_RESISTANCE,
                    DURATION_TICKS + 60, 0, false, false
            ));

            sp.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SPEED,
                    40, 1, false, false // Enhanced speed boost
            ));
        }

        // Enhanced initialization particle burst
        for (int i = 0; i < 80; i++) {
            Vec3 randomOffset = new Vec3(
                    (serverLevel.random.nextDouble() - 0.5) * 2.5,
                    (serverLevel.random.nextDouble() - 0.5) * 2.5,
                    (serverLevel.random.nextDouble() - 0.5) * 2.5
            );

            Vec3 particlePos = origin.add(randomOffset);
            serverLevel.sendParticles(
                    ParticleTypes.FLAME,
                    particlePos.x, particlePos.y, particlePos.z,
                    2, randomOffset.x * 0.3, randomOffset.y * 0.3, randomOffset.z * 0.3, 0.15
            );
        }
    }

    private void generateEnhancedFireParticles(ServerLevel serverLevel, Vec3 origin, Vec3 direction, float range, double progress) {
        int particleCount = PARTICLES_PER_TICK + (int) (progress * 25); // More particles with progression

        for (int i = 0; i < particleCount; i++) {
            double distanceRatio = Math.pow(serverLevel.random.nextDouble(), 0.2); // Even more particles closer to source
            double distance = distanceRatio * range * (0.1 + progress * 0.9);

            // Enhanced cone effect - much more pronounced spreading
            double baseSpread = 0.05; // Tighter at base
            double distanceSpread = (distance / range) * 2.5; // Much stronger cone expansion
            double progressSpread = progress * 0.8; // More spread with progress
            double totalSpread = baseSpread + distanceSpread + progressSpread;

            // Create more pronounced cone shape
            double coneAngle = Math.PI * 0.3 * (distance / range); // 54 degrees max cone
            double randomAngle = serverLevel.random.nextDouble() * Math.PI * 2;
            double coneRadius = Math.tan(coneAngle) * distance;
            double particleRadius = serverLevel.random.nextDouble() * coneRadius;

            Vec3 basePosition = origin.add(direction.scale(distance));

            // Calculate perpendicular vectors for cone distribution
            Vec3 up = new Vec3(0, 1, 0);
            Vec3 right = direction.cross(up).normalize();
            Vec3 actualUp = right.cross(direction).normalize();

            // Position particle within the cone
            Vec3 coneOffset = right.scale(Math.cos(randomAngle) * particleRadius)
                    .add(actualUp.scale(Math.sin(randomAngle) * particleRadius));

            Vec3 particlePosition = basePosition.add(coneOffset);

            // Enhanced particle velocity with cone-based direction
            Vec3 coneDirection = particlePosition.subtract(origin).normalize();
            Vec3 baseVelocity = coneDirection.scale(0.5 + serverLevel.random.nextDouble() * 0.7);
            Vec3 turbulence = new Vec3(
                    (serverLevel.random.nextDouble() - 0.5) * 0.2,
                    (serverLevel.random.nextDouble() - 0.5) * 0.15,
                    (serverLevel.random.nextDouble() - 0.5) * 0.2
            );
            Vec3 finalVelocity = baseVelocity.add(turbulence);

            // Enhanced particle density based on distance and cone position
            double densityFactor = 1.5 - (distance / range) * 0.4;
            double coneDensity = 1.0 - (particleRadius / Math.max(coneRadius, 0.1)) * 0.3;

            // Main flame particles with enhanced density
            if (serverLevel.random.nextDouble() < densityFactor * coneDensity) {
                serverLevel.sendParticles(
                        ParticleTypes.FLAME,
                        particlePosition.x, particlePosition.y, particlePosition.z,
                        2, finalVelocity.x, finalVelocity.y, finalVelocity.z, 0.04
                );
            }

            // Enhanced lava particles concentrated at the center of the cone
            if (particleRadius < coneRadius * 0.4 && serverLevel.random.nextFloat() < 0.9 * densityFactor) {
                serverLevel.sendParticles(
                        ParticleTypes.LAVA,
                        particlePosition.x, particlePosition.y, particlePosition.z,
                        1, finalVelocity.x * 0.6, -0.03 + finalVelocity.y * 0.4, finalVelocity.z * 0.6, 0.02
                );
            }

            // Enhanced smoke on the outer edges of the cone
            if (particleRadius > coneRadius * 0.3 && serverLevel.random.nextFloat() < 0.6) {
                serverLevel.sendParticles(
                        ParticleTypes.LARGE_SMOKE,
                        particlePosition.x, particlePosition.y, particlePosition.z,
                        1, finalVelocity.x * 0.15, finalVelocity.y * 0.15 + 0.08, finalVelocity.z * 0.15, 0.008
                );
            }

            // More falling embers for realistic effect
            if (distance > range * 0.2 && serverLevel.random.nextFloat() < 0.5) {
                serverLevel.sendParticles(
                        ParticleTypes.FALLING_LAVA,
                        particlePosition.x, particlePosition.y, particlePosition.z,
                        1, (serverLevel.random.nextDouble() - 0.5) * 0.15, -0.25, (serverLevel.random.nextDouble() - 0.5) * 0.15, 0.03
                );
            }

            // Additional small flame particles for intensity (removed blue soul fire)
            if (progress > 0.5 && serverLevel.random.nextFloat() < 0.4) {
                serverLevel.sendParticles(
                        ParticleTypes.SMALL_FLAME,
                        particlePosition.x, particlePosition.y, particlePosition.z,
                        1, finalVelocity.x * 0.9, finalVelocity.y * 0.9, finalVelocity.z * 0.9, 0.06
                );
            }
        }

        // Enhanced source particles for "mouth breathing" effect
        for (int i = 0; i < 12; i++) {
            Vec3 sourceOffset = new Vec3(
                    (serverLevel.random.nextDouble() - 0.5) * 0.4,
                    (serverLevel.random.nextDouble() - 0.5) * 0.25,
                    (serverLevel.random.nextDouble() - 0.5) * 0.4
            );
            Vec3 sourcePos = origin.add(direction.scale(0.8)).add(sourceOffset);

            serverLevel.sendParticles(
                    ParticleTypes.FLAME,
                    sourcePos.x, sourcePos.y, sourcePos.z,
                    2, direction.x * 1.0, direction.y * 1.0, direction.z * 1.0, 0.12
            );
        }
    }

    private void createEnvironmentalEffects(ServerLevel serverLevel, Vec3 origin, Vec3 direction, float range) {
        // Enhanced ground burning over multiple points
        for (int i = 1; i <= range; i += 1.5) {
            Vec3 flamePosition = origin.add(direction.scale(i));
            BlockPos flamePos = new BlockPos((int) flamePosition.x, (int) flamePosition.y, (int) flamePosition.z);
            BlockPos groundPos = flamePos.below();

            // Create fire on the ground with spread
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos spreadPos = flamePos.offset(x, 0, z);
                    BlockPos spreadGround = spreadPos.below();

                    if (serverLevel.isEmptyBlock(spreadPos) && !serverLevel.isEmptyBlock(spreadGround)
                            && serverLevel.random.nextFloat() < 0.7) {
                        serverLevel.setBlock(spreadPos, Blocks.FIRE.defaultBlockState(), 3);
                    }
                }
            }

            // Enhanced block melting
            if (serverLevel.random.nextDouble() < BLOCK_MELT_CHANCE) {
                meltSurroundingBlocks(serverLevel, flamePos);
            }
        }
    }

    private void meltSurroundingBlocks(ServerLevel serverLevel, BlockPos center) {
        // Enhanced melting radius
        for (int x = -2; x <= 2; x++) {
            for (int y = -1; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    BlockState state = serverLevel.getBlockState(pos);

                    // Enhanced melting with more block types
                    if (state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK)) {
                        serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        // Add steam particles
                        serverLevel.sendParticles(ParticleTypes.CLOUD,
                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                3, 0.2, 0.2, 0.2, 0.05);
                    } else if (state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE)) {
                        serverLevel.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
                    } else if (state.is(Blocks.FROSTED_ICE)) {
                        serverLevel.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
                    } else if (state.is(Blocks.POWDER_SNOW)) {
                        serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private void applyEnhancedFireDamage(ServerLevel serverLevel, Player player, Vec3 origin, Vec3 direction, float range, float damage) {
        // Enhanced damage zone - wider and more progressive
        Vec3 endPoint = origin.add(direction.scale(range));
        AABB damageZone = new AABB(origin, endPoint).inflate(2.0); // Increased damage radius

        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
                LivingEntity.class,
                damageZone,
                entity -> entity != player && entity.isAlive() && !damagedEntities.contains(entity.getId())
        );

        for (LivingEntity target : targets) {
            // Calculate distance to adjust damage
            double distance = target.position().distanceTo(origin);
            float adjustedDamage = damage * (1.2F - (float) (distance / (range + 3.0F))); // Enhanced damage calculation
            adjustedDamage = Math.max(adjustedDamage, damage * 0.4F); // Higher minimum damage

            // Apply damage with custom source
            DamageSource fireSource = player.damageSources().playerAttack(player);
            target.hurt(fireSource, adjustedDamage);

            // Enhanced effects on target
            target.setSecondsOnFire(15 + serverLevel.random.nextInt(10)); // 15-25 seconds burning

            // Enhanced knockback based on distance
            Vec3 knockbackDirection = target.position()
                    .subtract(player.position())
                    .normalize();

            double knockbackStrength = 0.8 - (distance / range) * 0.4; // Stronger knockback
            knockbackStrength = Math.max(knockbackStrength, 0.2);

            target.push(
                    knockbackDirection.x * knockbackStrength,
                    0.4 + knockbackStrength * 0.3, // Higher vertical knockback
                    knockbackDirection.z * knockbackStrength
            );

            // Add to damaged entities list (reset every DAMAGE_INTERVAL ticks)
            damagedEntities.add(target.getId());

            // Enhanced impact particles
            for (int i = 0; i < 15; i++) {
                Vec3 targetPos = target.position();
                serverLevel.sendParticles(
                        ParticleTypes.FLAME,
                        targetPos.x, targetPos.y + target.getBbHeight() / 2, targetPos.z,
                        2,
                        (serverLevel.random.nextDouble() - 0.5) * 0.8,
                        serverLevel.random.nextDouble() * 0.5,
                        (serverLevel.random.nextDouble() - 0.5) * 0.8,
                        0.04
                );
            }

            // Additional impact particles for enhanced effect
            for (int i = 0; i < 8; i++) {
                Vec3 targetPos = target.position();
                serverLevel.sendParticles(
                        ParticleTypes.LAVA,
                        targetPos.x, targetPos.y + target.getBbHeight() / 2, targetPos.z,
                        1,
                        (serverLevel.random.nextDouble() - 0.5) * 0.4,
                        serverLevel.random.nextDouble() * 0.2,
                        (serverLevel.random.nextDouble() - 0.5) * 0.4,
                        0.02
                );
            }

            // Enhanced impact sound
            serverLevel.playSound(
                    null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.PLAYERS, 1.5F, 1.3F
            );
        }

        // Clear damaged entities periodically to allow continuous damage
        if (serverLevel.getGameTime() % (DAMAGE_INTERVAL * 2) == 0) {
            damagedEntities.clear();
        }
    }

    @Override
    public boolean canActivateBelowMinCharge() {
        return false;
    }
}