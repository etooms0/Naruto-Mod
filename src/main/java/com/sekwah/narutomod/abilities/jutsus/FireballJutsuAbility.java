package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.ItachiEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.FireballJutsuEntity;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * More of a slight speed boost than an actual dash
 */
public class FireballJutsuAbility extends Ability implements Ability.Cooldown {

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 121;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if(ninjaData.getChakra() < 30) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra", Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(30, 30);
        return true;
    }

    public void performFromEntity(LivingEntity entity) {
        Level level = entity.level();

        double x = entity.getX();
        double y = entity.getEyeY() - 0.2f;
        double z = entity.getZ();

        // Calcul de la direction de la fireball
        float pitch = entity.getXRot();
        float yaw = entity.getYRot();

        // Si Itachi, on ajuste le pitch pour viser plus bas
        if(entity instanceof ItachiEntity) {
            pitch += 10; // vise 15° plus bas
        }

        // Convertir pitch et yaw en vecteur direction
        Vec3 look = Vec3.directionFromRotation(pitch, yaw);

        FireballJutsuEntity fireball = new FireballJutsuEntity(entity, look.x, look.y, look.z);
        fireball.setPos(x, y, z);

        level.addFreshEntity(fireball);
    }


    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
            ninjaData.scheduleDelayedTickEvent((delayedPlayer) -> {
                Vec3 shootSpeed = player.getLookAngle();
                FireballJutsuEntity fireball = new FireballJutsuEntity(player, shootSpeed.x, shootSpeed.y, shootSpeed.z);
                player.level().addFreshEntity(fireball);
                player.level().playSound(null, player, NarutoSounds.FIREBALL_SHOOT.get(), SoundSource.PLAYERS, 1f, 1.0f);
            }, 10);
    }

    @Override
    public int getCooldown() {
        return 3 * 20;
    }
}
