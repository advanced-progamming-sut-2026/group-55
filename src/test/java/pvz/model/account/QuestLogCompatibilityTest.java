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
import pvz.model.quest.QuestLog;
import pvz.model.utils.SaveManager;

class QuestLogCompatibilityTest {
    @TempDir
    Path tempDirectory;

    @Test
    void initializesQuestLogWhenOldSaveHasNoQuestField()
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
                    "gender": "x"
                  }
                ]
                """
        );
        Type type = new TypeToken<ArrayList<User>>() { }.getType();

        List<User> users = SaveManager.load(saveFile.toFile(), type);
        QuestLog questLog = users.get(0).getQuestLog();

        assertNotNull(questLog);
        assertTrue(questLog.getAll().isEmpty());
    }
}
