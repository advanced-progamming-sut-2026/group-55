package pvz.model.core;

public enum HorizontalDirection {
    RIGHT(1),
    LEFT(-1);

    private final int sign;

    HorizontalDirection(int sign) {
        this.sign = sign;
    }

    public int sign() {
        return sign;
    }
}