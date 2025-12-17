package dev.sakura.account;

import com.google.gson.*;
import dev.sakura.Sakura;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.apache.logging.log4j.core.util.IOUtils.EOF;

//TODO:这个类换成Sakura的写法，这个类感觉有点狗屎。
public abstract class ConfigFile {
    protected static final Gson GSON = new GsonBuilder().setLenient().setPrettyPrinting().create();

    private final String fileName;
    private final Path filepath;

    public ConfigFile(Path dir, String path) {
        if (!Files.exists(dir)) {
            try {
                Files.createDirectory(dir);
            } catch (IOException e) {
                Sakura.LOGGER.error("Could not create {} dir", dir);
                e.printStackTrace();
            }
        }
        fileName = dir.getFileName().toString();
        filepath = dir.resolve(toJsonPath(path));
    }

    protected String read(Path path) throws IOException {
        StringBuilder content = new StringBuilder();
        InputStream in = Files.newInputStream(path);
        int b;
        while ((b = in.read()) != EOF) {
            content.append((char) b);
        }
        in.close();
        return content.toString();
    }

    protected String serialize(Object obj) {
        return GSON.toJson(obj);
    }

    protected JsonObject parseObject(String json) {
        return parse(json, JsonObject.class);
    }

    protected JsonArray parseArray(String json) {
        return parse(json, JsonArray.class);
    }

    protected <T> T parse(String json, Class<T> type) {
        try {
            return GSON.fromJson(json, type);
        } catch (JsonSyntaxException e) {
            Sakura.LOGGER.error("Invalid json syntax!");
            e.printStackTrace();
        }
        return null;
    }

    protected void write(Path path, String content) throws IOException {
        OutputStream out = Files.newOutputStream(path);
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        out.write(bytes, 0, bytes.length);
        out.close();
    }

    public String getFileName() {
        return fileName;
    }

    public Path getFilepath() {
        return filepath;
    }

    public abstract void save();

    public abstract void load();

    private String toJsonPath(String fileName) {
        return String.format("%s.json", fileName).toLowerCase();
    }
}
