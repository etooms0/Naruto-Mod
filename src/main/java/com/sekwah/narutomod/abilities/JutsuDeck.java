package com.sekwah.narutomod.abilities;

import com.sekwah.narutomod.registries.NarutoRegistries;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class JutsuDeck {

    public boolean canDoJutsu(Ability ability) {
        ResourceLocation abilityKey = NarutoRegistries.ABILITIES.getResourceKey(ability)
                .map(key -> key.location())
                .orElse(null);
        if (abilityKey == null) return false;

        for (Ability selected : selectedAbilities) {
            ResourceLocation selectedKey = NarutoRegistries.ABILITIES.getResourceKey(selected)
                    .map(key -> key.location())
                    .orElse(null);
            if (abilityKey.equals(selectedKey)) {
                return true;
            }
        }
        return false;
    }


    public void setAbilities(List<Ability> abilities) {
        selectedAbilities.clear();
        selectedAbilities.addAll(abilities);
    }

    private final List<Ability> selectedAbilities = new ArrayList<>();

    public void addAbility(Ability ability) {
        selectedAbilities.add(ability);
    }

    public void removeAbility(Ability ability) {
        selectedAbilities.remove(ability);
    }

    public List<Ability> getAbilities() {
        return selectedAbilities;
    }

    public int getTotalWeight() {
        int total = 0;
        for (Ability ability : selectedAbilities) {
            total += ability.getWeight(); // getWeight() est implémenté dans chaque jutsu
        }
        return total;
    }

    public boolean canAddAbility(Ability ability, int maxPoints) {
        return getTotalWeight() + ability.getWeight() <= maxPoints;
    }

    // 🔽 SERIALIZE deck -> NBT
    public ListTag serializeNBT() {
        ListTag list = new ListTag();
        for (Ability ability : selectedAbilities) {
            NarutoRegistries.ABILITIES.getResourceKey(ability).ifPresent(
                    key -> list.add(StringTag.valueOf(key.location().toString()))
            );
        }
        return list;
    }

    // 🔽 DESERIALIZE deck <- NBT
    public void deserializeNBT(ListTag list) {
        selectedAbilities.clear();
        for (Tag tag : list) {
            if (tag instanceof StringTag stringTag) {
                ResourceLocation location = new ResourceLocation(stringTag.getAsString());
                Ability ability = NarutoRegistries.ABILITIES.getValue(location);
                if (ability != null) {
                    selectedAbilities.add(ability);
                }
            }
        }
    }
}
