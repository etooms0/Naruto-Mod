package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mod.EventBusSubscriber
public class EarthSphereLiftJutsuAbility extends Ability implements Ability.Cooldown {

    /**
     * Stocke les données de l’attraction :
     * - attractorPoint : le point d’attraction (80 blocs au-dessus de la cible)
     * - targetPos : la position initiale de la cible
     * - startTick : le tick auquel débute l’effet
     * - explosionDelayTicks : délai total avant explosion (240 ticks ici)
     * - fallingBlocks : liste des falling blocks créés initialement
     * - sphereFormed : indique si la sphère solide est terminée
     * - currentLayer : le nombre de couches actuellement construites (de 0 à maxRadius)
     */
    public static class AttractorData {
        public final Level level;
        public final Vec3 attractorPoint;
        public final BlockPos targetPos;
        public final long startTick;
        public final long explosionDelayTicks;
        public final List<Entity> fallingBlocks = new ArrayList<>();
        public boolean sphereFormed = false;
        public int currentLayer = 0;

        public AttractorData(Level level, Vec3 attractorPoint, BlockPos targetPos, long startTick, long explosionDelayTicks) {
            this.level = level;
            this.attractorPoint = attractorPoint;
            this.targetPos = targetPos;
            this.startTick = startTick;
            this.explosionDelayTicks = explosionDelayTicks;
        }
    }

    private static final List<AttractorData> activeAttractors = new ArrayList<>();

    @Override
    public long defaultCombo() {
        return 112233; // Exemple de combinaison
    }

