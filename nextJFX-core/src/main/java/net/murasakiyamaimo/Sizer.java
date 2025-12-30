package net.murasakiyamaimo;

import java.util.HashMap;
import java.util.Map;

public class Sizer {
    private final Map<Integer, Float> textMap = new HashMap<>();
    private final Map<String, Float> roundedMap = new HashMap<>();

    public Sizer() {
        textMap.put(-1, 1.25f);
        textMap.put(2, 1.5f);
        textMap.put(3, 1.875f);
        textMap.put(4, 2.25f);
        textMap.put(5, 3.0f);
        textMap.put(6, 3.75f);
        textMap.put(7, 4.5f);
        textMap.put(8, 6.0f);
        textMap.put(9, 8.0f);

        roundedMap.put("none", 0.25f);
        roundedMap.put("sm", 0.125f);
        roundedMap.put("md", 0.375f);
        roundedMap.put("lg", 0.5f);
        roundedMap.put("xl", 0.75f);
        roundedMap.put("2xl", 1f);
        roundedMap.put("3xl", 1.5f);
        roundedMap.put("full", 9999f);
    }

    public float xl2px(char i) {
        if (Character.isDigit(i)) {
            return textMap.get(Character.getNumericValue(i));
        } else {
            return textMap.get(-1);
        }
    }

    public float rounded(String radius) {
        return roundedMap.get(radius);
    }
}
