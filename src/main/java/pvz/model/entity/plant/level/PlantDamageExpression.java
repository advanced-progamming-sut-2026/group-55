package pvz.model.entity.plant.level;

import java.math.BigDecimal;

public final class PlantDamageExpression {
    private PlantDamageExpression() {}

    public static String addToDamageValues(String expression, double amount) {
        if (expression == null || expression.isBlank() || amount == 0) {
            return expression;
        }
        String stripped = expression.strip();
        if (stripped.equalsIgnoreCase("Insta-kill")) {
            return expression;
        }
        if (stripped.contains("x")) {
            String[] parts = stripped.toLowerCase().split("x", -1);
            if (parts.length != 2) {
                return expression;
            }
            return addNumeric(parts[0], amount) + "x" + parts[1];
        }
        if (stripped.contains("/")) {
            String[] parts = stripped.split("/", -1);
            StringBuilder result = new StringBuilder();
            for (int index = 0; index < parts.length; index++) {
                if (index > 0) {
                    result.append('/');
                }
                result.append(addNumeric(parts[index], amount));
            }
            return result.toString();
        }
        return addNumeric(stripped, amount);
    }

    private static String addNumeric(String text, double amount) {
        try {
            return format(Double.parseDouble(text.strip()) + amount);
        } catch (NumberFormatException ignored) {
            return text;
        }
    }

    private static String format(double value) {
        if (Math.rint(value) == value) {
            return Long.toString(Math.round(value));
        }
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}
