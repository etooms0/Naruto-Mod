package com.sekwah.narutomod.client.renderer.entity;

import com.sekwah.narutomod.entity.IchirakuEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;

public class IchirakuRenderer extends HumanoidMobRenderer<IchirakuEntity, HumanoidModel<IchirakuEntity>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("narutomod", "textures/entity/friendly/ichiraku.png");

    public IchirakuRenderer(EntityRendererProvider.Context context) {
        super(context,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)),
                0.5f);

        // Bake armor parts
        ModelPart innerArmorPart = context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR);
        ModelPart outerArmorPart = context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR);

        // Create armor models
        HumanoidModel<IchirakuEntity> innerArmorModel = new HumanoidModel<>(innerArmorPart);
        HumanoidModel<IchirakuEntity> outerArmorModel = new HumanoidModel<>(outerArmorPart);

        // Add armor layer
        ModelManager modelManager = context.getModelManager();
        this.addLayer(new HumanoidArmorLayer<>(
                this,
                innerArmorModel,
                outerArmorModel,
                modelManager
        ));
    }

    @Override
    public ResourceLocation getTextureLocation(IchirakuEntity entity) {
        return TEXTURE;
    }
}