package pvz.model.quest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Persistent quest state container owned by a User. */
public final class QuestLog {
    private Map<String, QuestProgress> progressByQuestId =
            new LinkedHashMap<>();

    public QuestProgress getOrCreate(String questId) {
        String normalized = QuestSpec.normalizeId(questId);
        QuestProgress existing = progressMap().get(normalized);
        if (existing != null) {
            return existing;
        }

        QuestProgress created = new QuestProgress(normalized);
        progressMap().put(normalized, created);
        return created;
    }

    public QuestProgress find(String questId) {
        return progressMap().get(QuestSpec.normalizeId(questId));
    }

    public List<QuestProgress> getAll() {
        return List.copyOf(new ArrayList<>(progressMap().values()));
    }

    public int completedCount() {
        return (int) progressMap().values().stream()
                .filter(QuestProgress::isCompleted)
                .count();
    }

    public int claimedCount() {
        return (int) progressMap().values().stream()
                .filter(QuestProgress::isClaimed)
                .count();
    }

    private Map<String, QuestProgress> progressMap() {
        if (progressByQuestId == null) {
            progressByQuestId = new LinkedHashMap<>();
        }
        return progressByQuestId;
    }
}
