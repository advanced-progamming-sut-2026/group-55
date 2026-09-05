package pvz.model.quest;

import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import pvz.model.account.PlayerPlant;
import pvz.model.account.User;

/**
 * Reads quest metrics that already exist in the persistent User model.
 * Event-only metrics are intentionally left unsupported and are advanced
 * through {@link QuestEvent} instead.
 */
public final class UserQuestProgressSource implements QuestProgressSource {

    @Override
    public boolean supports(QuestMetric metric) {
        Objects.requireNonNull(metric, "quest metric cannot be null");
        return switch (metric) {
            case GAMES_PLAYED,
                    CLEARED_STAGES,
                    OWNED_PLANTS,
                    UPGRADED_PLANTS,
                    SEEN_ZOMBIES -> true;
            default -> false;
        };
    }

    @Override
    public int currentValue(User user, QuestObjective objective) {
        Objects.requireNonNull(user, "user cannot be null");
        Objects.requireNonNull(objective, "quest objective cannot be null");
        if (!supports(objective.metric())) {
            throw new IllegalArgumentException(
                    "unsupported snapshot quest metric: "
                            + objective.metric()
            );
        }

        return switch (objective.metric()) {
            case GAMES_PLAYED -> Math.max(0, user.getGamesPlayed());
            case CLEARED_STAGES -> clearedStages(user);
            case OWNED_PLANTS -> objective.hasSubject()
                    ? ownedPlantValue(user, objective.subjectId())
                    : distinctOwnedPlantCount(user);
            case UPGRADED_PLANTS -> objective.hasSubject()
                    ? upgradedPlantValue(user, objective.subjectId())
                    : distinctUpgradedPlantCount(user);
            case SEEN_ZOMBIES -> objective.hasSubject()
                    ? seenZombieValue(user, objective.subjectId())
                    : distinctSeenZombieCount(user);
            default -> throw new IllegalStateException(
                    "unexpected snapshot quest metric"
            );
        };
    }

    private int clearedStages(User user) {
        int stored = Math.max(0, user.getClearedStages());
        int adventure = user.getAdventureProgress()
                .getCompletedLevelIds()
                .size();
        return Math.max(stored, adventure);
    }

    private int ownedPlantValue(User user, String plantName) {
        return user.getOwnedPlant(plantName) == null ? 0 : 1;
    }

    private int distinctOwnedPlantCount(User user) {
        Set<String> names = new HashSet<>();
        for (PlayerPlant plant : user.getUnlockedPlants()) {
            if (plant != null && plant.getPlantName() != null) {
                names.add(normalize(plant.getPlantName()));
            }
        }
        return names.size();
    }

    private int upgradedPlantValue(User user, String plantName) {
        PlayerPlant plant = user.getOwnedPlant(plantName);
        return plant != null && plant.getLevel() > 1 ? 1 : 0;
    }

    private int distinctUpgradedPlantCount(User user) {
        Set<String> names = new HashSet<>();
        for (PlayerPlant plant : user.getUnlockedPlants()) {
            if (plant != null
                    && plant.getPlantName() != null
                    && plant.getLevel() > 1) {
                names.add(normalize(plant.getPlantName()));
            }
        }
        return names.size();
    }

    private int seenZombieValue(User user, String zombieId) {
        String target = normalize(zombieId);
        return user.getSeenZombies().stream()
                .filter(Objects::nonNull)
                .map(UserQuestProgressSource::normalize)
                .anyMatch(target::equals)
                ? 1
                : 0;
    }

    private int distinctSeenZombieCount(User user) {
        Set<String> ids = new HashSet<>();
        for (String zombie : user.getSeenZombies()) {
            if (zombie != null && !zombie.isBlank()) {
                ids.add(normalize(zombie));
            }
        }
        return ids.size();
    }

    private static String normalize(String value) {
        return value.strip().toLowerCase(Locale.ROOT);
    }
}
