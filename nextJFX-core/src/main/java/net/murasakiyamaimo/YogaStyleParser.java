package net.murasakiyamaimo;

import org.appliedenergistics.yoga.*;

import java.util.HashMap;
import java.util.Map;

public class YogaStyleParser {
    private final Sizer sizer = new Sizer();
    private final int rem;
    private final int fbWidth;
    private final int fbHeight;

    public YogaStyleParser(int rem, int fbWidth, int fbHeight) {
        this.rem = rem;
        this.fbWidth = fbWidth;
        this.fbHeight = fbHeight;
    }

    /**
     * TailwindクラスをYogaNodeに適用
     */
    public YogaStyle parseClasses(String className) {
        YogaStyle style = new YogaStyle();
        
        if (className == null || className.isEmpty()) {
            return style;
        }

        for (String css : className.split("\\s+")) {
            applyCssClass(css, style);
        }

        return style;
    }

    private void applyCssClass(String css, YogaStyle style) {
        String[] parts = css.split("-");

        // 背景色
        if (css.startsWith("bg-")) {
            style.bgColor = new String[]{parts[0], parts[1], parts.length > 2 ? parts[2] : "none"};
        }
        
        // テキスト色
        else if (css.startsWith("text-")) {
            if (parts.length > 1 && parts[1].contains("xl")) {
                style.textSize = sizer.xl2px(parts[1].charAt(0));
            } else {
                style.textColor = new String[]{parts[0], parts[1], parts.length > 2 ? parts[2] : "900"};
            }
        }
        
        // 高さ
        else if (css.startsWith("h-")) {
            parseHeight(parts, style);
        }
        
        // 幅
        else if (css.startsWith("w-")) {
            parseWidth(parts, style);
        }
        
        // 最小/最大高さ
        else if (css.startsWith("min-h-")) {
            style.minHeight = parseSize(parts[2]);
        } else if (css.startsWith("max-h-")) {
            style.maxHeight = parseSize(parts[2]);
        }
        
        // 最小/最大幅
        else if (css.startsWith("min-w-")) {
            style.minWidth = parseSize(parts[2]);
        } else if (css.startsWith("max-w-")) {
            style.maxWidth = parseSize(parts[2]);
        }
        
        // パディング
        else if (css.startsWith("p-")) {
            parsePadding(parts, style);
        }
        
        // マージン
        else if (css.startsWith("m-")) {
            parseMargin(parts, style);
        }
        
        // Flexbox
        else if (css.equals("flex")) {
            style.display = YogaDisplay.FLEX;
        } else if (css.startsWith("flex-")) {
            parseFlex(parts, style);
        }
        
        // Flex Direction
        else if (css.equals("flex-row")) {
            style.flexDirection = YogaFlexDirection.ROW;
        } else if (css.equals("flex-row-reverse")) {
            style.flexDirection = YogaFlexDirection.ROW_REVERSE;
        } else if (css.equals("flex-col")) {
            style.flexDirection = YogaFlexDirection.COLUMN;
        } else if (css.equals("flex-col-reverse")) {
            style.flexDirection = YogaFlexDirection.COLUMN_REVERSE;
        }
        
        // Justify Content
        else if (css.startsWith("justify-")) {
            parseJustifyContent(parts[1], style);
        }
        
        // Align Items
        else if (css.startsWith("items-")) {
            parseAlignItems(parts[1], style);
        }
        
        // Align Self
        else if (css.startsWith("self-")) {
            parseAlignSelf(parts[1], style);
        }
        
        // Flex Wrap
        else if (css.equals("flex-wrap")) {
            style.flexWrap = YogaWrap.WRAP;
        } else if (css.equals("flex-wrap-reverse")) {
            style.flexWrap = YogaWrap.WRAP_REVERSE;
        } else if (css.equals("flex-nowrap")) {
            style.flexWrap = YogaWrap.NO_WRAP;
        }
        
        // 角丸
        else if (css.startsWith("rounded-")) {
            parseRounded(parts, style);
        }
        
        // Position
        else if (css.equals("absolute")) {
            style.positionType = YogaPositionType.ABSOLUTE;
        } else if (css.equals("relative")) {
            style.positionType = YogaPositionType.RELATIVE;
        }
        
        // Gap
        else if (css.startsWith("gap-")) {
            style.gap = parseNumericValue(parts[1]) * rem;
        }
    }

    private void parseHeight(String[] parts, YogaStyle style) {
        if (parts[1].equals("screen")) {
            style.height = fbHeight;
            style.heightUnit = YogaUnit.POINT;
        } else if (parts[1].equals("full")) {
            style.height = 100;
            style.heightUnit = YogaUnit.PERCENT;
        } else if (parts[1].equals("auto")) {
            style.heightUnit = YogaUnit.AUTO;
        } else {
            float value = parseNumericValue(parts[1]);
            style.height = value * rem / 4; // Tailwindは4単位
            style.heightUnit = YogaUnit.POINT;
        }
    }

