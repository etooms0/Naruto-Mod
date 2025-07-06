package com.sekwah.narutomod.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
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

public class ObitoCloneEntity extends Monster {

    private ObitoEntity owner;
    private int teleportCooldown = 0;

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
            // Particules portails légères autour
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

            if (teleportCooldown <= 0) {
                teleportCooldown = 20 * 3; // téléporte toutes les 3 secondes

                double dx = (random.nextDouble() - 0.5) * 10; // décalage X aléatoire jusqu'à 5 blocs à gauche/droite
                double dy = (random.nextInt(3) - 1); // décalage Y aléatoire -1, 0 ou +1 bloc
                double dz = (random.nextDouble() - 0.5) * 10; // décalage Z aléatoire

                this.teleportTo(this.getX() + dx, this.getY() + dy, this.getZ() + dz);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
            }

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
        // Pas de loot pour clone
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 30.0D);
    }
}
