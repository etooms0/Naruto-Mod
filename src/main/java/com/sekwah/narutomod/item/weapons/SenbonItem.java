package com.sekwah.narutomod.item.weapons;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.sekwah.narutomod.entity.projectile.SenbonEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SenbonItem extends Item {

    // Cooldown en ticks (20 ticks = 1 seconde)
    private static final int COOLDOWN_TICKS = 20*1;

    protected final Multimap<Attribute, AttributeModifier> weaponAttributes;

    public SenbonItem(Properties properties) {
        super(properties);
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Tool modifier", 1.0D, AttributeModifier.Operation.ADDITION)
        );
        builder.put(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Tool modifier", -1.8D, AttributeModifier.Operation.ADDITION)
        );
        this.weaponAttributes = builder.build();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // ➊ Si l'objet est en cooldown, bloquer l'utilisation
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        // ➋ Son de tir
        world.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.ARROW_SHOOT,
                SoundSource.PLAYERS,
                1.0F,
                1.0F / (world.random.nextFloat() * 0.4F + 1.2F) + 0.5F
        );

        // ➌ Serveur : spawn du projectile et démarrage du cooldown
        if (!world.isClientSide) {
            SenbonEntity senbon = new SenbonEntity(world, player);
            senbon.shootFromRotation(
                    player,
                    player.getXRot(),
                    player.getYRot(),
                    0.0F,
                    3.0F,
                    1.0F
            );
            senbon.setBaseDamage(0.5D);
            world.addFreshEntity(senbon);

            // ➍ Appliquer le cooldown (similaire aux Ender Pearls)
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        }

        // ➎ Consommation de l'item si non créatif
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResultHolder.sidedSuccess(stack, world.isClientSide);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(
            EquipmentSlot slot, ItemStack stack
    ) {
        return slot == EquipmentSlot.MAINHAND
                ? this.weaponAttributes
                : super.getAttributeModifiers(slot, stack);
    }
}