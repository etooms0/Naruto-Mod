package com.sekwah.narutomod.client.renderer;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.block.NarutoBlocks;
import com.sekwah.narutomod.client.model.entity.ShadowCloneModel;
import com.sekwah.narutomod.client.model.entity.SubstitutionLogModel;
import com.sekwah.narutomod.client.model.entity.SusanoModel;
import com.sekwah.narutomod.client.model.item.model.*;
import com.sekwah.narutomod.client.model.jutsu.FireballJutsuModel;
import com.sekwah.narutomod.client.model.jutsu.WaterBulletModel;
import com.sekwah.narutomod.client.renderer.entity.*;
import com.sekwah.narutomod.client.renderer.entity.jutsuprojectile.FireballJutsuRenderer;
import com.sekwah.narutomod.client.renderer.entity.jutsuprojectile.WaterBulletJutsuRenderer;
import com.sekwah.narutomod.entity.NarutoEntities;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import com.sekwah.narutomod.client.renderer.entity.DeidaraRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Example of new entity render events
 * https://github.com/MinecraftForge/MinecraftForge/blob/1.17.x/src/test/java/net/minecraftforge/debug/client/rendering/EntityRendererEventsTest.java
 */
@Mod.EventBusSubscriber(value=Dist.CLIENT, modid=NarutoMod.MOD_ID, bus= Mod.EventBusSubscriber.Bus.MOD)
public class NarutoRenderEvents {

    public static final BlockEntityWithoutLevelRenderer NARUTO_RENDERER = new NarutoResourceManager();

    @SubscribeEvent
    public static void entityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(NarutoEntities.KUNAI.get(), KunaiRenderer::new);
        event.registerEntityRenderer(NarutoEntities.EXPLOSIVE_KUNAI.get(), ExplosiveKunaiRenderer::new);
        event.registerEntityRenderer(NarutoEntities.SENBON.get(), SenbonRenderer::new);
        event.registerEntityRenderer(NarutoEntities.SHURIKEN.get(), ShurikenRenderer::new);
        event.registerEntityRenderer(NarutoEntities.PAPER_BOMB.get(), PaperBombRenderer::new);

        event.registerEntityRenderer(NarutoEntities.FIREBALL_JUTSU.get(), FireballJutsuRenderer::new);
        event.registerEntityRenderer(NarutoEntities.WATER_BULLET_JUTSU.get(), WaterBulletJutsuRenderer::new);
        event.registerEntityRenderer(NarutoEntities.SHADOW_CLONE.get(), ShadowCloneRenderer::new);
        event.registerEntityRenderer(NarutoEntities.SUSANO.get(), SusanoRenderer::new);
        event.registerEntityRenderer(NarutoEntities.DEIDARA.get(), DeidaraRenderer::new);
        event.registerEntityRenderer(NarutoEntities.DEIDARA_CLONE.get(), DeidaraRenderer::new);
        event.registerEntityRenderer(NarutoEntities.ICHIRAKU.get(), IchirakuRenderer::new);
        event.registerEntityRenderer(NarutoEntities.ITACHI.get(),  ItachiRenderer::new);
        event.registerEntityRenderer(NarutoEntities.HIDAN.get(),  HidanRenderer::new);
        event.registerEntityRenderer(NarutoEntities.PAIN.get(), PainRenderer::new);

        event.registerEntityRenderer(NarutoEntities.SUBSTITUTION_LOG.get(), SubstitutionLogRenderer::new);

    }

    @SubscribeEvent
    public static void reloadListener(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new NarutoResourceManager());
    }

    @SubscribeEvent
    public static void layerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event)
    {
        // Items
        event.registerLayerDefinition(AnbuMaskModel.LAYER_LOCATION, () -> AnbuMaskModel.createLayer(true));
        event.registerLayerDefinition(AnbuMaskModel.LAYER_LOCATION_WITHOUT_EARS, () -> AnbuMaskModel.createLayer(false));
        event.registerLayerDefinition(HeadbandModel.LAYER_LOCATION, HeadbandModel::createLayer);

        event.registerLayerDefinition(FlakJacketNewModel.LAYER_LOCATION, FlakJacketNewModel::createLayer);
        event.registerLayerDefinition(FlakJacketModel.LAYER_LOCATION, FlakJacketModel::createLayer);
        event.registerLayerDefinition(AnbuArmorModel.LAYER_LOCATION, AnbuArmorModel::createLayer);
        event.registerLayerDefinition(AkatsukiCloakModel.LAYER_LOCATION, AkatsukiCloakModel::createLayer);

        // Jutsu
        event.registerLayerDefinition(FireballJutsuModel.LAYER_LOCATION, FireballJutsuModel::createLayer);
        event.registerLayerDefinition(WaterBulletModel.LAYER_LOCATION, WaterBulletModel::createLayer);


        // Entity
        event.registerLayerDefinition(SubstitutionLogModel.LAYER_LOCATION, SubstitutionLogModel::createBodyLayer);
        event.registerLayerDefinition(ShadowCloneModel.LAYER_LOCATION, ShadowCloneModel::createBodyLayer);

    }

/*    @SubscribeEvent
    public static void entityLayers(EntityRenderersEvent.AddLayers event)
    {
        LivingEntityRenderer<Player, ? extends EntityModel<Player>> renderer = event.getRenderer(EntityType.PLAYER);
    }*/


}
