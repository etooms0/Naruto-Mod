package com.sekwah.narutomod.entity;

import com.mojang.authlib.GameProfile;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.entity.item.PaperBombEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.FireballJutsuEntity;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.WaterBulletJutsuEntity;
import com.sekwah.narutomod.entity.projectile.ExplosiveKunaiEntity;
import com.sekwah.narutomod.entity.projectile.KunaiEntity;
import com.sekwah.narutomod.entity.projectile.SenbonEntity;
import com.sekwah.narutomod.entity.projectile.ShurikenEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.spongepowered.asm.mixin.Shadow;

import static com.sekwah.narutomod.NarutoMod.MOD_ID;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class NarutoEntities {

    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MOD_ID);

    public static final RegistryObject<EntityType<KunaiEntity>> KUNAI = register("kunai",
            EntityType.Builder.<KunaiEntity>of(KunaiEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).setTrackingRange(8));

    public static final RegistryObject<EntityType<SenbonEntity>> SENBON = register("senbon",
            EntityType.Builder.<SenbonEntity>of(SenbonEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).setTrackingRange(8));

    public static final RegistryObject<EntityType<ExplosiveKunaiEntity>> EXPLOSIVE_KUNAI = register("explosive_kunai",
            EntityType.Builder.<ExplosiveKunaiEntity>of(ExplosiveKunaiEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).setTrackingRange(8));

    public static final RegistryObject<EntityType<ShurikenEntity>> SHURIKEN = register("shuriken",
            EntityType.Builder.<ShurikenEntity>of(ShurikenEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).setTrackingRange(8));

    // func_233608_b_ is updateInterval
    public static final RegistryObject<EntityType<PaperBombEntity>> PAPER_BOMB = register("paper_bomb",
            EntityType.Builder.<PaperBombEntity>of(PaperBombEntity::new, MobCategory.MISC).fireImmune().sized(0.5F, 0.5F).setTrackingRange(10).clientTrackingRange(10));

    public static final RegistryObject<EntityType<ShadowCloneEntity>> SHADOW_CLONE = register("shadow_clone",
            EntityType.Builder.<ShadowCloneEntity>of((type, level) -> new ShadowCloneEntity((EntityType<? extends ShadowCloneEntity>) type, level, new GameProfile(null, "TempClone")), MobCategory.CREATURE)
                    .fireImmune()
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(8));

    public static final RegistryObject<EntityType<IchirakuEntity>> ICHIRAKU =
            ENTITIES.register("ichiraku",
                    () -> EntityType.Builder.of(IchirakuEntity::new, MobCategory.CREATURE)
                            .sized(0.6f, 1.8f)
                            .build("ichiraku"));

    public static final RegistryObject<EntityType<SusanoEntity>> SUSANO = register("susano",
            EntityType.Builder.<SusanoEntity>of(SusanoEntity::new, MobCategory.MISC)
                    .sized(1.5F, 3.5F) // adjust the size as needed
                    .clientTrackingRange(10)
                    .setTrackingRange(10)
                    //.noSummon() // optional: prevents natural spawning
                    .fireImmune()); // optional: if desired


    public static final RegistryObject<EntityType<DeidaraEntity>> DEIDARA = register("deidara",
            EntityType.Builder.<DeidaraEntity>of(DeidaraEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F) // taille humanoïde
                    .clientTrackingRange(10)
                    .setTrackingRange(32)
                    .fireImmune());

    public static final RegistryObject<EntityType<com.sekwah.narutomod.entity.DeidaraCloneEntity>> DEIDARA_CLONE =
            register("deidara_clone",
                    EntityType.Builder.<com.sekwah.narutomod.entity.DeidaraCloneEntity>of(DeidaraCloneEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.8F) // même taille que l'original ou adaptée selon ton besoin
                            .clientTrackingRange(10)
                            .setTrackingRange(32)
                            .fireImmune());

    public static final RegistryObject<EntityType<ItachiEntity>> ITACHI = ENTITIES.register("itachi",
            () -> EntityType.Builder.of(ItachiEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F) // taille humanoïde
                    .clientTrackingRange(10)
                    .setTrackingRange(32)
                    .fireImmune()
                    .build("itachi"));

    public static final RegistryObject<EntityType<HidanEntity>> HIDAN = ENTITIES.register("hidan",
            () -> EntityType.Builder.of(HidanEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F) // taille humanoïde
                    .clientTrackingRange(10)
                    .setTrackingRange(32)
                    .fireImmune()
                    .build("hidan"));

    public static final RegistryObject<EntityType<PainEntity>> PAIN = ENTITIES.register("pain",
            () -> EntityType.Builder.of(PainEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F) // taille humanoïde
                    .clientTrackingRange(10)
                    .setTrackingRange(32)
                    .fireImmune()
                    .build("pain"));

    public static final RegistryObject<EntityType<ObitoCloneEntity>> OBITO_CLONE = ENTITIES.register("obito_clone",
            () -> EntityType.Builder.of(ObitoCloneEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F) // taille humanoïde
                    .clientTrackingRange(10)
                    .setTrackingRange(32)
                    .fireImmune()
                    .build("obito_clone"));


    public static final RegistryObject<EntityType<ObitoEntity>> OBITO = ENTITIES.register("obito",
            () -> EntityType.Builder.of(ObitoEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F) // taille humanoïde
                    .clientTrackingRange(10)
                    .setTrackingRange(32)
                    .fireImmune()
                    .build("obito"));

    public static final RegistryObject<EntityType<FireballJutsuEntity>> FIREBALL_JUTSU = register("fireball_jutsu",
            EntityType.Builder.<FireballJutsuEntity>of(FireballJutsuEntity::new, MobCategory.MISC).sized(1.5F, 1.5F).clientTrackingRange(4).updateInterval(10));

    public static final RegistryObject<EntityType<WaterBulletJutsuEntity>> WATER_BULLET_JUTSU = register("water_bullet_jutsu",
            EntityType.Builder.<WaterBulletJutsuEntity>of(WaterBulletJutsuEntity::new, MobCategory.MISC).fireImmune().sized(0.3F, 0.3F).clientTrackingRange(4).updateInterval(10));


    public static final RegistryObject<EntityType<SubstitutionLogEntity>> SUBSTITUTION_LOG = register("substitution_log",
            EntityType.Builder.<SubstitutionLogEntity>of(SubstitutionLogEntity::new, MobCategory.MISC).fireImmune().sized(0.3F, 0.3F).clientTrackingRange(4));

    private static <T extends Entity> RegistryObject<EntityType<T>> register(String key, EntityType.Builder<T> builder) {
        return ENTITIES.register(key, () -> builder.build(key));
    }

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }

    @SubscribeEvent
    public static void entityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(SUSANO.get(), SusanoEntity.createAttributes().build());
        event.put(SHADOW_CLONE.get(), ShadowCloneEntity.createAttributes().build());
        event.put(SUBSTITUTION_LOG.get(), SubstitutionLogEntity.createAttributes().build());
        event.put(NarutoEntities.DEIDARA.get(), DeidaraEntity.createAttributes().build());
        event.put(NarutoEntities.DEIDARA_CLONE.get(), DeidaraCloneEntity.createAttributes().build());
        event.put(NarutoEntities.ICHIRAKU.get(), IchirakuEntity.createAttributes().build());
        event.put(ITACHI.get(), ItachiEntity.createAttributes().build());
        event.put(HIDAN.get(), HidanEntity.createAttributes().build());
        event.put(PAIN.get(), PainEntity.createAttributes().build());
        event.put(OBITO.get(), ObitoEntity.createAttributes().build());
        event.put(OBITO_CLONE.get(), ObitoCloneEntity.createAttributes().build());
}


}
