package com.sekwah.narutomod.entity;

import com.sekwah.narutomod.item.NarutoItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;

import javax.annotation.Nullable;

/**
 * Cette classe étend DeidaraEntity pour créer un clone avec un comportement spécifique à la mort.
 */
public class DeidaraCloneEntity extends DeidaraEntity {

    public DeidaraCloneEntity(EntityType<? extends DeidaraCloneEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D) // indispensable pour les attaques
                .add(Attributes.MOVEMENT_SPEED, 0.5D); // tu peux ajuster
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.5D, false));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
    }

    @Override
    public void die(DamageSource cause) {
        if (!this.level().isClientSide()) {
            BlockPos center = this.blockPosition();
            Level level = this.level();

            if (level instanceof ServerLevel serverLevel) {
                // Fumée dense avant l’explosion
                for (int i = 0; i < 100; i++) {
                    double dx = this.getX() + (this.getRandom().nextDouble() - 0.5) * 4;
                    double dy = this.getY() + this.getRandom().nextDouble() * 2;
                    double dz = this.getZ() + (this.getRandom().nextDouble() - 0.5) * 4;
                    serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, dx, dy, dz, 1, 0, 0.01, 0, 0.01);
                }

                // Explosion localisée
                serverLevel.explode(
                        this,
                        center.getX() + 0.5,
                        center.getY() + 0.5,
                        center.getZ() + 0.5,
                        3.0F,
                        Level.ExplosionInteraction.TNT
                );
            }
        }

        // Supprime uniquement le clone (ne pas appeler super.die())
        this.discard();
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world,
                                        DifficultyInstance difficulty,
                                        MobSpawnType reason,
                                        @Nullable SpawnGroupData data,
                                        @Nullable CompoundTag dataTag) {
        // Appelle la logique de base
        data = super.finalizeSpawn(world, difficulty, reason, data, dataTag);

        // Équipe la cape Akatsuki
        ItemStack cloak = new ItemStack(NarutoItems.AKATSUKI_CLOAK.get());
        this.setItemSlot(EquipmentSlot.CHEST, cloak);
        this.setDropChance(EquipmentSlot.CHEST, 0.0F);

        return data;
    }

}