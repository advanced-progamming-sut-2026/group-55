package pvz.model.account;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pvz.model.utils.SaveManager;

class AdventureProgressCompatibilityTest {
    @TempDir
    Path tempDirectory;

    @Test
    void initializesProgressWhenLoadingAnOldSaveWithoutProgressField()
            throws Exception {
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
                    "gender": "x",
                    "unlockedChapters": ["ancient-egypt"]
                  }
                ]
                """
        );
        Type type = new TypeToken<ArrayList<User>>() { }.getType();

        List<User> users = SaveManager.load(saveFile.toFile(), type);
        AdventureProgress progress = users.get(0).getAdventureProgress();

        assertNotNull(progress);
        assertTrue(progress.getCompletedLevelIds().isEmpty());
        assertTrue(progress.getRewardUnlockedLevelIds().isEmpty());
    }
}
