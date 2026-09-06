package pvz.model.quest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import pvz.model.minigame.MinigameCatalog;

/**
 * Immutable registry of Travel Log quest definitions.
 *
 * <p>The default catalog intentionally mixes quests that can already be
 * measured from the current persistent user model with future event-driven
 * quests. Future quests stay unavailable until the phase that owns their
 * gameplay hook explicitly activates them through QuestService.</p>
 */
public final class QuestCatalog {
    public static final String ADVENTURE_FIRST_CLEAR =
            "adventure-first-clear";
    public static final String ADVENTURE_SECOND_CLEAR =
            "adventure-second-clear";
    public static final String ADVENTURE_ANCIENT_EGYPT_COMPLETE =
            "adventure-ancient-egypt-complete";

    public static final String DAILY_PLAY_ONE = "daily-play-one";
    public static final String DAILY_PLAY_THREE = "daily-play-three";

    public static final String CHALLENGE_PLANT_COLLECTOR =
            "challenge-plant-collector";
    public static final String CHALLENGE_FIRST_UPGRADE =
            "challenge-first-upgrade";
    public static final String CHALLENGE_ZOMBIE_SCHOLAR =
            "challenge-zombie-scholar";
    public static final String CHALLENGE_ZOMBIE_HUNTER =
            "challenge-zombie-hunter";
    public static final String CHALLENGE_SUN_SPENDER =
            "challenge-sun-spender";

    public static final String MINIGAME_VASE_BREAKER =
            "minigame-vase-breaker";
    public static final String MINIGAME_WALL_NUT_BOWLING =
            "minigame-wall-nut-bowling";
    public static final String MINIGAME_I_ZOMBIE =
            "minigame-i-zombie";

    private static final Comparator<QuestSpec> DISPLAY_ORDER =
            Comparator.comparingInt(
                            (QuestSpec spec) -> spec.priority().sortOrder()
                    )
                    .thenComparing(QuestSpec::name)
                    .thenComparing(QuestSpec::id);

    private final Map<String, QuestSpec> questsById;
    private final Map<QuestCategory, List<QuestSpec>> questsByCategory;

    public QuestCatalog(Collection<QuestSpec> quests) {
        Objects.requireNonNull(quests, "quests cannot be null");

        Map<String, QuestSpec> byId = new LinkedHashMap<>();
        Map<QuestCategory, List<QuestSpec>> byCategory =
                new EnumMap<>(QuestCategory.class);

        for (QuestSpec quest : quests) {
            Objects.requireNonNull(quest, "quest cannot be null");
            QuestSpec duplicate = byId.putIfAbsent(quest.id(), quest);
            if (duplicate != null) {
                throw new IllegalArgumentException(
                        "duplicate quest id: " + quest.id()
                );
            }
            byCategory.computeIfAbsent(
                    quest.category(),
                    ignored -> new ArrayList<>()
            ).add(quest);
        }

        Map<QuestCategory, List<QuestSpec>> frozenCategories =
                new EnumMap<>(QuestCategory.class);
        for (QuestCategory category : QuestCategory.values()) {
            List<QuestSpec> categoryQuests = byCategory.getOrDefault(
                    category,
                    List.of()
            ).stream().sorted(DISPLAY_ORDER).toList();
            frozenCategories.put(category, categoryQuests);
        }

        this.questsById = Map.copyOf(byId);
        this.questsByCategory = Map.copyOf(frozenCategories);
    }

    public static QuestCatalog createDefault() {
        return new QuestCatalog(defaultQuests());
    }

    public QuestSpec find(String questId) {
        if (questId == null || questId.isBlank()) {
            return null;
        }
        return questsById.get(QuestSpec.normalizeId(questId));
    }

    public QuestSpec require(String questId) {
        QuestSpec quest = find(questId);
        if (quest == null) {
            throw new IllegalArgumentException(
                    "unknown quest id: " + questId
            );
        }
        return quest;
    }

    public List<QuestSpec> all() {
        return questsById.values().stream()
                .sorted(DISPLAY_ORDER)
                .toList();
    }

