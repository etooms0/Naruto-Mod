package com.sekwah.narutomod.client;

import com.sekwah.narutomod.abilities.JutsuDeck;

public class ClientNinjaData {

    private static final JutsuDeck jutsuDeck = new JutsuDeck();

    public static JutsuDeck getJutsuDeck() {
        return jutsuDeck;
    }

    public static void reset() {
        jutsuDeck.setAbilities(new java.util.ArrayList<>());
    }
}
