package pvz.model.minigame;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Persistent per-user minigame stage-clear history. */
public final class MinigameProgress {
    private Map<String, MinigameProgressEntry> progressByMinigameId =
            new LinkedHashMap<>();

    public MinigameProgressEntry find(String minigameId) {
        if (minigameId == null || minigameId.isBlank()) {
            return null;
        }
        return progressMap().get(MinigameSpec.normalizeId(minigameId));
    }

    public List<MinigameProgressEntry> getAll() {
        return List.copyOf(new ArrayList<>(progressMap().values()));
    }

    /** Number of distinct minigame stages successfully cleared by the user. */
    public int getCompletedStageCount() {
        long total = 0;
        for (MinigameProgressEntry entry : progressMap().values()) {
            total += entry.getCompletedStageCount();
            if (total >= Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
        }
        return (int) total;
    }

    MinigameProgressEntry getOrCreate(String minigameId) {
        String normalized = MinigameSpec.normalizeId(minigameId);
        MinigameProgressEntry existing = progressMap().get(normalized);
        if (existing != null) {
            return existing;
        }

        MinigameProgressEntry created = new MinigameProgressEntry(normalized);
        progressMap().put(normalized, created);
        return created;
    }

    private Map<String, MinigameProgressEntry> progressMap() {
        if (progressByMinigameId == null) {
            progressByMinigameId = new LinkedHashMap<>();
        }
        return progressByMinigameId;
    }
}
