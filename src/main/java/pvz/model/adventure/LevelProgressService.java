package pvz.model.adventure;

import java.util.ArrayList;
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

        AdventureProgress progress = user.getAdventureProgress();
        if (progress.isLevelRewardUnlocked(level.id())) {
            return true;
        }
        if (!isSequentialChapterAccessible(user, level.chapterId())) {
            return false;
        }
        LevelSpec previous = catalog.previousLevel(level);
        return previous == null || progress.isLevelCompleted(previous.id());
    }

    /**
     * Returns whether the chapter can be entered from the Adventure menu.
     *
     * <p>Stored chapter unlocks remain authoritative for compatibility, but
     * access can also be derived from progress. This keeps old saves usable
     * when new chapter data is added after the save was created and prevents
     * explicit level rewards from becoming unreachable behind a chapter
     * gate.</p>
     */
    public boolean isChapterAccessible(User user, String chapterId) {
        Objects.requireNonNull(user, "user cannot be null");
        ChapterSpec chapter = catalog.findChapter(chapterId);
        if (chapter == null) {
            return false;
        }
        List<LevelSpec> levels = catalog.levelsInChapter(chapter.id());
        if (levels.isEmpty()) {
            return false;
        }
        if (isSequentialChapterAccessible(user, chapter.id())) {
            return true;
        }
        return hasDirectAccessInsideChapter(user, levels);
    }

    public boolean isChapterCompleted(User user, String chapterId) {
        Objects.requireNonNull(user, "user cannot be null");
        List<LevelSpec> levels = catalog.levelsInChapter(chapterId);
        if (levels.isEmpty()) {
            return false;
        }
        return levels.stream().allMatch(
                level -> user.getAdventureProgress()
                        .isLevelCompleted(level.id())
        );
    }

    /**
     * Persists chapter unlocks that are already implied by the user's saved
     * level progress and the current catalog.
     *
     * <p>This method only adds access; it never relocks content. It is safe to
     * run when opening the Adventure menu after game data changes.</p>
     */
    public ReconciliationResult reconcileProgress(User user) {
        Objects.requireNonNull(user, "user cannot be null");
        List<String> unlockedChapters = new ArrayList<>();

        for (ChapterSpec chapter : catalog.chapters()) {
            if (user.isChapterUnlocked(chapter.id())) {
                continue;
            }
            if (!isSequentialChapterAccessible(user, chapter.id())) {
                continue;
            }
            user.unlockChapter(chapter.id());
            unlockedChapters.add(chapter.id());
        }

        return new ReconciliationResult(unlockedChapters);
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

        LevelSpec nextLevel = catalog.nextLevel(level);
        boolean nextLevelWasUnlocked = nextLevel != null
                && isUnlocked(user, nextLevel);

        progress.completeLevel(level.id());
        incrementClearedStages(user);

        ReconciliationResult reconciliation = reconcileProgress(user);
        String unlockedChapterId = firstUnlockedChapterAfter(
                reconciliation,
                level.chapterId()
        );
        String unlockedLevelId = newlyUnlockedLevelId(
                user,
                nextLevel,
                nextLevelWasUnlocked,
                unlockedChapterId
        );

        addLevelUnlockNews(user, unlockedLevelId);
        return new CompletionResult(
                true,
                unlockedLevelId,
                unlockedChapterId
        );
    }

    private void incrementClearedStages(User user) {
        if (user.getClearedStages() < Integer.MAX_VALUE) {
            user.setClearedStages(user.getClearedStages() + 1);
        }
    }

    private String newlyUnlockedLevelId(
            User user,
            LevelSpec nextLevel,
            boolean nextLevelWasUnlocked,
            String unlockedChapterId
    ) {
        if (nextLevel != null
                && !nextLevelWasUnlocked
                && isUnlocked(user, nextLevel)) {
            return nextLevel.id();
        }
        ChapterSpec chapter = catalog.findChapter(unlockedChapterId);
        return firstLevelId(chapter);
    }

    private String firstUnlockedChapterAfter(
            ReconciliationResult reconciliation,
            String completedChapterId
    ) {
        ChapterSpec expectedNext = catalog.nextChapter(completedChapterId);
        if (expectedNext == null) {
            return null;
        }
        return reconciliation.unlockedChapterIds().stream()
                .filter(id -> id.equalsIgnoreCase(expectedNext.id()))
                .findFirst()
                .orElse(null);
    }

    private boolean isSequentialChapterAccessible(
            User user,
            String chapterId
    ) {
        ChapterSpec chapter = catalog.findChapter(chapterId);
        if (chapter == null
                || catalog.levelsInChapter(chapter.id()).isEmpty()) {
            return false;
        }
        if (user.isChapterUnlocked(chapter.id())) {
            return true;
        }
        ChapterSpec previous = previousChapter(chapter);
        return previous == null
                || (isSequentialChapterAccessible(user, previous.id())
                        && isChapterCompleted(user, previous.id()));
    }

    private boolean hasDirectAccessInsideChapter(
            User user,
            List<LevelSpec> levels
    ) {
        AdventureProgress progress = user.getAdventureProgress();
        return levels.stream().anyMatch(level ->
                progress.isLevelCompleted(level.id())
                        || progress.isLevelRewardUnlocked(level.id())
        );
    }

    private ChapterSpec previousChapter(ChapterSpec chapter) {
        List<ChapterSpec> chapters = catalog.chapters();
        int index = chapters.indexOf(chapter);
        if (index <= 0) {
            return null;
        }
        return chapters.get(index - 1);
    }

    private void addLevelUnlockNews(User user, String levelId) {
        LevelSpec level = catalog.findLevel(levelId);
        if (level == null) {
            return;
        }
        user.addNews(
                "Level Unlocked",
                level.name() + " is now available!"
        );
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

    public record ReconciliationResult(List<String> unlockedChapterIds) {
        public ReconciliationResult {
            unlockedChapterIds = List.copyOf(unlockedChapterIds);
        }

        public boolean changed() {
            return !unlockedChapterIds.isEmpty();
        }
    }
}
