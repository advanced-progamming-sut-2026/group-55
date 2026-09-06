package pvz.model.minigame;

import java.util.Objects;
import pvz.model.account.User;

/**
 * Validated mutation/query boundary for persistent minigame stage progress.
 * Saving remains the caller's transaction responsibility so Phase 7 can
 * persist minigame, quest and battle settlement state atomically.
 */
public final class MinigameProgressService {
    private final MinigameCatalog catalog;

    public MinigameProgressService(MinigameCatalog catalog) {
        this.catalog = Objects.requireNonNull(
                catalog,
                "minigame catalog cannot be null"
        );
    }

    public CompletionResult recordSuccessfulCompletion(
            User user,
            MinigameStageRoute route
    ) {
        Objects.requireNonNull(route, "minigame stage route cannot be null");
        return recordSuccessfulCompletion(
                user,
                route.minigameId(),
                route.stageNumber()
        );
    }

    public CompletionResult recordSuccessfulCompletion(
            User user,
            String minigameId,
            int stageNumber
    ) {
        Objects.requireNonNull(user, "user cannot be null");
        MinigameSpec spec = catalog.require(minigameId);
        String stageId = spec.stageId(stageNumber);

        MinigameStageState state = stageState(user, spec.id(), stageNumber);
        if (state == MinigameStageState.LOCKED) {
            throw new IllegalStateException(
                    "minigame stage is locked: " + stageId
            );
        }

        MinigameProgressEntry progress = user.getMinigameProgress()
                .getOrCreate(spec.id());
        boolean firstStageClear = progress.markStageCompleted(stageId);

        return new CompletionResult(
                firstStageClear,
                firstStageClear,
                progress.getCompletedStageCount(),
                user.getMinigameProgress().getCompletedStageCount()
        );
    }

    public boolean isStageCompleted(
            User user,
            MinigameStageRoute route
    ) {
        Objects.requireNonNull(route, "minigame stage route cannot be null");
        return isStageCompleted(
                user,
                route.minigameId(),
                route.stageNumber()
        );
    }

    public boolean isStageCompleted(
            User user,
            String minigameId,
            int stageNumber
    ) {
        Objects.requireNonNull(user, "user cannot be null");
        MinigameSpec spec = catalog.require(minigameId);
        MinigameProgressEntry progress = user.getMinigameProgress()
                .find(spec.id());
        return progress != null
                && progress.isStageCompleted(spec.stageId(stageNumber));
    }

    public int completedStageCount(User user) {
        Objects.requireNonNull(user, "user cannot be null");
        return user.getMinigameProgress().getCompletedStageCount();
    }

    public MinigameStageState stageState(
            User user,
            MinigameStageRoute route
    ) {
        Objects.requireNonNull(route, "minigame stage route cannot be null");
        return stageState(
                user,
                route.minigameId(),
                route.stageNumber()
        );
    }

    public MinigameStageState stageState(
            User user,
            String minigameId,
            int stageNumber
    ) {
        Objects.requireNonNull(user, "user cannot be null");
        MinigameSpec spec = catalog.require(minigameId);
        String stageId = spec.stageId(stageNumber);

        MinigameProgressEntry progress = user.getMinigameProgress()
                .find(spec.id());
        if (progress != null && progress.isStageCompleted(stageId)) {
            return MinigameStageState.COMPLETED;
        }

        if (stageNumber == 1) {
            return MinigameStageState.AVAILABLE;
        }

        if (progress == null) {
            return MinigameStageState.LOCKED;
        }

        for (int previousStage = 1; previousStage < stageNumber; previousStage++) {
            if (!progress.isStageCompleted(spec.stageId(previousStage))) {
                return MinigameStageState.LOCKED;
            }
        }

        return MinigameStageState.AVAILABLE;
    }

    public int completedStageCount(User user, String minigameId) {
        Objects.requireNonNull(user, "user cannot be null");
        MinigameSpec spec = catalog.require(minigameId);
        MinigameProgressEntry progress = user.getMinigameProgress()
                .find(spec.id());
        return progress == null ? 0 : progress.getCompletedStageCount();
    }

    public record CompletionResult(
            boolean changed,
            boolean firstStageClear,
            int minigameCompletedStageCount,
            int totalCompletedStageCount
    ) { }
}
