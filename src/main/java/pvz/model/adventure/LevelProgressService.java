package pvz.model.adventure;

import java.util.List;
import java.util.Objects;
import pvz.model.account.AdventureProgress;
import pvz.model.account.User;

public final class LevelProgressService {
    private final LevelCatalog catalog;

    public LevelProgressService(LevelCatalog catalog) {
        this.catalog = Objects.requireNonNull(
                catalog,
                "level catalog cannot be null"
        );
    }

    public LevelState state(User user, LevelSpec level) {
        Objects.requireNonNull(user, "user cannot be null");
        Objects.requireNonNull(level, "level cannot be null");

        if (user.getAdventureProgress().isLevelCompleted(level.id())) {
            return LevelState.COMPLETED;
        }
        return isUnlocked(user, level)
                ? LevelState.AVAILABLE
                : LevelState.LOCKED;
    }

    public boolean isUnlocked(User user, LevelSpec level) {
        Objects.requireNonNull(user, "user cannot be null");
        Objects.requireNonNull(level, "level cannot be null");

        if (!user.isChapterUnlocked(level.chapterId())) {
            return false;
        }
        LevelSpec previous = catalog.previousLevel(level);
        return previous == null
                || user.getAdventureProgress()
                        .isLevelCompleted(previous.id());
    }

    public CompletionResult completeLevel(User user, String levelId) {
        Objects.requireNonNull(user, "user cannot be null");
        LevelSpec level = catalog.requireLevel(levelId);
        AdventureProgress progress = user.getAdventureProgress();

        if (progress.isLevelCompleted(level.id())) {
            return CompletionResult.unchanged();
        }
        if (!isUnlocked(user, level)) {
            throw new IllegalStateException(
                    "cannot complete a locked level: " + level.id()
            );
        }
        progress.completeLevel(level.id());

        if (user.getClearedStages() < Integer.MAX_VALUE) {
            user.setClearedStages(user.getClearedStages() + 1);
        }

        LevelSpec nextLevel = catalog.nextLevel(level);
        if (nextLevel != null) {
            return new CompletionResult(true, nextLevel.id(), null);
        }

        ChapterSpec nextChapter = unlockNextChapterIfReady(
                user,
                level.chapterId()
        );
        return new CompletionResult(
                true,
                firstLevelId(nextChapter),
                nextChapter == null ? null : nextChapter.id()
        );
    }

    private ChapterSpec unlockNextChapterIfReady(
            User user,
            String chapterId
    ) {
        List<LevelSpec> chapterLevels = catalog.levelsInChapter(chapterId);
        boolean chapterCompleted = chapterLevels.stream().allMatch(
                level -> user.getAdventureProgress()
                        .isLevelCompleted(level.id())
        );
        if (!chapterCompleted) {
            return null;
        }

        ChapterSpec nextChapter = catalog.nextChapter(chapterId);
        if (nextChapter == null
                || catalog.levelsInChapter(nextChapter.id()).isEmpty()) {
            return null;
        }
        user.unlockChapter(nextChapter.id());
        return nextChapter;
    }

    private String firstLevelId(ChapterSpec chapter) {
        if (chapter == null) {
            return null;
        }
        List<LevelSpec> levels = catalog.levelsInChapter(chapter.id());
        return levels.isEmpty() ? null : levels.get(0).id();
    }

    public enum LevelState {
        LOCKED,
        AVAILABLE,
        COMPLETED
    }

    public record CompletionResult(
            boolean newlyCompleted,
            String unlockedLevelId,
            String unlockedChapterId
    ) {
        private static CompletionResult unchanged() {
            return new CompletionResult(false, null, null);
        }
    }
}
