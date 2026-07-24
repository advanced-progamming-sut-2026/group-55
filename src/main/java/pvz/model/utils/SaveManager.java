package pvz.model.utils;

import com.google.gson.*;
import java.io.*;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class SaveManager {
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (src, typeOfSrc, context) ->
                    new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE)))
            .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (json, typeOfT, context) ->
                    LocalDate.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE))
            .create();

    public static <T> List<T> load(File file, Type listType) {
        if (!file.exists()) {
            return new ArrayList<>();
        }
        try (FileReader reader = new FileReader(file)) {
            List<T> loaded = gson.fromJson(reader, listType);
            return loaded != null ? loaded : new ArrayList<>();
        } catch (Exception e) {
            System.err.println("Error loading file: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static <T> boolean save(File file, List<T> data) {
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(data, writer);
            return true;
        } catch (IOException e) {
            System.err.println("Error saving file: " + e.getMessage());
            return false;
        }
    }
}