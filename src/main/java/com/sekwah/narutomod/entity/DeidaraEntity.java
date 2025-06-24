package com.sekwah.narutomod.entity;

import com.sekwah.narutomod.item.NarutoItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
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
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;

import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

public class DeidaraEntity extends Monster {

    private int clonesSpawned = 0;
    private final ServerBossEvent bossBar = new ServerBossEvent(
            Component.literal("Deidara"), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_20);

    public DeidaraEntity(EntityType<? extends DeidaraEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired(); // Pas de despawn
    }

    private void spawnClone(ServerLevel serverLevel) {
        DeidaraCloneEntity clone = new DeidaraCloneEntity(
                NarutoEntities.DEIDARA_CLONE.get(),
                serverLevel
        );
        // Décalage aléatoire pour ne pas overlapper
        double dx = this.getX() + (this.random.nextDouble() - 0.5) * 4;
        double dz = this.getZ() + (this.random.nextDouble() - 0.5) * 4;
        clone.moveTo(dx, this.getY(), dz, this.getYRot(), this.getXRot());

        // Transfert de la cible
        LivingEntity target = this.getTarget();
        if (target != null) {
            clone.setTarget(target);
        }

        // Équipe la même cape sur le clone
        ItemStack cloak = new ItemStack(NarutoItems.AKATSUKI_CLOAK.get());
        clone.setItemSlot(EquipmentSlot.CHEST, cloak);
        clone.setDropChance(EquipmentSlot.CHEST, 0.0F);

        serverLevel.addFreshEntity(clone);
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





    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);

        if (result && !this.level().isClientSide() && !(this instanceof DeidaraCloneEntity)) {
            ServerLevel serverLevel = (ServerLevel) this.level();

            // Calcule combien de clones Deidara aurait dû générer selon sa perte de vie
            float maxHealth = this.getMaxHealth();
            float currentHealth = this.getHealth();
            int expectedClones = 4 - (int)(currentHealth / (maxHealth / 5)); // 1 clone tous les 1/5e

            while (clonesSpawned < expectedClones) {
                spawnClone(serverLevel);
                clonesSpawned++;
            }
        }

        return result;
    }


    @Override
    public void die(DamageSource cause) {
        super.die(cause);

        // On exécute le code uniquement côté serveur
        if (!this.level().isClientSide()) {
            BlockPos center = this.blockPosition();
            Level level = this.level();
            int radius = 30; // Rayon de 30 blocs

            // Parcours de la zone pour former une sphère complète centrée sur 'center'
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    // On calcule la partie horizontale du rayon
                    double horizontalSquare = x * x + z * z;
                    if (horizontalSquare > radius * radius)
                        continue; // On ne traite que les (x,z) à l'intérieur du cercle du rayon

                    // Calcul de la hauteur maximale pour rester dans la sphère
                    int maxY = (int) Math.sqrt(radius * radius - horizontalSquare);
                    for (int y = -maxY; y <= maxY; y++) {
                        BlockPos pos = center.offset(x, y, z);

                        // Enlève le bloc s'il est destructible (pour ne pas toucher à la bedrock par exemple)
                        if (level.getBlockState(pos).getDestroySpeed(level, pos) >= 0) {
                            level.removeBlock(pos, false);
                        }

                        // Si l'espace est vide et que le bloc en dessous est solide,
                        // il y a 30 % de chance d'y placer du feu.
                        if (level.isEmptyBlock(pos)
                                && level.getBlockState(pos.below()).isSolidRender(level, pos.below())
                                && level.getRandom().nextFloat() < 0.3f) {
                            level.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
                        }
                    }
                }
            }

            // Déclenche l'explosion centrale pour l'effet visuel et sonore.
            // On utilise ici la signature avec ExplosionInteraction (par exemple, TNT).
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.explode(
                        this,
                        center.getX() + 0.5,
                        center.getY() + 0.5,
                        center.getZ() + 0.5,
                        6.0F,
                        Level.ExplosionInteraction.TNT
                );
            }
        }
    }


    // Attributs du boss (force, vie, vitesse…)
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    // Définir les objectifs (IA)
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1D, true));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.8D));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        // Met à jour la barre de boss avec la vie actuelle
        if (!this.level().isClientSide()) {
            bossBar.setProgress(this.getHealth() / this.getMaxHealth());
        }
    }

    // Appliquer la barre de boss aux joueurs proches
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

    // Ne loot rien pour le moment
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        // Tu pourras ici ajouter une drop spéciale plus tard
    }

    // Paquet de spawn sur le client
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}