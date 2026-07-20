package pvz.model.entity;

import pvz.model.core.Updatable;

public abstract class Entity implements Updatable {
    protected String name;

    public String getName() {
        return name;
    }

    public abstract double getX();

    public abstract double getY();

    public int getTileX() {
        return (int) Math.floor(getX()) + 1;
    }

    public int getTileY() {
        return (int) Math.floor(getY()) + 1;
    }

    protected static double tileCenter(int tileCoordinate) {
        return tileCoordinate - 0.5;
    }
}
