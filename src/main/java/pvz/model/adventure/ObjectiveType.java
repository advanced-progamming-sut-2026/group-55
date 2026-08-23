package pvz.model.adventure;

import java.util.Locale;

public enum ObjectiveType {
    CLEAR_ALL_WAVES;

    public static ObjectiveType fromCsv(String value) {
        return valueOf(value.strip().toUpperCase(Locale.ROOT));
    }
}
