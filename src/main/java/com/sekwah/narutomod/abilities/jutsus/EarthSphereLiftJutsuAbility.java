package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Jutsu qui enferme la cible dans une sphère creuse de terre (hollow sphere)
 * et qui l'élève graduellement (l'intégralité de la sphère se déplace) jusqu'à
 * atteindre 50 blocs d'altitude, puis explose.
 */
@Mod.EventBusSubscriber
public class EarthSphereLiftJutsuAbility extends Ability implements Ability.Cooldown {

    // Structure pour stocker les données relatives à une sphère en cours de montée.
    public static class RisingSphereData {
        public final Level level;
        public final BlockPos originalCenter;
        public final int radius;
        public int currentOffset; // décalage vertical actuel (en blocs)
        public int tickCounter;   // pour contrôler la vitesse de montée
        public final int targetOffset; // hauteur cible en blocs

        public RisingSphereData(Level level, BlockPos center, int radius, int targetOffset) {
            this.level = level;
            this.originalCenter = center;
            this.radius = radius;
            this.currentOffset = 0;
            this.tickCounter = 0;
            this.targetOffset = targetOffset;
        }

        // Retourne le centre actuel (original + offset vertical)
        public BlockPos getCurrentCenter() {
            return originalCenter.offset(0, currentOffset, 0);
        }
    }

    // Liste des sphères qui montent actuellement
    private static final List<RisingSphereData> activeRisingSpheres = new ArrayList<>();

    @Override
    public long defaultCombo() {
        return 112233;  // Exemple de combo pour ce jutsu
    }

    @Override
    public int getCooldown() {
        return 15 * 20;  // Par exemple, 15 secondes de cooldown
    }

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    /**
     * Récupère l'entité que le joueur regarde.
     */
    private Entity getLookedAtEntity(ServerPlayer player, double maxDistance) {
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getLookAngle().scale(maxDistance);
        Vec3 targetPos = eyePos.add(lookVec);

        for (Entity entity : player.level().getEntities(player,
                player.getBoundingBox().expandTowards(lookVec).inflate(1.0))) {
            if (entity.getBoundingBox().intersects(eyePos, targetPos)) {
                return entity;
            }
        }
        return null;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!(player instanceof ServerPlayer serverPlayer))
            return;

        // Détection de l'entité visée
        Entity target = getLookedAtEntity(serverPlayer, 50.0);
        if (target == null) {
            player.displayClientMessage(Component.literal("Aucune entit\u00E9 d\u00E9tect\u00E9e").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (target == player) {
            player.displayClientMessage(Component.literal("Impossible de cibler soi-m\u00EAme").withStyle(ChatFormatting.RED), true);
            return;
        }

        Level level = serverPlayer.level();
        int radius = 5; // La coquille aura un diamètre d'environ 11 blocs.
        // Création initiale de la sphère creuse autour de la cible
        createHollowEarthSphere(level, target, radius);

        // Stocker la sphère pour la faire monter : on veut qu'elle atteigne 50 blocs
        BlockPos center = target.blockPosition();
        activeRisingSpheres.add(new RisingSphereData(level, center, radius, 50));
    }

    /**
     * Crée une sphère creuse (coquille) autour de l'entité cible.
     * On place des blocs de terre uniquement sur la surface (où la distance par rapport au centre est > radius - 1 et ≤ radius).
     */
    private void createHollowEarthSphere(Level level, Entity target, int radius) {
        BlockPos center = target.blockPosition();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double distance = Math.sqrt(x * x + y * y + z * z);
                    if (distance <= radius && distance > radius - 1) {
                        BlockPos pos = center.offset(x, y, z);
                        level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    /**
     * Supprime l'intégralité de la coquille de la sphère à partir du centre décalé.
     * On parcourt tous les voxels définissant la coquille (condition sur la distance) et on les remplace par de l'air.
     *
     * @param currentOffset décalage vertical à appliquer au centre d'origine
     */
    private static void removeEntireSphere(Level level, BlockPos center, int radius, int currentOffset) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double distance = Math.sqrt(x * x + y * y + z * z);
                    if (distance <= radius && distance > radius - 1) {
                        BlockPos pos = center.offset(x, y + currentOffset, z);
                        if (level.getBlockState(pos).getBlock() == Blocks.DIRT) {
                            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }
    }

    /**
     * Place l'intégralité de la coquille de la sphère à partir du centre décalé.
     */
    private static void placeEntireSphere(Level level, BlockPos center, int radius, int currentOffset) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double distance = Math.sqrt(x * x + y * y + z * z);
                    if (distance <= radius && distance > radius - 1) {
                        BlockPos pos = center.offset(x, y + currentOffset, z);
                        level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    /**
     * Cet événement est appelé chaque tick serveur.
     * Il fait monter progressivement chaque sphère active.
     * Ici, on fait monter la sphère d'une unité toutes les 2 ticks.
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;

        Iterator<RisingSphereData> it = activeRisingSpheres.iterator();
        while (it.hasNext()) {
            RisingSphereData sphere = it.next();
            sphere.tickCounter++;
            if (sphere.tickCounter >= 2) { // montée plus rapide : 1 bloc toutes les 2 ticks
                sphere.tickCounter = 0;
                Level level = sphere.level;
                // Retirer l'ancienne position de la sphère si ce n'est pas la première mise en place.
                if (sphere.currentOffset >= 0) {
                    removeEntireSphere(level, sphere.originalCenter, sphere.radius, sphere.currentOffset);
                }
                // Augmenter le décalage vertical
                sphere.currentOffset++;

                // Placer la sphère à la nouvelle position
                placeEntireSphere(level, sphere.originalCenter, sphere.radius, sphere.currentOffset);

                // Téléporter toutes les entités présentes dans le volume de la nouvelle sphère pour qu'elles montent avec elle.
                BlockPos currentCenter = sphere.getCurrentCenter();
                level.getEntities(null, new net.minecraft.world.phys.AABB(
                        currentCenter.getX() - sphere.radius, currentCenter.getY() - sphere.radius,
                        currentCenter.getZ() - sphere.radius,
                        currentCenter.getX() + sphere.radius, currentCenter.getY() + sphere.radius,
                        currentCenter.getZ() + sphere.radius)).forEach(entity -> {
                    // On téléporte l'entité de 1 bloc vers le haut.
                    entity.teleportTo(entity.getX(), entity.getY() + 1, entity.getZ());
                });

                // Si le décalage atteint la hauteur cible (ici 50 blocs)
                if (sphere.currentOffset >= sphere.targetOffset) {
                    // Déclencher l'explosion final
                    Vec3 explosionPos = new Vec3(
                            currentCenter.getX() + 0.5,
                            currentCenter.getY() + 0.5,
                            currentCenter.getZ() + 0.5);
                    level.explode(null, explosionPos.x, explosionPos.y, explosionPos.z, 4.0F, Level.ExplosionInteraction.TNT);
                    // Supprimer la sphère (effacer tous les blocs)
                    removeEntireSphere(level, sphere.originalCenter, sphere.radius, sphere.currentOffset);
                    it.remove();
                }
            }
        }
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        int chakraCost = 70;
        if (ninjaData.getChakra() < chakraCost) {
            player.displayClientMessage(
                    Component.translatable("jutsu.fail.notenoughchakra",
                            Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)),
                    true);
            return false;
        }
        ninjaData.useChakra(chakraCost, 200);
        return true;
    }
}