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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber
public class EarthWallAbility extends Ability implements Ability.Cooldown {

    private static final List<WallData> activeWalls = new ArrayList<>();

    public static class WallData {
        public final BlockPos origin;
        public final Level level;
        public final long startTime;
        public final Vec3 rightVec;
        public final Map<Integer, List<BlockPos>> blocksPerLayer = new HashMap<>();
        public int currentHeight = 0;

        public WallData(BlockPos origin, Level level, long startTime, Vec3 rightVec) {
            this.origin = origin;
            this.level = level;
            this.startTime = startTime;
            this.rightVec = rightVec;
        }
    }

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        Vec3 look = player.getLookAngle().normalize();
        Vec3 right = new Vec3(-look.z, 0, look.x).normalize();

        Vec3 wallCenter = player.position().add(player.getLookAngle().normalize().scale(4));
        BlockPos basePos = getSurface(player.level(), wallCenter);



        WallData wall = new WallData(basePos, player.level(), player.level().getGameTime(), right);
        activeWalls.add(wall);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Iterator<WallData> iterator = activeWalls.iterator();
        while (iterator.hasNext()) {
            WallData wall = iterator.next();
            long elapsed = wall.level.getGameTime() - wall.startTime;

            int width = 5;
            int maxHeight = 7;

            // Phase d'apparition (une couche par tick)
            if (elapsed < maxHeight) {
                int y = wall.currentHeight;

                List<BlockPos> layer = new ArrayList<>();

                for (int i = -width / 2; i <= width / 2; i++) {
                    Vec3 offset = wall.rightVec.scale(i);
                    double dx = wall.origin.getX() + offset.x;
                    double dz = wall.origin.getZ() + offset.z;
                    BlockPos pos = new BlockPos((int) Math.round(dx), wall.origin.getY() + y, (int) Math.round(dz));

                    wall.level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
                    layer.add(pos);
                }

                wall.blocksPerLayer.put(y, layer);
                wall.currentHeight++;
            }

            // Phase de disparition (une couche par tick)
            if (elapsed >= 100 && elapsed < 100 + maxHeight) {
                int yToRemove = maxHeight - (int) (elapsed - 100);
                List<BlockPos> layer = wall.blocksPerLayer.get(yToRemove - 1);
                if (layer != null) {
                    for (BlockPos pos : layer) {
                        if (wall.level.getBlockState(pos).is(Blocks.DIRT)) {
                            wall.level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        }
                    }
                    wall.blocksPerLayer.remove(yToRemove - 1);
                }
            }

            // Fin du jutsu, suppression finale de tous les blocs restants
            if (elapsed >= 100 + maxHeight) {
                for (List<BlockPos> layer : wall.blocksPerLayer.values()) {
                    for (BlockPos pos : layer) {
                        if (wall.level.getBlockState(pos).is(Blocks.DIRT)) {
                            wall.level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        }
                    }
                }
                iterator.remove();
            }
        }
    }

    private static BlockPos getSurface(Level level, Vec3 start) {
        // Toujours commencer la détection 2 blocs AU-DESSUS du joueur (évite les pentes, trous, etc.)
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(start.x, start.y + 2, start.z);
        int maxSearch = 20;

        for (int i = 0; i < maxSearch; i++) {
            if (!level.getBlockState(pos).isAir()) {
                return pos.above(); // On pose le mur juste au-dessus du premier bloc solide
            }
            pos.move(0, -1, 0);
        }

        return BlockPos.containing(start); // fallback si on trouve rien
    }


    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        int chakraCost = 20;
        if (ninjaData.getChakra() < chakraCost) {
            player.displayClientMessage(Component.literal("Not enough chakra!").withStyle(ChatFormatting.RED), true);
            return false;
        }
        ninjaData.useChakra(chakraCost, 200);
        return true;
    }

    @Override
    public int getCooldown() {
        return 5 * 20;
    }

    @Override
    public long defaultCombo() {
        return 3112;
    }

    @Override
    public int getPointCost() {
        return 1;
    }
}
