package pvz.model.quest;

/**
 * Metrics that can drive quest progress.
 *
 * <p>The first group can be read from the current user snapshot. The event
 * metrics are intentionally independent from Battle/Adventure/Minigame
 * classes so later phases can publish progress without coupling those systems
 * directly to Travel Log.</p>
 */
public enum QuestMetric {
    GAMES_PLAYED,
    CLEARED_STAGES,
    OWNED_PLANTS,
    UPGRADED_PLANTS,
    SEEN_ZOMBIES,

    BATTLE_COMPLETED,
    LEVEL_COMPLETED,
    CHAPTER_COMPLETED,
    ZOMBIE_KILLED,
    PLANT_PLACED,
    PLANT_UPGRADED,
    SUN_SPENT,
    COINS_EARNED,
    DIAMONDS_EARNED,
    SEED_PACKETS_COLLECTED,
    MINIGAME_COMPLETED
}
