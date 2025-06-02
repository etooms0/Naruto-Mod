package com.sekwah.narutomod.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import java.util.Collections;

public class ShadowCloneEntity extends LivingEntity {

    private int aliveTicks = 20 * 3;
    private final GameProfile gameProfile;
    private Player owner;

    public void setOwner(Player player) {
        this.owner = player;
    }

    @Override
    public HumanoidArm getMainArm() {
        return this.owner != null ? this.owner.getMainArm() : HumanoidArm.RIGHT;
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
            GameProfile profile = this.owner.getGameProfile();
            return Minecraft.getInstance().getSkinManager().getInsecureSkinLocation(profile);
        }
        return new ResourceLocation("minecraft", "textures/entity/steve.png"); // Texture par défaut
    }

    public ShadowCloneEntity(EntityType<? extends ShadowCloneEntity> type, Level level) {
        super(type, level);
        this.gameProfile = new GameProfile(null, "steve"); // Valeur par défaut
    }

    public ShadowCloneEntity(EntityType<? extends ShadowCloneEntity> type, Level level, GameProfile profile) {
        super(type, level);
        this.gameProfile = profile;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Le clone disparaît plutôt que de prendre des dégâts
        this.kill();
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        this.aliveTicks--;
        if (this.aliveTicks <= 0) {
            this.kill();
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