package pvz.model.session;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Boundary used by a running session to validate and consume a greenhouse
 * stored boost without coupling battle logic to account persistence.
 *
 * <p>A future server-backed account implementation can provide the same two
 * operations without changing {@link GameSession} or {@code GameController}.</p>
 */
public interface StoredPlantBoostAccess {
    boolean isAvailable(String plantName);

    boolean consume(String plantName);

    /**
     * Local-only fallback for model tests and callers with no persistent
     * account integration. It still remembers consumed names so restarting a
     * runtime does not recreate a one-shot boost from the config snapshot.
     */
    static StoredPlantBoostAccess untracked() {
        return new StoredPlantBoostAccess() {
            private final Set<String> consumed = new HashSet<>();

            @Override
            public boolean isAvailable(String plantName) {
                return !consumed.contains(normalize(plantName));
            }

            @Override
            public boolean consume(String plantName) {
                return consumed.add(normalize(plantName));
            }

            private String normalize(String plantName) {
                if (plantName == null) {
                    return "";
                }
                return plantName.strip().toLowerCase(Locale.ROOT);
            }
        };
    }
}
