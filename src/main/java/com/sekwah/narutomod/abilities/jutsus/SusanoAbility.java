package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.entity.SusanoEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class SusanoAbility extends Ability implements Ability.Cooldown {

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    // Spécifie la combinaisons à effectuer pour activer le jutsu.
    @Override
    public long defaultCombo() {
        return 3322; // Par exemple, à moduler selon votre système.
    }

    // Vérifie le coût en chakra – ici 80 points.
    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (ninjaData.getChakra() < 80) {
            player.displayClientMessage(
                    Component.translatable("jutsu.fail.notenoughchakra",
                            Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)),
                    true
            );
            return false;
        }
        ninjaData.useChakra(80, 80);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        // Durée d'invocation du Susano : 1 minute = 60 * 20 ticks = 1200 ticks.
        final int duration = 60 * 20;

        // Crée l'entité Susano, qui sera notre exosquelette autour du joueur.
        SusanoEntity susano = new SusanoEntity(NarutoEntities.SUSANO.get(), player.level());
        susano.setOwner(player);
        // Positionne-le exactement au même endroit que le joueur.
        susano.setPos(player.getX(), player.getY(), player.getZ());
        player.level().addFreshEntity(susano);

        // (Optionnel) On peut ajouter ici des effets visuels ou sonores d'invocation.

        // Planifie la suppression du Susano après la durée (1 min)
        ninjaData.scheduleDelayedTickEvent((delayedPlayer) -> {
            susano.discard();
        }, duration);
    }

    // Cooldown de l'ability – ici 1 minute (1200 ticks) ; adaptez-le si besoin.
    @Override
    public int getCooldown() {
        return 60 * 20;
    }
}