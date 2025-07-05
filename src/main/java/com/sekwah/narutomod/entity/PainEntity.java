package com.sekwah.narutomod.entity;

import com.sekwah.narutomod.abilities.jutsus.EarthSphereLiftJutsuAbility;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.item.NarutoItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

public class PainEntity extends Monster {

    private int jutsuCooldown = 0;
    private final ServerBossEvent bossBar = new ServerBossEvent(
            Component.literal("Pain"), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);

    public PainEntity(EntityType<? extends PainEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3D, true));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.8D));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }




    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            bossBar.setProgress(this.getHealth() / this.getMaxHealth());

            if (jutsuCooldown > 0) {
                jutsuCooldown--;

                // Attire ou repousse les entités autour de lui
                double radius = 5.0D;

                for (LivingEntity entity : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius))) {
                    if (entity != this && !(entity instanceof PainEntity)) {
                        double dx = entity.getX() - this.getX();
                        double dz = entity.getZ() - this.getZ();
                        double dist = Math.max(0.1, Math.sqrt(dx * dx + dz * dz));

                        boolean repel = this.tickCount % 400 < 200; // alterne entre attraction et répulsion toutes les 10s

                        if (repel) {
                            // Repousse
                            entity.push(dx / dist * 2.0, 0.5, dz / dist * 2.0);
                        } else {
                            // Attire
                            double attractionStrength = 0.5 / dist;
                            entity.setDeltaMovement(
                                    entity.getDeltaMovement().add(
                                            -dx / dist * attractionStrength,
                                            0,
                                            -dz / dist * attractionStrength
                                    )
                            );
                        }
                    }
                }
            } else {
                // Utilise le jutsu quand le cooldown est terminé
                double range = 15.0D;
                LivingEntity target = this.level()
                        .getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(range))
                        .stream()
                        .filter(e -> e != this && !(e instanceof PainEntity))
                        .findFirst()
                        .orElse(null);

                if (target != null) {
                    new EarthSphereLiftJutsuAbility().performFromEntity(this, target.position());
                    jutsuCooldown = 20 * 50; // 15s de cooldown
                }
            }
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
        // Tu peux mettre un message genre : "La douleur... est nécessaire."
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0D)
                .add(Attributes.ATTACK_DAMAGE, 18.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.ARMOR, 6.0D);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world,
                                        DifficultyInstance difficulty,
                                        MobSpawnType reason,
                                        @Nullable SpawnGroupData data,
                                        @Nullable CompoundTag dataTag) {
        data = super.finalizeSpawn(world, difficulty, reason, data, dataTag);

        ItemStack cloak = new ItemStack(NarutoItems.AKATSUKI_CLOAK.get());
        this.setItemSlot(EquipmentSlot.CHEST, cloak);
        this.setDropChance(EquipmentSlot.CHEST, 0.0F);
        return data;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
