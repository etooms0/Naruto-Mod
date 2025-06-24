package com.sekwah.narutomod.client.renderer.entity;

import com.sekwah.narutomod.entity.DeidaraEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class DeidaraRenderer extends HumanoidMobRenderer<DeidaraEntity, HumanoidModel<DeidaraEntity>> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            "narutomod", "textures/entity/boss/deidara.png");

    public DeidaraRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(DeidaraEntity entity) {
        return TEXTURE;
    }
}