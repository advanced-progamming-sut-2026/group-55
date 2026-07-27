package pvz.model.utils;

import com.google.gson.*;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
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
        } catch (JsonParseException | IOException e) {
            throw new IllegalStateException("Save file is corrupted", e);
        }
    }

    public static <T> boolean save(File file, List<T> data) {
        Path path = file.toPath();
        Path tempPath = Paths.get(file.getAbsolutePath() + ".tmp");
        Path backupPath = Paths.get(file.getAbsolutePath() + ".bak");

        try {
            try (FileWriter writer = new FileWriter(tempPath.toFile())) {
                gson.toJson(data, writer);
                writer.flush();
            }

            if (Files.exists(path)) {
                Files.move(path, backupPath, StandardCopyOption.REPLACE_EXISTING);
            }

            Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING);

            return true;
        } catch (IOException e) {
            System.err.println("Critical Error during save: " + e.getMessage());
            return false;
        }
    }
}
