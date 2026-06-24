package com.mickdev.ropeplus.client;

import com.mickdev.ropeplus.entity.FreeFormRopeEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Client-side hookshot / zipline state. Replaces the 1.12 ClientProxy flags.
 * This class intentionally has no Minecraft client imports so it can be
 * classloaded safely anywhere.
 */
public final class ClientRopeState {

    /** true while the local player has a hookshot rope deployed */
    public static boolean hasRopeOut;
    /** set true to make the rope disconnect (with a small jump boost) */
    public static boolean shouldDisconnect;
    /** -1 = zipping in, 0 = no action, >0 = extend rope by that amount */
    public static float ropeChangeState;
    /** true while the player's grappling hook is out (swing animation) */
    public static boolean grapplingHookOut;

    /** active zipline ride, if any */
    @Nullable
    public static FreeFormRopeEntity zipline;
    public static float ziplineProgress;
    public static int ziplineTicker;
    public static boolean wasZiplining;

    /** cooldown before the next sneak range-extend request */
    public static int extendCooldown;

    private ClientRopeState() {
    }

    public static void reset() {
        hasRopeOut = false;
        shouldDisconnect = false;
        ropeChangeState = 0f;
        grapplingHookOut = false;
        zipline = null;
        ziplineProgress = 0f;
        ziplineTicker = 0;
        wasZiplining = false;
        extendCooldown = 0;
    }
}
