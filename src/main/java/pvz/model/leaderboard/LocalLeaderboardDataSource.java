package pvz.model.leaderboard;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.adventure.ChapterSpec;
import pvz.model.adventure.LevelCatalog;
import pvz.model.adventure.LevelSpec;
import pvz.model.quest.QuestCatalog;
import pvz.model.quest.QuestProgress;
import pvz.model.quest.QuestResetPolicy;
import pvz.model.quest.QuestSpec;

/** Local Phase-2 leaderboard source backed by the registered users save. */
public final class LocalLeaderboardDataSource
        implements LeaderboardDataSource {
    private static final Comparator<LevelSpec> ADVENTURE_ORDER =
            Comparator.comparingInt((LevelSpec level) -> level.number());

    private final UserManager userManager;
    private final LevelCatalog levelCatalog;
    private final QuestCatalog questCatalog;

    public LocalLeaderboardDataSource(
            UserManager userManager,
            LevelCatalog levelCatalog,
            QuestCatalog questCatalog
    ) {
        this.userManager = Objects.requireNonNull(
                userManager,
                "user manager cannot be null"
        );
        this.levelCatalog = Objects.requireNonNull(
                levelCatalog,
                "level catalog cannot be null"
        );
        this.questCatalog = Objects.requireNonNull(
                questCatalog,
                "quest catalog cannot be null"
        );
    }

    @Override
    public List<LeaderboardEntry> loadEntries() {
        return userManager.getAll().stream()
                .map(this::entryFor)
                .toList();
    }

    private LeaderboardEntry entryFor(User user) {
        return new LeaderboardEntry(
                user.getUsername(),
                user.getNickname(),
                latestAdventureStanding(user),
                user.getMinigameProgress().getCompletedStageCount(),
                completedQuestCount(user, QuestResetPolicy.DAILY),
                completedQuestCount(user, QuestResetPolicy.NEVER),
                Math.max(0, user.getMaxMewPoint())
        );
    }

    private AdventureStanding latestAdventureStanding(User user) {
        LevelSpec latestLevel = null;
        ChapterSpec latestChapter = null;

        for (String levelId : user.getAdventureProgress()
                .getCompletedLevelIds()) {
            LevelSpec level = levelCatalog.findLevel(levelId);
            if (level == null) {
                continue;
            }
            ChapterSpec chapter = levelCatalog.findChapter(level.chapterId());
            if (chapter == null) {
                continue;
            }

            if (latestLevel == null
                    || isAfter(chapter, level, latestChapter, latestLevel)) {
                latestChapter = chapter;
                latestLevel = level;
            }
        }

        if (latestLevel == null || latestChapter == null) {
            return AdventureStanding.none();
        }

        return new AdventureStanding(
                latestChapter.id(),
                latestChapter.name(),
                latestChapter.order(),
                latestLevel.id(),
                latestLevel.name(),
                latestLevel.number()
        );
    }

    private boolean isAfter(
            ChapterSpec candidateChapter,
            LevelSpec candidateLevel,
            ChapterSpec currentChapter,
            LevelSpec currentLevel
    ) {
        if (currentChapter == null || currentLevel == null) {
            return true;
        }
        if (candidateChapter.order() != currentChapter.order()) {
            return candidateChapter.order() > currentChapter.order();
        }
        return ADVENTURE_ORDER.compare(candidateLevel, currentLevel) > 0;
    }

    /** Lifetime quest completions used by the leaderboard. */
    private int completedQuestCount(
            User user,
            QuestResetPolicy resetPolicy
    ) {
        long completed = 0;
        for (QuestSpec spec : questCatalog.all()) {
            if (spec.resetPolicy() != resetPolicy) {
                continue;
            }
            QuestProgress progress = user.getQuestLog().find(spec.id());
            if (progress == null) {
                continue;
            }
            completed += progress.getLifetimeCompletionCount();
            if (completed >= Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
        }
        return (int) completed;
    }
}
