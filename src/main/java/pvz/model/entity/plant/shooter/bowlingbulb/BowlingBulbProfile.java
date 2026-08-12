package pvz.model.entity.plant.shooter.bowlingbulb;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import pvz.model.core.Game;
import pvz.model.entity.plant.PlantCategory;
import pvz.model.entity.plant.PlantSpec;

public record BowlingBulbProfile(
        List<Bulb> bulbs
) {
    private static final List<String> BULB_NAMES =
            List.of(
                    "Aquamarine",
                    "Blue",
                    "Orange"
            );

    private static final List<Long> RECHARGE_TICKS =
            List.of(
                    2L * Game.TICKS_PER_SECOND,
                    5L * Game.TICKS_PER_SECOND,
                    10L * Game.TICKS_PER_SECOND
            );

    public BowlingBulbProfile {
        bulbs = List.copyOf(
                Objects.requireNonNull(
                        bulbs,
                        "bulb profiles cannot be null"
                )
        );

        if (bulbs.isEmpty()) {
            throw new IllegalArgumentException(
                    "bowling bulb needs at least one bulb profile"
            );
        }
    }

    public static BowlingBulbProfile from(
            PlantSpec spec
    ) {
        Objects.requireNonNull(
                spec,
                "plant spec cannot be null"
        );

        if (spec.getCategory() != PlantCategory.SHOOTER
                || !spec.getName()
                .strip()
                .toLowerCase(Locale.ROOT)
                .equals("bowling bulb")) {

            throw new IllegalArgumentException(
                    spec.getName()
                            + " is not Bowling Bulb"
            );
        }

        List<Double> damageValues =
                parseDamageValues(spec.getDamage());

        if (damageValues.size() != BULB_NAMES.size()) {
            throw new IllegalArgumentException(
                    "Bowling Bulb needs exactly three damage values"
            );
        }

        List<Bulb> bulbs =
                new ArrayList<>(BULB_NAMES.size());

        for (int index = 0;
             index < BULB_NAMES.size();
             index++) {

            bulbs.add(
                    new Bulb(
                            BULB_NAMES.get(index),
                            damageValues.get(index),
                            RECHARGE_TICKS.get(index)
                    )
            );
        }

        return new BowlingBulbProfile(bulbs);
    }

    private static List<Double> parseDamageValues(
            String damageText
    ) {
        String[] parts = damageText
                .strip()
                .split("/");

        List<Double> values =
                new ArrayList<>(parts.length);

        try {
            for (String part : parts) {
                values.add(
                        Double.parseDouble(part)
                );
            }
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "invalid Bowling Bulb damage values: "
                            + damageText,
                    exception
            );
        }

        return values;
    }

    public record Bulb(
            String name,
            double damage,
            long rechargeTicks
    ) {
        public Bulb {
            name = Objects.requireNonNull(
                    name,
                    "bulb name cannot be null"
            ).strip();

            if (name.isEmpty()) {
                throw new IllegalArgumentException(
                        "bulb name cannot be blank"
                );
            }

            if (damage < 0) {
                throw new IllegalArgumentException(
                        "bulb damage cannot be negative"
                );
            }

            if (rechargeTicks <= 0) {
                throw new IllegalArgumentException(
                        "bulb recharge must be positive"
                );
            }
        }
    }
}
