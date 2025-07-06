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

        if (!this.level().isClientSide()) {
            bossBar.setProgress(this.getHealth() / this.getMaxHealth());

            if (ultimateCooldown > 0) {
                ultimateCooldown--;
            } else {
                // Lance l'ultimate si un joueur est proche
                double range = 8.0D;
                List<Player> players = this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(range));
                if (!players.isEmpty()) {
                    this.obitoUltimate();
                    ultimateCooldown = 20 * 40; // cooldown 40s
                }
            }

            // Spawn clone quand la vie est sous 50%
            if (this.getHealth() < this.getMaxHealth() / 2 && this.level().random.nextInt(400) == 0) {
                ObitoCloneEntity clone = new ObitoCloneEntity(NarutoEntities.OBITO_CLONE.get(), this.level());
                clone.moveTo(this.getX() + random.nextDouble() * 2, this.getY(), this.getZ() + random.nextDouble() * 2, this.getYRot(), this.getXRot());
                clone.setOwner(this);
                this.level().addFreshEntity(clone);
            }

            // Téléportation aléatoire pour fuir un peu
            if (this.level().random.nextInt(600) == 0) {
                double tx = this.getX() + (random.nextDouble() - 0.5) * 16;
                double ty = this.getY() + random.nextInt(3) - 1;
                double tz = this.getZ() + (random.nextDouble() - 0.5) * 16;
                this.teleportTo(tx, ty, tz);
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

    @Override
    public void die(DamageSource cause) {
        super.die(cause);
        // Tu peux mettre un message ou un drop spécial ici
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
