package com.sekwah.narutomod.client.model.entity;

import com.sekwah.narutomod.entity.SusanoEntity;
import net.minecraft.resources.ResourceLocation;
import com.sekwah.narutomod.NarutoMod;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.model.GeoModel;



public class SusanoModel extends GeoModel<SusanoEntity> {
    @Override
    public ResourceLocation getModelResource(SusanoEntity animatable) {
        return new ResourceLocation(NarutoMod.MOD_ID, "geo/susano.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SusanoEntity animatable) {
        return new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/susano.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SusanoEntity animatable) {
        return new ResourceLocation(NarutoMod.MOD_ID, "animations/susano.animation.json");
    }


}