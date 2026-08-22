package pvz.model.entity.zombie;

import java.util.Map;
import java.util.Objects;

public record ZombieBehaviorDefinition(
        String type,
        Map<String, String> parameters
) {
    public ZombieBehaviorDefinition {
        Objects.requireNonNull(type, "behavior type cannot be null");
        type = type.strip().toUpperCase();
        if (type.isEmpty()) {
            throw new IllegalArgumentException("behavior type cannot be blank");
        }
        parameters = Map.copyOf(parameters);
    }

    public int requirePositiveInt(String name) {
        String value = parameters.get(name);
        if (value == null) {
            throw new IllegalArgumentException(
                    "missing parameter '" + name + "' for " + type
            );
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) {
                throw new NumberFormatException();
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "parameter '" + name + "' for " + type
                            + " must be a positive integer"
            );
        }
    }
}
