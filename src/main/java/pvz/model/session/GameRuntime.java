package pvz.model.session;

import java.util.Objects;
import pvz.controller.game.GameController;

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
        Objects.requireNonNull(config, "session config cannot be null");

        if (isActive()) {
            throw new IllegalStateException(
                    "a game session is already running"
            );
        }

        session = sessionFactory.create(config);
        session.start();
        controller = new GameController(session);
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
}