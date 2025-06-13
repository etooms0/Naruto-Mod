package com.sekwah.narutomod.anims;

import net.minecraft.world.entity.LivingEntity;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import net.minecraft.client.model.HumanoidModel;

public class CloneAnimHandler {

    public static <T extends HumanoidModel<?>> void applySprintingAnim(ShadowCloneEntity clone, T model) {
            // Animation sprint Naruto
            model.rightArm.setRotation(1.412787F, 0F, 0F);
            model.rightArm.setPos(-5F, 3.933333F, -5F);

            model.leftArm.setRotation(1.412787F, 0F, 0F);
            model.leftArm.setPos(5F, 3.266667F, -5F);

            model.head.xRot = 0F;
            // Pour l'animation sprint, la tête est légèrement avancée...
            model.head.setPos(0F, 3.133333F - 1F, -6F);

            model.body.setRotation(0.5435722F, 0F, 0F);
            // ...et le torse est déplacé en avant
            model.body.setPos(0F, 3F - 1F, -5.5F);
    }

    public static <T extends HumanoidModel<?>> void resetAnim(ShadowCloneEntity clone, T model){
            // Réinitialisation complète lorsque le clone n'est plus en sprint
            // Ces valeurs correspondent aux positions/rotations par défaut.
            model.rightArm.setRotation(0F, 0F, 0F);
            model.rightArm.setPos(-5F, 2F, 0F);

            model.leftArm.setRotation(0F, 0F, 0F);
            model.leftArm.setPos(5F, 2F, 0F);

            model.head.setRotation(0F, 0F, 0F);
            model.head.setPos(0F, 2F, 0F);

            model.body.setRotation(0F, 0F, 0F);
            model.body.setPos(0F, 0F, 0F);
    }

}