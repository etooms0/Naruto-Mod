package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber  // Pour recevoir l'événement de tick serveur
public class FreezeCubeJutsuAbility extends Ability implements Ability.Cooldown {

    // Stocke la position centrale de chaque cube de glace actif et le temps restant (en ticks).
    private static final Map<BlockPos, Integer> activeFreezeCubes = new HashMap<>();

    @Override
    public long defaultCombo() {
        return 1223; // Par exemple, la combinaison de touches pour ce jutsu.
    }

    @Override
    public int getCooldown() {
        return 60 * 20 * 4;
    }

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    /**
     * Détermine l'entité que le joueur (ServerPlayer) regarde, jusqu'à une distance maxDistance.
     */
    private LivingEntity getLookedAtEntity(ServerPlayer player, double maxDistance) {
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getLookAngle().scale(maxDistance);
        Vec3 targetPos = eyePos.add(lookVec);

        for (Entity entity : player.level().getEntities(player, player.getBoundingBox().expandTowards(lookVec).inflate(1.0))) {
            if (entity instanceof LivingEntity livingEntity &&
                    entity != player && // évite soi-même
                    entity.getBoundingBox().intersects(eyePos, targetPos)) {
                return livingEntity;
            }
        }
        return null;
    }


    @Override
    public void performServer(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // Recherche de l'entité regardée par le joueur.
        Entity target = getLookedAtEntity(serverPlayer, 50.0);
        if (target == null) {
            player.displayClientMessage(Component.literal("Aucune entité détectée dans le champ de vision").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (target == player) {
            player.displayClientMessage(Component.literal("Impossible de cibler soi-même").withStyle(ChatFormatting.RED), true);
            return;
        }

        // Création du cube de glace autour de l'entité ciblée.
        createFreezeCube(serverPlayer.level(), target);

        // Stocke la position centrale du cube avec un timer de 5 secondes (5*20 = 100 ticks).
        BlockPos center = target.blockPosition();
        activeFreezeCubes.put(center, 5 * 20);
    }

    /**
     * Crée un cube de glace centré sur l'entité (cube de 5x5x5 blocs, avec un rayon de 2 blocs).
     */
    private void createFreezeCube(Level level, Entity target) {
        BlockPos center = target.blockPosition();
        int radius = 2; // le cube ira de -2 à +2 sur chaque axe (5 blocs de côté)
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    // Si la case est vide, place de la glace. Vous pouvez choisir Blocks.ICE ou Blocks.PACKED_ICE selon l'esthétique souhaitée.
                    if (level.isEmptyBlock(pos)) {
                        level.setBlock(pos, Blocks.ICE.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    /**
     * Supprime le cube de glace en remplaçant les blocs ICE par de l'air.
     */
    private static void removeFreezeCube(Level level, BlockPos center) {
        int radius = 2;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (level.getBlockState(pos).getBlock() == Blocks.ICE) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    /**
     * Cet événement est appelé à chaque tick serveur et décrémente le timer pour chaque cube de glace actif.
     * Lorsqu’un cube atteint 0, il est supprimé.
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Map<BlockPos, Integer> updatedTimers = new HashMap<>();

        for (Map.Entry<BlockPos, Integer> entry : activeFreezeCubes.entrySet()) {
            BlockPos center = entry.getKey();
            int timeLeft = entry.getValue();
            if (timeLeft <= 1) {
                Level level = event.getServer().overworld();  // Suppose que le cube est créé dans l'Overworld
                if (level != null) {
                    removeFreezeCube(level, center);
                }
            } else {
                updatedTimers.put(center, timeLeft - 1);
            }
        }

        activeFreezeCubes.clear();
        activeFreezeCubes.putAll(updatedTimers);
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