package pvz.model.account;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class AdventureProgress {
    private Set<String> completedLevelIds = new LinkedHashSet<>();

    public boolean completeLevel(String levelId) {
        return completedLevelIds().add(normalize(levelId));
    }

    public boolean isLevelCompleted(String levelId) {
        return completedLevelIds().contains(normalize(levelId));
    }

    public Set<String> getCompletedLevelIds() {
        return Set.copyOf(completedLevelIds());
    }

    private Set<String> completedLevelIds() {
        if (completedLevelIds == null) {
            completedLevelIds = new LinkedHashSet<>();
        }
        return completedLevelIds;
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
