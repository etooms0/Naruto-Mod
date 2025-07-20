package com.sekwah.narutomod.capabilities;

import com.sekwah.narutomod.jutsu.JutsuData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JutsuSlotData {
    public static final int MAX_POINTS = 5;
    private final List<JutsuData> equipped = new ArrayList<>();

    public List<JutsuData> getEquippedJutsus() {
        return Collections.unmodifiableList(equipped);
    }

    public int getTotalCost() {
        return equipped.stream()
                .mapToInt(JutsuData::getPointCost)
                .sum();
    }

    public boolean canEquip(JutsuData j) {
        return getTotalCost() + j.getPointCost() <= MAX_POINTS;
    }

    public boolean equip(JutsuData j) {
        if (j != null && canEquip(j) && !equipped.contains(j)) {
            equipped.add(j);
            return true;
        }
        return false;
    }


    public boolean isEquipped(String jutsuId) {
        return equipped.stream()
                .anyMatch(j -> j.getId().equals(jutsuId));
    }

    public boolean remove(JutsuData j) {
        return equipped.remove(j);
    }

    public void clear() {
        equipped.clear();
    }
}