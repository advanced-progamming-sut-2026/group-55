package pvz.model.account;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class AdventureProgress {
    private Set<String> completedLevelIds = new LinkedHashSet<>();
    private Set<String> rewardUnlockedLevelIds = new LinkedHashSet<>();

    public boolean completeLevel(String levelId) {
        return completedLevelIds().add(normalize(levelId));
    }

    public boolean isLevelCompleted(String levelId) {
        return completedLevelIds().contains(normalize(levelId));
    }

    public Set<String> getCompletedLevelIds() {
        return Set.copyOf(completedLevelIds());
    }

    /**
     * Stores level unlocks granted outside normal sequential Adventure
     * progression (for example, Travel Log rewards).
     */
    public boolean unlockLevel(String levelId) {
        return rewardUnlockedLevelIds().add(normalize(levelId));
    }

    public boolean isLevelRewardUnlocked(String levelId) {
        return rewardUnlockedLevelIds().contains(normalize(levelId));
    }

    public Set<String> getRewardUnlockedLevelIds() {
        return Set.copyOf(rewardUnlockedLevelIds());
    }

    private Set<String> completedLevelIds() {
        if (completedLevelIds == null) {
            completedLevelIds = new LinkedHashSet<>();
        }
        return completedLevelIds;
    }

    private Set<String> rewardUnlockedLevelIds() {
        if (rewardUnlockedLevelIds == null) {
            rewardUnlockedLevelIds = new LinkedHashSet<>();
        }
        return rewardUnlockedLevelIds;
    }

    private static String normalize(String value) {
        Objects.requireNonNull(value, "level id cannot be null");
        String normalized = value.strip().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("level id cannot be blank");
        }
        return normalized;
    }
}
