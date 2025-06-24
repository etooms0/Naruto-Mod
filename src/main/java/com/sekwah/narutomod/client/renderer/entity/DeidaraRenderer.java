package com.sekwah.narutomod.client.renderer.entity;

import com.sekwah.narutomod.entity.DeidaraEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;

public class DeidaraRenderer extends HumanoidMobRenderer<DeidaraEntity, HumanoidModel<DeidaraEntity>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("narutomod", "textures/entity/boss/deidara.png");

    public DeidaraRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);

        // 1) on bake directement les parties d'armure
        ModelPart innerArmorPart = context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR);
        ModelPart outerArmorPart = context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR);

        // 2) on crée nos modèles d'armure
        HumanoidModel<DeidaraEntity> innerArmorModel = new HumanoidModel<>(innerArmorPart);
        HumanoidModel<DeidaraEntity> outerArmorModel = new HumanoidModel<>(outerArmorPart);

        // 3) on ajoute la couche d'armure en lui passant context.getModelManager()
        ModelManager modelManager = context.getModelManager();
        this.addLayer(new HumanoidArmorLayer<>(
                this,
                innerArmorModel,
                outerArmorModel,
                modelManager
        ));
    }

    @Override
    public ResourceLocation getTextureLocation(DeidaraEntity entity) {
        return TEXTURE;
    }
}