package pvz.model.selection;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import pvz.model.entity.plant.PlantCategory;
import pvz.model.entity.plant.PlantSpec;

/**
 * Data-only rules for a plant-selection setup.
 *
 * <p>The normal Adventure flow uses {@link #normal()}. Special levels can later
 * provide constrained rules without adding level-id conditions to the plant
 * selection controller or screen.</p>
 */
public record PlantSelectionRules(
        int maxSlots,
        int lockedSlots,
        List<String> forcedPlants,
        Set<String> allowedPlants,
        Set<String> bannedPlants,
        Set<PlantCategory> bannedFamilies
) {
    public static final int DEFAULT_MAX_SLOTS = 8;

    public PlantSelectionRules {
        if (maxSlots <= 0) {
            throw new IllegalArgumentException("max slots must be positive");
        }
        if (lockedSlots < 0 || lockedSlots >= maxSlots) {
            throw new IllegalArgumentException(
                    "locked slots must be between 0 and maxSlots - 1"
            );
        }

        forcedPlants = normalizeList(forcedPlants, "forced plants");
        allowedPlants = normalizeSet(allowedPlants, "allowed plants");
        bannedPlants = normalizeSet(bannedPlants, "banned plants");
        bannedFamilies = Set.copyOf(Objects.requireNonNull(
                bannedFamilies,
                "banned families cannot be null"
        ));

        if (forcedPlants.size() > selectableCapacity(maxSlots, lockedSlots)) {
            throw new IllegalArgumentException(
                    "forced plants exceed selectable slot capacity"
            );
        }
        for (String forcedPlant : forcedPlants) {
            if (bannedPlants.contains(forcedPlant)) {
                throw new IllegalArgumentException(
                        "a forced plant cannot also be banned: " + forcedPlant
                );
            }
            if (!allowedPlants.isEmpty() && !allowedPlants.contains(forcedPlant)) {
                throw new IllegalArgumentException(
                        "a forced plant must be included in allowed plants: "
                                + forcedPlant
                );
            }
        }
    }

    public static PlantSelectionRules normal() {
        return new PlantSelectionRules(
                DEFAULT_MAX_SLOTS,
                0,
                List.of(),
                Set.of(),
                Set.of(),
                Set.of()
        );
    }

    public int selectableCapacity() {
        return selectableCapacity(maxSlots, lockedSlots);
    }

    public boolean isForced(String plantName) {
        if (plantName == null || plantName.isBlank()) {
            return false;
        }
        return forcedPlants.contains(normalize(plantName));
    }

    public boolean isSelectable(PlantSpec spec) {
        Objects.requireNonNull(spec, "plant spec cannot be null");
        String normalizedName = normalize(spec.getName());
        if (bannedPlants.contains(normalizedName)) {
            return false;
        }
        if (bannedFamilies.contains(spec.getCategory())) {
            return false;
        }
        return allowedPlants.isEmpty() || allowedPlants.contains(normalizedName);
    }

    private static int selectableCapacity(int maxSlots, int lockedSlots) {
        return maxSlots - lockedSlots;
    }

    private static List<String> normalizeList(List<String> values, String fieldName) {
        Objects.requireNonNull(values, fieldName + " cannot be null");
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String value : values) {
            unique.add(normalizeRequired(value, fieldName));
        }
        return List.copyOf(unique);
    }

    private static Set<String> normalizeSet(Set<String> values, String fieldName) {
        Objects.requireNonNull(values, fieldName + " cannot be null");
        return values.stream()
                .map(value -> normalizeRequired(value, fieldName))
                .collect(Collectors.toUnmodifiableSet());
    }

    private static String normalizeRequired(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " cannot contain null");
        String normalized = normalize(value);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot contain blank names");
        }
        return normalized;
    }

    private static String normalize(String value) {
        return value.strip().toLowerCase(Locale.ROOT);
    }
}
