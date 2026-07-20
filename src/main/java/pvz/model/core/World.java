package pvz.model.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import pvz.model.entity.collectible.Collectible;

public final class World {
    private final Game game;
    private final Board board;
    private final SunBank sunBank;

    private final List<Collectible> collectibles = new ArrayList<>();

    public World(Game game, Board board, SunBank sunBank) {
        this.game = Objects.requireNonNull(game);
        this.board = Objects.requireNonNull(board);
        this.sunBank = Objects.requireNonNull(sunBank);
    }

    public Game game() {
        return game;
    }

    public Board board() {
        return board;
    }

    public SunBank sunBank() {
        return sunBank;
    }

    public void addCollectible(Collectible collectible) {
        collectibles.add(Objects.requireNonNull(collectible));
    }

    public void removeCollectible(Collectible collectible) {
        collectibles.remove(collectible);
    }

    public List<Collectible> getCollectibles() {
        return List.copyOf(collectibles);
    }
}