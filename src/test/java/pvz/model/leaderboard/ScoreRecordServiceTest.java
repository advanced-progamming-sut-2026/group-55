package pvz.model.leaderboard;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pvz.model.account.User;
import pvz.model.account.UserManager;

class ScoreRecordServiceTest {
    @TempDir Path directory;

    @Test void keepsOnlyBestScoreAndPersistsIt() {
        var users = new UserManager(directory.resolve("users.json").toString());
        var user = new User("player", "hash", "Player", "p@example.com", "x");
        users.add(user);
        var service = new ScoreRecordService(users);
        assertEquals(ScoreRecordService.Result.UPDATED, service.recordFinishedRun(user, 90));
        assertEquals(ScoreRecordService.Result.UNCHANGED, service.recordFinishedRun(user, 20));
        assertEquals(ScoreRecordService.Result.UNCHANGED, service.recordFinishedRun(user, 90));
        assertThrows(IllegalArgumentException.class, () -> service.recordFinishedRun(user, -1));
        users.reload();
        assertEquals(90, users.getAll().get(0).getMaxMewPoint());
    }

    @Test void failedSaveRestoresSameUserObject() {
        var users = new UserManager(directory.resolve("fail.json").toString()) {
            @Override public boolean save() { return false; }
        };
        var user = new User("player", "hash", "Player", "p@example.com", "x");
        user.setMaxMewPoint(40);
        users.add(user);
        assertEquals(ScoreRecordService.Result.SAVE_FAILED,
                new ScoreRecordService(users).recordFinishedRun(user, 90));
        assertEquals(40, user.getMaxMewPoint());
    }
}
