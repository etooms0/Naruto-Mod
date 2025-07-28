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

    // Configuration constants - Plus équilibrées
    private static final int POINT_COST = 1;
    private static final long DEFAULT_COMBO = 1121L;
    private static final int DURATION_TICKS = 6 * 20; // 6 secondes
    private static final int COOLDOWN_TICKS = 15 * 20; // 15 secondes
    private static final int DAMAGE_INTERVAL = 8; // Moins de spam de dégâts
    private static final float BASE_RANGE = 8.0F;
    private static final float MAX_RANGE = 16.0F;
    private static final int PARTICLES_PER_TICK = 25; // Plus de particules
    private static final double CHAKRA_CONSUME_INTERVAL = 3;
    private static final int CHAKRA_COST_PER_TICK = 2;
    private static final float BASE_DAMAGE = 2.0F;
    private static final float MAX_DAMAGE = 4.0F;
    private static final double GROUND_BURN_INTERVAL = 8;
    private static final double BLOCK_MELT_CHANCE = 0.3;

    // Tracking des entités touchées pour éviter le spam de dégâts
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

        // Vérification stricte de l'équipement - bloque TOUT si pas équipé
        if (!data.getSlotData().isEquipped(id)) {
            player.displayClientMessage(
                    Component.literal("This jutsu is not in your deck!")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            // Arrêter immédiatement l'utilisation si elle a commencé
            player.stopUsingItem();
            return false;
        }

        // Vérification du chakra minimum requis
        if (data.getChakra() < CHAKRA_COST_PER_TICK * 3) {
            player.displayClientMessage(
                    Component.literal("Not enough chakra")
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

        // Consommation progressive de chakra
        if (ticksChanneled % CHAKRA_CONSUME_INTERVAL == 0) {
            int chakraCost = calculateChakraCost(ticksChanneled);
            data.useChakra(chakraCost, chakraCost);

            if (data.getChakra() <= 0) {
                player.stopUsingItem();
                player.displayClientMessage(
                        Component.literal("No chakra")
                                .withStyle(ChatFormatting.BLUE),
                        true
                );
                return;
            }
        }

        // Calculs dynamiques basés sur la progression
        double progress = Math.min(ticksChanneled / (double) DURATION_TICKS, 1.0);
        float currentRange = BASE_RANGE + (MAX_RANGE - BASE_RANGE) * (float) progress;
        float currentDamage = BASE_DAMAGE + (MAX_DAMAGE - BASE_DAMAGE) * (float) progress;

        Vec3 origin = player.getEyePosition(1.0F);
        Vec3 direction = player.getLookAngle().normalize();

        // Effets de démarrage améliorés
        if (ticksChanneled == 0) {
            initializeFireJutsu(serverLevel, player, origin);
        }

        // Génération de particules améliorées
        generateFireParticles(serverLevel, origin, direction, currentRange, progress);

        // Effets environnementaux améliorés
        if (ticksChanneled % GROUND_BURN_INTERVAL == 0) {
            createEnvironmentalEffects(serverLevel, origin, direction, currentRange);
        }

        // Système de dégâts amélioré
        if (ticksChanneled % DAMAGE_INTERVAL == 0) {
            applyFireDamage(serverLevel, player, origin, direction, currentRange, currentDamage);
        }

        // Effets sonores continus
        if (ticksChanneled % 20 == 0) {
            serverLevel.playSound(
                    null, origin.x, origin.y, origin.z,
                    SoundEvents.FIRECHARGE_USE,
                    SoundSource.PLAYERS, 1.5F, 0.8F + (float) progress * 0.4F
            );
        }
    }

    private int calculateChakraCost(int ticksChanneled) {
        // Coût progressif : plus on maintient, plus ça coûte cher
        double progressFactor = Math.min(ticksChanneled / (double) DURATION_TICKS, 1.0);
        return CHAKRA_COST_PER_TICK + (int) (progressFactor * 2);
    }

    private void initializeFireJutsu(ServerLevel serverLevel, Player player, Vec3 origin) {
        // Sons plus dramatiques
        serverLevel.playSound(
                null, origin.x, origin.y, origin.z,
                SoundEvents.FIRE_AMBIENT,
                SoundSource.PLAYERS, 3.0F, 0.7F
        );

        serverLevel.playSound(
                null, origin.x, origin.y, origin.z,
                SoundEvents.BLAZE_SHOOT,
                SoundSource.PLAYERS, 2.0F, 1.2F
        );

        // Résistance au feu + effets bonus
        if (player instanceof ServerPlayer sp) {
            sp.addEffect(new MobEffectInstance(
                    MobEffects.FIRE_RESISTANCE,
                    DURATION_TICKS + 60, 0, false, false
            ));

            // Bonus de vitesse temporaire pour l'immersion
            sp.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SPEED,
                    40, 0, false, false
            ));
        }

        // Explosion de particules initiale
        for (int i = 0; i < 50; i++) {
            Vec3 randomOffset = new Vec3(
                    (serverLevel.random.nextDouble() - 0.5) * 2,
                    (serverLevel.random.nextDouble() - 0.5) * 2,
                    (serverLevel.random.nextDouble() - 0.5) * 2
            );

            Vec3 particlePos = origin.add(randomOffset);
            serverLevel.sendParticles(
                    ParticleTypes.FLAME,
                    particlePos.x, particlePos.y, particlePos.z,
                    1, randomOffset.x * 0.2, randomOffset.y * 0.2, randomOffset.z * 0.2, 0.1
            );
        }
    }

    private void generateFireParticles(ServerLevel serverLevel, Vec3 origin, Vec3 direction, float range, double progress) {
        int particleCount = PARTICLES_PER_TICK + (int) (progress * 15);

        // Effet lance-flammes avec dispersion progressive
        for (int i = 0; i < particleCount; i++) {
            // Distance avec distribution favorisant le début du jet
            double distanceRatio = Math.pow(serverLevel.random.nextDouble(), 0.4); // Favorise les valeurs basses
            double distance = distanceRatio * range * (0.3 + progress * 0.7); // Commence court, s'allonge

            // Dispersion qui augmente avec la distance (effet cône)
            double baseSpread = 0.1;
            double distanceSpread = (distance / range) * 0.8; // Plus c'est loin, plus ça se disperse
            double totalSpread = baseSpread + distanceSpread + progress * 0.3;

            Vec3 basePosition = origin.add(direction.scale(distance));

            // Dispersion conique naturelle
            Vec3 randomDirection = new Vec3(
                    (serverLevel.random.nextGaussian()) * totalSpread,
                    (serverLevel.random.nextGaussian()) * totalSpread * 0.7, // Moins de dispersion verticale
                    (serverLevel.random.nextGaussian()) * totalSpread
            );

            Vec3 particlePosition = basePosition.add(randomDirection);

            // Vitesse des particules avec turbulence
            Vec3 baseVelocity = direction.scale(0.3 + serverLevel.random.nextDouble() * 0.4);
            Vec3 turbulence = new Vec3(
                    (serverLevel.random.nextDouble() - 0.5) * 0.2,
                    (serverLevel.random.nextDouble() - 0.5) * 0.15,
                    (serverLevel.random.nextDouble() - 0.5) * 0.2
            );
            Vec3 finalVelocity = baseVelocity.add(turbulence);

            // Densité de particules selon la distance (plus dense au début)
            double densityFactor = 1.0 - (distance / range) * 0.6;

            // Particules de flammes principales avec variation de densité
            if (serverLevel.random.nextDouble() < densityFactor) {
                serverLevel.sendParticles(
                        ParticleTypes.FLAME,
                        particlePosition.x, particlePosition.y, particlePosition.z,
                        1, finalVelocity.x, finalVelocity.y, finalVelocity.z, 0.02
                );
            }

            // Particules de lave concentrées au centre du jet
            if (totalSpread < 0.4 && serverLevel.random.nextFloat() < 0.8 * densityFactor) {
                serverLevel.sendParticles(
                        ParticleTypes.LAVA,
                        particlePosition.x, particlePosition.y, particlePosition.z,
                        1, finalVelocity.x * 0.5, -0.05 + finalVelocity.y * 0.3, finalVelocity.z * 0.5, 0.01
                );
            }

            // Fumée dispersée sur les bords
            if (totalSpread > 0.3 && serverLevel.random.nextFloat() < 0.4) {
                serverLevel.sendParticles(
                        ParticleTypes.LARGE_SMOKE,
                        particlePosition.x, particlePosition.y, particlePosition.z,
                        1, finalVelocity.x * 0.1, finalVelocity.y * 0.1 + 0.05, finalVelocity.z * 0.1, 0.005
                );
            }

            // Braises qui tombent (effet réaliste)
            if (distance > range * 0.3 && serverLevel.random.nextFloat() < 0.3) {
                serverLevel.sendParticles(
                        ParticleTypes.FALLING_LAVA,
                        particlePosition.x, particlePosition.y, particlePosition.z,
                        1, (serverLevel.random.nextDouble() - 0.5) * 0.1, -0.2, (serverLevel.random.nextDouble() - 0.5) * 0.1, 0.02
                );
            }
        }

        // Particules supplémentaires à la source pour l'effet "sortie de bouche"
        for (int i = 0; i < 8; i++) {
            Vec3 sourceOffset = new Vec3(
                    (serverLevel.random.nextDouble() - 0.5) * 0.3,
                    (serverLevel.random.nextDouble() - 0.5) * 0.2,
                    (serverLevel.random.nextDouble() - 0.5) * 0.3
            );
            Vec3 sourcePos = origin.add(direction.scale(0.5)).add(sourceOffset);

            serverLevel.sendParticles(
                    ParticleTypes.FLAME,
                    sourcePos.x, sourcePos.y, sourcePos.z,
                    1, direction.x * 0.8, direction.y * 0.8, direction.z * 0.8, 0.1
            );
        }
    }

    private void createEnvironmentalEffects(ServerLevel serverLevel, Vec3 origin, Vec3 direction, float range) {
        // Brûler le sol sur plusieurs points
        for (int i = 1; i <= range; i += 2) {
            Vec3 flamePosition = origin.add(direction.scale(i));
            BlockPos flamePos = new BlockPos((int) flamePosition.x, (int) flamePosition.y, (int) flamePosition.z);
            BlockPos groundPos = flamePos.below();

            // Créer du feu sur le sol
            if (serverLevel.isEmptyBlock(flamePos) && !serverLevel.isEmptyBlock(groundPos)) {
                serverLevel.setBlock(flamePos, Blocks.FIRE.defaultBlockState(), 3);
            }

            // Faire fondre certains blocs (neige, glace, etc.)
            if (serverLevel.random.nextDouble() < BLOCK_MELT_CHANCE) {
                meltSurroundingBlocks(serverLevel, flamePos);
            }
        }
    }

    private void meltSurroundingBlocks(ServerLevel serverLevel, BlockPos center) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    BlockState state = serverLevel.getBlockState(pos);

                    // Faire fondre la neige et la glace
                    if (state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK)) {
                        serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    } else if (state.is(Blocks.ICE)) {
                        serverLevel.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
                    } else if (state.is(Blocks.FROSTED_ICE)) {
                        serverLevel.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private void applyFireDamage(ServerLevel serverLevel, Player player, Vec3 origin, Vec3 direction, float range, float damage) {
        // Zone d'effet plus large et progressive
        Vec3 endPoint = origin.add(direction.scale(range));
        AABB damageZone = new AABB(origin, endPoint).inflate(1.5);

        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
                LivingEntity.class,
                damageZone,
                entity -> entity != player && entity.isAlive() && !damagedEntities.contains(entity.getId())
        );

        for (LivingEntity target : targets) {
            // Calculer la distance pour ajuster les dégâts
            double distance = target.position().distanceTo(origin);
            float adjustedDamage = damage * (1.0F - (float) (distance / (range + 2.0F)));
            adjustedDamage = Math.max(adjustedDamage, damage * 0.3F); // Dégâts minimum

            // Appliquer les dégâts avec source personnalisée
            DamageSource fireSource = player.damageSources().playerAttack(player);
            target.hurt(fireSource, adjustedDamage);

            // Effets sur la cible
            target.setSecondsOnFire(12 + serverLevel.random.nextInt(8)); // 12-20 secondes

            // Knockback amélioré basé sur la distance
            Vec3 knockbackDirection = target.position()
                    .subtract(player.position())
                    .normalize();

            double knockbackStrength = 0.5 - (distance / range) * 0.3;
            knockbackStrength = Math.max(knockbackStrength, 0.1);

            target.push(
                    knockbackDirection.x * knockbackStrength,
                    0.3 + knockbackStrength * 0.2,
                    knockbackDirection.z * knockbackStrength
            );

            // Ajouter à la liste des entités déjà touchées (reset tous les DAMAGE_INTERVAL ticks)
            damagedEntities.add(target.getId());

            // Particules d'impact
            for (int i = 0; i < 10; i++) {
                Vec3 targetPos = target.position();
                serverLevel.sendParticles(
                        ParticleTypes.FLAME,
                        targetPos.x, targetPos.y + target.getBbHeight() / 2, targetPos.z,
                        1,
                        (serverLevel.random.nextDouble() - 0.5) * 0.5,
                        serverLevel.random.nextDouble() * 0.3,
                        (serverLevel.random.nextDouble() - 0.5) * 0.5,
                        0.02
                );
            }

            // Son d'impact
            serverLevel.playSound(
                    null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.PLAYERS, 1.0F, 1.5F
            );
        }

        // Clear damaged entities periodically pour permettre des dégâts continus
        if (serverLevel.getGameTime() % (DAMAGE_INTERVAL * 2) == 0) {
            damagedEntities.clear();
        }
    }

    @Override
    public boolean canActivateBelowMinCharge() {
        return false;
    }
}