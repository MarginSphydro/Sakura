package dev.sakura.manager;

public class Managers {


    private static boolean initialized;

    public static void init() {
        if (initialized) return;


        initialized = true;
    }
}
