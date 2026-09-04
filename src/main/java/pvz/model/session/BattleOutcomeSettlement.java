package pvz.model.session;

import java.util.Objects;
import pvz.model.account.User;
import pvz.model.adventure.LevelProgressService;
import pvz.model.core.BattleResources;

/** Applies the persistent outcome of one finished battle exactly once. */
public final class BattleOutcomeSettlement {
    private final BattleRewardSettlement rewardSettlement;
    private final LevelProgressService progressService;

    public BattleOutcomeSettlement(LevelProgressService progressService) {
        this(
                new BattleRewardSettlement(),
                progressService
        );
    }

    BattleOutcomeSettlement(
            BattleRewardSettlement rewardSettlement,
            LevelProgressService progressService
    ) {
        this.rewardSettlement = Objects.requireNonNull(
                rewardSettlement,
                "reward settlement cannot be null"
        );
        this.progressService = Objects.requireNonNull(
                progressService,
                "progress service cannot be null"
        );
    }

    public Result settle(
            GameSessionStatus status,
            String levelId,
            BattleResources resources,
            User user,
            boolean returnPlantFood
    ) {
        validateStatus(status);
        Objects.requireNonNull(levelId, "level id cannot be null");
        Objects.requireNonNull(resources, "battle resources cannot be null");
        Objects.requireNonNull(user, "user cannot be null");

        /*
         * Validate before progress is touched. The wallet transfer flag is
         * the authoritative exactly-once guard for this battle attempt.
         */
        rewardSettlement.validate(resources, user, returnPlantFood);

        LevelProgressService.CompletionResult progress = null;
        if (status == GameSessionStatus.WON) {
            progress = progressService.completeLevel(user, levelId);
        }

        BattleRewardSettlement.Result rewards = rewardSettlement.settle(
                resources,
                user,
                returnPlantFood
        );

        return new Result(
                rewards,
                progress != null && progress.newlyCompleted(),
                progress == null ? null : progress.unlockedLevelId(),
                progress == null ? null : progress.unlockedChapterId()
        );
    }

    public int returnRemainingPlantFood(
            BattleResources resources,
            User user
    ) {
        return rewardSettlement.returnRemainingPlantFood(resources, user);
    }

    private void validateStatus(GameSessionStatus status) {
        Objects.requireNonNull(status, "session status cannot be null");
        if (status == GameSessionStatus.CREATED
                || status == GameSessionStatus.RUNNING) {
            throw new IllegalStateException(
                    "battle outcome cannot be settled before the battle ends"
            );
        }
    }

    public record Result(
            BattleRewardSettlement.Result rewards,
            boolean newlyCompleted,
            String unlockedLevelId,
            String unlockedChapterId
    ) {
        public Result {
            Objects.requireNonNull(rewards, "rewards cannot be null");
        }
    }
}