    @Override
    public int getCooldown() {
        return 30 * 20; // 30 secondes de cooldown
    }

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    /**
     * Recherche l'entité que le joueur regarde, jusqu'à une distance maximale.
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

        // Recherche de la cible regardée par le joueur (jusqu'à 50 blocs)
        Entity target = getLookedAtEntity(serverPlayer, 50.0);
        if (target == null) {
            player.displayClientMessage(Component.literal("Aucune entité détectée")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        if (target == player) {
            player.displayClientMessage(Component.literal("Impossible de se cibler soi-même")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        Level level = serverPlayer.level();
        Vec3 targetPosVec = target.position();

        // Définir le point attracteur : 80 blocs au-dessus de la cible
        Vec3 attractor = targetPosVec.add(0, 20, 0);

        // Attirer immédiatement toutes les entités vivantes (mobs, joueurs, etc.) dans un rayon de 15 autour de la cible
        AABB entityAABB = new AABB(
                targetPosVec.x - 15, targetPosVec.y - 15, targetPosVec.z - 15,
                targetPosVec.x + 15, targetPosVec.y + 15, targetPosVec.z + 15
        );
        for (Entity e : level.getEntitiesOfClass(LivingEntity.class, entityAABB)) {
            LivingEntity living = (LivingEntity) e;
            Vec3 delta = attractor.subtract(living.position());
            if (delta.length() > 0) {
                Vec3 velocity = delta.normalize().scale(0.2);
                living.setDeltaMovement(velocity);
            }
        }

        // Pour les blocs du sol, on scanne une zone restreinte autour de la cible (rayon horizontal 7, hauteur ≈7)
        int horizRadius = 7;
        int verticalRange = 7;
        BlockPos targetPos = target.blockPosition();

        // Création des données d'attraction avec un délai total avant explosion de 240 ticks
        AttractorData data = new AttractorData(level, attractor, targetPos, level.getGameTime(), 20*20);

        // Augmenter le nombre de FallingBlockEntity : environ 30 % des blocs candidats seront transformés
        RandomSource random = level.getRandom();
        float spawnChance = 0.3f;

        // Récupérer le champ privé "blockState" de FallingBlockEntity via réflexion
        Field blockStateField = null;
        try {
            blockStateField = FallingBlockEntity.class.getDeclaredField("blockState");
            blockStateField.setAccessible(true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // Parcourir la zone pour transformer quelques blocs en FallingBlockEntity
        for (int x = -horizRadius; x <= horizRadius; x++) {
            for (int z = -horizRadius; z <= horizRadius; z++) {
                for (int y = -verticalRange / 2; y <= verticalRange / 2; y++) {
                    BlockPos pos = targetPos.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (!state.isAir() && state.getBlock() != Blocks.BEDROCK) {
                        if (random.nextFloat() > spawnChance)
                            continue;
                        // Retirer le bloc du monde
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        // Créer le FallingBlockEntity
                        FallingBlockEntity fallingBlock = new FallingBlockEntity(EntityType.FALLING_BLOCK, level);
                        fallingBlock.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                        if (blockStateField != null) {
                            try {
                                blockStateField.set(fallingBlock, state);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                        fallingBlock.setNoGravity(true);
                        Vec3 blockCenter = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                        Vec3 motion = attractor.subtract(blockCenter).normalize().scale(0.2);
                        fallingBlock.setDeltaMovement(motion);
                        level.addFreshEntity(fallingBlock);
                        data.fallingBlocks.add(fallingBlock);
                    }
                }
            }
        }

        activeAttractors.add(data);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;
        Iterator<AttractorData> it = activeAttractors.iterator();
        while (it.hasNext()) {
            AttractorData data = it.next();
            Level level = data.level;
            long currentTick = level.getGameTime();
            long elapsed = currentTick - data.startTick;

            // Pendant la formation, attirer les FallingBlockEntity et les entités vivantes
            if (!data.sphereFormed) {
                for (Entity blockEnt : data.fallingBlocks) {
                    if (!blockEnt.isRemoved()) {
                        Vec3 currentPos = blockEnt.position();
                        Vec3 desired = data.attractorPoint.subtract(currentPos).normalize().scale(0.2);
                        blockEnt.setDeltaMovement(desired);
                    }
                }
                AABB pullAABB = new AABB(
                        data.targetPos.getX() - 15, data.targetPos.getY() - 15, data.targetPos.getZ() - 15,
                        data.targetPos.getX() + 15, data.targetPos.getY() + 15, data.targetPos.getZ() + 15
                );
                for (Entity e : level.getEntitiesOfClass(LivingEntity.class, pullAABB)) {
                    LivingEntity living = (LivingEntity) e;
                    Vec3 delta = data.attractorPoint.subtract(living.position());
                    if (delta.length() > 0) {
                        Vec3 force = delta.normalize().scale(0.2);
                        living.setDeltaMovement(force);
                    }
                }

                // Dès 120 ticks, commencer la construction progressive couche par couche
                int formationDelay = 20*8;
                int layerInterval = 5;
                int maxRadius = 10;
                if (elapsed >= formationDelay) {
                    int newLayer = (int) ((elapsed - formationDelay) / layerInterval);
                    if (newLayer > data.currentLayer) {
                        data.currentLayer = newLayer;
                        if (data.currentLayer > maxRadius) {
                            data.currentLayer = maxRadius;
                            data.sphereFormed = true;
                        }
                        BlockPos center = new BlockPos(
                                (int) Math.floor(data.attractorPoint.x),
                                (int) Math.floor(data.attractorPoint.y),
                                (int) Math.floor(data.attractorPoint.z)
                        );
                        for (int x = -data.currentLayer; x <= data.currentLayer; x++) {
                            for (int y = -data.currentLayer; y <= data.currentLayer; y++) {
                                for (int z = -data.currentLayer; z <= data.currentLayer; z++) {
                                    double dist = Math.sqrt(x * x + y * y + z * z);
                                    if (dist <= data.currentLayer && dist <= maxRadius) {
                                        BlockPos pos = center.offset(x, y, z);
                                        level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
                                    }
                                }
                            }
                        }
                        BlockPos groundCenter = data.targetPos;
                        for (int x = -data.currentLayer; x <= data.currentLayer; x++) {
                            for (int z = -data.currentLayer; z <= data.currentLayer; z++) {
                                for (int y = -data.currentLayer; y <= 0; y++) {
                                    double dist = Math.sqrt(x * x + y * y + z * z);
                                    if (dist <= data.currentLayer && dist <= maxRadius) {
                                        BlockPos pos = groundCenter.offset(x, y, z);
                                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (data.sphereFormed && elapsed >= data.explosionDelayTicks) {
                Vec3 exp = data.attractorPoint;
                level.explode(null, exp.x, exp.y, exp.z, 110.0F, Level.ExplosionInteraction.TNT);
                it.remove();
            }
        }
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        int chakraCost = 100;
        if (ninjaData.getChakra() < chakraCost) {
            player.displayClientMessage(
                    Component.translatable("jutsu.fail.notenoughchakra",
                            Component.translatable(this.getTranslationKey(ninjaData))
                                    .withStyle(ChatFormatting.YELLOW)),
                    true);
            return false;
        }
        ninjaData.useChakra(chakraCost, 200);
        return true;
    }
}