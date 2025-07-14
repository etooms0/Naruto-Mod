package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sekwah.narutomod.client.model.entity.ShadowCloneModel;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class ShadowCloneRenderer extends LivingEntityRenderer<ShadowCloneEntity, ShadowCloneModel<ShadowCloneEntity>> {

    public ShadowCloneRenderer(EntityRendererProvider.Context manager) {
        super(manager, new ShadowCloneModel<>(manager.bakeLayer(ShadowCloneModel.LAYER_LOCATION)), 0.5F);
        // Ajout du layer d'armure standard pour une gestion correcte des pièces d'armure
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidArmorModel<>(manager.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(manager.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                manager.getModelManager()));
        this.addLayer(new net.minecraft.client.renderer.entity.layers.ItemInHandLayer<>(this, manager.getItemInHandRenderer()));

    }

    @Override
    public void render(ShadowCloneEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        this.shadowRadius = 0.3F;
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }


    @Override
    public ResourceLocation getTextureLocation(ShadowCloneEntity entity) {
        if (entity.getGameProfile() != null) {
            ResourceLocation skin = Minecraft.getInstance().getSkinManager().getInsecureSkinLocation(entity.getGameProfile());
            return skin;
        }
        return new ResourceLocation("minecraft", "textures/entity/player/slim/steve.png");
    }


    @Override
    protected void scale(ShadowCloneEntity entity, PoseStack poseStack, float partialTicks) {
        super.scale(entity, poseStack, partialTicks);
        poseStack.scale(1.0F, 1.0F, 1.0F);
    }

    @Override
    protected void renderNameTag(ShadowCloneEntity entity, Component component, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.renderNameTag(entity, component, poseStack, buffer, packedLight);
    }

    @Override
    protected void setupRotations(ShadowCloneEntity entity, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTicks) {
        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTicks);
        if (entity.isAggressive()) {
            poseStack.translate(0.0D, 0.2D, 0.0D);
        }
    }
}