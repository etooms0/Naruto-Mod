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

@Mod.EventBusSubscriber
public class EarthSphereLiftJutsuAbility extends Ability implements Ability.Cooldown {

    /**
     * Données associées à une sphère en mouvement.
     */
    public static class RisingSphereData {
        public final Level level;
        public final BlockPos originalCenter;
        public final int radius;
        public int currentOffset; // décalage vertical actuel
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

        // Centre actuel de la sphère (origine + décalage vertical)
        public BlockPos getCurrentCenter() {
            return originalCenter.offset(0, currentOffset, 0);
        }
    }

    // Liste de toutes les sphères qui montent actuellement
    private static final List<RisingSphereData> activeRisingSpheres = new ArrayList<>();

    @Override
    public long defaultCombo() {
        return 112233;  // Exemple de combinaison pour ce jutsu
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
     * Recherche l'entité que le joueur (ServerPlayer) regarde, jusqu'à une distance donnée.
     */
    private Entity getLookedAtEntity(ServerPlayer player, double maxDistance) {
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getLookAngle().scale(maxDistance);
        Vec3 targetPos = eyePos.add(lookVec);

        for (Entity entity : player.level().getEntities(player, player.getBoundingBox().expandTowards(lookVec).inflate(1.0))) {
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

        // Détection de l'entité regardée
        Entity target = getLookedAtEntity(serverPlayer, 50.0);
        if (target == null) {
            player.displayClientMessage(
                    Component.literal("Aucune entit\u00E9 d\u00E9tect\u00E9e dans le champ de vision").withStyle(ChatFormatting.RED),
                    true);
            return;
        }
        if (target == player) {
            player.displayClientMessage(
                    Component.literal("Impossible de cibler soi-m\u00EAme").withStyle(ChatFormatting.RED),
                    true);
            return;
        }

        Level level = serverPlayer.level();
        int radius = 5;  // La sphère aura un diamètre d'environ 11 blocs.
        // Crée la sphère creuse autour de la cible
        createHollowEarthSphere(level, target, radius);

        // Stocke la sphère pour le déplacement : ici, on veut qu'elle monte de 50 blocs
        BlockPos center = target.blockPosition();
        activeRisingSpheres.add(new RisingSphereData(level, center, radius, 50));
    }

    /**
     * Crée une sphère creuse (coquille) autour de la cible.
     * Seuls les voxels dont la distance est comprise entre (radius - 1) et radius sont remplacés par de la terre.
     */
    private void createHollowEarthSphere(Level level, Entity target, int radius) {
        BlockPos center = target.blockPosition();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double distance = Math.sqrt(x * x + y * y + z * z);
                    if (distance <= radius && distance > (radius - 1)) {
                        BlockPos pos = center.offset(x, y, z);
                        if (level.isEmptyBlock(pos)) { // on peut choisir de ne remplacer que l'air
                            level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }
    }

    /**
     * Supprime l'intégralité de la coquille (les blocs de terre) à la position donnée.
     */
    private static void removeEntireSphere(Level level, BlockPos center, int radius, int currentOffset) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double distance = Math.sqrt(x * x + y * y + z * z);
                    if (distance <= radius && distance > (radius - 1)) {
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
     * Place l'intégralité de la coquille (les blocs de terre) à la position donnée (centre décalé).
     */
    private static void placeEntireSphere(Level level, BlockPos center, int radius, int currentOffset) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double distance = Math.sqrt(x * x + y * y + z * z);
                    if (distance <= radius && distance > (radius - 1)) {
                        BlockPos pos = center.offset(x, y + currentOffset, z);
                        level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    /**
     * Cet événement se déclenche chaque tick serveur.
     * La sphère active monte progressivement :
     * - On retire la version à l'offset précédent, on incrémente l'offset et on replace l'intégralité de la coquille à la nouvelle altitude.
     * - On téléporte ensuite toutes les entités dans la zone vers le haut pour qu'elles montent avec la structure.
     * - Lorsque l'offset atteint la hauteur cible (50 blocs), on déclenche une explosion et on efface la structure.
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Iterator<RisingSphereData> it = activeRisingSpheres.iterator();
        while (it.hasNext()) {
            RisingSphereData sphere = it.next();
            sphere.tickCounter++;
            // Pour une montée plus rapide, ici on déplace la sphère d'un bloc chaque tick.
            if (sphere.tickCounter >= 1) {
                sphere.tickCounter = 0;
                Level level = sphere.level;
                // Supprimer l'ancienne position de la sphère
                removeEntireSphere(level, sphere.originalCenter, sphere.radius, sphere.currentOffset);
                // Incrémentez l'offset (vous pouvez augmenter cette valeur pour accélérer davantage)
                sphere.currentOffset += 1;
                // Placez toute la sphère à la nouvelle position
                placeEntireSphere(level, sphere.originalCenter, sphere.radius, sphere.currentOffset);

                // Téléportation des entités situées dans la zone de la sphère pour qu'elles montent avec elle
                BlockPos currentCenter = sphere.getCurrentCenter();
                level.getEntities(null, new net.minecraft.world.phys.AABB(
                        currentCenter.getX() - sphere.radius, currentCenter.getY() - sphere.radius,
                        currentCenter.getZ() - sphere.radius,
                        currentCenter.getX() + sphere.radius, currentCenter.getY() + sphere.radius,
                        currentCenter.getZ() + sphere.radius)).forEach(entity -> {
                    entity.teleportTo(entity.getX(), entity.getY() + 1, entity.getZ());
                });

                // Si l'offset atteint la hauteur cible (50 blocs)
                if (sphere.currentOffset >= sphere.targetOffset) {
                    Vec3 explosionPos = new Vec3(
                            currentCenter.getX() + 0.5,
                            currentCenter.getY() + 0.5,
                            currentCenter.getZ() + 0.5);
                    // Déclenche l'explosion destructive.
                    level.explode(null, explosionPos.x, explosionPos.y, explosionPos.z, 4.0F, Level.ExplosionInteraction.TNT);

                    // Efface toutes les couches de la structure
                    for (int i = 0; i < sphere.currentOffset; i++) {
                        removeEntireSphere(level, sphere.originalCenter, sphere.radius, i);
                    }
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