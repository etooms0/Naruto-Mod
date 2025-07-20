package com.sekwah.narutomod.jutsu;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import net.minecraft.network.chat.Component;
import net.minecraftforge.registries.RegistryObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Stocke les métadonnées (id + coût + displayName) de chaque jutsu,
 * en se basant sur la DeferredRegister de NarutoAbilities.
 */
public class JutsuRegistry {

    private static final Map<String, JutsuData> MAP = new HashMap<>();

    /** Enregistre un JutsuData sous l’ID donné. */
    public static void register(String id, int cost, Component displayName) {
        MAP.put(id, new JutsuData(id, cost, displayName));
    }

    /** Récupère un JutsuData par son identifiant. */
    public static JutsuData getById(String id) {
        return MAP.get(id);
    }

    /** Renvoie tous les JutsuData enregistrés. */
    public static Collection<JutsuData> getAll() {
        return MAP.values();
    }

    /**
     * À appeler une seule fois en commonSetup pour remplir le registre.
     */
    public static void registerDefaults() {
        // Pour chaque RegistryObject<Ability> enregistré dans NarutoAbilities.ABILITY
        for (RegistryObject<Ability> entry : NarutoAbilities.ABILITY.getEntries()) {
            // 1) Extraire l'id (le "path" du ResourceLocation), ex "fireball"
            String id = entry.getId().getPath();

            // 2) Récupérer l'instance d'Ability et son coût
            Ability ability = entry.get();
            int cost = ability.getPointCost();

            // 3) Construire un Component traduit "jutsu.<id>"
            Component name = Component.translatable("jutsu." + id);

            // 4) Enregistrer
            register(id, cost, name);
        }
    }
}