package com.sekwah.narutomod.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;

public class ObitoCloneEntity extends Monster {

    private ObitoEntity owner;
    private int teleportCooldown = 0;
    private int kamuiCooldown = 0;
    private int invisibleCooldown = 0;
    private int lifeTime = 20 * 20; // 20 secondes

    public ObitoCloneEntity(EntityType<? extends ObitoCloneEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public void setOwner(ObitoEntity owner) {
        this.owner = owner;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.7D, true));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1.0D));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            for (int i = 0; i < 2; i++) {
                double ox = (this.random.nextDouble() - 0.5) * this.getBbWidth();
                double oy = this.random.nextDouble() * this.getBbHeight();
                double oz = (this.random.nextDouble() - 0.5) * this.getBbWidth();
                this.level().addParticle(ParticleTypes.PORTAL, this.getX() + ox, this.getY() + oy, this.getZ() + oz, 0, 0, 0);
            }
        }

        if (!this.level().isClientSide()) {
            lifeTime--;
            teleportCooldown--;
            kamuiCooldown--;
            invisibleCooldown--;

            // Téléportation régulière
            if (teleportCooldown <= 0) {
                teleportCooldown = 20 * 3;
                double dx = (random.nextDouble() - 0.5) * 10;
                double dy = (random.nextInt(3) - 1);
                double dz = (random.nextDouble() - 0.5) * 10;
                this.teleportTo(this.getX() + dx, this.getY() + dy, this.getZ() + dz);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
            }

            // Kamui : chance de désorienter la cible au contact
            if (kamuiCooldown <= 0) {
                List<Player> players = this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(2.0));
                for (Player player : players) {
                    if (this.canAttack(player)) {
                        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 1));
                        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 2));
                        this.level().playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_SCREAM, SoundSource.HOSTILE, 1.0F, 0.8F);
                        kamuiCooldown = 20 * 6; // 6s cooldown
                        break;
                    }
                }
            }

            // Camouflage temporaire (invisibilité)
            if (invisibleCooldown <= 0) {
                this.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0));
                invisibleCooldown = 20 * 10; // toutes les 10s
            }

            // Durée de vie écoulée
            if (lifeTime <= 0) {
                this.remove(RemovalReason.DISCARDED);
            }
        }
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState blockIn) {
        this.playSound(SoundEvents.ENDERMAN_TELEPORT, 0.15F, 1.0F);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        // pas de loot
    }

    @Override
    public void die(DamageSource cause) {
        super.die(cause);
        if (!this.level().isClientSide()) {
            ServerLevel server = (ServerLevel) this.level();
            server.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 1, this.getZ(), 15, 0.3, 0.5, 0.3, 0.01);
            server.playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_DEATH, SoundSource.HOSTILE, 1.0F, 1.2F);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 30.0D);
    }
}