    private void parseWidth(String[] parts, YogaStyle style) {
        if (parts[1].equals("screen")) {
            style.width = fbWidth;
            style.widthUnit = YogaUnit.POINT;
        } else if (parts[1].equals("full")) {
            style.width = 100;
            style.widthUnit = YogaUnit.PERCENT;
        } else if (parts[1].equals("auto")) {
            style.widthUnit = YogaUnit.AUTO;
        } else {
            float value = parseNumericValue(parts[1]);
            style.width = value * rem / 4;
            style.widthUnit = YogaUnit.POINT;
        }
    }

    private void parsePadding(String[] parts, YogaStyle style) {
        float value = parseNumericValue(parts[parts.length - 1]) * rem / 4;
        
        if (parts.length == 2) {
            // p-4 (全方向)
            style.paddingTop = style.paddingRight = style.paddingBottom = style.paddingLeft = value;
        } else if (parts.length == 3) {
            // px-4, py-4, pt-4, pr-4, pb-4, pl-4
            switch (parts[1]) {
                case "x" -> style.paddingLeft = style.paddingRight = value;
                case "y" -> style.paddingTop = style.paddingBottom = value;
                case "t" -> style.paddingTop = value;
                case "r" -> style.paddingRight = value;
                case "b" -> style.paddingBottom = value;
                case "l" -> style.paddingLeft = value;
            }
        }
    }

    private void parseMargin(String[] parts, YogaStyle style) {
        String lastPart = parts[parts.length - 1];
        float value;
        
        if (lastPart.equals("auto")) {
            value = Float.NaN; // YogaではNaNがautoを表す
        } else {
            value = parseNumericValue(lastPart) * rem / 4;
        }
        
        if (parts.length == 2) {
            // m-4 (全方向)
            style.marginTop = style.marginRight = style.marginBottom = style.marginLeft = value;
        } else if (parts.length == 3) {
            // mx-4, my-4, mt-4, mr-4, mb-4, ml-4
            switch (parts[1]) {
                case "x" -> style.marginLeft = style.marginRight = value;
                case "y" -> style.marginTop = style.marginBottom = value;
                case "t" -> style.marginTop = value;
                case "r" -> style.marginRight = value;
                case "b" -> style.marginBottom = value;
                case "l" -> style.marginLeft = value;
            }
        }
    }

    private void parseFlex(String[] parts, YogaStyle style) {
        if (parts.length == 2) {
            // flex-1, flex-2, etc.
            try {
                float flexValue = Float.parseFloat(parts[1]);
                style.flex = flexValue;
            } catch (NumberFormatException e) {
                // flex-auto, flex-initial, flex-none
                switch (parts[1]) {
                    case "auto" -> {
                        style.flexGrow = 1;
                        style.flexShrink = 1;
                        style.flexBasis = Float.NaN; // auto
                    }
                    case "initial" -> {
                        style.flexGrow = 0;
                        style.flexShrink = 1;
                        style.flexBasis = Float.NaN; // auto
                    }
                    case "none" -> {
                        style.flexGrow = 0;
                        style.flexShrink = 0;
                        style.flexBasis = Float.NaN; // auto
                    }
                }
            }
        } else if (parts.length == 3) {
            // flex-grow-1, flex-shrink-1
            float value = parseNumericValue(parts[2]);
            if (parts[1].equals("grow")) {
                style.flexGrow = value;
            } else if (parts[1].equals("shrink")) {
                style.flexShrink = value;
            }
        }
    }

    private void parseJustifyContent(String value, YogaStyle style) {
        style.justifyContent = switch (value) {
            case "start" -> YogaJustify.FLEX_START;
            case "end" -> YogaJustify.FLEX_END;
            case "center" -> YogaJustify.CENTER;
            case "between" -> YogaJustify.SPACE_BETWEEN;
            case "around" -> YogaJustify.SPACE_AROUND;
            case "evenly" -> YogaJustify.SPACE_EVENLY;
            default -> YogaJustify.FLEX_START;
        };
    }

    private void parseAlignItems(String value, YogaStyle style) {
        style.alignItems = switch (value) {
            case "start" -> YogaAlign.FLEX_START;
            case "end" -> YogaAlign.FLEX_END;
            case "center" -> YogaAlign.CENTER;
            case "baseline" -> YogaAlign.BASELINE;
            case "stretch" -> YogaAlign.STRETCH;
            default -> YogaAlign.STRETCH;
        };
    }

