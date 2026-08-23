package pvz.model.adventure;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class LevelCatalog {
    private final Map<String, ChapterSpec> chaptersById;
    private final Map<String, LevelSpec> levelsById;

    public LevelCatalog(
            Map<String, ChapterSpec> chaptersById,
            Map<String, LevelSpec> levelsById
    ) {
        this.chaptersById = normalizedCopy(
                chaptersById,
                "chapters"
        );
        this.levelsById = normalizedCopy(levelsById, "levels");
    }

    private static <T> Map<String, T> normalizedCopy(
            Map<String, T> source,
            String field
    ) {
        Objects.requireNonNull(source, field + " cannot be null");
        Map<String, T> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(
                normalize(key),
                Objects.requireNonNull(value, field + " entry cannot be null")
        ));
        return Map.copyOf(result);
    }

    public ChapterSpec findChapter(String id) {
        if (id == null) {
            return null;
        }
        return chaptersById.get(normalize(id));
    }

    public LevelSpec findLevel(String id) {
        if (id == null) {
            return null;
        }
        return levelsById.get(normalize(id));
    }

    public LevelSpec requireLevel(String id) {
        LevelSpec level = findLevel(id);
        if (level == null) {
            throw new IllegalArgumentException("unknown level id: " + id);
        }
        return level;
    }

    public List<ChapterSpec> chapters() {
        return chaptersById.values().stream()
                .sorted(Comparator.comparingInt(ChapterSpec::order))
                .toList();
    }

    public List<LevelSpec> levelsInChapter(String chapterId) {
        String key = normalize(chapterId);
        return levelsById.values().stream()
                .filter(level -> normalize(level.chapterId()).equals(key))
                .sorted(Comparator.comparingInt(LevelSpec::number))
                .toList();
    }

    public LevelSpec findLevelInChapter(
            String chapterId,
            String idOrNumber
    ) {
        if (chapterId == null || idOrNumber == null) {
            return null;
        }
        String target = normalize(idOrNumber);
        return levelsInChapter(chapterId).stream()
                .filter(level -> normalize(level.id()).equals(target)
                        || Integer.toString(level.number()).equals(target))
                .findFirst()
                .orElse(null);
    }

    public LevelSpec previousLevel(LevelSpec level) {
        return adjacentLevel(level, -1);
    }

    public LevelSpec nextLevel(LevelSpec level) {
        return adjacentLevel(level, 1);
    }

    public ChapterSpec nextChapter(String chapterId) {
        ChapterSpec chapter = findChapter(chapterId);
        if (chapter == null) {
            return null;
        }
        List<ChapterSpec> ordered = chapters();
        int index = ordered.indexOf(chapter);
        if (index < 0 || index + 1 >= ordered.size()) {
            return null;
        }
        return ordered.get(index + 1);
    }

    private LevelSpec adjacentLevel(LevelSpec level, int offset) {
        Objects.requireNonNull(level, "level cannot be null");
        List<LevelSpec> ordered = levelsInChapter(level.chapterId());
        int index = ordered.stream()
                .map(LevelSpec::id)
                .map(LevelCatalog::normalize)
                .toList()
                .indexOf(normalize(level.id()));
        int target = index + offset;
        if (index < 0 || target < 0 || target >= ordered.size()) {
            return null;
        }
        return ordered.get(target);
    }

    private static String normalize(String value) {
        return value.strip().toLowerCase(Locale.ROOT);
    }
}
