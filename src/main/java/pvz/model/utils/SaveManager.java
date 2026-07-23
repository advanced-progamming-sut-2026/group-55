package pvz.model.utils;

import com.google.gson.*;
import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SaveManager {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static <T> List<T> load(File file, Type listType) {
        if (!file.exists()) {
            return new ArrayList<>();
        }

        try (FileReader reader = new FileReader(file)) {
            List<T> loaded = gson.fromJson(reader, listType);
            return loaded != null ? loaded : new ArrayList<>();
        } catch (IOException | JsonParseException e) {
            return new ArrayList<>();
        }
    }

    public static <T> boolean save(File file, List<T> data) {
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(data, writer);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}