package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber
public class BarrierJutsuAbility extends Ability implements Ability.Cooldown {

    private static final Map<BlockPos, Integer> activeCages = new HashMap<>();
    private static final int radius = 12;

    @Override
    public long defaultCombo() {
        return 333;
    }

    @Override
    public int getCooldown() {
        return 3 * 20;
    }

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (ninjaData.getChakra() < 50) return;

        ninjaData.useChakra(50, 20 * 10);
        BlockPos center = serverPlayer.blockPosition();

        createBarrier(serverPlayer);
        activeCages.put(center, 10 * 20); // ✅ Stocke la cage avec un timer de **15 secondes** (10 ticks * 20)
    }

    private void createBarrier(ServerPlayer player) {
        Level level = player.level();
        BlockPos center = player.blockPosition();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double dist = Math.sqrt(x * x + y * y + z * z);

                    // on forme une coquille d'une épaisseur de 1 bloc
                    if (dist <= radius && dist > radius - 1) {
                        BlockPos pos = center.offset(x, y, z);
                        level.setBlock(pos, Blocks.PACKED_ICE.defaultBlockState(), 3);
                    }

                    // on peut aussi mettre un vrai sol plein si tu veux :
                    if (y == -radius && dist <= radius - 1) {
                        BlockPos pos = center.offset(x, y, z);
                        level.setBlock(pos, Blocks.PACKED_ICE.defaultBlockState(), 3);
                    }
                }
            }
        }
    }



    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Map<BlockPos, Integer> updatedTimers = new HashMap<>();

        activeCages.forEach((center, timeLeft) -> {
            if (timeLeft <= 1) {
                Level level = event.getServer().overworld();
                removeBarrier(level, center);
            } else {
                updatedTimers.put(center, timeLeft - 1); // 🔥 Décrémente correctement le timer
            }
        });

        activeCages.clear();
        activeCages.putAll(updatedTimers); // ✅ Met à jour la liste après décrémentation
    }

    private static void removeBarrier(Level level, BlockPos center) {

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double dist = Math.sqrt(x * x + y * y + z * z);

                    // Supprimer la coquille
                    if (dist <= radius && dist > radius - 1) {
                        BlockPos pos = center.offset(x, y, z);
                        if (level.getBlockState(pos).is(Blocks.PACKED_ICE)) {
                            level.removeBlock(pos, false);
                        }
                    }

                    // Supprimer le sol (bas de la sphère)
                    if (y == -radius && dist <= radius - 1) {
                        BlockPos pos = center.offset(x, y, z);
                        if (level.getBlockState(pos).is(Blocks.PACKED_ICE)) {
                            level.removeBlock(pos, false);
                        }
                    }
                }
            }
        }
    }



    private boolean shouldPlaceBars(BlockPos center, BlockPos pos, int radiusX, int radiusZ) {
        return Math.abs(pos.getX() - center.getX()) == radiusX || Math.abs(pos.getZ() - center.getZ()) == radiusZ;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        int chakraCost = 30;
        if (ninjaData.getChakra() < chakraCost) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(chakraCost, 200);
        return true;
    }
}