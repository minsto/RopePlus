package com.mickdev.ropeplus.common;

import com.mickdev.ropeplus.RopePlus;
import com.mickdev.ropeplus.entity.FreeFormRopeEntity;
import com.mickdev.ropeplus.entity.GrapplingHookEntity;
import com.mickdev.ropeplus.item.HookshotItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * Server-side state: per player hookshot ropes and thrown grappling hooks,
 * plus the gradual downward rope growth tasks (port of the 1.12 pseudo tile entities).
 */
@EventBusSubscriber(modid = RopePlus.MODID)
public final class RopeManager {

    private static final Map<UUID, FreeFormRopeEntity> PLAYER_ROPES = new HashMap<>();
    private static final Map<UUID, GrapplingHookEntity> PLAYER_HOOKS = new HashMap<>();
    private static final Map<UUID, Float> PENDING_RANGE_BONUS = new HashMap<>();
    private static final List<GrowthTask> GROWTH_TASKS = new ArrayList<>();

    private RopeManager() {
    }

    // ---------- hookshot rope tracking ----------

    @Nullable
    public static FreeFormRopeEntity getPlayerRope(Player player) {
        FreeFormRopeEntity rope = PLAYER_ROPES.get(player.getUUID());
        if (rope != null && !rope.isAlive()) {
            PLAYER_ROPES.remove(player.getUUID());
            return null;
        }
        return rope;
    }

    public static void setPlayerRope(Player player, @Nullable FreeFormRopeEntity rope) {
        if (rope == null) {
            PLAYER_ROPES.remove(player.getUUID());
        } else {
            PLAYER_ROPES.put(player.getUUID(), rope);
        }
    }

    // ---------- hookshot range bonus (sneak before firing) ----------

    public static float getPendingRangeBonus(Player player) {
        return PENDING_RANGE_BONUS.getOrDefault(player.getUUID(), 0.0F);
    }

    /** Sneak-extend while aiming: +10 range on the next shot, capped at 200 total. */
    public static boolean tryAddPendingRangeBonus(Player player) {
        float maxBonus = (float) (HookshotItem.BOOSTED_RANGE_CAP - HookshotItem.baseMaxRange());
        float current = getPendingRangeBonus(player);
        if (current >= maxBonus) {
            return false;
        }
        if (!HookshotItem.consumeTwoCartridges(player)) {
            return false;
        }
        PENDING_RANGE_BONUS.put(player.getUUID(), Math.min(maxBonus, current + (float) HookshotItem.RANGE_PER_EXTEND));
        return true;
    }

    public static float takePendingRangeBonus(Player player) {
        Float bonus = PENDING_RANGE_BONUS.remove(player.getUUID());
        return bonus != null ? bonus : 0.0F;
    }

    // ---------- grappling hook tracking ----------

    @Nullable
    public static GrapplingHookEntity getPlayerHook(Player player) {
        GrapplingHookEntity hook = PLAYER_HOOKS.get(player.getUUID());
        if (hook != null && !hook.isAlive()) {
            PLAYER_HOOKS.remove(player.getUUID());
            return null;
        }
        return hook;
    }

    public static void setPlayerHook(Player player, @Nullable GrapplingHookEntity hook) {
        if (hook == null) {
            PLAYER_HOOKS.remove(player.getUUID());
        } else {
            PLAYER_HOOKS.put(player.getUUID(), hook);
        }
    }

    // ---------- rope growth ----------

    /**
     * Queues a rope column growing downwards from {@code pos}, one block every second.
     */
    public static void queueGrowth(ServerLevel level, BlockPos pos, BlockState ropeState, int remaining) {
        if (remaining > 0) {
            GROWTH_TASKS.add(new GrowthTask(level, pos, ropeState, remaining, 20));
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        // collect follow-up tasks separately: adding to GROWTH_TASKS while
        // iterating it throws a ConcurrentModificationException
        List<GrowthTask> followUps = null;
        Iterator<GrowthTask> it = GROWTH_TASKS.iterator();
        while (it.hasNext()) {
            GrowthTask task = it.next();
            if (task.level != level) {
                continue;
            }
            if (--task.delay > 0) {
                continue;
            }
            it.remove();
            BlockPos below = task.pos.below();
            BlockState existing = level.getBlockState(below);
            if (existing.isAir() || existing.canBeReplaced()) {
                level.setBlockAndUpdate(below, task.ropeState);
                if (task.remaining - 1 > 0) {
                    if (followUps == null) {
                        followUps = new ArrayList<>();
                    }
                    followUps.add(new GrowthTask(level, below, task.ropeState, task.remaining - 1, 20));
                }
            }
        }
        if (followUps != null) {
            GROWTH_TASKS.addAll(followUps);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PLAYER_ROPES.clear();
        PLAYER_HOOKS.clear();
        PENDING_RANGE_BONUS.clear();
        GROWTH_TASKS.clear();
    }

    private static final class GrowthTask {
        final ServerLevel level;
        final BlockPos pos;
        final BlockState ropeState;
        final int remaining;
        int delay;

        GrowthTask(ServerLevel level, BlockPos pos, BlockState ropeState, int remaining, int delay) {
            this.level = level;
            this.pos = pos;
            this.ropeState = ropeState;
            this.remaining = remaining;
            this.delay = delay;
        }
    }
}
