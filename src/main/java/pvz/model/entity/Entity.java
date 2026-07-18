package pvz.model.entity;

import pvz.model.core.Updatable;

public abstract class Entity implements Updatable {
    protected int row;
    protected double health;
    protected String name;

    public String getName() {
        return name;
    }
}