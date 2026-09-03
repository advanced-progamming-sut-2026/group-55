package pvz.model.session;

import java.util.Objects;
import pvz.controller.game.GameController;
import pvz.model.core.ZombieDiscoveryListener;

public final class GameRuntime {
    private final GameSessionFactory sessionFactory;

    private GameSession session;
    private GameController controller;

    public GameRuntime(GameSessionFactory sessionFactory) {
        this.sessionFactory = Objects.requireNonNull(
                sessionFactory,
                "session factory cannot be null"
        );
    }

    public void start(GameSessionConfig config) {
        start(config, ZombieDiscoveryListener.none());
    }

    public void start(
            GameSessionConfig config,
            ZombieDiscoveryListener discoveryListener
    ) {
        Objects.requireNonNull(config, "session config cannot be null");
        Objects.requireNonNull(
                discoveryListener,
                "zombie discovery listener cannot be null"
        );

        if (isActive()) {
            throw new IllegalStateException(
                    "a game session is already running"
            );
        }

        GameSession startedSession = createStartedSession(
                config,
                discoveryListener
        );
        session = startedSession;
        controller = new GameController(startedSession);
    }

    public void restart(GameSessionConfig config) {
        restart(config, ZombieDiscoveryListener.none());
    }

    /**
     * Replaces the current battle with a completely fresh session.
     *
     * <p>The replacement is created before the old session is aborted, so a
     * configuration or factory failure cannot leave a working battle half
     * cleared.</p>
     */
    public void restart(
            GameSessionConfig config,
            ZombieDiscoveryListener discoveryListener
    ) {
        Objects.requireNonNull(config, "session config cannot be null");
        Objects.requireNonNull(
                discoveryListener,
                "zombie discovery listener cannot be null"
        );

        GameSession replacement = createStartedSession(
                config,
                discoveryListener
        );
        GameController replacementController = new GameController(
                replacement
        );

        if (isActive()) {
            session.abort();
        }
        session = replacement;
        controller = replacementController;
    }

    public String handle(String input) {
        Objects.requireNonNull(input, "input cannot be null");

        if (!isActive()) {
            return "no active game session!";
        }

        return controller.handle(input);
    }

    public boolean isActive() {
        return session != null && session.isRunning();
    }

    public boolean isFinished() {
        return session != null && session.isFinished();
    }

    public GameSessionStatus status() {
        if (session == null) {
            throw new IllegalStateException("no game session exists");
        }

        return session.status();
    }

    public GameSession session() {
        if (session == null) {
            throw new IllegalStateException("no game session exists");
        }

        return session;
    }

    public void abort() {
        if (!isActive()) {
            throw new IllegalStateException(
                    "there is no active game session to abort"
            );
        }

        session.abort();
    }

    public void clear() {
        if (isActive()) {
            throw new IllegalStateException(
                    "cannot clear a running game session"
            );
        }

        session = null;
        controller = null;
    }

    private GameSession createStartedSession(
            GameSessionConfig config,
            ZombieDiscoveryListener discoveryListener
    ) {
        GameSession created = sessionFactory.create(
                config,
                discoveryListener
        );
        created.start();
        return created;
    }
}
