package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerEvents {

    @SubscribeEvent
    public static void onEntityUpdate(LivingEvent.LivingTickEvent event) {
        // Do chakra updates and other stuff here unless handled in capabilities
    }

    // Handle if they have some agility perk or leaps.
    //
    @SubscribeEvent
    public static void livingFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player){
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
                if (!ninjaData.isNinjaModeEnabled()) {
                    return;
                }
                float distance = event.getDistance();
                if(distance < 9){
                    distance *= 0.3f;
                }
                if(distance > 3) {
                    distance -= 5f;
                    distance *= 0.6f;
                }
                event.setDistance(distance);
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        Entity target = event.getTarget();

        if (player.level().isClientSide || !(target instanceof LivingEntity)) return;

        // 🌀 Mets à jour la cible des clones existants
        for (ShadowCloneEntity clone : player.level().getEntitiesOfClass(ShadowCloneEntity.class, player.getBoundingBox().inflate(10))) {
            if (clone.getOwner() == player) {
                clone.updateTarget((LivingEntity) target); // ✅ Change la cible du clone à chaque attaque
            }
        }
    }

}
