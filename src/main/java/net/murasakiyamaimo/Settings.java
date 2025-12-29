package net.murasakiyamaimo;

import java.awt.*;
import java.util.ArrayList;

public class Settings {
    private ArrayList<Integer> defaultColor = new ArrayList<>();
    private int[] windowSize = { 800, 600 };
    private String windowName = "NextJFX";
    private int rem = 16;

    public Settings() {
        setDefaultColor("#ffffff");
    }

    public void setRem(int rem) {
        this.rem = rem;
    }

    public int getRem() {
        return rem;
    }

    public void setWindowName(String name) {
        windowName = name;
    }

    public String getWindowName() {
        return windowName;
    }

    public void setDefaultColor(ArrayList<Integer> defaultColor) {
        this.defaultColor = defaultColor;
    }

    public void setDefaultColor(String color) {
        Color rgbColor = Color.decode(color);
        ArrayList<Integer> rgb = new ArrayList<>();
        rgb.add(rgbColor.getRed());
        rgb.add(rgbColor.getGreen());
        rgb.add(rgbColor.getBlue());

        defaultColor = rgb;
    }

    public void setWindowSize(int[] windowSize) {
        this.windowSize = windowSize;
    }

    public int[] getWindowSize() {
        return windowSize;
    }

    public ArrayList<Integer> getDefaultColor() {
        return defaultColor;
    }
}
