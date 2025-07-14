package com.sekwah.narutomod.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
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
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

public class ObitoEntity extends Monster {

    private int invulnerableTicks = 0;


    private final ServerBossEvent bossBar = new ServerBossEvent(
            Component.literal("Obito Uchiha"), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);

    private int ultimateCooldown = 0;
    private final Random random = new Random();

    public ObitoEntity(EntityType<? extends ObitoEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.5D, true));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.8D));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) return;

        bossBar.setProgress(getHealth() / getMaxHealth());

        if (getHealth() < getMaxHealth() * 0.25F && invulnerableTicks < 200) {
            this.setInvulnerable(true);
            invulnerableTicks++;
        } else {
            this.setInvulnerable(false);
        }


        tryCastUltimate();
        trySpawnClone();
        tryRandomTeleportToTarget();
    }

    private void trySpawnClone() {
        if (this.getHealth() < this.getMaxHealth() / 2 && this.level().random.nextInt(400) == 0) {
            ObitoCloneEntity clone = new ObitoCloneEntity(NarutoEntities.OBITO_CLONE.get(), this.level());
            clone.moveTo(
                    this.getX() + (random.nextDouble() - 0.5) * 4.0,
                    this.getY(),
                    this.getZ() + (random.nextDouble() - 0.5) * 4.0,
                    this.getYRot(),
                    this.getXRot()
            );
            clone.setOwner(this);
            this.level().addFreshEntity(clone);

            this.level().playSound(null, this.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1.0F, 1.0F);
            for (int i = 0; i < 10; i++) {
                double dx = this.getX() + (random.nextDouble() - 0.5) * 2;
                double dy = this.getY() + 1 + random.nextDouble();
                double dz = this.getZ() + (random.nextDouble() - 0.5) * 2;
                this.level().addParticle(ParticleTypes.SMOKE, dx, dy, dz, 0, 0.1, 0);
            }
        }
    }



    private void tryCastUltimate() {
        if (ultimateCooldown > 0) {
            ultimateCooldown--;
            return;
        }

        double range = 8.0D;
        List<Player> players = this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(range));
        if (!players.isEmpty()) {
            this.castUltimate();
            ultimateCooldown = 20 * 40; // cooldown 40 secondes
        }
    }


    private void tryRandomTeleportToTarget() {
        if (random.nextInt(500) == 0) {
            Player target = level().getNearestPlayer(this, 10);
            if (target != null) {
                double dx = -Math.sin(Math.toRadians(target.getYRot())) * 2.0;
                double dz = Math.cos(Math.toRadians(target.getYRot())) * 2.0;
                double tx = target.getX() + dx;
                double ty = target.getY();
                double tz = target.getZ() + dz;
                teleportTo(tx, ty, tz);
                level().playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.0F);
            }
        }
    }


    private void obitoUltimate() {
        // AOE simple : inflige des dégâts + effet de confusion et ralentissement
        double radius = 7.0D;
        List<LivingEntity> entities = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius), e -> e != this);

        this.level().playSound(null, this.blockPosition(), SoundEvents.WITHER_HURT, SoundSource.HOSTILE, 1.2F, 1.0F);

        for (LivingEntity e : entities) {
            float newHealth = e.getHealth() - 20.0F;
            e.setHealth(Math.max(newHealth, 0)); // Evite la vie négative
            e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 140, 2));
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 4));
        }

        // Particules dragon breath autour
        for (int i = 0; i < 60; i++) {
            double px = this.getX() + (random.nextDouble() - 0.5) * 14;
            double py = this.getY() + random.nextDouble() * 4 + 1;
            double pz = this.getZ() + (random.nextDouble() - 0.5) * 14;
            this.level().addParticle(ParticleTypes.DRAGON_BREATH, px, py, pz, 0, 0.1, 0);
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    private void castUltimate() {
        List<LivingEntity> affected = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(7.0D), e -> e != this);

        level().playSound(null, blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.5F, 0.9F);

        for (LivingEntity target : affected) {
            // Aspiration vers Obito
            double dx = this.getX() - target.getX();
            double dz = this.getZ() - target.getZ();
            double pullStrength = 0.5D;
            target.setDeltaMovement(dx * pullStrength, 0.1, dz * pullStrength);

            // Dégâts et effets
            target.hurt(damageSources().magic(), 15.0F);
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 1));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 2));
        }

        for (int i = 0; i < 40; i++) {
            double px = getX() + (random.nextDouble() - 0.5) * 10;
            double py = getY() + random.nextDouble() * 3;
            double pz = getZ() + (random.nextDouble() - 0.5) * 10;
            level().addParticle(ParticleTypes.PORTAL, px, py, pz, 0, 0, 0);
        }
    }


    @Override
    public void die(DamageSource cause) {
        this.setInvulnerable(false); // Assure qu’il est tuable à la fin
        super.die(cause);
    }


    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 350.0D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.ARMOR, 8.0D);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

}
