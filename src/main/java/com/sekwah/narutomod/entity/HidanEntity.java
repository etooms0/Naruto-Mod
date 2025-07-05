package com.sekwah.narutomod.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerPlayer;
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

public class HidanEntity extends Monster {

    private LivingEntity linkedTarget = null;
    private int ritualCooldown = 0;
    private final float LINK_RANGE = 30F;

    public HidanEntity(EntityType<? extends HidanEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.8D));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            if (linkedTarget != null) {
                if (!linkedTarget.isAlive() || this.distanceTo(linkedTarget) > LINK_RANGE) {
                    linkedTarget = null;
                    // Optionnel : feedback visuel ou sonore
                }
            }

            if (ritualCooldown > 0) {
                ritualCooldown--;
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (super.doHurtTarget(target)) {
            if (ritualCooldown == 0 && target instanceof LivingEntity living) {
                this.linkedTarget = living;
                ritualCooldown = 600; // 30 secondes
                this.level().broadcastEntityEvent(this, (byte) 12); // feedback client
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (linkedTarget != null && linkedTarget.isAlive() && amount > 0) {
            linkedTarget.hurt(source, amount);
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return super.isInvulnerableTo(source);
    }

    @Override
    public void die(DamageSource cause) {
        super.die(cause);
        linkedTarget = null;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 200.0D)
                .add(Attributes.ATTACK_DAMAGE, 12.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.ARMOR, 4.0D);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Hidan");
    }
}
