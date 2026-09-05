package pvz.model.leaderboard;

import java.util.Objects;

/**
 * Stable Adventure progress snapshot used by the leaderboard.
 *
 * <p>The numeric chapter/level coordinates are kept separate from display
 * text so later sorting does not depend on localized labels. A standing with
 * no completed Adventure level is represented by {@link #none()}.</p>
 */
public record AdventureStanding(
        String chapterId,
        String chapterName,
        int chapterOrder,
        String levelId,
        String levelName,
        int levelNumber
) {
    public AdventureStanding {
        if (chapterOrder < 0) {
            throw new IllegalArgumentException(
                    "chapter order cannot be negative"
            );
        }
        if (levelNumber < 0) {
            throw new IllegalArgumentException(
                    "level number cannot be negative"
            );
        }

        boolean noProgress = chapterOrder == 0 && levelNumber == 0;
        if (noProgress) {
            chapterId = null;
            chapterName = null;
            levelId = null;
            levelName = null;
        } else {
            if (chapterOrder == 0 || levelNumber == 0) {
                throw new IllegalArgumentException(
                        "chapter order and level number must both be positive"
                );
            }
            chapterId = requireText(chapterId, "chapter id");
            chapterName = requireText(chapterName, "chapter name");
            levelId = requireText(levelId, "level id");
            levelName = requireText(levelName, "level name");
        }
    }

    public static AdventureStanding none() {
        return new AdventureStanding(null, null, 0, null, null, 0);
    }

    public boolean hasProgress() {
        return chapterOrder > 0 && levelNumber > 0;
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field + " cannot be null");
        String checked = value.strip();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return checked;
    }
}
