package com.sekwah.narutomod.entity;

import com.mojang.datafixers.util.Pair;
import com.mojang.authlib.GameProfile;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.Collections;
import java.util.List;

public class ShadowCloneEntity extends TamableAnimal {

    private int aliveTicks = 5 * 60 * 20;
    private final GameProfile gameProfile;
    private Player owner;

    public ShadowCloneEntity(EntityType<? extends ShadowCloneEntity> type, Level level) {
        super(type, level);
        this.gameProfile = new GameProfile(null, "Steve"); // Évite l'erreur en initialisant gameProfile
    }

    public ShadowCloneEntity(EntityType<? extends ShadowCloneEntity> type, Level level, GameProfile profile) {
        super(type, level);
        this.gameProfile = profile;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (target instanceof LivingEntity victim) {
            float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE); // Récupère la force du clone
            return victim.hurt(this.damageSources().mobAttack(this), damage); // Inflige des dégâts sans erreur
        }
        return false;
    }

    public void setOwner(Player player) {
        this.owner = player;
        this.setCustomName(player.getName());
        this.setCustomNameVisible(true);
        System.out.println("[DEBUG] - Clone créé avec pour owner : " + player.getName().getString());
        copyEquipmentFromOwner(); // Copie l'armure et l'objet en main du joueur
    }

    @Override
    public HumanoidArm getMainArm() {
        return this.owner != null ? this.owner.getMainArm() : HumanoidArm.RIGHT;
    }

    @Override
    public ShadowCloneEntity getBreedOffspring(ServerLevel level, AgeableMob parent) {
        return null; // Empêche la création de progéniture
    }

    // --- Modification cruciale pour que le clone stocke son propre équipement ---
    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        super.setItemSlot(slot, stack); // Stocke l'objet sur le clone lui-même
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return super.getItemBySlot(slot); // Récupère l'objet stocké sur le clone
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return super.getArmorSlots(); // Utilise l’implémentation par défaut
    }
    // ---------------------------------------------------------

    public Player getOwner() {
        return this.owner;
    }

    public GameProfile getGameProfile() {
        return this.gameProfile;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 2.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D); // Permet au clone de voir les ennemis et de les attaquer
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.2D, 5.0F, 1.0F, false)); // Le clone suit son propriétaire
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this)); // Défend son propriétaire en attaquant ceux qui l'agressent
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this)); // Attaque automatiquement les cibles du propriétaire
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2D, true)); // Frappe l'ennemi avec une attaque directe
    }

    public boolean shouldSprint() {
        return this.getDeltaMovement().lengthSqr() > 0.02D; // Sprint si déplacement normal
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide) {
            if (this.getHealth() - amount <= 0) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        NarutoSounds.CLONE_POOF.get(), SoundSource.PLAYERS, 0.3F, 1.0F);
                this.remove(RemovalReason.KILLED);
            } else {
                this.setHealth(this.getHealth() - amount);
                Entity attacker = source.getEntity();
                if (attacker != null) {
                    Vec3 direction = this.position().subtract(attacker.position()).normalize();
                    this.push(direction.x * 0.5, 0.4, direction.z * 0.5);
                    this.hasImpulse = true;
                }
            }
        }
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        this.aliveTicks--;

        // Réaction si l'owner est attaqué
        if (this.owner != null && this.owner.getLastHurtByMob() != null) {
            this.updateTarget(this.owner.getLastHurtByMob());
        }

        // Évite que les clones attaquent entre eux
        if (this.getTarget() instanceof ShadowCloneEntity) {
            this.setTarget(null);
            this.setAggressive(false);
        }

        this.setSprinting(this.shouldSprint());
        if (this.aliveTicks <= 0) {
            if (!this.level().isClientSide) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        NarutoSounds.CLONE_POOF.get(), SoundSource.PLAYERS, 0.3F, 1.0F);
            }
            this.remove(RemovalReason.KILLED);
        }
    }

    public void copyEquipmentFromOwner() {
        if (this.owner != null) {
            System.out.println("[DEBUG] - Copie de l'équipement du joueur : " + this.owner.getName().getString());
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack item = this.owner.getItemBySlot(slot);
                System.out.println("[DEBUG] - Slot " + slot.getName() +
                        " → Objet du joueur : " + (item.isEmpty() ? "Aucun" : item.getDisplayName().getString()));
                // Stocke cet objet sur le clone lui-même
                this.setItemSlot(slot, item);
                // Vérification après assignation
                System.out.println("[DEBUG] - Slot " + slot.getName() +
                        " → Contenu après setItemSlot() : " +
                        (this.getItemBySlot(slot).isEmpty() ? "NON" : this.getItemBySlot(slot).getDisplayName().getString()));
            }
            if (!this.level().isClientSide) {
                System.out.println("[DEBUG] - Envoi du paquet de mise à jour visuelle au client...");
                ClientboundSetEquipmentPacket packet = new ClientboundSetEquipmentPacket(
                        this.getId(),
                        List.of(
                                Pair.of(EquipmentSlot.MAINHAND, this.getItemBySlot(EquipmentSlot.MAINHAND)),
                                Pair.of(EquipmentSlot.OFFHAND, this.getItemBySlot(EquipmentSlot.OFFHAND)),
                                Pair.of(EquipmentSlot.FEET, this.getItemBySlot(EquipmentSlot.FEET)),
                                Pair.of(EquipmentSlot.LEGS, this.getItemBySlot(EquipmentSlot.LEGS)),
                                Pair.of(EquipmentSlot.CHEST, this.getItemBySlot(EquipmentSlot.CHEST)),
                                Pair.of(EquipmentSlot.HEAD, this.getItemBySlot(EquipmentSlot.HEAD))
                        )
                );
                for (Player player : this.level().players()) {
                    if (player instanceof ServerPlayer serverPlayer) {
                        System.out.println("[DEBUG] - Envoi des données au joueur : " + serverPlayer.getName().getString());
                        serverPlayer.connection.send(packet);
                    }
                }
            }
        } else {
            System.out.println("[DEBUG] - Impossible de copier l'équipement : Owner est null !");
        }
    }

    public void updateTarget(LivingEntity newTarget) {
        if (newTarget != null && newTarget.isAlive()) {
            this.setTarget(newTarget);
            this.setAggressive(true);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket entityPacket) {
        super.recreateFromPacket(entityPacket);
        this.setYBodyRot(entityPacket.getYRot());
    }
}