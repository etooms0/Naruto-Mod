package com.sekwah.narutomod.entity;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkHooks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
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
        // Toujours refuser tout dommage
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        // Désactive toute collision physique
        return false;
    }

    @Override
    public boolean isPickable() {
        // IGNORE Susano lors du ray-trace (clic et attaque)
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        // Toujours invulnérable
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        // côté client ?
        if (this.level().isClientSide()) {
            return;
        }
        // serveur seulement
        if (owner != null && owner.isAlive()) {
            setPos(owner.getX(), owner.getY(), owner.getZ());
        } else {
            System.out.println("[SusanoEntity] owner invalid → discard()");
            discard();
        }
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
                .add(Attributes.MAX_HEALTH, 20.0D)   // ou la vie max que tu veux
                .add(Attributes.MOVEMENT_SPEED, 0.0D); // Susano ne bouge pas par lui-même
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        System.out.println("[SusanoEntity] getAddEntityPacket() called");
        //noinspection unchecked
        return (Packet<ClientGamePacketListener>) NetworkHooks.getEntitySpawningPacket(this);
    }


}