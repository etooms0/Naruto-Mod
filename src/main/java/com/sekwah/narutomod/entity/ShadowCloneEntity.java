package com.sekwah.narutomod.entity;

import com.mojang.authlib.GameProfile;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraftforge.client.event.sound.SoundEvent;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collections;
import java.util.Objects;

public class ShadowCloneEntity extends TamableAnimal {

    private int aliveTicks = 5*60*20;
    private final GameProfile gameProfile;
    private Player owner;

    public ShadowCloneEntity(EntityType<? extends ShadowCloneEntity> type, Level level) {
        super(type, level);
        this.gameProfile = new GameProfile(null, "Steve"); // ✅ Évite l'erreur en initialisant gameProfile

        // Vérification avant d'appliquer les valeurs
        if (this.getAttribute(Attributes.MAX_HEALTH) != null) {
            Objects.requireNonNull(this.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(2.0D);
        }
        if (this.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            Objects.requireNonNull(this.getAttribute(Attributes.MOVEMENT_SPEED)).setBaseValue(0.3D);
        }
        if (this.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            Objects.requireNonNull(this.getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(5.0D);
        }
    }

    public ShadowCloneEntity(EntityType<? extends ShadowCloneEntity> type, Level level, GameProfile profile) {
        super(type, level);
        this.gameProfile = profile;
    }



    @Override
    public boolean doHurtTarget(Entity target) {
        if (target instanceof LivingEntity victim) {
            float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE); // ✅ Récupère la force du clone
            return victim.hurt(this.damageSources().mobAttack(this), damage); // ✅ Inflige des dégâts sans erreur
        }
        return false;
    }

    public void setOwner(Player player) {
        this.owner = player;
        this.setCustomName(player.getName());
        this.setCustomNameVisible(true);
    }



    @Override
    public HumanoidArm getMainArm() {
        return this.owner != null ? this.owner.getMainArm() : HumanoidArm.RIGHT;
    }

    @Override
    public ShadowCloneEntity getBreedOffspring(ServerLevel level, AgeableMob parent) {
        return null; // ✅ Empêche la création de progéniture
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        if (this.owner != null) {
            this.owner.setItemSlot(slot, stack); // Copie l'objet au joueur
        }
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return this.owner != null ? this.owner.getItemBySlot(slot) : ItemStack.EMPTY;
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return this.owner != null ? this.owner.getArmorSlots() : Collections.emptyList();
    }

    public Player getOwner() {
        return this.owner;
    }

    public GameProfile getGameProfile() {
        return this.gameProfile;
    }

    public ResourceLocation getSkinTexture() {
        if (this.owner != null) {
            return Minecraft.getInstance().getSkinManager().getInsecureSkinLocation(owner.getGameProfile());
        }
        return new ResourceLocation("minecraft", "textures/entity/alex.png"); // ✅ Texture de secours
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 2.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D); // ✅ Permet au clone de voir les ennemis et les attaquer

    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide) {
            if (this.getHealth() - amount <= 0) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        NarutoSounds.CLONE_POOF.get(),
                        SoundSource.PLAYERS, 0.3F, 1.0F);

                this.remove(RemovalReason.KILLED);
            } else {
                this.setHealth(this.getHealth() - amount);
            }
        }
        return true;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();

        this.goalSelector.addGoal(1, new FollowOwnerGoal(this, 1.2D, 5.0F, 1.0F, false)); // ✅ Le clone suit son propriétaire !
        // ✅ Défend son propriétaire en attaquant ceux qui l'agressent
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));

        // ✅ Attaque automatiquement les cibles du propriétaire
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));

        // ✅ Le clone attaque activement sa cible au corps à corps
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2D, true)); // ⚔️ Frappe l'ennemi avec une attaque directe
    }

    public boolean shouldSprint() {
        if (this.getOwner() != null && this.distanceTo(this.getOwner()) > 5.0F) {
            return true; // ✅ Sprint si trop éloigné du joueur
        }

        if (this.getTarget() != null && this.distanceTo(this.getTarget()) > 5.0F) {
            return true; // ✅ Sprint si trop éloigné de sa cible
        }

        return false; // 🚶 Sinon, il marche normalement
    }

    @Override
    public void tick() {
        super.tick();
        this.aliveTicks--;

        if (this.shouldSprint()) {
            this.setSprinting(true); // 🚀 Sprint activé
        } else {
            this.setSprinting(false); // 🚶 Arrête de sprinter
        }

        if (this.aliveTicks <= 0) {
            if (!this.level().isClientSide) { // ✅ Assure que le son joue uniquement côté serveur
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        NarutoSounds.CLONE_POOF.get(), // ✅ Joue le son enregistré
                        SoundSource.PLAYERS, 0.3F, 1.0F);

            }

            this.remove(RemovalReason.KILLED); // ✅ Supprime le clone proprement
        }
    }

    public void updateTarget(LivingEntity newTarget) {
        if (newTarget != null && newTarget.isAlive()) {
            this.setTarget(newTarget); // ✅ Change la cible actuelle du clone
            this.setAggressive(true);  // ✅ Assure qu'il passe bien en mode combat
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