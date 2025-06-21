package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sekwah.narutomod.client.model.entity.SusanoModel;
import com.sekwah.narutomod.entity.SusanoEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SusanoRenderer extends GeoEntityRenderer<SusanoEntity> {

    @Override
    public void render(SusanoEntity entity, float yaw, float pt, PoseStack ms,
                       MultiBufferSource buffer, int light) {
        System.out.println("[SusanoRenderer] render() called for " + entity + " @ " + entity.blockPosition());
        super.render(entity, yaw, pt, ms, buffer, light);
    }

    public SusanoRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SusanoModel());
        this.shadowRadius = 0f;  // pas d'ombre projetée
    }
}