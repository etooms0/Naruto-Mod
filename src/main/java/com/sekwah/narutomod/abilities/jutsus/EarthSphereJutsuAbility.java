package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.registries.NarutoRegistries;
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
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber  // Nous n'avons pas besoin de tick si la sphère est permanente, mais on garde l'annotation si d'autres events sont nécessaires.
public class EarthSphereJutsuAbility extends Ability implements Ability.Cooldown {


    @Override
    public int getPointCost() {
        return 1;
    }

    @Override
    public long defaultCombo() {
        return 1232;  // Exemple : la combo de touches à effectuer pour ce jutsu
    }

    @Override
    public int getCooldown() {
        return 20 * 60 * 4;  // Cooldown de 10 secondes (10 * 20 ticks)
    }

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    /**
     * Récupère l'entité que le joueur ServerPlayer regarde jusqu'à maxDistance.
     * On parcourt toutes les entités situées dans l'AABB étendue vers la direction du regard.
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

        // Détection de l'entité visée
        Entity target = getLookedAtEntity(serverPlayer, 50.0);
        if (target == null) {
            player.displayClientMessage(Component.literal("Aucune entit\u00E9 d\u00E9tect\u00E9e dans le champ de vision").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (target == player) {
            player.displayClientMessage(Component.literal("Impossible de cibler soi-même").withStyle(ChatFormatting.RED), true);
            return;
        }

        // Création de la sphère de terre autour de la cible
        createEarthSphere(serverPlayer.level(), target);
    }

    /**
     * Génère une sphère de terre autour de l'entité cible.
     * La sphère est définie par un rayon donné (ici, 5 blocs) et remplace les blocs dans la zone par des blocs de terre (Blocks.DIRT).
     * La sphère reste indéfiniment en place (pas de timer de suppression).
     */
    private void createEarthSphere(Level level, Entity target) {
        BlockPos center = target.blockPosition();
        int radius = 5; // rayon en blocs : la sphère aura un diamètre de 11 blocs
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double distance = Math.sqrt(x * x + y * y + z * z);
                    // Place le bloc seulement si la distance est comprise entre (radius - 1) et le rayon,
                    // ce qui crée une coquille d'une épaisseur d'environ 1 bloc.
                    if (distance <= radius && distance > radius - 1) {
                        BlockPos pos = center.offset(x, y, z);
                        // On remplace le bloc par de la terre (ici Blocks.DIRT, vous pouvez adapter si besoin)
                        level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        // get ID
        String jutsuId = NarutoRegistries.ABILITIES
                .getResourceKey(this)
                .map(r -> r.location().getPath())
                .orElse("");

        // Verify if jutsu in deck
        if (!ninjaData.getSlotData().isEquipped(jutsuId)) {
            player.displayClientMessage(
                    Component.literal("This jutsu is not in your deck"),
                    true
            );
            return false;
        }
        int chakraCost = 50;
        if (ninjaData.getChakra() < chakraCost) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(chakraCost, 60);
        return true;
    }
}