package com.sekwah.narutomod.entity;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

public class IchirakuEntity extends PathfinderMob {

    public IchirakuEntity(EntityType<? extends IchirakuEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired(); // ne se désoriente pas quand on s'éloigne
    }

    /** Attributs de vie/vitesse par défaut */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    /** Utilise une navigation “amphibie” pour pouvoir marcher/nager */
    @Override
    protected PathNavigation createNavigation(Level world) {
        return new AmphibiousPathNavigation(this, world);
    }

    @Override
    protected void registerGoals() {
        // 0 = priorité la plus haute : remonter à la surface si plongé
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // promenade aléatoire
        this.goalSelector.addGoal(1, new RandomStrollGoal(this, 0.6D));
        // regarder le joueur à proximité
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
        // regard aléatoire
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    /** Assure la synchro client-serveur au spawn */
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}