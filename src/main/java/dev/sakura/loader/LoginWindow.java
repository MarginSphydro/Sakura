package dev.sakura.loader;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.opengl.GL;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVGGL2.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

public class LoginWindow {
    private long window;
    private String username = "";
    private String password = "";
    private String regUsername = "";
    private String regPassword = "";
    private String status = "";
    private int statusColor = 0xFFFFFF;
    private boolean inputUsername = true;
    private boolean loginSuccess = false;
    private boolean shouldClose = false;
    private boolean isRegisterMode = false;
    private boolean rememberMe = false;
    private double mouseX, mouseY;

    private static final Path CREDENTIALS_FILE = Path.of(System.getProperty("user.home"), ".sakura", "credentials.dat");
    private static final byte[] ENCRYPTION_KEY = "SakuraClient2024".getBytes(); // 16 bytes for AES-128

    private long vg;

    private static final int COLOR_BG = 0x1a1a1a;
    private static final int COLOR_CARD = 0x252525;
    private static final int COLOR_INPUT = 0x2d2d2d;
    private static final int COLOR_INPUT_ACTIVE = 0x3d3d3d;
    private static final int COLOR_ACCENT = 0xFFB7C5;
    private static final int COLOR_TEXT = 0xFFFFFF;
    private static final int COLOR_TEXT_DIM = 0x888888;
    private static final int COLOR_SUCCESS = 0x4CAF50;
    private static final int COLOR_ERROR = 0xF44336;

    private static final int W = 420, H = 400;
    private static final int PADDING = 30;
    private static final int INPUT_HEIGHT = 36;
    private static final int INPUT_WIDTH = W - PADDING * 2;
    private static final int BUTTON_HEIGHT = 40;
    private static final float RADIUS = 8;

