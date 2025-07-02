package com.sekwah.narutomod.menu;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;

public class IchirakuTradeMenu extends AbstractContainerMenu {

    private final Merchant merchant;
    private final Inventory playerInventory;
    private final Level level;
    private int currentOfferIndex = 0;



    public IchirakuTradeMenu(int windowId, Inventory playerInventory, Merchant merchant, Level level) {
        super(null, windowId); // Tu peux mettre ton type de menu ici plus tard
        this.merchant = merchant;
        this.playerInventory = playerInventory;
        this.level = level;

        // Slots trade : 2 entrées + 1 sortie

        // Input 1 (ex: pain)
        this.addSlot(new Slot(new SimpleTradeInventory(merchant, 0), 0, 36, 17));

        // Input 2 (si trade à deux ingrédients)
        this.addSlot(new Slot(new SimpleTradeInventory(merchant, 1), 1, 62, 17));

        // Output (résultat)
        this.addSlot(new Slot(new SimpleTradeInventory(merchant, 2), 2, 120, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // interdit de poser dans le slot résultat
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                if (merchant != null) {
                    MerchantOffers offers = merchant.getOffers();
                    if (offers != null && currentOfferIndex < offers.size()) {
                        MerchantOffer offer = offers.get(currentOfferIndex);

                        // Retirer costA
                        player.getInventory().removeItem(offer.getCostA());

                        // Retirer costB si présent
                        if (!offer.getCostB().isEmpty()) {
                            player.getInventory().removeItem(offer.getCostB());
                        }

                        // Ici tu peux ajouter effets sonores, particules, etc.
                    }
                }
                super.onTake(player, stack);
            }
        });

        // Inventaire joueur (hotbar + inventaire)
        int startX = 8;
        int startY = 84;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, startX + col * 18, startY + row * 18));
            }
        }
        for (int hotbar = 0; hotbar < 9; hotbar++) {
            this.addSlot(new Slot(playerInventory, hotbar, startX + hotbar * 18, startY + 58));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true; // tu peux faire une vérif de distance si tu veux
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            ItemStack copyStack = originalStack.copy();

            if (index < 3) { // slots trade
                if (!this.moveItemStackTo(originalStack, 3, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else { // inventaire joueur vers trade
                if (!this.moveItemStackTo(originalStack, 0, 2, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (originalStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            return copyStack;
        }
        return ItemStack.EMPTY;
    }

    private static class SimpleTradeInventory implements net.minecraft.world.Container {

        private final Merchant merchant;
        private final int slotIndex;

        public SimpleTradeInventory(Merchant merchant, int slotIndex) {
            this.merchant = merchant;
            this.slotIndex = slotIndex;
        }

        @Override
        public int getContainerSize() {
            return 3;
        }

        @Override
        public boolean isEmpty() {
            MerchantOffers offers = merchant.getOffers();
            if (offers == null || offers.isEmpty()) return true;
            MerchantOffer offer = offers.get(0);
            return offer.getCostA().isEmpty() && offer.getCostB().isEmpty() && offer.getResult().isEmpty();
        }

        @Override
        public ItemStack getItem(int index) {
            MerchantOffers offers = merchant.getOffers();
            if (offers == null || offers.isEmpty()) return ItemStack.EMPTY;
            MerchantOffer offer = offers.get(0);

            return switch (index) {
                case 0 -> offer.getCostA();
                case 1 -> offer.getCostB();
                case 2 -> offer.getResult();
                default -> ItemStack.EMPTY;
            };
        }

        @Override
        public ItemStack removeItem(int index, int count) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItemNoUpdate(int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public void setItem(int index, ItemStack stack) {}

        @Override
        public void setChanged() {}

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {}

    }
}
