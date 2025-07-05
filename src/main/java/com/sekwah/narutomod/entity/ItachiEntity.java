package com.sekwah.narutomod.entity;

import com.sekwah.narutomod.abilities.jutsus.FireballJutsuAbility;
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
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

import static com.sekwah.narutomod.abilities.NarutoAbilities.FIREBALL;

public class ItachiEntity extends Monster {

    private static final FireballJutsuAbility FIREBALL = new FireballJutsuAbility();
    private final ServerBossEvent bossBar = new ServerBossEvent(
            Component.literal("Itachi Uchiha"), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
    private int fireballCooldown = 0;

    public ItachiEntity(EntityType<? extends ItachiEntity> type, Level level) {
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

            LivingEntity target = this.getTarget();

            if (target instanceof Player player) {

                // Fireball Jutsu toutes les 5 secondes
                if (fireballCooldown <= 0 && this.distanceTo(player) < 20) {
                    FIREBALL.performFromEntity(this);
                    fireballCooldown = 20*10; // 5 secondes
                } else {
                    fireballCooldown--;
                }

                // Genjutsu : chance de confusion légère
                if (this.random.nextFloat() < 0.005F) { // plus rare (0.5%)
                    player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.CONFUSION,
                            60, 0, // durée 3 sec, niveau 0 (léger)
                            false, false, true));
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

        if (!this.level().isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) this.level();
            BlockPos pos = this.blockPosition();

            // Spawn 5 chauves-souris autour d’Itachi
            for (int i = 0; i < 5; i++) {
                Bat bat = EntityType.BAT.create(serverLevel);
                if (bat != null) {
                    double offsetX = (this.random.nextDouble() - 0.5) * 2;
                    double offsetY = this.random.nextDouble();
                    double offsetZ = (this.random.nextDouble() - 0.5) * 2;
                    bat.moveTo(pos.getX() + 0.5 + offsetX, pos.getY() + 1.0 + offsetY, pos.getZ() + 0.5 + offsetZ, 0, 0);
                    serverLevel.addFreshEntity(bat);
                }
            }
        }
    }




    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 250.0D)
                .add(Attributes.ATTACK_DAMAGE, 14.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.ARMOR, 5.0D);
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

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        // Exemple : drop d’un objet spécial plus tard
    }
}