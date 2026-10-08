package net.murasakiyamaimo;

import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.system.MemoryStack.stackPush;

/**
 * NanoVGを使用してテキストのサイズを測定
 */
public class TextMeasurer {
    private final long vg;
    private final String fontName;
    private final int rem;
    
    public TextMeasurer(long vg, String fontName, int rem) {
        this.vg = vg;
        this.fontName = fontName;
        this.rem = rem;
    }
    
    /**
     * テキストの幅と高さを測定
     */
    public TextBounds measureText(String text, float textSize) {
        if (text == null || text.isEmpty()) {
            return new TextBounds(0, 0);
        }
        
        // NanoVGのフォント設定
        nvgFontSize(vg, textSize * rem);
        nvgFontFace(vg, fontName);
        
        try (MemoryStack stack = stackPush()) {
            FloatBuffer bounds = stack.mallocFloat(4);
            
            // テキストの境界ボックスを取得
            // bounds[0] = minX, bounds[1] = minY, bounds[2] = maxX, bounds[3] = maxY
            nvgTextBounds(vg, 0, 0, text, bounds);
            
            float width = bounds.get(2) - bounds.get(0);
            float height = bounds.get(3) - bounds.get(1);
            
            return new TextBounds(width, height);
        }
    }
    
    /**
     * テキストの実際の描画高さを取得（アセンダー、ディセンダーを含む）
     */
    public float getTextHeight(float textSize) {
        nvgFontSize(vg, textSize * rem);
        nvgFontFace(vg, fontName);
        
        try (MemoryStack stack = stackPush()) {
            FloatBuffer ascender = stack.mallocFloat(1);
            FloatBuffer descender = stack.mallocFloat(1);
            FloatBuffer lineh = stack.mallocFloat(1);
            
            nvgTextMetrics(vg, ascender, descender, lineh);
            
            return lineh.get(0);
        }
    }
    
    /**
     * テキストの境界情報を保持するクラス
     */
    public static class TextBounds {
        public final float width;
        public final float height;
        
        public TextBounds(float width, float height) {
            this.width = width;
            this.height = height;
        }
    }
}
