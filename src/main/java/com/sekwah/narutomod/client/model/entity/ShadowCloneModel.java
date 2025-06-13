package com.sekwah.narutomod.client.model.entity;

import com.sekwah.narutomod.entity.ShadowCloneEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import com.sekwah.narutomod.anims.CloneAnimHandler;
import com.sekwah.narutomod.NarutoMod;
import net.minecraft.world.entity.LivingEntity;

public class ShadowCloneModel<T extends LivingEntity> extends HumanoidModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(NarutoMod.MOD_ID, "shadow_clone"), "main");

    public ShadowCloneModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        return LayerDefinition.create(meshDefinition, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        if (entity instanceof ShadowCloneEntity clone && clone.isSprinting()) {
            CloneAnimHandler.applySprintingAnim(clone, this); // ✅ Anime le modèle des clones !
        }
        if (entity instanceof ShadowCloneEntity clone && !clone.isSprinting()) {
            CloneAnimHandler.resetAnim(clone, this); // ✅ Anime le modèle des clones !
        }

    }

}