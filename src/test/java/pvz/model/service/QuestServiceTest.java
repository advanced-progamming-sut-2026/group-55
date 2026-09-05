package pvz.model.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.quest.QuestCategory;
import pvz.model.quest.QuestEvent;
import pvz.model.quest.QuestMetric;
import pvz.model.quest.QuestObjective;
import pvz.model.quest.QuestPriority;
import pvz.model.quest.QuestProgress;
import pvz.model.quest.QuestResetPolicy;
import pvz.model.quest.QuestReward;
import pvz.model.quest.QuestSpec;
import pvz.model.quest.QuestState;
import pvz.model.quest.UserQuestProgressSource;

class QuestServiceTest {
    @TempDir
    Path tempDirectory;

    @Test
    void snapshotQuestSynchronizesAndCompletes() {
        Fixture fixture = fixture(clock("2026-09-05T08:00:00Z"));
        QuestSpec quest = quest(
                "play-three",
                QuestObjective.global(QuestMetric.GAMES_PLAYED, 3),
                QuestResetPolicy.NEVER,
                QuestReward.coins(100)
        );
        fixture.user.setGamesPlayed(3);

        assertTrue(fixture.service.synchronize(fixture.user, quest));

        QuestProgress progress = fixture.user.getQuestLog().find(quest.id());
        assertEquals(3, progress.getValue());
        assertEquals(QuestState.COMPLETED, progress.getState());
    }

    @Test
    void dailySnapshotUsesCycleBaselineInsteadOfLifetimeTotal() {
        Clock dayOne = clock("2026-09-05T08:00:00Z");
        Fixture fixture = fixture(dayOne);
        QuestSpec quest = quest(
                "daily-games",
                QuestObjective.global(QuestMetric.GAMES_PLAYED, 2),
                QuestResetPolicy.DAILY,
                QuestReward.coins(100)
        );
        fixture.user.setGamesPlayed(20);

        fixture.service.synchronize(fixture.user, quest);
        QuestProgress progress = fixture.user.getQuestLog().find(quest.id());
        assertEquals(20, progress.getBaselineValue());
        assertEquals(0, progress.getValue());

        fixture.user.setGamesPlayed(21);
        fixture.service.synchronize(fixture.user, quest);
        assertEquals(1, progress.getValue());
        assertEquals(QuestState.AVAILABLE, progress.getState());
    }

    @Test
    void dailyQuestResetsClaimAndProgressOnNextDate() {
        Fixture first = fixture(clock("2026-09-05T08:00:00Z"));
        QuestSpec quest = quest(
                "daily-battle",
                QuestObjective.global(QuestMetric.BATTLE_COMPLETED, 1),
                QuestResetPolicy.DAILY,
                QuestReward.coins(100)
        );

        first.service.recordEvent(
                first.user,
                quest,
                QuestEvent.battleCompleted()
        );
        assertTrue(first.service.claim(first.user, quest).success());
        assertEquals(
                QuestState.CLAIMED,
                first.user.getQuestLog().find(quest.id()).getState()
        );

        QuestService nextDayService = service(
                first.manager,
                clock("2026-09-06T08:00:00Z")
        );
        assertTrue(nextDayService.synchronize(first.user, quest));

        QuestProgress progress = first.user.getQuestLog().find(quest.id());
        assertEquals(QuestState.AVAILABLE, progress.getState());
        assertEquals(0, progress.getValue());
    }

