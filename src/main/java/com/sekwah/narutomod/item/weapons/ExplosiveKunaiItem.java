package com.sekwah.narutomod.item.weapons;

import com.sekwah.narutomod.entity.projectile.ExplosiveKunaiEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ExplosiveKunaiItem extends KunaiItem {

    // Cooldown en ticks (10 ticks = 0.5 seconde)
    private static final int COOLDOWN_TICKS = 20*5;

    public ExplosiveKunaiItem(Properties properties) {
        super(properties);
    }

    @Override
    public AbstractArrow createShootingEntity(Level world, Player shooter) {
        return new ExplosiveKunaiEntity(world, shooter);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Applique l'explosion à l'impact
        if (!attacker.level().isClientSide) {
            ExplosiveKunaiEntity.explodeKunai(attacker);
        }

        // Si on n'est pas en créatif, on consomme l'item
        if (!(attacker instanceof Player p && p.getAbilities().instabuild)) {
            stack.shrink(1);
        }

        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // ➊ Bloquer si en cooldown
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        // ➋ Lancer le projectile
        InteractionResultHolder<ItemStack> result = super.use(world, player, hand);

        // ➌ Si efficace, démarrer le cooldown
        if (result.getResult().consumesAction() && !world.isClientSide) {
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        }

        return result;
    }
}