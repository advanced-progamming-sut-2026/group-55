package pvz.model;

import pvz.model.utils.SaveManager;
import java.io.File; // این ایمپورت برای شناختن File الزامی است
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class BaseManager<T> {
    protected final String filePath;
    protected final File file;
    protected final List<T> items;
    private final Type listType;

    public BaseManager(String filePath, Type listType) {
        this.filePath = filePath;
        this.listType = listType;
        this.file = new File(filePath);

        this.items = SaveManager.load(this.file, listType);
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


    public boolean exists(Predicate<T> condition) {
        return items.stream().anyMatch(condition);
    }

    public boolean save() {
        return SaveManager.save(this.file, items);
    }

    public void reload() {
        List<T> loadedItems = SaveManager.load(this.file, this.listType);
        this.items.clear();
        this.items.addAll(loadedItems);
    }
}
