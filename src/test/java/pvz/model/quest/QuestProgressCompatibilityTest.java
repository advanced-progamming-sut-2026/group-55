package pvz.model.quest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import pvz.model.utils.SaveManager;

class QuestProgressCompatibilityTest {
    @Test
    void oldCompletedQuestWithoutLifetimeCounterMigratesOneKnownCompletion()
            throws Exception {
        File file = Files.createTempFile(
                "quest-progress-compatibility",
                ".json"
        ).toFile();

        try {
            Files.writeString(
                    file.toPath(),
                    """
                    [
                      {
                        "questId": "daily-play-one",
                        "value": 1,
                        "state": "CLAIMED"
                      }
                    ]
                    """
            );

            Type listType = new TypeToken<List<QuestProgress>>() {
            }.getType();
            List<QuestProgress> loaded = SaveManager.load(file, listType);
            QuestProgress progress = loaded.getFirst();

            assertEquals(1, progress.getLifetimeCompletionCount());

            progress.resetForCycle(LocalDate.of(2026, 9, 6));

            assertEquals(1, progress.getLifetimeCompletionCount());
            assertEquals(QuestState.AVAILABLE, progress.getState());
        } finally {
            Files.deleteIfExists(file.toPath());
        }
    }
}
