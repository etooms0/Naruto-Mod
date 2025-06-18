package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.ClientAttractionPacket;
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

    public static class AttractorData {
        public final Level level;
        public final Vec3 attractorPoint;
        public final BlockPos targetPos;
        public final long startTick;
        public final long explosionDelayTicks;
        public final List<Entity> fallingBlocks = new ArrayList<>();
        public boolean sphereFormed = false;
        public int currentLayer = 0;
        public final Entity caster;  // <- Ajout du lanceur ici

        public AttractorData(Level level, Vec3 attractorPoint, BlockPos targetPos, long startTick, long explosionDelayTicks, Entity caster) {
            this.level = level;
            this.attractorPoint = attractorPoint;
            this.targetPos = targetPos;
            this.startTick = startTick;
            this.explosionDelayTicks = explosionDelayTicks;
            this.caster = caster;  // Initialisation du lanceur
        }
    }


    private static final List<AttractorData> activeAttractors = new ArrayList<>();

    @Override
    public long defaultCombo() {
        return 112233;
    }

    @Override
    public int getCooldown() {
        return 30 * 20;
    }

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

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
        Vec3 attractor = targetPosVec.add(0, 20, 0);

        AABB entityAABB = new AABB(
                targetPosVec.x - 15, targetPosVec.y - 15, targetPosVec.z - 15,
                targetPosVec.x + 15, targetPosVec.y + 15, targetPosVec.z + 15
        );
        for (Entity e : level.getEntitiesOfClass(LivingEntity.class, entityAABB)) {
            LivingEntity living = (LivingEntity) e;
            Vec3 delta = attractor.subtract(living.position());
            if (delta.length() > 0) {
                Vec3 velocity = delta.normalize().scale(0.6); // vitesse augmentée
                living.setDeltaMovement(velocity);
                PacketHandler.sentToTracking(new ClientAttractionPacket(living.getId(), velocity), living);

            }
        }

        int horizRadius = 7;
        int verticalRange = 7;
        BlockPos targetPos = target.blockPosition();

        AttractorData data = new AttractorData(level, attractor, targetPos, level.getGameTime(), 20 * 10, player);
        RandomSource random = level.getRandom();
        float spawnChance = 0.3f;

        Field blockStateField = null;
        try {
            blockStateField = FallingBlockEntity.class.getDeclaredField("blockState");
            blockStateField.setAccessible(true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        for (int x = -horizRadius; x <= horizRadius; x++) {
            for (int z = -horizRadius; z <= horizRadius; z++) {
                for (int y = -verticalRange / 2; y <= verticalRange / 2; y++) {
                    BlockPos pos = targetPos.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (!state.isAir() && state.getBlock() != Blocks.BEDROCK) {
                        if (random.nextFloat() > spawnChance)
                            continue;
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
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
                        Vec3 motion = attractor.subtract(blockCenter).normalize().scale(0.6); // vitesse augmentée
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
        // Ne traiter qu'à la fin de la tick pour éviter les doubles calculs
        if (event.phase != TickEvent.Phase.END) return;

        Iterator<AttractorData> it = activeAttractors.iterator();
        while (it.hasNext()) {
            AttractorData data = it.next();
            Level level = data.level;
            long currentTick = level.getGameTime();
            long elapsed = currentTick - data.startTick;

            if (!data.sphereFormed) {
                // Attirer les FallingBlockEntities vers le point d’attraction
                for (Entity blockEnt : data.fallingBlocks) {
                    if (!blockEnt.isRemoved()) {
                        Vec3 currentPos = blockEnt.position();
                        Vec3 direction = data.attractorPoint.subtract(currentPos).normalize().scale(0.6);
                        blockEnt.setDeltaMovement(direction);
                    }
                }

                // Attirer les LivingEntities dans un AABB autour du point d’attraction
                AABB pullAABB = new AABB(
                        data.targetPos.getX() - 15, data.targetPos.getY() - 15, data.targetPos.getZ() - 15,
                        data.targetPos.getX() + 15, data.targetPos.getY() + 15, data.targetPos.getZ() + 15
                );

                for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, pullAABB)) {
                    // Exclure le lanceur pour ne pas s'attirer soi-même
                    if (living == data.caster) continue;

                    Vec3 delta = data.attractorPoint.subtract(living.position());
                    if (delta.length() > 0) {
                        Vec3 force = delta.normalize().scale(0.6);
                        living.setDeltaMovement(force);

                        // Envoi du paquet au joueur ciblé pour synchroniser client/serveur
                        if (living instanceof ServerPlayer serverPlayer) {
                            PacketHandler.sendToPlayer(new ClientAttractionPacket(living.getId(), force), serverPlayer);
                        }
                        PacketHandler.sentToTracking(new ClientAttractionPacket(living.getId(), force), living);
                    }
                }

                // Formation progressive de la sphère
                final int formationDelay = 20; // ticks avant début formation sphère
                final int layerInterval = 5;   // ticks entre ajout de couches
                final int maxRadius = 10;      // rayon max de la sphère

                if (elapsed >= formationDelay) {
                    int newLayer = (int) ((elapsed - formationDelay) / layerInterval);

                    if (newLayer > data.currentLayer) {
                        data.currentLayer = Math.min(newLayer, maxRadius);

                        if (data.currentLayer == maxRadius) {
                            data.sphereFormed = true;
                        }

                        BlockPos center = new BlockPos(
                                (int) Math.floor(data.attractorPoint.x),
                                (int) Math.floor(data.attractorPoint.y),
                                (int) Math.floor(data.attractorPoint.z)
                        );

                        // Pose des blocs DIRT en forme sphérique
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

                        // Nettoyage de l’air sous la sphère (retirer bloc sous le centre)
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

            // Explosion et nettoyage après formation de la sphère
            if (data.sphereFormed && elapsed >= data.explosionDelayTicks) {
                Vec3 expCenter = data.attractorPoint;
                BlockPos centerPos = new BlockPos(
                        (int) Math.floor(expCenter.x),
                        (int) Math.floor(expCenter.y),
                        (int) Math.floor(expCenter.z)
                );
                final int radius = 10;

                // Suppression blocs DIRT formant la sphère
                for (int x = -radius; x <= radius; x++) {
                    for (int y = -radius; y <= radius; y++) {
                        for (int z = -radius; z <= radius; z++) {
                            BlockPos pos = centerPos.offset(x, y, z);
                            if (level.getBlockState(pos).is(Blocks.DIRT)) {
                                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                            }
                        }
                    }
                }

                // Explosion sans drop d’items (TNT-style)
                level.explode(null, expCenter.x, expCenter.y, expCenter.z, 4.0F, true, Level.ExplosionInteraction.TNT);

                // Suppression de l’attracteur de la liste active
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
