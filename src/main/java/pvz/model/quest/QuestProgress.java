package pvz.model.quest;

import java.time.LocalDate;
import java.util.Objects;

/** Persistent per-user state for one quest. */
public final class QuestProgress {
    private String questId;
    private int value;
    private QuestState state;
    private LocalDate cycleDate;
    private int baselineValue;
    private int lifetimeCompletionCount;

    public QuestProgress(String questId) {
        this.questId = QuestSpec.normalizeId(questId);
        this.state = QuestState.AVAILABLE;
    }

    public String getQuestId() {
        return QuestSpec.normalizeId(questId);
    }

    public int getValue() {
        return Math.max(0, value);
    }

    public QuestState getState() {
        if (state == null) {
            state = QuestState.AVAILABLE;
        }
        return state;
    }

    public LocalDate getCycleDate() {
        return cycleDate;
    }

    public int getBaselineValue() {
        return Math.max(0, baselineValue);
    }

    /**
     * Number of times this quest has been completed across its lifetime.
     * DAILY quests keep this value when a new daily cycle resets progress.
     *
     * <p>Old saves created before this counter existed can still contain a
     * completed/claimed quest. In that case the current completion is safely
     * migrated as one known completion; older historical cycles cannot be
     * reconstructed and are deliberately not invented.</p>
     */
    public int getLifetimeCompletionCount() {
        if (lifetimeCompletionCount < 0) {
            lifetimeCompletionCount = 0;
        }
        if (lifetimeCompletionCount == 0 && isCompleted()) {
            lifetimeCompletionCount = 1;
        }
        return lifetimeCompletionCount;
    }

    public boolean isCompleted() {
        return getState() == QuestState.COMPLETED
                || getState() == QuestState.CLAIMED;
    }

    public boolean isClaimed() {
        return getState() == QuestState.CLAIMED;
    }

    public void setValue(int value) {
        if (value < 0) {
            throw new IllegalArgumentException(
                    "quest progress cannot be negative"
            );
        }
        this.value = value;
    }

    public void addValue(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException(
                    "quest progress amount cannot be negative"
            );
        }
        long newValue = (long) getValue() + amount;
        value = newValue > Integer.MAX_VALUE
                ? Integer.MAX_VALUE
                : (int) newValue;
    }

    public void markCompleted() {
        QuestState current = getState();
        if (current == QuestState.CLAIMED
                || current == QuestState.COMPLETED) {
            return;
        }
        if (current == QuestState.UNAVAILABLE) {
            throw new IllegalStateException(
                    "unavailable quest cannot be completed"
            );
        }
        state = QuestState.COMPLETED;
        if (lifetimeCompletionCount < Integer.MAX_VALUE) {
            lifetimeCompletionCount++;
        }
    }

    public void markClaimed() {
        if (getState() != QuestState.COMPLETED) {
            throw new IllegalStateException(
                    "only completed quests can be claimed"
            );
        }
        state = QuestState.CLAIMED;
    }

    public void markUnavailable() {
        if (getState() == QuestState.CLAIMED) {
            throw new IllegalStateException(
                    "claimed quest cannot become unavailable"
            );
        }
        state = QuestState.UNAVAILABLE;
    }

    public void activate() {
        if (getState() == QuestState.UNAVAILABLE) {
            state = QuestState.AVAILABLE;
        }
    }

    public void initializeCycle(LocalDate date, int baselineValue) {
        if (cycleDate != null) {
            return;
        }
        cycleDate = Objects.requireNonNull(
                date,
                "quest cycle date cannot be null"
        );
        setBaselineValue(baselineValue);
    }

    public void resetForCycle(LocalDate date) {
        resetForCycle(date, 0);
    }

    public void resetForCycle(LocalDate date, int baselineValue) {
        // Preserve the only completion that can be inferred from a legacy
        // save before clearing the old cycle state.
        getLifetimeCompletionCount();
        cycleDate = Objects.requireNonNull(
                date,
                "quest cycle date cannot be null"
        );
        setBaselineValue(baselineValue);
        value = 0;
        state = QuestState.AVAILABLE;
    }

    private void setBaselineValue(int baselineValue) {
        if (baselineValue < 0) {
            throw new IllegalArgumentException(
                    "quest baseline cannot be negative"
            );
        }
        this.baselineValue = baselineValue;
    }
}
