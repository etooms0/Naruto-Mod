package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.entity.SusanoEntity;
import com.sekwah.narutomod.registries.NarutoRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class SusanoAbility extends Ability implements Ability.Cooldown {

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public int getPointCost() {
        return 100;
    }

    @Override
    public long defaultCombo() {
        return 3322L;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        // get ID
        String jutsuId = NarutoRegistries.ABILITIES
                .getResourceKey(this)
                .map(r -> r.location().getPath())
                .orElse("");

        // Verify if jutsu in deck
        if (!ninjaData.getSlotData().isEquipped(jutsuId)) {
            player.displayClientMessage(
                    Component.literal("This jutsu is not in your deck"),
                    true
            );
            return false;
        }
        if (ninjaData.getChakra() < 80) {
            player.displayClientMessage(
                    Component.translatable(
                            "jutsu.fail.notenoughchakra",
                            Component.translatable(this.getTranslationKey(ninjaData))
                                    .withStyle(ChatFormatting.YELLOW)
                    ),
                    true
            );
            return false;
        }
        ninjaData.useChakra(80, 60);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        // … tes logs, cost, etc.

        SusanoEntity susano = new SusanoEntity(NarutoEntities.SUSANO.get(), player.level());
        susano.setOwner(player);
        susano.startRiding(player, true);

        // 1) Position
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        susano.setPos(x, y, z);
        // 2) Copie de la rotation du regard
        float yaw   = player.getYRot();  // gauche/droite
        float pitch = player.getXRot();  // haut/bas
        susano.setYRot(yaw);
        susano.setXRot(pitch);
        // 3) Pour que le renderer oriente TOUT le corps
        susano.yBodyRot = yaw;           // rotation du buste
        susano.yHeadRot = player.yHeadRot; // rotation de la tête (identique ici)
        // 4) Maintenant on spawn
        player.level().addFreshEntity(susano);

        // 5) Disparition auto
        ninjaData.scheduleDelayedTickEvent(delayed -> susano.discard(), 60 * 20);
    }

    @Override
    public int getCooldown() {
        return 5 * 60 * 20;
    }
}