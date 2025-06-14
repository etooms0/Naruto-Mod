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
        BlockPos center = player.blockPosition().below(); // 🔥 Abaisse la cage sous le joueur
        int radiusX = 7, radiusZ = 7, height = 10;

        for (int x = -radiusX; x <= radiusX; x++) {
            for (int y = 0; y <= height; y++) {
                for (int z = -radiusZ; z <= radiusZ; z++) {
                    BlockPos pos = center.offset(x, y, z);

                    if (y == 0) { // ✅ Sol en bois sous le joueur, pas à son niveau
                        level.setBlock(pos, Blocks.OAK_PLANKS.defaultBlockState(), 3);
                    } else if (shouldPlaceBars(center, pos, radiusX, radiusZ)) {
                        level.setBlock(pos, Blocks.IRON_BARS.defaultBlockState(), 3);
                    } else if (y == height) {
                        level.setBlock(pos, Blocks.OAK_LOG.defaultBlockState(), 3);
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
        int radiusX = 7, radiusZ = 7, height = 10;

        for (int x = -radiusX; x <= radiusX; x++) {
            for (int y = -1; y <= height; y++) { // 🔥 Ajoute `-1` pour inclure le sol
                for (int z = -radiusZ; z <= radiusZ; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    level.removeBlock(pos, false);
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