package pvz.model.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pvz.model.minigame.MinigameCatalog;
import pvz.model.minigame.MinigameProgressService;
import pvz.model.utils.SaveManager;

class MinigameProgressCompatibilityTest {
    @TempDir
    Path tempDirectory;

    @Test
    void oldSaveWithoutMinigameProgressStartsAtZero() throws Exception {
        Path saveFile = tempDirectory.resolve("old-save.json");
        Files.writeString(
                saveFile,
                """
                [
                  {
                    "username": "old-player",
                    "passwordHash": "hash",
                    "nickname": "Old Player",
                    "email": "old@example.com",
                    "gender": "x"
                  }
                ]
                """
        );
        Type type = new TypeToken<ArrayList<User>>() { }.getType();

        List<User> users = SaveManager.load(saveFile.toFile(), type);
        User user = users.get(0);

        assertNotNull(user.getMinigameProgress());
        assertEquals(0, user.getMinigameProgress()
                .getCompletedStageCount());
    }

    @Test
    void minigameProgressSurvivesUserManagerSaveAndReload() {
        Path saveFile = tempDirectory.resolve("users.json");
        UserManager manager = new UserManager(saveFile.toString());
        User user = new User(
                "player",
                "hash",
                "Player",
                "player@example.com",
                "x"
        );
        manager.add(user);

        MinigameProgressService service = new MinigameProgressService(
                MinigameCatalog.createDefault()
        );
        service.recordSuccessfulCompletion(
                user,
                MinigameCatalog.WALL_NUT_BOWLING,
                1
        );
        service.recordSuccessfulCompletion(
                user,
                MinigameCatalog.WALL_NUT_BOWLING,
                1
        );

        org.junit.jupiter.api.Assertions.assertTrue(manager.save());
        manager.reload();
        User reloaded = manager.find(candidate ->
                candidate.getUsername().equals("player")
        );

        assertNotNull(reloaded);
        assertEquals(
                1,
                reloaded.getMinigameProgress().getCompletedStageCount()
        );
    }
}
