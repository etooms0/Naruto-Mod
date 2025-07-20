package com.sekwah.narutomod.jutsu;

import net.minecraft.network.chat.Component;

public class JutsuData {
    private final String id;
    private final int pointCost;
    private final Component displayName;

    public JutsuData(String id, int pointCost, Component displayName) {
        this.id = id;
        this.pointCost = pointCost;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public int getPointCost() {
        return pointCost;
    }

    public Component getDisplayName() {
        return displayName;
    }
}