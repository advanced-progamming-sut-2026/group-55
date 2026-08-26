package pvz.model.entity.plant.category.explosive;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import pvz.model.core.Updatable;
import pvz.model.core.World;

public final class GrapeshotVolley implements Updatable {

    private static final int[][] DIRECTIONS = {
            {-1, -1}, {-1, 0}, {-1, 1},
            {0, -1}, {0, 1},
            {1, -1}, {1, 0}, {1, 1}
    };

    private final World world;

    private final ExplosiveProfile profile;

    private final long startTick;

    private final List<Grape> grapes = new ArrayList<>();

    GrapeshotVolley(
            World world,
            int column,
            int row,
            ExplosiveProfile profile,
            long startTick
    ) {
        this.world = Objects.requireNonNull(world, "world cannot be null");
        this.profile = Objects.requireNonNull(
                profile,
                "explosive profile cannot be null"
        );
        this.startTick = startTick;

        for (int[] direction : DIRECTIONS) {
            grapes.add(new Grape(column, row, direction[0], direction[1]));
        }
    }

    @Override
    public void update(long tick) {
        long elapsedTicks = tick - startTick;

        if (elapsedTicks < 0) {
            return;
        }

        if (elapsedTicks >= profile.maxLifetimeTicks()) {
            deactivateAll();
            world.game().unregister(this);
            return;
        }

        if (elapsedTicks % profile.stepIntervalTicks() != 0) {
            return;
        }

        for (Grape grape : grapes) {
            if (grape.active) {
                step(grape);
            }
        }

        if (getActiveGrapeCount() == 0) {
            world.game().unregister(this);
        }
    }

    public int getGrapeCount() {
        return grapes.size();
    }

    public int getActiveGrapeCount() {
        return (int) grapes.stream().filter(grape -> grape.active).count();
    }

    public List<GrapeState> getGrapeStates() {
        return grapes.stream()
                .map(grape -> new GrapeState(
                        grape.column,
                        grape.row,
                        grape.deltaColumn,
                        grape.deltaRow,
                        grape.bounceCount,
                        grape.active
                ))
                .toList();
    }

    private void step(Grape grape) {
        int columns = world.board().getCols();
        int rows = world.board().getRows();

        int nextColumn = grape.column + grape.deltaColumn;
        int nextRow = grape.row + grape.deltaRow;

        boolean bounced = false;

        if (nextColumn < 1 || nextColumn > columns) {
            grape.deltaColumn = -grape.deltaColumn;
            nextColumn = grape.column + grape.deltaColumn;
            bounced = true;
        }

        if (nextRow < 1 || nextRow > rows) {
            grape.deltaRow = -grape.deltaRow;
            nextRow = grape.row + grape.deltaRow;
            bounced = true;
        }

        if (bounced) {
            grape.bounceCount++;
        }

        grape.column = nextColumn;
        grape.row = nextRow;

        world.damageEnemyContentsInArea(
                grape.column,
                grape.row,
                0,
                profile.grapeDamage()
        );

        if (grape.bounceCount >= profile.maxBounces()) {
            grape.active = false;
        }
    }

    private void deactivateAll() {
        for (Grape grape : grapes) {
            grape.active = false;
        }
    }

    public record GrapeState(
            int column,
            int row,
            int deltaColumn,
            int deltaRow,
            int bounceCount,
            boolean active
    ) {
    }

    private static final class Grape {
        private int column;
        private int row;
        private int deltaColumn;
        private int deltaRow;
        private int bounceCount;
        private boolean active = true;

        private Grape(
                int column,
                int row,
                int deltaColumn,
                int deltaRow
        ) {
            this.column = column;
            this.row = row;
            this.deltaColumn = deltaColumn;
            this.deltaRow = deltaRow;
        }
    }
}
