package com.sekwah.narutomod.entity;

import com.sekwah.narutomod.menu.IchirakuTradeMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.network.NetworkHooks;

import java.util.EnumSet;

public class IchirakuEntity extends Villager {

    private Player tradingPlayer;
    private Player followPlayer;

    public IchirakuEntity(EntityType<? extends Villager> type, Level level) {
        super(type, level);
        this.setPersistenceRequired(); // ne despawn pas
        this.setVillagerData(this.getVillagerData()
                .setProfession(VillagerProfession.FARMER)
                .setLevel(1));
        // Nom jaune toujours visible
        this.setCustomName(Component.literal("Ichiraku").withStyle(style -> style.withColor(TextColor.fromRgb(0xFFD700))));
        this.setCustomNameVisible(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Villager.createAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D); // vitesse ralentie
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 6.0F));
        // Suivre un joueur seulement si on lui a donné le bol (followPlayer != null)
        this.goalSelector.addGoal(2, new FollowPlayerGoal(this, 0.3D, 2.0F, 10.0F));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 0.2D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Villager.class, 6.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(6, new SayRandomPhraseGoal(this));
    }

    @Override
    protected void updateTrades() {
        MerchantOffers offers = this.getOffers();
        offers.clear();

        offers.add(new MerchantOffer(
                new ItemStack(Items.BREAD, 1),
                ItemStack.EMPTY,
                new ItemStack(Items.EMERALD, 2),
                10, 0, 0f
        ));

        offers.add(new MerchantOffer(
                new ItemStack(Items.COOKED_BEEF, 2),
                ItemStack.EMPTY,
                new ItemStack(Items.EMERALD, 5),
                8, 0, 0f
        ));
    }

    // Retourne les offres de trades actuelles (pour affichage)
    public MerchantOffers getCurrentTrades() {
        return this.getOffers();
    }



    @Override
    public void setTradingPlayer(Player player) {
        this.tradingPlayer = player;
    }

    @Override
    public Player getTradingPlayer() {
        return this.tradingPlayer;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide) {
            if (player instanceof ServerPlayer serverPlayer) {
                // Ouvre le menu de trade avec 4 arguments (windowId, playerInventory, merchant, level)
                serverPlayer.openMenu(new SimpleMenuProvider(
                        (windowId, inventory, p) -> new IchirakuTradeMenu(windowId, inventory, this, this.level()),
                        this.getDisplayName()
                ));
            }
            return InteractionResult.CONSUME; // Interaction prise en compte côté serveur
        }
        return InteractionResult.SUCCESS; // côté client, succès simple
    }


    @Override
    public void tick() {
        super.tick();

        // Lumière autour
        if (!this.level().isClientSide) {
            BlockPos pos = this.blockPosition();
            int light = this.level().getBrightness(LightLayer.BLOCK, pos);
            if (light < 15) {
                // Forcer mise à jour lumière, subtil mais limité en vanilla
                BlockState state = this.level().getBlockState(pos);
                this.level().sendBlockUpdated(pos, state, state, 3);
            }
        } else {
            // Particules légères "fumée de ramen" autour
            if (this.random.nextInt(10) == 0) {
                double x = this.getX() + (this.random.nextDouble() - 0.5D) * 0.5D;
                double y = this.getY() + 1.0D;
                double z = this.getZ() + (this.random.nextDouble() - 0.5D) * 0.5D;
                this.level().addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.02, 0);
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        // Chance de drop un bol à la mort
        if (this.random.nextFloat() < 0.2F) {
            this.spawnAtLocation(new ItemStack(Items.BOWL));
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Ichiraku").withStyle(style -> style.withColor(TextColor.fromRgb(0xFFD700)));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    // Goal pour suivre doucement le joueur qui lui a donné un bol
    static class FollowPlayerGoal extends Goal {
        private final IchirakuEntity ichiraku;
        private Player player;
        private final double speed;
        private final float minDist;
        private final float maxDist;

        public FollowPlayerGoal(IchirakuEntity ichiraku, double speed, float minDist, float maxDist) {
            this.ichiraku = ichiraku;
            this.speed = speed;
            this.minDist = minDist;
            this.maxDist = maxDist;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            this.player = this.ichiraku.followPlayer;
            if (this.player == null || this.player.isSpectator() || !this.player.isAlive()) return false;
            double dist = this.ichiraku.distanceToSqr(this.player);
            return dist > minDist * minDist && dist < maxDist * maxDist;
        }

        @Override
        public void start() {
            // pas besoin ici
        }

        @Override
        public void stop() {
            ichiraku.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (player == null) return;
            ichiraku.getLookControl().setLookAt(player, 30, 30);
            double dist = ichiraku.distanceTo(player);
            if (dist > maxDist) {
                ichiraku.getNavigation().moveTo(player, speed);
            } else if (dist < minDist) {
                ichiraku.getNavigation().stop();
            }
        }
    }

    // Goal qui fait dire des phrases aléatoires
    static class SayRandomPhraseGoal extends Goal {
        private final IchirakuEntity ichiraku;
        private int cooldown = 0;
        private static final String[] PHRASES = {
                "Bienvenue à Ichiraku!",
                "Le meilleur ramen du village!",
                "Un bol de ramen pour toi?",
                "Prenez soin de vous!",
                "La patience est la clé du bon goût."
        };

        public SayRandomPhraseGoal(IchirakuEntity ichiraku) {
            this.ichiraku = ichiraku;
            this.setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return --cooldown <= 0 && ichiraku.random.nextInt(5) == 0;
        }

        @Override
        public void start() {
            cooldown = 200 + ichiraku.random.nextInt(200);
            ichiraku.level().players().forEach(p ->
                    p.sendSystemMessage(Component.literal("<Ichiraku> " + PHRASES[ichiraku.random.nextInt(PHRASES.length)])));
        }
    }
}
