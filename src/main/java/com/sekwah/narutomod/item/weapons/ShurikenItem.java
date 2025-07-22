package com.sekwah.narutomod.item.weapons;

import com.sekwah.narutomod.entity.projectile.ShurikenEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ShurikenItem extends Item {

    // Cooldown en ticks (10 ticks = 0.5 seconde)
    private static final int COOLDOWN_TICKS = 20*1;

    public ShurikenItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // ➊ Si l'objet est en cooldown, on bloque l'utilisation
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        // ➋ Démarre le cooldown
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);

        // ➌ Son de tir
        world.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.ARROW_SHOOT,
                SoundSource.PLAYERS,
                1.0F,
                1.0F / (world.getRandom().nextFloat() * 0.4F + 1.2F) + 0.5F
        );

        // ➍ Création et lancement du projectile
        if (!world.isClientSide) {
            AbstractArrow shuriken = createShootingEntity(world, player);
            shuriken.shootFromRotation(
                    player,
                    player.getXRot(),
                    player.getYRot(),
                    0.0F,
                    3.0F,
                    1.0F
            );
            shuriken.setBaseDamage(2.5D);
            world.addFreshEntity(shuriken);
        }

        // ➎ Consommation de l'item si non créatif
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResultHolder.sidedSuccess(stack, world.isClientSide);
    }

    public AbstractArrow createShootingEntity(Level world, Player player) {
        ShurikenEntity entity = new ShurikenEntity(world, player);
        entity.pickup = player.getAbilities().instabuild
                ? AbstractArrow.Pickup.CREATIVE_ONLY
                : AbstractArrow.Pickup.ALLOWED;
        return entity;
    }
}