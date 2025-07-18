package com.sekwah.narutomod.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.c2s.ServerUpdateJutsuDeckPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class JutsuDeckScreen extends Screen {

    private Checkbox waterBulletCheckbox;
    private final int imageWidth = 176;
    private final int imageHeight = 166;

    public JutsuDeckScreen() {
        super(Component.literal("Jutsu Deck"));
    }

    @Override
    protected void init() {
        int centerX = (this.width - imageWidth) / 2;
        int centerY = (this.height - imageHeight) / 2;

        waterBulletCheckbox = new Checkbox(centerX + 10, centerY + 20, 20, 20, Component.literal("Water Bullet"), false);
        this.addRenderableWidget(waterBulletCheckbox);

        this.addRenderableWidget(Button.builder(Component.literal("Done"), (btn) -> {
            if (waterBulletCheckbox.selected()) {
                ResourceLocation waterBulletId = new ResourceLocation("narutomod", "water_bullet");
                PacketHandler.NARUTO_CHANNEL.sendToServer(new ServerUpdateJutsuDeckPacket(waterBulletId, true));
            }
            this.minecraft.setScreen(null);
        }).pos(centerX + 50, centerY + 100).size(80, 20).build());


    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
