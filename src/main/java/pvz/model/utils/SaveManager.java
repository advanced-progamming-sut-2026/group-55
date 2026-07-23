package pvz.model.utils;

import com.google.gson.*;
import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.*;

public class SaveManager {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger logger = Logger.getLogger(SaveManager.class.getName());
    private static final File saveFile = new File(System.getProperty("user.dir"), "save.json");

    public static <T> List<T> load(Type listType) {
        if (!saveFile.exists()) return new ArrayList<>();

        try (FileReader reader = new FileReader(saveFile)) {
            List<T> loaded = gson.fromJson(reader, listType);
            return loaded != null ? loaded : new ArrayList<>();
        } catch (IOException | JsonSyntaxException e) {
            logger.log(Level.WARNING, "Load failed, returning empty list", e);
            return new ArrayList<>();
        }
    }

    public static <T> boolean save(List<T> data) {
        try (FileWriter writer = new FileWriter(saveFile)) {
            gson.toJson(data, writer);
            return true;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Save failed", e);
            return false;
        }
    }
}