    @Test
    void eventQuestMatchesSubjectAndIgnoresUnavailableQuest() {
        Fixture fixture = fixture(clock("2026-09-05T08:00:00Z"));
        QuestSpec quest = quest(
                "kill-cones",
                QuestObjective.forSubject(
                        QuestMetric.ZOMBIE_KILLED,
                        "conehead",
                        2
                ),
                QuestResetPolicy.NEVER,
                QuestReward.coins(100)
        );

        assertFalse(fixture.service.recordEvent(
                fixture.user,
                quest,
                QuestEvent.zombieKilled("default")
        ));
        assertTrue(fixture.service.recordEvent(
                fixture.user,
                quest,
                QuestEvent.zombieKilled("ConeHead")
        ));
        assertEquals(
                1,
                fixture.user.getQuestLog().find(quest.id()).getValue()
        );

        fixture.service.setAvailable(fixture.user, quest, false);
        fixture.service.recordEvent(
                fixture.user,
                quest,
                QuestEvent.zombieKilled("conehead")
        );
        assertEquals(
                1,
                fixture.user.getQuestLog().find(quest.id()).getValue()
        );
    }

    @Test
    void claimPaysOnceAndPersistsClaimedState() {
        Fixture fixture = fixture(clock("2026-09-05T08:00:00Z"));
        QuestSpec quest = quest(
                "first-clear",
                QuestObjective.global(QuestMetric.CLEARED_STAGES, 1),
                QuestResetPolicy.NEVER,
                QuestReward.coins(500),
                QuestReward.diamonds(3),
                QuestReward.levelUnlock("egypt-special-1")
        );
        fixture.user.setClearedStages(1);
        int coinsBefore = fixture.user.getCoins();
        int diamondsBefore = fixture.user.getDiamonds();

        QuestService.ClaimResult first = fixture.service.claim(
                fixture.user,
                quest
        );
        QuestService.ClaimResult duplicate = fixture.service.claim(
                fixture.user,
                quest
        );

        assertEquals(QuestService.ClaimStatus.SUCCESS, first.status());
        assertEquals(
                QuestService.ClaimStatus.ALREADY_CLAIMED,
                duplicate.status()
        );
        assertEquals(coinsBefore + 500, fixture.user.getCoins());
        assertEquals(diamondsBefore + 3, fixture.user.getDiamonds());

        fixture.manager.reload();
        User loaded = fixture.manager.find(user ->
                user.getUsername().equals("player")
        );
        assertNotNull(loaded);
        assertEquals(
                QuestState.CLAIMED,
                loaded.getQuestLog().find(quest.id()).getState()
        );
        assertTrue(loaded.getAdventureProgress()
                .isLevelRewardUnlocked("egypt-special-1"));
        assertEquals(coinsBefore + 500, loaded.getCoins());
    }

    @Test
    void claimDoesNotChangeStateWhenRewardCannotBeApplied() {
        Fixture fixture = fixture(clock("2026-09-05T08:00:00Z"));
        QuestSpec quest = quest(
                "bad-seed-reward",
                QuestObjective.global(QuestMetric.GAMES_PLAYED, 1),
                QuestResetPolicy.NEVER,
                QuestReward.seedPackets("Never Owned Plant", 10)
        );
        fixture.user.setGamesPlayed(1);

        QuestService.ClaimResult result = fixture.service.claim(
                fixture.user,
                quest
        );

        assertEquals(
                QuestService.ClaimStatus.REWARD_BLOCKED,
                result.status()
        );
        assertEquals(
                QuestState.COMPLETED,
                fixture.user.getQuestLog().find(quest.id()).getState()
        );
    }


    @Test
    void futureQuestStartsUnavailableUntilExplicitlyActivated() {
        Fixture fixture = fixture(clock("2026-09-05T08:00:00Z"));
        QuestSpec quest = new QuestSpec(
                "future-kills",
                "Future Kills",
                "Future battle hook",
                QuestCategory.CHALLENGE,
                QuestPriority.HIGH,
                QuestObjective.global(QuestMetric.ZOMBIE_KILLED, 2),
                List.of(QuestReward.diamonds(5)),
                QuestResetPolicy.NEVER,
                false
        );

        assertTrue(fixture.service.recordEvent(
                fixture.user,
                quest,
                QuestEvent.zombieKilled("default")
        ));
        QuestProgress progress = fixture.user.getQuestLog().find(quest.id());
        assertEquals(QuestState.UNAVAILABLE, progress.getState());
        assertEquals(0, progress.getValue());

        assertTrue(fixture.service.setAvailable(fixture.user, quest, true));
        assertTrue(fixture.service.recordEvent(
                fixture.user,
                quest,
                QuestEvent.zombieKilled("default")
        ));
        assertEquals(QuestState.AVAILABLE, progress.getState());
        assertEquals(1, progress.getValue());
    }

