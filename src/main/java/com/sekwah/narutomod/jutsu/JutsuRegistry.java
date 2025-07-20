package com.sekwah.narutomod.jutsu;

import net.minecraft.network.chat.Component;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class JutsuRegistry {
    private static final Map<String, JutsuData> MAP = new HashMap<>();

    public static void register(String id, int cost, Component name) {
        MAP.put(id, new JutsuData(id, cost, name));
    }

    public static JutsuData getById(String id) {
        return MAP.get(id);
    }

    public static Collection<JutsuData> getAll() {
        return MAP.values();
    }

    /** Appelée en commonSetup pour remplir la table. */
    public static void registerDefaults() {
        register("fireball",       2, Component.translatable("jutsu.fireball"));
        register("shadow_clone",   2, Component.translatable("jutsu.shadow_clone"));
        register("multiple_shadow_clone", 3, Component.translatable("jutsu.multiple_shadow_clone"));
        register("earth_sphere",   3, Component.translatable("jutsu.earth_sphere"));
        register("leap",           1, Component.translatable("jutsu.leap"));
        // ... ajoute tous tes jutsus ici
    }
}