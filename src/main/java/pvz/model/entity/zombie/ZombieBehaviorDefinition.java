package pvz.model.entity.zombie;

import java.util.Locale;
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

    public double requirePositiveDouble(String name) {
        String value = parameters.get(name);
        if (value == null) {
            throw new IllegalArgumentException(
                    "missing parameter '" + name + "' for " + type
            );
        }
        try {
            double parsed = Double.parseDouble(value);
            if (!Double.isFinite(parsed) || parsed <= 0) {
                throw new NumberFormatException();
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "parameter '" + name + "' for " + type
                            + " must be a positive number"
            );
        }
    }

    public double requireRatio(String name) {
        double value = requirePositiveDouble(name);
        if (value > 1) {
            throw new IllegalArgumentException(
                    "parameter '" + name + "' for " + type
                            + " must be in the range (0, 1]"
            );
        }
        return value;
    }

    public boolean requireBoolean(String name) {
        String value = parameters.get(name);
        if (value == null) {
            throw new IllegalArgumentException(
                    "missing parameter '" + name + "' for " + type
            );
        }
        return switch (value.strip().toLowerCase(Locale.ROOT)) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new IllegalArgumentException(
                    "parameter '" + name + "' for " + type
                            + " must be true or false"
            );
        };
    }

    public String requireText(String name) {
        String value = parameters.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "missing parameter '" + name + "' for " + type
            );
        }
        return value.strip();
    }

    public String optionalText(String name) {
        String value = parameters.get(name);
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }

    public int optionalPositiveInt(String name, int defaultValue) {
        String value = parameters.get(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
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

    public double optionalPositiveDouble(
            String name,
            double defaultValue
    ) {
        String value = parameters.get(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            double parsed = Double.parseDouble(value);
            if (!Double.isFinite(parsed) || parsed <= 0) {
                throw new NumberFormatException();
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "parameter '" + name + "' for " + type
                            + " must be a positive number"
            );
        }
    }
}
