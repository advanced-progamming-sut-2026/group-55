package pvz.model.leaderboard;

import java.util.Comparator;
import java.time.Clock;
import java.time.LocalDate;
import pvz.model.quest.UserQuestProgressSource;
import pvz.model.quest.QuestState;
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
    private final Clock clock;
    private final UserQuestProgressSource progressSource = new UserQuestProgressSource();

    public LocalLeaderboardDataSource(
            UserManager userManager,
            LevelCatalog levelCatalog,
            QuestCatalog questCatalog
    ) {
        this(userManager, levelCatalog, questCatalog, Clock.systemDefaultZone());
    }

    public LocalLeaderboardDataSource(UserManager userManager,
            LevelCatalog levelCatalog, QuestCatalog questCatalog, Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
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
            int recorded = progress == null ? 0 : progress.getLifetimeCompletionCount();
            completed += recorded;
            if (hasUnrecordedCompletion(user, spec, progress, recorded)) {
                completed++;
            }
            if (completed >= Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
        }
        return (int) completed;
    }
    /** Read-only projection: never resets another player's daily cycle or pays rewards. */
    private boolean hasUnrecordedCompletion(User user, QuestSpec spec,
            QuestProgress progress, int recorded) {
        if (!progressSource.supports(spec.objective().metric())) return false;
        if (progress != null && (progress.isCompleted() || progress.isClaimed())) return false;
        if ((progress == null || progress.getState() == QuestState.UNAVAILABLE)
                && !spec.initiallyAvailable()) return false;
        int value = Math.max(0, progressSource.currentValue(user, spec.objective()));
        if (spec.resetPolicy() == QuestResetPolicy.DAILY) {
            // Missing/expired baselines cannot tell us when the activity happened.
            if (progress == null || !LocalDate.now(clock).equals(progress.getCycleDate())) {
                return false;
            }
            value = Math.max(0, value - progress.getBaselineValue());
        } else if (recorded > 0) {
            return false;
        }
        return value >= spec.objective().target();
    }

}
