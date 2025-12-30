package dev.sakura.verify.agent;

import net.bytebuddy.asm.Advice;

import java.util.Map;

public class GRCBAdvice {
    @Advice.OnMethodExit
    @SuppressWarnings("unchecked")
    public static void onExit(@Advice.Argument(0) String name, @Advice.Return(readOnly = false) byte[] result) {
        Map<String, byte[]> cloudClasses = (Map<String, byte[]>) System.getProperties().get("sakura.cloud.classes");
        if (cloudClasses != null) {
            byte[] cloudBytes = cloudClasses.get(name);
            if (cloudBytes != null) {
                result = cloudBytes;
            }
        }
    }
}