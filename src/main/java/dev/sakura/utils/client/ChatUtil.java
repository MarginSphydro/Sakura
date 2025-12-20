package dev.sakura.utils.client;

import dev.sakura.Sakura;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;

import static dev.sakura.Sakura.mc;

/**
 * @Author：Gu-Yuemang
 * @Date：2025/11/14 08:54
 */
public class ChatUtil {
    private static final String PREFIX = "§7[§5" + Sakura.MOD_NAME + "§7] ";

    public static void component(Text component) {
        ChatHud chat = MinecraftClient.getInstance().inGameHud.getChatHud();
        chat.addMessage(component);
    }

    public static void addChatMessage(String message) {
        addChatMessage(true, message);
    }

    public static void addChatMessage(boolean prefix, String message) {
        component(Text.literal((prefix ? PREFIX : "") + message));
    }

    public static void sendMessage(String message) {
        if (mc.player != null) {
            Text component = Text.literal(message.replace('&', '§'));
            mc.player.sendMessage(component, false);
        }
    }
}