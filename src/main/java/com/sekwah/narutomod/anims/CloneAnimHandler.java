package com.sekwah.narutomod.anims;

import net.minecraft.world.entity.LivingEntity;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import net.minecraft.client.model.HumanoidModel;

public class CloneAnimHandler {

    public static <T extends HumanoidModel<?>> void applySprintingAnim(ShadowCloneEntity clone, T model) {
        if (clone.isSprinting()) {
            model.rightArm.xRot = 1.412787F;
            model.rightArm.yRot = 0F;
            model.rightArm.zRot = 0F;
            model.rightArm.setPos(-5F, 3.933333F, -5F);

            model.leftArm.xRot = 1.412787F;
            model.leftArm.yRot = 0F;
            model.leftArm.zRot = 0F;
            model.leftArm.setPos(5F, 3.266667F, -5F);

            model.head.xRot = 0F;
            model.head.setPos(0F, 3.133333F - 1F, -5F - 1F);

            model.body.xRot = 0.5435722F;
            model.body.setPos(0F, 3F - 1F, -3.5F - 2F);
        }
    }
}