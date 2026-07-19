package pvz.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class BaseManager<T> {
    protected final String filePath;
    protected final List<T> items;
    protected final Gson gson;
    private final Type listType;

    public BaseManager(String filePath, Type listType) {
        this.filePath = filePath;
        this.listType = listType;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.items = loadAll();
    }

    private List<T> loadAll() {
        try (FileReader reader = new FileReader(filePath)) {
            List<T> loaded = gson.fromJson(reader, listType);
            return loaded != null ? loaded : new ArrayList<>();
        } catch (IOException e) {

            return new ArrayList<>();
        }
    }

    public void add(T item) {
        items.add(item);
    }

    public List<T> getAll() {
        return new ArrayList<>(items);
    }

    public T find(Predicate<T> condition) {
        return items.stream()
                .filter(condition)
                .findFirst()
                .orElse(null);
    }

    public List<T> filter(Predicate<T> condition) {
        return items.stream()
                .filter(condition)
                .collect(Collectors.toList());
    }

    public boolean exists(Predicate<T> condition) {
        return items.stream().anyMatch(condition);
    }

    public boolean save() {
        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(items, writer);
            writer.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}