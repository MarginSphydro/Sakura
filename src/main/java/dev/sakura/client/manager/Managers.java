package dev.sakura.client.manager;

import dev.sakura.client.manager.impl.*;

public class Managers {
    private static boolean initialized;

    public static AccountManager ACCOUNT;
    public static RenderManager RENDER;
    public static RotationManager ROTATION;
    public static ExtrapolationManager EXTRAPOLATION;
    public static ChatAnimationUpdater CHAT_ANIMATION;

    public static void init() {
        if (initialized) return;

        ACCOUNT = new AccountManager();
        RENDER = new RenderManager();
        ROTATION = new RotationManager();
        EXTRAPOLATION = new ExtrapolationManager();
        CHAT_ANIMATION = new ChatAnimationUpdater();

        initialized = true;
    }
}