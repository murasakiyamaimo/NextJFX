package net.murasakiyamaimo;

public class ClickableRegion {
    private final float x, y, width, height;

    public ClickableRegion(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean contains(float px, float py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }
}
