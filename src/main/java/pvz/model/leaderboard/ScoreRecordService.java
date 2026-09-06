package pvz.model.leaderboard;

import java.util.Objects;
import pvz.model.account.User;
import pvz.model.account.UserManager;

/** Local score-mode integration point. Call once a valid run has finished.
 * Future server gameplay must validate and persist scores on the server.
 */
public final class ScoreRecordService {
    private final UserManager users;

    public ScoreRecordService(UserManager users) {
        this.users = Objects.requireNonNull(users);
    }

    public Result recordFinishedRun(User user, int score) {
        Objects.requireNonNull(user);
        if (score < 0) throw new IllegalArgumentException("score cannot be negative");
        if (users.getAll().stream().noneMatch(candidate -> candidate == user)) {
            throw new IllegalArgumentException("user must belong to this manager");
        }
        int previous = user.getMaxMewPoint();
        if (score <= Math.max(0, previous)) return Result.UNCHANGED;
        user.setMaxMewPoint(score);
        try {
            if (users.save()) return Result.UPDATED;
        } catch (RuntimeException exception) {
            user.setMaxMewPoint(previous);
            throw exception;
        }
        user.setMaxMewPoint(previous);
        return Result.SAVE_FAILED;
    }

    public enum Result { UPDATED, UNCHANGED, SAVE_FAILED }
}
