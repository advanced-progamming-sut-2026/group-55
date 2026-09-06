package pvz.model.minigame;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Persistent cleared-stage progress for one minigame owned by a User. */
public final class MinigameProgressEntry {
    private String minigameId;
    private Set<String> completedStageIds = new LinkedHashSet<>();

    public MinigameProgressEntry(String minigameId) {
        this.minigameId = MinigameSpec.normalizeId(minigameId);
    }

    public String getMinigameId() {
        return MinigameSpec.normalizeId(minigameId);
    }

    public List<String> getCompletedStageIds() {
        return List.copyOf(completedStages());
    }

    public int getCompletedStageCount() {
        return completedStages().size();
    }

    public boolean isStageCompleted(String stageId) {
        return completedStages().contains(normalizeStageId(stageId));
    }

    /** Records a successful first clear; replaying a cleared stage is idempotent. */
    boolean markStageCompleted(String stageId) {
        return completedStages().add(normalizeStageId(stageId));
    }

    private Set<String> completedStages() {
        if (completedStageIds == null) {
            completedStageIds = new LinkedHashSet<>();
        }
        return completedStageIds;
    }

    private static String normalizeStageId(String stageId) {
        return MinigameSpec.normalizeId(stageId);
    }
}
