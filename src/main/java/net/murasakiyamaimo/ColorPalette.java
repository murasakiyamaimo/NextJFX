package net.murasakiyamaimo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ColorPalette {
    private final Map<String, Map<String, String>> colorMap;

    public ColorPalette() {
        Map<String, Map<String, String>> map;
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode;
        try {
            rootNode = objectMapper.readTree(getClass().getClassLoader().getResourceAsStream("tailwind-palette.json"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        map = new HashMap<>();
        if (rootNode.isArray()) {
            map = StreamSupport.stream(rootNode.spliterator(), false)
                    .filter(paletteNode -> paletteNode.has("paletteName") && paletteNode.has("swatches"))
                    .collect(Collectors.toMap(
                            paletteNode -> paletteNode.get("paletteName").asText(),
                            paletteNode -> StreamSupport.stream(paletteNode.get("swatches").spliterator(), false)
                                    .collect(Collectors.toMap(
                                            swatchNode -> swatchNode.get("name").asText(),
                                            swatchNode -> swatchNode.get("color").asText()
                                    ))
                    ));
        }
        this.colorMap = map;
    }

    public String get(String color, String shade) {
        if (color.equals("none") || shade.equals("none")) {
            return "defaultColor";
        }
        Map<String, String> shades = colorMap.get(color);
        if (shades != null) {
            String hexColor = shades.get(shade);
            if (hexColor != null) {
                return hexColor;
            }
        }
        return "#ffff";
    }
}
