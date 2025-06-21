package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.entity.SusanoEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class SusanoAbility extends Ability implements Ability.Cooldown {

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 3322L;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
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
        ninjaData.useChakra(80, 80);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        System.out.println("[SusanoAbility] --> performServer START");
        System.out.println("  player = " + player.getName().getString()
                + " / chakra = " + ninjaData.getChakra());

        // instanciation
        SusanoEntity susano = new SusanoEntity(NarutoEntities.SUSANO.get(), player.level());
        System.out.println("[SusanoAbility] new SusanoEntity created: " + susano);

        susano.setOwner(player);
        System.out.println("[SusanoAbility] owner set on Susano");

        susano.setPos(player.getX(), player.getY(), player.getZ());
        System.out.println("[SusanoAbility] position set to " + player.blockPosition());

        player.level().addFreshEntity(susano);
        System.out.println("[SusanoAbility] addFreshEntity CALLED");

        System.out.println("[SusanoAbility] scheduling discard in 1200 ticks");
        ninjaData.scheduleDelayedTickEvent(delayed -> {
            System.out.println("[SusanoAbility] discard callback fired");
            susano.discard();
        }, 60 * 20);

        System.out.println("[SusanoAbility] --> performServer END");
    }

    @Override
    public int getCooldown() {
        return 60 * 20;
    }
}