    private void parseAlignSelf(String value, YogaStyle style) {
        style.alignSelf = switch (value) {
            case "auto" -> YogaAlign.AUTO;
            case "start" -> YogaAlign.FLEX_START;
            case "end" -> YogaAlign.FLEX_END;
            case "center" -> YogaAlign.CENTER;
            case "baseline" -> YogaAlign.BASELINE;
            case "stretch" -> YogaAlign.STRETCH;
            default -> YogaAlign.AUTO;
        };
    }

    private void parseRounded(String[] parts, YogaStyle style) {
        if (parts.length == 2) {
            // rounded-md, rounded-lg, etc.
            float radius = sizer.rounded(parts[1]) * rem;
            style.borderRadiusAll = radius;
        } else if (parts.length == 3) {
            // rounded-t-md, rounded-tl-md, etc.
            float radius = sizer.rounded(parts[2]) * rem;
            switch (parts[1]) {
                case "t" -> style.borderRadiusTopLeft = style.borderRadiusTopRight = radius;
                case "r" -> style.borderRadiusTopRight = style.borderRadiusBottomRight = radius;
                case "b" -> style.borderRadiusBottomLeft = style.borderRadiusBottomRight = radius;
                case "l" -> style.borderRadiusBottomLeft = style.borderRadiusTopLeft = radius;
                case "tl" -> style.borderRadiusTopLeft = radius;
                case "tr" -> style.borderRadiusTopRight = radius;
                case "br" -> style.borderRadiusBottomRight = radius;
                case "bl" -> style.borderRadiusBottomLeft = radius;
            }
        }
    }

    private float parseNumericValue(String value) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private float parseSize(String value) {
        if (value.equals("screen")) {
            return fbHeight;
        } else if (value.equals("full")) {
            return 100; // percent
        } else {
            return parseNumericValue(value) * rem / 4;
        }
    }

    /**
     * YogaStyleクラス - パース結果を保持
     */
    public static class YogaStyle {
        // Display
        public YogaDisplay display = YogaDisplay.FLEX;
        
        // Flex Direction
        public YogaFlexDirection flexDirection = YogaFlexDirection.COLUMN;
        
        // Flex
        public float flex = Float.NaN;
        public float flexGrow = 0;
        public float flexShrink = 1;
        public float flexBasis = Float.NaN;
        
        // Justify & Align
        public YogaJustify justifyContent = YogaJustify.FLEX_START;
        public YogaAlign alignItems = YogaAlign.STRETCH;
        public YogaAlign alignSelf = YogaAlign.AUTO;
        public YogaWrap flexWrap = YogaWrap.NO_WRAP;
        
        // Dimensions
        public float width = Float.NaN;
        public float height = Float.NaN;
        public YogaUnit widthUnit = YogaUnit.AUTO;
        public YogaUnit heightUnit = YogaUnit.AUTO;
        
        public float minWidth = Float.NaN;
        public float minHeight = Float.NaN;
        public float maxWidth = Float.NaN;
        public float maxHeight = Float.NaN;
        
        // Padding
        public float paddingTop = 0;
        public float paddingRight = 0;
        public float paddingBottom = 0;
        public float paddingLeft = 0;
        
        // Margin
        public float marginTop = 0;
        public float marginRight = 0;
        public float marginBottom = 0;
        public float marginLeft = 0;
        
        // Position
        public YogaPositionType positionType = YogaPositionType.RELATIVE;
        
        // Gap
        public float gap = 0;
        
        // Border Radius (非Yogaプロパティ、描画用)
        public float borderRadiusAll = 0;
        public float borderRadiusTopLeft = 0;
        public float borderRadiusTopRight = 0;
        public float borderRadiusBottomRight = 0;
        public float borderRadiusBottomLeft = 0;
        
        // Colors (非Yogaプロパティ、描画用)
        public String[] bgColor = {"bg", "none", "none"};
        public String[] textColor = {"text", "gray", "900"};
        public float textSize = 1;
        
        public Map<String, Float> getBorderRadius() {
            Map<String, Float> radius = new HashMap<>();
            if (borderRadiusAll > 0) {
                radius.put("all", borderRadiusAll);
            } else {
                if (borderRadiusTopLeft > 0) radius.put("tl", borderRadiusTopLeft);
                if (borderRadiusTopRight > 0) radius.put("tr", borderRadiusTopRight);
                if (borderRadiusBottomRight > 0) radius.put("br", borderRadiusBottomRight);
                if (borderRadiusBottomLeft > 0) radius.put("bl", borderRadiusBottomLeft);
            }
            return radius;
        }
    }
}
