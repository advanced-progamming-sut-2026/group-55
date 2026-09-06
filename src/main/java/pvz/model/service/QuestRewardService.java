package pvz.model.service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import pvz.model.account.PlayerPlant;
import pvz.model.account.User;
import pvz.model.quest.QuestReward;
import pvz.model.quest.QuestRewardType;

/** Applies validated Travel Log rewards to a user. */
public final class QuestRewardService {

    public Validation validate(User user, List<QuestReward> rewards) {
        Objects.requireNonNull(user, "user cannot be null");
        Objects.requireNonNull(rewards, "quest rewards cannot be null");

        Set<String> plantsUnlockedByReward = new HashSet<>();
        for (QuestReward reward : rewards) {
            Objects.requireNonNull(reward, "quest reward cannot be null");
            if (reward.type() == QuestRewardType.PLANT_UNLOCK) {
                plantsUnlockedByReward.add(normalize(reward.targetId()));
            }
        }

        for (QuestReward reward : rewards) {
            if (reward.type() != QuestRewardType.SEED_PACKETS) {
                continue;
            }
            boolean alreadyOwned = user.getOwnedPlant(reward.targetId()) != null;
            boolean unlockedTogether = plantsUnlockedByReward.contains(
                    normalize(reward.targetId())
            );
            if (!alreadyOwned && !unlockedTogether) {
                return new Validation(
                        false,
                        "seed packet reward requires an owned plant: "
                                + reward.targetId()
                );
            }
        }
        return new Validation(true, null);
    }

    public void apply(User user, List<QuestReward> rewards) {
        Validation validation = validate(user, rewards);
        if (!validation.valid()) {
            throw new IllegalStateException(validation.message());
        }

        // Unlocks are applied first so a quest may unlock a plant and grant
        // seed packets for that same plant in one atomic claim.
        for (QuestReward reward : rewards) {
            if (reward.type() == QuestRewardType.PLANT_UNLOCK) {
                unlockPlant(user, reward.targetId());
            }
        }

        for (QuestReward reward : rewards) {
            switch (reward.type()) {
                case COINS -> user.addCoins(reward.amount());
                case DIAMONDS -> user.addDiamonds(reward.amount());
                case PLANT_UNLOCK -> {
                    // Already handled in the first pass.
                }
                case LEVEL_UNLOCK -> user.getAdventureProgress()
                        .unlockLevel(reward.targetId());
                case SEED_PACKETS -> addSeedPackets(
                        user,
                        reward.targetId(),
                        reward.amount()
                );
            }
        }
    }

    private void unlockPlant(User user, String plantName) {
        if (user.getOwnedPlant(plantName) == null) {
            user.addPlant(new PlayerPlant(plantName));
        }
    }

    private void addSeedPackets(
            User user,
            String plantName,
            int amount
    ) {
        PlayerPlant plant = user.getOwnedPlant(plantName);
        if (plant == null) {
            throw new IllegalStateException(
                    "seed packet reward target is not owned: " + plantName
            );
        }
        plant.addSeedPackets(amount);
    }

    private static String normalize(String value) {
        return value.strip().toLowerCase(Locale.ROOT);
    }

    public record Validation(boolean valid, String message) {
    }
}
