package com.sekwah.narutomod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.sekwah.narutomod.capabilities.JutsuSlotData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.jutsu.JutsuData;
import com.sekwah.narutomod.jutsu.JutsuRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class JutsuDeckCommand {

    // Limite de points pour le deck
    private static final int MAX_POINTS = 5;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("jutsudeck")

                        // /jutsudeck list
                        .then(Commands.literal("list")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(cap -> {
                                        JutsuSlotData deck = cap.getSlotData();
                                        ctx.getSource().sendSuccess(
                                                () -> Component.literal("Deck (" + deck.getTotalCost() + "/" + MAX_POINTS + ")"),
                                                false
                                        );
                                        deck.getEquippedJutsus().forEach(j -> {
                                            ctx.getSource().sendSuccess(
                                                    () -> Component.literal("• ")
                                                            .append(j.getDisplayName())
                                                            .append(Component.literal(" (" + j.getPointCost() + ")")),
                                                    false
                                            );
                                        });
                                    });
                                    return 1;
                                })
                        )

                        // /jutsudeck add <id>
                        .then(Commands.literal("add")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String id = StringArgumentType.getString(ctx, "id");
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();

                                            JutsuData data = JutsuRegistry.getById(id);
                                            if (data == null) {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("Jutsu inconnu : " + id)
                                                );
                                                return 0;
                                            }

                                            return player.getCapability(NinjaCapabilityHandler.NINJA_DATA).map(cap -> {
                                                if (cap.getSlotData().equip(data)) {
                                                    ctx.getSource().sendSuccess(
                                                            () -> Component.literal("Ajouté : ")
                                                                    .append(data.getDisplayName()),
                                                            false
                                                    );
                                                    return 1;
                                                } else {
                                                    ctx.getSource().sendFailure(
                                                            Component.literal("Impossible d’équiper : ")
                                                                    .append(data.getDisplayName())
                                                    );
                                                    return 0;
                                                }
                                            }).orElse(0);
                                        })
                                )
                        )

                        // /jutsudeck remove <id>
                        .then(Commands.literal("remove")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String id = StringArgumentType.getString(ctx, "id");
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();

                                            JutsuData data = JutsuRegistry.getById(id);
                                            if (data == null) {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("Jutsu inconnu : " + id)
                                                );
                                                return 0;
                                            }

                                            return player.getCapability(NinjaCapabilityHandler.NINJA_DATA).map(cap -> {
                                                if (cap.getSlotData().remove(data)) {
                                                    ctx.getSource().sendSuccess(
                                                            () -> Component.literal("Retiré : ")
                                                                    .append(data.getDisplayName()),
                                                            false
                                                    );
                                                    return 1;
                                                } else {
                                                    ctx.getSource().sendFailure(
                                                            Component.literal("")
                                                                    .append(data.getDisplayName())
                                                                    .append(Component.literal(" n’était pas équipé"))
                                                    );
                                                    return 0;
                                                }
                                            }).orElse(0);
                                        })
                                )
                        )

                        // /jutsudeck clear
                        .then(Commands.literal("clear")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(cap -> {
                                        cap.getSlotData().clear();
                                        ctx.getSource().sendSuccess(
                                                () -> Component.literal("Deck vidé"),
                                                false
                                        );
                                    });
                                    return 1;
                                })
                        )

                        // /jutsudeck points
                        .then(Commands.literal("points")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(cap -> {
                                        int remaining = MAX_POINTS - cap.getSlotData().getTotalCost();
                                        ctx.getSource().sendSuccess(
                                                () -> Component.literal(remaining + "/" + MAX_POINTS),
                                                false
                                        );
                                    });
                                    return 1;
                                })
                        )

                        // /jutsudeck all
                        .then(Commands.literal("all")
                                .executes(ctx -> {
                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal("Tous les jutsus disponibles :"),
                                            false
                                    );
                                    for (JutsuData j : JutsuRegistry.getAll()) {
                                        ctx.getSource().sendSuccess(
                                                () -> Component.literal("• ")
                                                        .append(Component.literal(j.getId()))
                                                        .append(Component.literal(" (" + j.getPointCost() + " pts)")),
                                                false
                                        );
                                    }
                                    return 1;
                                })
                        )
        );
    }
}