    public List<QuestSpec> byCategory(QuestCategory category) {
        Objects.requireNonNull(category, "quest category cannot be null");
        return questsByCategory.getOrDefault(category, List.of());
    }

    public List<QuestSpec> initiallyAvailable() {
        return all().stream()
                .filter(QuestSpec::initiallyAvailable)
                .toList();
    }

    public List<QuestSpec> initiallyUnavailable() {
        return all().stream()
                .filter(spec -> !spec.initiallyAvailable())
                .toList();
    }

    public int size() {
        return questsById.size();
    }

    private static List<QuestSpec> defaultQuests() {
        return List.of(
                adventureFirstClear(),
                adventureSecondClear(),
                ancientEgyptComplete(),
                dailyPlayOne(),
                dailyPlayThree(),
                plantCollector(),
                firstUpgrade(),
                zombieScholar(),
                zombieHunter(),
                sunSpender(),
                vaseBreaker(),
                wallNutBowling(),
                iZombie()
        );
    }

    private static QuestSpec adventureFirstClear() {
        return quest(
                ADVENTURE_FIRST_CLEAR,
                "First Victory",
                "Complete your first Adventure stage.",
                QuestCategory.ADVENTURE,
                QuestPriority.CRITICAL,
                QuestObjective.global(QuestMetric.CLEARED_STAGES, 1),
                QuestResetPolicy.NEVER,
                true,
                QuestReward.coins(500),
                QuestReward.plantUnlock("Gold Bloom")
        );
    }

    private static QuestSpec adventureSecondClear() {
        return quest(
                ADVENTURE_SECOND_CLEAR,
                "Ancient Egypt Explorer",
                "Complete 2 Adventure stages.",
                QuestCategory.ADVENTURE,
                QuestPriority.CRITICAL,
                QuestObjective.global(QuestMetric.CLEARED_STAGES, 2),
                QuestResetPolicy.NEVER,
                true,
                QuestReward.coins(750),
                QuestReward.plantUnlock("Pepper-pult")
        );
    }

    private static QuestSpec ancientEgyptComplete() {
        return quest(
                ADVENTURE_ANCIENT_EGYPT_COMPLETE,
                "Conquer Ancient Egypt",
                "Complete the Ancient Egypt chapter.",
                QuestCategory.ADVENTURE,
                QuestPriority.CRITICAL,
                QuestObjective.forSubject(
                        QuestMetric.CHAPTER_COMPLETED,
                        "ancient-egypt",
                        1
                ),
                QuestResetPolicy.NEVER,
                false,
                QuestReward.coins(1500),
                QuestReward.plantUnlock("Caulipower")
        );
    }

    private static QuestSpec dailyPlayOne() {
        return quest(
                DAILY_PLAY_ONE,
                "Daily Warm-up",
                "Play 1 battle today.",
                QuestCategory.DAILY,
                QuestPriority.MEDIUM,
                QuestObjective.global(QuestMetric.GAMES_PLAYED, 1),
                QuestResetPolicy.DAILY,
                true,
                QuestReward.coins(250)
        );
    }

    private static QuestSpec dailyPlayThree() {
        return quest(
                DAILY_PLAY_THREE,
                "Daily Defender",
                "Play 3 battles today.",
                QuestCategory.DAILY,
                QuestPriority.LOW,
                QuestObjective.global(QuestMetric.GAMES_PLAYED, 3),
                QuestResetPolicy.DAILY,
                true,
                QuestReward.coins(350),
                QuestReward.seedPackets("Peashooter", 5)
        );
    }

    private static QuestSpec plantCollector() {
        return quest(
                CHALLENGE_PLANT_COLLECTOR,
                "Growing Collection",
                "Own 30 different plants.",
                QuestCategory.CHALLENGE,
                QuestPriority.HIGH,
                QuestObjective.global(QuestMetric.OWNED_PLANTS, 30),
                QuestResetPolicy.NEVER,
                true,
                QuestReward.diamonds(5)
        );
    }

