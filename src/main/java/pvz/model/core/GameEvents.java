package pvz.model.core;

import java.util.ArrayList;
import java.util.List;

public final class GameEvents {
    private static final List<String> MESSAGES = new ArrayList<>();

    private GameEvents() {
    }

    public static void publish(String message) {
        MESSAGES.add(message);
    }

    public static List<String> drain() {
        List<String> copy = List.copyOf(MESSAGES);
        MESSAGES.clear();
        return copy;
    }
}
