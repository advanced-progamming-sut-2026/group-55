package pvz.model.entity.zombie;

import java.util.Objects;

public record ArmorSpec(
        String id,
        String name,
        double maxHealth,
        boolean metallic
) {
    public ArmorSpec {
        id = requireText(id, "armor id");
        name = requireText(name, "armor name");
        if (maxHealth <= 0) {
            throw new IllegalArgumentException("armor health must be positive");
        }
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field + " cannot be null");
        String result = value.strip();
        if (result.isEmpty()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return result;
    }
}