    public boolean show() {
        try {
            if (Files.exists(CREDENTIALS_FILE)) {
                String encrypted = Files.readString(CREDENTIALS_FILE);
                String decrypted = decrypt(encrypted);
                String[] parts = decrypted.split("\n", 2);
                if (parts.length == 2) {
                    username = parts[0];
                    password = parts[1];
                    rememberMe = true;
                }
            }
        } catch (Exception ignored) {
        }

        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            System.err.println("[LoginWindow] Failed to initialize window");
            return false;
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 2);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);

        window = glfwCreateWindow(W, H, "Sakura Client", NULL, NULL);
        if (window == NULL) {
            System.err.println("[LoginWindow] Failed to create window");
            glfwTerminate();
            return false;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            glfwGetWindowSize(window, pWidth, pHeight);

            var vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            if (vidmode != null) {
                glfwSetWindowPos(window,
                        (vidmode.width() - pWidth.get(0)) / 2,
                        (vidmode.height() - pHeight.get(0)) / 2);
            }
        }

        try {
            GLFWImage.Buffer icons = GLFWImage.malloc(2);

            ByteBuffer icon32 = loadIcon("/assets/sakura/icons/icon_32x32.png", 32);
            if (icon32 != null) {
                icons.position(0).width(32).height(32).pixels(icon32);
            }

            ByteBuffer icon16 = loadIcon("/assets/sakura/icons/icon_16x16.png", 16);
            if (icon16 != null) {
                icons.position(1).width(16).height(16).pixels(icon16);
            }

            icons.position(0);
            glfwSetWindowIcon(window, icons);
            icons.free();

        } catch (Exception e) {
            System.err.println("[LoginWindow] Failed to set icon: " + e.getMessage());
        }

        glfwMakeContextCurrent(window);
        GL.createCapabilities();
        glfwSwapInterval(1);

        vg = nvgCreate(NVG_ANTIALIAS | NVG_STENCIL_STROKES);
        if (vg == NULL) {
            System.err.println("[LoginWindow] Failed to create NanoVG context");
            glfwDestroyWindow(window);
            glfwTerminate();
            return false;
        }

        initFont();

        glfwShowWindow(window);

        // Input callbacks
        glfwSetCharCallback(window, (win, codepoint) -> {
            char c = (char) codepoint;
            if (isRegisterMode) {
                if (inputUsername) regUsername += c;
                else regPassword += c;
            } else {
                if (inputUsername) username += c;
                else password += c;
            }
        });

        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS || action == GLFW_REPEAT) {
                if (key == GLFW_KEY_BACKSPACE) {
                    if (isRegisterMode) {
                        if (inputUsername && !regUsername.isEmpty()) {
                            regUsername = regUsername.substring(0, regUsername.length() - 1);
                        } else if (!inputUsername && !regPassword.isEmpty()) {
                            regPassword = regPassword.substring(0, regPassword.length() - 1);
                        }
                    } else {
                        if (inputUsername && !username.isEmpty()) {
                            username = username.substring(0, username.length() - 1);
                        } else if (!inputUsername && !password.isEmpty()) {
                            password = password.substring(0, password.length() - 1);
                        }
                    }
                } else if (key == GLFW_KEY_TAB) {
                    inputUsername = !inputUsername;
                } else if (key == GLFW_KEY_ENTER) {
                    if (isRegisterMode) {
                        performRegister();
                    } else {
                        performLogin();
                    }
                } else if (key == GLFW_KEY_ESCAPE) {
                    shouldClose = true;
                }
            }
        });

        glfwSetCursorPosCallback(window, (win, xpos, ypos) -> {
            mouseX = xpos;
            mouseY = ypos;
        });

        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
                handleMouseClick(mouseX, mouseY);
            }
        });

        while (!glfwWindowShouldClose(window) && !shouldClose && !loginSuccess) {
            render();
            glfwSwapBuffers(window);
            glfwPollEvents();
        }

        nvgDelete(vg);
        glfwDestroyWindow(window);
        glfwTerminate();

        return loginSuccess;
    }

    private void render() {
        glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);

        nvgBeginFrame(vg, W, H, 1);

        int y = 20;

        drawText("Sakura Client", W / 2f, y + 15, COLOR_ACCENT, true, 28);
        y += 50;

        int tabW = (INPUT_WIDTH - 10) / 2;
        drawRoundedRect(PADDING, y, tabW, 32, 6, !isRegisterMode ? COLOR_ACCENT : COLOR_INPUT);
        drawText("登录", PADDING + tabW / 2f, y + 16, !isRegisterMode ? 0x1E1E1E : COLOR_TEXT_DIM, true, 16);

        drawRoundedRect(PADDING + tabW + 10, y, tabW, 32, 6, isRegisterMode ? COLOR_ACCENT : COLOR_INPUT);
        drawText("注册", PADDING + tabW + 10 + tabW / 2f, y + 16, isRegisterMode ? 0x1E1E1E : COLOR_TEXT_DIM, true, 16);
        y += 50;

        drawText("用户名", PADDING, y, COLOR_TEXT_DIM, false, 14);
        y += 22;
        String currentUser = isRegisterMode ? regUsername : username;
        drawRoundedRect(PADDING, y, INPUT_WIDTH, INPUT_HEIGHT, RADIUS, inputUsername ? COLOR_INPUT_ACTIVE : COLOR_INPUT);
        if (inputUsername) drawRoundedRectStroke(PADDING, y, INPUT_WIDTH, INPUT_HEIGHT, RADIUS, COLOR_ACCENT);
        drawText(currentUser + (inputUsername ? "|" : ""), PADDING + 12, y + INPUT_HEIGHT / 2, COLOR_TEXT, false, 16);
        y += INPUT_HEIGHT + 16;

        drawText("密码", PADDING, y, COLOR_TEXT_DIM, false, 14);
        y += 22;
        String currentPwd = isRegisterMode ? regPassword : password;
        drawRoundedRect(PADDING, y, INPUT_WIDTH, INPUT_HEIGHT, RADIUS, !inputUsername ? COLOR_INPUT_ACTIVE : COLOR_INPUT);
        if (!inputUsername) drawRoundedRectStroke(PADDING, y, INPUT_WIDTH, INPUT_HEIGHT, RADIUS, COLOR_ACCENT);
        String displayPwd = isRegisterMode ? currentPwd : "●".repeat(currentPwd.length());
        drawText(displayPwd + (!inputUsername ? "|" : ""), PADDING + 12, y + INPUT_HEIGHT / 2F, COLOR_TEXT, false, 16);
        y += INPUT_HEIGHT + 16;

        if (!isRegisterMode) {
            drawRoundedRect(PADDING, y, 20, 20, 4, rememberMe ? COLOR_ACCENT : COLOR_INPUT);
            if (rememberMe) {
                drawText("✓", PADDING + 10, y + 10, 0x1E1E1E, true, 14);
            }
            drawText("记住登录信息", PADDING + 28, y + 10, COLOR_TEXT_DIM, false, 14);
            y += 35;
        }

        if (!status.isEmpty()) {
            drawText(status, W / 2F, y, statusColor, true, 14);
        }
        y += 25;

        String btnText = isRegisterMode ? "注 册" : "登 录";
        drawRoundedRect(PADDING, y, INPUT_WIDTH, BUTTON_HEIGHT, RADIUS, COLOR_ACCENT);
        drawText(btnText, W / 2, y + BUTTON_HEIGHT / 2, 0x1E1E1E, true, 18);
        y += BUTTON_HEIGHT + 15;

        drawText("TAB 切换输入框  ·  ESC 退出", W / 2, H - 15, 0x555555, true, 12);

        nvgEndFrame(vg);
    }

    private void handleMouseClick(double mx, double my) {
        int tabW = (INPUT_WIDTH - 10) / 2;

        if (my >= 70 && my <= 102) {
            if (mx >= PADDING && mx <= PADDING + tabW) {
                isRegisterMode = false;
                status = "";
            } else if (mx >= PADDING + tabW + 10 && mx <= PADDING + tabW + 10 + tabW) {
                isRegisterMode = true;
                status = "";
            }
        } else if (mx >= PADDING && mx <= PADDING + INPUT_WIDTH && my >= 142 && my <= 142 + INPUT_HEIGHT) {
            inputUsername = true;
        } else if (mx >= PADDING && mx <= PADDING + INPUT_WIDTH && my >= 216 && my <= 216 + INPUT_HEIGHT) {
            inputUsername = false;
        } else if (!isRegisterMode && mx >= PADDING && mx <= PADDING + 150 && my >= 268 && my <= 288) {
            rememberMe = !rememberMe;
        } else if (mx >= PADDING && mx <= PADDING + INPUT_WIDTH) {
            int buttonY = isRegisterMode ? 293 : 328;
            if (my >= buttonY && my <= buttonY + BUTTON_HEIGHT) {
                if (isRegisterMode) {
                    performRegister();
                } else {
                    performLogin();
                }
            }
        }
    }

    private void drawRoundedRect(float x, float y, float width, float height, float radius, int color) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor nvgColor = NVGColor.malloc(stack);
            nvgRGBA((byte) ((color >> 16) & 0xFF), (byte) ((color >> 8) & 0xFF), (byte) (color & 0xFF), (byte) 255, nvgColor);

            nvgBeginPath(vg);
            nvgRoundedRect(vg, x, y, width, height, radius);
            nvgFillColor(vg, nvgColor);
            nvgFill(vg);
        }
    }

    private void drawRoundedRectStroke(float x, float y, float width, float height, float radius, int color) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor nvgColor = NVGColor.malloc(stack);
            nvgRGBA((byte) ((color >> 16) & 0xFF), (byte) ((color >> 8) & 0xFF), (byte) (color & 0xFF), (byte) 255, nvgColor);

            nvgBeginPath(vg);
            nvgRoundedRect(vg, x, y, width, height, radius);
            nvgStrokeColor(vg, nvgColor);
            nvgStrokeWidth(vg, 2);
            nvgStroke(vg);
        }
    }

    private ByteBuffer loadIcon(String path, int expectedSize) {
        try {
            InputStream is = LoginWindow.class.getResourceAsStream(path);
            if (is == null) return null;

            byte[] bytes = is.readAllBytes();
            is.close();

            ByteBuffer imageBuffer = memAlloc(bytes.length);
            imageBuffer.put(bytes);
            imageBuffer.flip();

            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer w = stack.mallocInt(1);
                IntBuffer h = stack.mallocInt(1);
                IntBuffer comp = stack.mallocInt(1);

                ByteBuffer image = STBImage.stbi_load_from_memory(imageBuffer, w, h, comp, 4);
                memFree(imageBuffer);

                if (image != null) {
                    return image;
                }
            }
        } catch (Exception e) {
            System.err.println("[LoginWindow] Failed to load icon " + path + ": " + e.getMessage());
        }
        return null;
    }

    private void initFont() {
        // Try loading fonts - prefer Chinese-capable fonts
        String[] fontPaths = {
                "C:/Windows/Fonts/msyh.ttc",      // Microsoft YaHei (中文)
                "C:/Windows/Fonts/simhei.ttf",    // SimHei (中文)
                "C:/Windows/Fonts/simsun.ttc",    // SimSun (中文)
                "C:/Windows/Fonts/arial.ttf"      // Fallback
        };

        for (String path : fontPaths) {
            java.io.File fontFile = new java.io.File(path);
            if (fontFile.exists()) {
                int font = nvgCreateFont(vg, "default", path);
                if (font != -1) {
                    return;
                }
            }
        }

        // Try loading from resources
        try {
            InputStream is = LoginWindow.class.getResourceAsStream("/assets/sakura/fonts/kuriyama.ttf");
            if (is != null) {
                byte[] bytes = is.readAllBytes();
                is.close();
                ByteBuffer buffer = memAlloc(bytes.length);
                buffer.put(bytes);
                buffer.flip();
                nvgCreateFontMem(vg, "default", buffer, false);
            }
        } catch (Exception e) {
            System.err.println("[LoginWindow] Failed to load font: " + e.getMessage());
        }
    }

    private void drawText(String text, float x, float y, int color, boolean centered, float fontSize) {
        if (text == null || text.isEmpty()) return;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor nvgColor = NVGColor.malloc(stack);
            nvgRGBA((byte) ((color >> 16) & 0xFF), (byte) ((color >> 8) & 0xFF), (byte) (color & 0xFF), (byte) 255, nvgColor);

            nvgFontSize(vg, fontSize);
            nvgFontFace(vg, "default");
            nvgFillColor(vg, nvgColor);

            if (centered) {
                nvgTextAlign(vg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
            } else {
                nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
            }

            nvgText(vg, x, y, text);
        }
    }

    private void performLogin() {
        if (username.isEmpty() || password.isEmpty()) {
            status = "请填写用户名和密码";
            statusColor = COLOR_ERROR;
            return;
        }

        status = "正在连接服务器...";
        statusColor = COLOR_TEXT_DIM;
        render();
        glfwSwapBuffers(window);

        boolean success = VerificationManager.getInstance().verify(username, password);

        if (success) {
            status = "登录成功！";
            statusColor = COLOR_SUCCESS;
            loginSuccess = true;

            if (rememberMe) {
                saveCredentials();
            } else {
                deleteCredentials();
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
        } else {
            status = VerificationManager.getInstance().getFailReason();
            statusColor = COLOR_ERROR;
        }
    }

    private void performRegister() {
        if (regUsername.isEmpty() || regPassword.isEmpty()) {
            status = "请填写用户名和密码";
            statusColor = COLOR_ERROR;
            return;
        }

        if (regPassword.length() < 6) {
            status = "密码长度至少6位";
            statusColor = COLOR_ERROR;
            return;
        }

        status = "正在注册...";
        statusColor = COLOR_TEXT_DIM;
        render();
        glfwSwapBuffers(window);

        VerificationManager.RegisterResult result = VerificationManager.getInstance().register(regUsername, regPassword);

        if (result.success()) {
            status = "注册成功，请联系管理员审核";
            statusColor = COLOR_SUCCESS;
        } else {
            status = result.message();
            statusColor = COLOR_ERROR;
        }
    }

    private void saveCredentials() {
        try {
            Files.createDirectories(CREDENTIALS_FILE.getParent());
            String data = username + "\n" + password;
            String encrypted = encrypt(data);
            Files.writeString(CREDENTIALS_FILE, encrypted);
        } catch (Exception e) {
            System.err.println("[LoginWindow] Failed to save credentials: " + e.getMessage());
        }
    }

    private void deleteCredentials() {
        try {
            Files.deleteIfExists(CREDENTIALS_FILE);
        } catch (Exception ignored) {
        }
    }

    //@AutoNative
    //@NativeVirtualization(VirtualMachine.TIGER_BLACK)
    private String encrypt(String data) throws Exception {
        SecretKeySpec key = new SecretKeySpec(ENCRYPTION_KEY, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encrypted = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encrypted);
    }

    //@AutoNative
    //@NativeVirtualization(VirtualMachine.TIGER_BLACK)
    private String decrypt(String encryptedData) throws Exception {
        SecretKeySpec key = new SecretKeySpec(ENCRYPTION_KEY, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
        return new String(decrypted);
    }
}
