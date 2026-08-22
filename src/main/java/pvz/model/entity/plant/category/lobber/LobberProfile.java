package pvz.model.entity.plant.category.lobber;

import java.util.Objects;

final class LobberProfile {
    private final LobberShot regularShot;
    private final LobberShot specialShot;
    private final double specialShotChance;

    LobberProfile(LobberShot regularShot) {
        this(regularShot, null, 0);
    }

    LobberProfile(
            LobberShot regularShot,
            LobberShot specialShot,
            double specialShotChance
    ) {
        this.regularShot = Objects.requireNonNull(
                regularShot,
                "regular lobber shot cannot be null"
        );
        this.specialShot = specialShot;

        if (!Double.isFinite(specialShotChance)
                || specialShotChance < 0
                || specialShotChance > 1) {
            throw new IllegalArgumentException(
                    "special shot chance must be between zero and one"
            );
        }

        if ((specialShot == null)
                != (specialShotChance == 0)) {
            throw new IllegalArgumentException(
                    "special shot and its chance must be configured together"
            );
        }

        this.specialShotChance = specialShotChance;
    }

    LobberShot plantFoodShot() {
        if (specialShot != null) {
            return specialShot;
        }

        return regularShot;
    }

    LobberShot selectShot(double randomValue) {
        if (!Double.isFinite(randomValue)
                || randomValue < 0
                || randomValue >= 1) {
            throw new IllegalArgumentException(
                    "random value must be in the range [0, 1)"
            );
        }

        if (specialShot != null
                && randomValue < specialShotChance) {
            return specialShot;
        }

        return regularShot;
    }
}
