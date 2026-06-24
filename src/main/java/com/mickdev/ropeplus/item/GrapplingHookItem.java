package com.mickdev.ropeplus.item;

import com.mickdev.ropeplus.client.ClientRopeState;
import com.mickdev.ropeplus.common.RopeManager;
import com.mickdev.ropeplus.entity.GrapplingHookEntity;
import com.mickdev.ropeplus.network.GrapplingHookPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Throwable grappling hook. Throw it onto a ledge to plant a hook with a rope;
 * use again while it flies to recall it (yanking harpooned entities in).
 */
public class GrapplingHookItem extends Item {

    public GrapplingHookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // prevent held/spammed right-clicks from instantly recalling a freshly thrown hook
        player.getCooldowns().addCooldown(this, 10);

        if (level.isClientSide && ClientRopeState.grapplingHookOut) {
            player.swing(hand);
        }

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            GrapplingHookEntity existing = RopeManager.getPlayerHook(player);
            if (existing != null) {
                existing.recallHook(player);
                PacketDistributor.sendToPlayer(serverPlayer, new GrapplingHookPayload(false));
                RopeManager.setPlayerHook(player, null);
            } else {
                level.playSound(null, player.blockPosition(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS,
                        1.0F, 1.0F / (level.random.nextFloat() * 0.1F + 0.95F));
                GrapplingHookEntity hook = new GrapplingHookEntity(level, player);
                level.addFreshEntity(hook);
                RopeManager.setPlayerHook(player, hook);
                PacketDistributor.sendToPlayer(serverPlayer, new GrapplingHookPayload(true));
                player.swing(hand);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
