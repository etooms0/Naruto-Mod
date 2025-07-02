package com.sekwah.narutomod.entity;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;

public class SusanoEntity extends Mob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.susano.idle");
    private Player owner;

    public SusanoEntity(EntityType<? extends SusanoEntity> type, Level level) {
        super(type, level);
        System.out.println("[SusanoEntity] <ctor> on level " + level.dimension().location());
        this.setNoGravity(true);
        this.setNoAi(true);
        this.noPhysics = true;
        this.setInvulnerable(true);
    }

    public void setOwner(Player player) {
        this.owner = player;
    }

    @Nullable
    public Player getOwner() {
        return this.owner;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;  // immune
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public boolean shouldRiderSit() {
        return false; // Ne modifie pas la pose du joueur
    }

    @Override
    public boolean isPassenger() {
        return true;
    }

    @Override
    public boolean canRiderInteract() {
        return false;
    }


    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            return;
        }

        if (owner != null && owner.isAlive()) {
            this.setPos(owner.getX(), owner.getY(), owner.getZ());
            float yaw   = owner.getYRot();
            float pitch = owner.getXRot();
            this.setYRot(yaw);
            this.setXRot(pitch);
            this.yBodyRot = yaw;
            this.yHeadRot = owner.yHeadRot;

            double radius = 2.0D; // zone autour du Susano où on détecte les projectiles
            AABB detectionBox = this.getBoundingBox().inflate(radius);

            for (Projectile projectile : this.level().getEntitiesOfClass(Projectile.class, detectionBox)) {
                if (projectile.isAlive() && !projectile.isRemoved()) {
                    Vec3 projPos = projectile.position();
                    Vec3 susanoPos = this.position();
                    Vec3 toProj = projPos.subtract(susanoPos);

                    Vec3 velocity = projectile.getDeltaMovement();
                    if (velocity.dot(toProj) < 0) { // projectile va vers Susano
                        projectile.setDeltaMovement(velocity.multiply(-1, -1, -1));
                    }
                }
            }
        } else {
            discard();
        }
    }

    @Override
    public double getMyRidingOffset() {
        return -1.5D; // ou -2.0D, à ajuster selon la hauteur de ton modèle
    }


    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, state -> {
            state.setAnimation(IDLE);
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEFINED;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        System.out.println("[SusanoEntity] getAddEntityPacket() called");
        //noinspection unchecked
        return (Packet<ClientGamePacketListener>) NetworkHooks.getEntitySpawningPacket(this);
    }
}