    private static QuestSpec firstUpgrade() {
        return quest(
                CHALLENGE_FIRST_UPGRADE,
                "Power Up",
                "Upgrade at least 1 plant.",
                QuestCategory.CHALLENGE,
                QuestPriority.HIGH,
                QuestObjective.global(QuestMetric.UPGRADED_PLANTS, 1),
                QuestResetPolicy.NEVER,
                true,
                QuestReward.diamonds(5),
                QuestReward.seedPackets("Peashooter", 5)
        );
    }

    private static QuestSpec zombieScholar() {
        return quest(
                CHALLENGE_ZOMBIE_SCHOLAR,
                "Zombie Scholar",
                "Discover 5 different zombies.",
                QuestCategory.CHALLENGE,
                QuestPriority.HIGH,
                QuestObjective.global(QuestMetric.SEEN_ZOMBIES, 5),
                QuestResetPolicy.NEVER,
                true,
                QuestReward.diamonds(5)
        );
    }

    private static QuestSpec zombieHunter() {
        return quest(
                CHALLENGE_ZOMBIE_HUNTER,
                "Zombie Hunter",
                "Defeat 25 zombies in battles.",
                QuestCategory.CHALLENGE,
                QuestPriority.HIGH,
                QuestObjective.global(QuestMetric.ZOMBIE_KILLED, 25),
                QuestResetPolicy.NEVER,
                true,
                QuestReward.diamonds(10)
        );
    }

    private static QuestSpec sunSpender() {
        return quest(
                CHALLENGE_SUN_SPENDER,
                "Sun Strategist",
                "Spend 1000 sun during battles.",
                QuestCategory.CHALLENGE,
                QuestPriority.HIGH,
                QuestObjective.global(QuestMetric.SUN_SPENT, 1000),
                QuestResetPolicy.NEVER,
                true,
                QuestReward.diamonds(10)
        );
    }

    private static QuestSpec vaseBreaker() {
        return quest(
                MINIGAME_VASE_BREAKER,
                "Vase Breaker",
                "Complete a Vase Breaker minigame.",
                QuestCategory.MINIGAME,
                QuestPriority.HIGH,
                QuestObjective.forSubject(
                        QuestMetric.MINIGAME_COMPLETED,
                        MinigameCatalog.VASE_BREAKER,
                        1
                ),
                QuestResetPolicy.NEVER,
                false,
                QuestReward.diamonds(10)
        );
    }

    private static QuestSpec wallNutBowling() {
        return quest(
                MINIGAME_WALL_NUT_BOWLING,
                "Wall-nut Bowling",
                "Complete a Wall-nut Bowling minigame.",
                QuestCategory.MINIGAME,
                QuestPriority.HIGH,
                QuestObjective.forSubject(
                        QuestMetric.MINIGAME_COMPLETED,
                        MinigameCatalog.WALL_NUT_BOWLING,
                        1
                ),
                QuestResetPolicy.NEVER,
                false,
                QuestReward.diamonds(10)
        );
    }

    private static QuestSpec iZombie() {
        return quest(
                MINIGAME_I_ZOMBIE,
                "I, Zombie",
                "Complete an I, Zombie minigame.",
                QuestCategory.MINIGAME,
                QuestPriority.HIGH,
                QuestObjective.forSubject(
                        QuestMetric.MINIGAME_COMPLETED,
                        MinigameCatalog.I_ZOMBIE,
                        1
                ),
                QuestResetPolicy.NEVER,
                false,
                QuestReward.diamonds(10)
        );
    }

    private static QuestSpec quest(
            String id,
            String name,
            String description,
            QuestCategory category,
            QuestPriority priority,
            QuestObjective objective,
            QuestResetPolicy resetPolicy,
            boolean initiallyAvailable,
            QuestReward... rewards
    ) {
        return new QuestSpec(
                id,
                name,
                description,
                category,
                priority,
                objective,
                List.of(rewards),
                resetPolicy,
                initiallyAvailable
        );
    }
}