    @Test
    void newlyEnabledDefaultQuestMigratesOldUnavailableProgress() {
        Fixture fixture = fixture(clock("2026-09-05T08:00:00Z"));
        QuestSpec oldSpec = new QuestSpec(
                "telemetry-upgrade",
                "Telemetry",
                "Old version",
                QuestCategory.CHALLENGE,
                QuestPriority.HIGH,
                QuestObjective.global(QuestMetric.ZOMBIE_KILLED, 2),
                List.of(QuestReward.diamonds(5)),
                QuestResetPolicy.NEVER,
                false
        );
        fixture.service.recordEvent(
                fixture.user,
                oldSpec,
                QuestEvent.zombieKilled("default")
        );
        assertEquals(
                QuestState.UNAVAILABLE,
                fixture.user.getQuestLog().find(oldSpec.id()).getState()
        );

        QuestSpec newSpec = new QuestSpec(
                oldSpec.id(),
                oldSpec.name(),
                "Hook is now implemented",
                oldSpec.category(),
                oldSpec.priority(),
                oldSpec.objective(),
                oldSpec.rewards(),
                oldSpec.resetPolicy(),
                true
        );

        QuestService.SyncResult result = fixture.service.synchronizeAndSave(
                fixture.user,
                List.of(newSpec)
        );

        assertTrue(result.saved());
        assertEquals(
                QuestState.AVAILABLE,
                fixture.user.getQuestLog().find(newSpec.id()).getState()
        );
    }

    @Test
    void eventBatchAdvancesMatchingQuestWithoutSavingPerEvent() {
        Fixture fixture = fixture(clock("2026-09-05T08:00:00Z"));
        QuestSpec quest = quest(
                "battle-sun",
                QuestObjective.global(QuestMetric.SUN_SPENT, 150),
                QuestResetPolicy.NEVER,
                QuestReward.coins(100)
        );

        int changes = fixture.service.recordEvents(
                fixture.user,
                List.of(quest),
                List.of(
                        QuestEvent.sunSpent(50),
                        QuestEvent.sunSpent(100)
                )
        );

        assertEquals(2, changes);
        QuestProgress progress = fixture.user.getQuestLog().find(quest.id());
        assertEquals(150, progress.getValue());
        assertEquals(QuestState.COMPLETED, progress.getState());
    }

    private Fixture fixture(Clock clock) {
        Path file = tempDirectory.resolve(
                "users-" + System.nanoTime() + ".json"
        );
        UserManager manager = new UserManager(file.toString());
        User user = new User(
                "player",
                "hash",
                "Player",
                "player@example.com",
                "x"
        );
        manager.add(user);
        manager.save();
        return new Fixture(manager, user, service(manager, clock));
    }

    private QuestService service(UserManager manager, Clock clock) {
        return new QuestService(
                manager,
                new UserQuestProgressSource(),
                new QuestRewardService(),
                clock
        );
    }

    private Clock clock(String instant) {
        return Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
    }

    private QuestSpec quest(
            String id,
            QuestObjective objective,
            QuestResetPolicy resetPolicy,
            QuestReward... rewards
    ) {
        return new QuestSpec(
                id,
                id,
                "Quest description",
                resetPolicy == QuestResetPolicy.DAILY
                        ? QuestCategory.DAILY
                        : QuestCategory.ADVENTURE,
                QuestPriority.HIGH,
                objective,
                List.of(rewards),
                resetPolicy
        );
    }

    private record Fixture(
            UserManager manager,
            User user,
            QuestService service
    ) {
    }
}
