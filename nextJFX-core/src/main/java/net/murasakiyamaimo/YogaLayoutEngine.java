package net.murasakiyamaimo;

import org.appliedenergistics.yoga.*;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class YogaLayoutEngine {
    private final YogaNode rootNode;
    private final YogaStyleParser styleParser;
    private final ColorPalette colorPalette;
    private final Map<YogaNode, NodeMetadata> nodeMetadata;
    private final TextMeasurer textMeasurer;
    private final int rem;
    private boolean layoutDirty;
    private List<RenderNode> cachedRenderNodes = null;

    public void markDirty() {
        layoutDirty = true;
    }
    
    public YogaLayoutEngine(int rem, int fbWidth, int fbHeight, ColorPalette colorPalette, long vg, String fontName) {
        this.rootNode = new YogaNode();
        this.styleParser = new YogaStyleParser(rem, fbWidth, fbHeight);
        this.colorPalette = colorPalette;
        this.nodeMetadata = new HashMap<>();
        this.textMeasurer = new TextMeasurer(vg, fontName, rem);
        this.rem = rem;
        
        // ルートノードの設定
        rootNode.setWidth(fbWidth);
        rootNode.setHeight(fbHeight);
    }
    
    /**
     * レイアウトツリーを構築
     */
    public YogaNode buildLayoutTree(List<Content> domContent) {
        // ルートノードをクリア
        rootNode.clearChildren();
        nodeMetadata.clear();
        
        // DOMからYogaノードツリーを構築
        for (Content content : domContent) {
            if (content instanceof Element) {
                YogaNode childNode = createElement((Element) content);
                if (childNode != null) {
                    rootNode.addChildAt(childNode, rootNode.getChildCount());
                }
            }
        }
        
        return rootNode;
    }
    
    /**
     * レイアウト計算を実行
     */
    public void calculateLayout() {
        rootNode.calculateLayout(YogaConstants.UNDEFINED, YogaConstants.UNDEFINED);
    }
    
    /**
     * 要素からYogaノードを作成
     */
    private YogaNode createElement(Element element) {
        YogaNode node = new YogaNode();
        
        // クラス名をパースしてスタイルを適用
        String className = element.getAttributeValue("className");
        YogaStyleParser.YogaStyle style = styleParser.parseClasses(className);
        applyStyleToNode(node, style);
        
        // メタデータを保存（描画用）
        NodeMetadata metadata = new NodeMetadata();
        metadata.element = element;
        metadata.style = style;
        metadata.tagName = element.getName();
        
        // テキストコンテンツの取得
        String textContent = null;
        boolean hasElementChildren = false;
        
        if (!element.getContent().isEmpty()) {
            StringBuilder textBuilder = new StringBuilder();
            for (Content content : element.getContent()) {
                if (content instanceof Text) {
                    String text = ((Text) content).getText().trim();
                    if (!text.isEmpty()) {
                        textBuilder.append(text);
                    }
                } else if (content instanceof Element) {
                    hasElementChildren = true;
                }
            }
            if (textBuilder.length() > 0) {
                textContent = textBuilder.toString();
            }
        }
        
        metadata.textContent = textContent;
        nodeMetadata.put(node, metadata);
        
        // テキストノード（子要素がなく、テキストのみを含む）の場合、MeasureFuncを設定
        if (textContent != null && !textContent.isEmpty() && !hasElementChildren) {
            final String finalTextContent = textContent;
            final float textSize = style.textSize;
            
            node.setMeasureFunction((yogaNode, width, widthMode, height, heightMode) -> {
                // テキストサイズを測定
                TextMeasurer.TextBounds bounds = textMeasurer.measureText(finalTextContent, textSize);
                
                // 幅の計算
                float measuredWidth;
                if (widthMode == YogaMeasureMode.EXACTLY) {
                    measuredWidth = width;
                } else if (widthMode == YogaMeasureMode.AT_MOST) {
                    measuredWidth = Math.min(bounds.width, width);
                } else {
                    measuredWidth = bounds.width;
                }
                
                // 高さの計算
                float measuredHeight;
                if (heightMode == YogaMeasureMode.EXACTLY) {
                    measuredHeight = height;
                } else if (heightMode == YogaMeasureMode.AT_MOST) {
                    measuredHeight = Math.min(bounds.height, height);
                } else {
                    measuredHeight = bounds.height;
                }
                
                // パディングを考慮
                float padL = yogaNode.getPadding(YogaEdge.LEFT).value;
                float padR = yogaNode.getPadding(YogaEdge.RIGHT).value;
                float padT = yogaNode.getPadding(YogaEdge.TOP).value;
                float padB = yogaNode.getPadding(YogaEdge.BOTTOM).value;

                float finalWidth = measuredWidth + (Float.isNaN(padL) ? 0 : padL) + (Float.isNaN(padR) ? 0 : padR);
                float finalHeight = measuredHeight + (Float.isNaN(padT) ? 0 : padT) + (Float.isNaN(padB) ? 0 : padB);

                float resultWidth = (widthMode == YogaMeasureMode.EXACTLY) ? width : finalWidth;
                float resultHeight = (heightMode == YogaMeasureMode.EXACTLY) ? height : finalHeight;

                return new YogaSize(resultWidth, resultHeight);
            });
        }
        
        // 子要素を追加
        for (Content child : element.getContent()) {
            if (child instanceof Element) {
                YogaNode childNode = createElement((Element) child);
                if (childNode != null) {
                    node.addChildAt(childNode, node.getChildCount());
                }
            }
        }
        
        return node;
    }
    
    /**
     * YogaStyleをYogaNodeに適用
     */
    private void applyStyleToNode(YogaNode node, YogaStyleParser.YogaStyle style) {
        // Display
        node.setDisplay(style.display);
        
        // Flex Direction
        node.setFlexDirection(style.flexDirection);
        
        // Flex
        if (!Float.isNaN(style.flex)) {
            node.setFlex(style.flex);
        } else {
            if (style.flexGrow > 0) node.setFlexGrow(style.flexGrow);
            if (style.flexShrink != 1) node.setFlexShrink(style.flexShrink);
            if (!Float.isNaN(style.flexBasis)) {
                node.setFlexBasis(style.flexBasis);
            }
        }
        
        // Justify & Align
        node.setJustifyContent(style.justifyContent);
        node.setAlignItems(style.alignItems);
        node.setAlignSelf(style.alignSelf);
        node.setWrap(style.flexWrap);
        
        // Dimensions
        if (!Float.isNaN(style.width)) {
            if (style.widthUnit == YogaUnit.PERCENT) {
                node.setWidthPercent(style.width);
            } else if (style.widthUnit == YogaUnit.POINT) {
                node.setWidth(style.width);
            } else {
                node.setWidthAuto();
            }
        }
        
        if (!Float.isNaN(style.height)) {
            if (style.heightUnit == YogaUnit.PERCENT) {
                node.setHeightPercent(style.height);
            } else if (style.heightUnit == YogaUnit.POINT) {
                node.setHeight(style.height);
            } else {
                node.setHeightAuto();
            }
        }
        
        // Min/Max Dimensions
        if (!Float.isNaN(style.minWidth)) node.setMinWidth(style.minWidth);
        if (!Float.isNaN(style.minHeight)) node.setMinHeight(style.minHeight);
        if (!Float.isNaN(style.maxWidth)) node.setMaxWidth(style.maxWidth);
        if (!Float.isNaN(style.maxHeight)) node.setMaxHeight(style.maxHeight);
        
        // Padding
        if (style.paddingTop > 0) node.setPadding(YogaEdge.TOP, style.paddingTop);
        if (style.paddingRight > 0) node.setPadding(YogaEdge.RIGHT, style.paddingRight);
        if (style.paddingBottom > 0) node.setPadding(YogaEdge.BOTTOM, style.paddingBottom);
        if (style.paddingLeft > 0) node.setPadding(YogaEdge.LEFT, style.paddingLeft);
        
        // Margin
        if (!Float.isNaN(style.marginTop)) {
            node.setMargin(YogaEdge.TOP, style.marginTop);
        }
        if (!Float.isNaN(style.marginRight)) {
            node.setMargin(YogaEdge.RIGHT, style.marginRight);
        }
        if (!Float.isNaN(style.marginBottom)) {
            node.setMargin(YogaEdge.BOTTOM, style.marginBottom);
        }
        if (!Float.isNaN(style.marginLeft)) {
            node.setMargin(YogaEdge.LEFT, style.marginLeft);
        }
        
        // Position
        node.setPositionType(style.positionType);
        
        // Gap
        if (style.gap > 0) {
            node.setGap(YogaGutter.ALL, style.gap);
        }
    }
    
    /**
     * ノードツリーを走査してレンダリング情報を取得
     */
    public List<RenderNode> getRenderNodes() {
        if (!layoutDirty && cachedRenderNodes != null) {
            return cachedRenderNodes;
        }
        cachedRenderNodes = new ArrayList<>();
        traverseForRender(rootNode, cachedRenderNodes);
        return cachedRenderNodes;
    }
    
    private void traverseForRender(YogaNode node, List<RenderNode> renderNodes) {
        NodeMetadata metadata = nodeMetadata.get(node);
        
        if (metadata != null) {
            RenderNode renderNode = new RenderNode();
            renderNode.x = node.getLayoutX();
            renderNode.y = node.getLayoutY();
            renderNode.width = node.getLayoutWidth();
            renderNode.height = node.getLayoutHeight();
            renderNode.style = metadata.style;
            renderNode.tagName = metadata.tagName;
            renderNode.textContent = metadata.textContent;
            renderNode.element = metadata.element;
            
            renderNodes.add(renderNode);
        }
        
        // 子ノードを走査
        for (int i = 0; i < node.getChildCount(); i++) {
            traverseForRender(node.getChild(i), renderNodes);
        }
    }
    
    /**
     * クリーンアップ
     */
    public void dispose() {
        disposeNode(rootNode);
        nodeMetadata.clear();
    }
    
    private void disposeNode(YogaNode node) {
        // Iterate backwards to safely remove children as we dispose them
        for (int i = node.getChildCount() - 1; i >= 0; i--) {
            YogaNode child = node.getChild(i);
            disposeNode(child); // Recursively dispose the child
            node.removeChild(i); // Remove child from this node
        }
    }
    
    /**
     * ノードメタデータ - 描画に必要な追加情報
     */
    private static class NodeMetadata {
        Element element;
        YogaStyleParser.YogaStyle style;
        String tagName;
        String textContent;
    }
    
    /**
     * レンダリングノード - 描画に必要な情報
     */
    public static class RenderNode {
        public float x;
        public float y;
        public float width;
        public float height;
        public YogaStyleParser.YogaStyle style;
        public String tagName;
        public String textContent;
        public Element element;
        
        public boolean isDiv() {
            return "div".equals(tagName);
        }
        
        public boolean hasText() {
            return textContent != null && !textContent.isEmpty();
        }
    }
}