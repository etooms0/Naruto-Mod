package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sekwah.narutomod.client.model.entity.ShadowCloneModel;
import com.sekwah.narutomod.client.model.entity.SubstitutionLogModel;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import com.sekwah.narutomod.entity.SubstitutionLogEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;

import static com.sekwah.narutomod.entity.NarutoEntities.SHADOW_CLONE;

public class ShadowCloneRenderer extends LivingEntityRenderer<ShadowCloneEntity, ShadowCloneModel<ShadowCloneEntity>>{

    public ShadowCloneRenderer(EntityRendererProvider.Context manager) {
        super(manager, new ShadowCloneModel<>(manager.bakeLayer(ShadowCloneModel.LAYER_LOCATION)), 0.5F);
    }

    public void render(ShadowCloneEntity p_115976_, float p_115977_, float p_115978_, PoseStack p_115979_, MultiBufferSource p_115980_, int p_115981_) {
        this.shadowRadius = 0.3F;
        super.render(p_115976_, p_115977_, p_115978_, p_115979_, p_115980_, p_115981_);
    }

    @Override
    public ResourceLocation getTextureLocation(ShadowCloneEntity entity) {
        if (entity.getOwner() != null) {
            return Minecraft.getInstance().getSkinManager().getInsecureSkinLocation(entity.getOwner().getGameProfile());
        }
        return new ResourceLocation("minecraft", "textures/entity/player/slim/steve.png"); // ✅ Chemin mis à jour !
    }

}
