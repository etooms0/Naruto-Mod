package com.sekwah.narutomod.client.renderer.entity;

import com.sekwah.narutomod.entity.ItachiEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;

public class ItachiRenderer extends HumanoidMobRenderer<ItachiEntity, HumanoidModel<ItachiEntity>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("narutomod", "textures/entity/boss/itachi.png");

    public ItachiRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);

        ModelPart innerArmorPart = context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR);
        ModelPart outerArmorPart = context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR);

        HumanoidModel<ItachiEntity> innerArmorModel = new HumanoidModel<>(innerArmorPart);
        HumanoidModel<ItachiEntity> outerArmorModel = new HumanoidModel<>(outerArmorPart);

        ModelManager modelManager = context.getModelManager();
        this.addLayer(new HumanoidArmorLayer<>(
                this,
                innerArmorModel,
                outerArmorModel,
                modelManager
        ));
    }

    @Override
    public ResourceLocation getTextureLocation(ItachiEntity entity) {
        return TEXTURE;
    }
}
