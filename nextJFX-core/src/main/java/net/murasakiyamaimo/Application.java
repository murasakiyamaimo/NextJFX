package net.murasakiyamaimo;

import org.appliedenergistics.yoga.*;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.nanovg.NVGColor.create;
import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVGGL3.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Application {
    private Settings settings = new Settings();

    private String code;

    private ArrayList<Integer> defaultColor = new ArrayList<>();
    private long window;
    private long vg;
    private final ColorPalette colorPalette = new ColorPalette();
    private int fbWidth, fbHeight;
    private int rem;

    private static final String FONT_NAME = "mplus-1p";

    private JFX caller;
    private final Map<ClickableRegion, MethodInvocation> clickableElements = new HashMap<>(); // 型をMethodInvocationに変更
    
    // メソッド検索のキャッシュ
    private final Map<String, MethodHandle> methodHandleCache = new HashMap<>();
    private static final Pattern HANDLER_PATTERN = Pattern.compile("(\\w+)(?:\\((.*)\\))?");
    private static final Pattern ARG_PATTERN = Pattern.compile("(?<=\\s|^)(?:\"[^\"]*\"|'[^']*'|[^,]+?)(?=\\s*($|,))");

    private double clickX, clickY;
    private boolean isClicked;

    private final SAXBuilder saxBuilder = new SAXBuilder();

    private YogaLayoutEngine layoutEngine;

    public void launch(JFX layout, Settings settings) {
        new Application().run(layout, settings);
    }

    public void run(JFX caller, Settings settings) {
        this.caller = caller;

        code = caller.render();

        System.out.println("LWJGL " + Version.getVersion() + "!");
        System.out.println("Using Yoga Layout for flexbox-based layouts");

        this.settings = settings;

        defaultColor = this.settings.getDefaultColor();

        init();
        try {
            loop();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        long totalTime = 0;

        for (Long time : times) {
            totalTime += time;
        }

        long averageTime = totalTime / times.size();
        System.out.println(averageTime);

        // 終了処理
        if (layoutEngine != null) {
            layoutEngine.dispose();
        }

        nvgDelete(vg); // この行を追加
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    private void init() {

        // エラーコールバック
        GLFWErrorCallback.createPrint(System.err).set();

        // GLFW初期化
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        // OpenGLのバージョン指定 (ここでは3.2 Core Profile)
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_STENCIL_BITS, 8);

        // ウィンドウ作成
        window = glfwCreateWindow(settings.getWindowSize()[0], settings.getWindowSize()[1], settings.getWindowName(), NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        rem = settings.getRem();

        // コンテキスト設定
        glfwMakeContextCurrent(window);
        // V-Sync有効化
        glfwSwapInterval(1);

        GL.createCapabilities();

        vg = nvgCreate(NVG_ANTIALIAS | NVG_STENCIL_STROKES);
        if (vg == NULL) {
            throw new RuntimeException("Could not init nanovg");
        }

        glfwSetMouseButtonCallback(window, (window, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
                double[] x = new double[1], y = new double[1];
                glfwGetCursorPos(window, x, y);
                this.clickX = x[0];
                this.clickY = y[0];
                this.isClicked = true;
            }
        });

        // フォントの読み込み
        try {
            ByteBuffer fontBuffer = ioResourceToByteBuffer("fonts/MPLUS1p-Regular.ttf", 512 * 1024);
            int font = nvgCreateFontMem(vg, FONT_NAME, fontBuffer, false);
            if (font == -1) {
                throw new RuntimeException("Could not add font");
            }
            MemoryUtil.memFree(fontBuffer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // ウィンドウ表示
        glfwShowWindow(window);
    }

    private void renderDiv(YogaLayoutEngine.RenderNode node) {
        String[] bgColor = node.style.bgColor;
        ArrayList<Integer> rgb = hex2rgb(colorPalette.get(bgColor[1], bgColor[2]));

        NVGColor color = create();
        nvgBeginPath(vg);

        Map<String, Float> radius = node.style.getBorderRadius();
        float radTopLeft;
        float radTopRight;
        float radBotRight;
        float radBotLeft;

        if (radius.containsKey("all")) {
            radTopLeft = radTopRight = radBotRight = radBotLeft = radius.get("all");
        } else {
            radTopLeft = radius.getOrDefault("tl", 0f);
            radTopRight = radius.getOrDefault("tr", 0f);
            radBotRight = radius.getOrDefault("br", 0f);
            radBotLeft = radius.getOrDefault("bl", 0f);
        }

        nvgRoundedRectVarying(vg, node.x, node.y, node.width, node.height,
                             radTopLeft, radTopRight, radBotRight, radBotLeft);
        nvgFillColor(vg, nvgRGBAf((float) rgb.get(0) / 255, (float) rgb.get(1) / 255, (float) rgb.get(2) / 255, 1.0f, color));
        nvgFill(vg);
    }

    private void renderText(YogaLayoutEngine.RenderNode node) {
        if (!node.hasText()) return;

        String[] textColor = node.style.textColor;
        ArrayList<Integer> rgb = hex2rgb(colorPalette.get(textColor[1], textColor[2]));
        float textSize = node.style.textSize;

        NVGColor color = create();
        nvgFontSize(vg, textSize * rem);
        nvgFontFace(vg, FONT_NAME);
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_TOP);
        nvgFillColor(vg, nvgRGBAf((float) rgb.get(0) / 255, (float) rgb.get(1) / 255, (float) rgb.get(2) / 255, 1.0f, color));
        nvgText(vg, node.x, node.y + 9, node.textContent);
    }

    // MethodInvocation クラスの定義
    private static class MethodInvocation {
        private final MethodHandle methodHandle;
        private final List<String> argumentStrings; // 生の引数文字列
        private final JFX callerInstance; // メソッドを呼び出すインスタンス
        private final Object[] preParsedArguments;

        public MethodInvocation(MethodHandle methodHandle, List<String> argumentStrings, JFX callerInstance) {
            this.methodHandle = methodHandle;
            this.argumentStrings = argumentStrings;
            this.callerInstance = callerInstance;
            this.preParsedArguments = parseArguments(argumentStrings);
        }

        private Object[] parseArguments(List<String> argumentStrings) {
            List<Object> actualArguments = new ArrayList<>();
            actualArguments.add(callerInstance); // 第1引数はレシーバ(this)

            for (String argString : argumentStrings) {
                argString = argString.trim();
                if ((argString.startsWith("'") && argString.endsWith("'")) || (argString.startsWith("\"") && argString.endsWith("\""))) {
                    actualArguments.add(argString.substring(1, argString.length() - 1));
                } else if (argString.matches("-?\\d+")) {
                    actualArguments.add(Integer.parseInt(argString));
                } else if (argString.equalsIgnoreCase("true") || argString.equalsIgnoreCase("false")) {
                    actualArguments.add(Boolean.parseBoolean(argString));
                } else {
                    actualArguments.add(argString);
                }
            }
            return actualArguments.toArray();
        }

        public void invoke() throws Throwable {
            methodHandle.invokeWithArguments(preParsedArguments);
        }
    }

    private MethodHandle findMethodHandle(String methodName, List<Class<?>> parameterTypes) throws NoSuchMethodException, IllegalAccessException {
        String cacheKey = methodName + parameterTypes.toString();
        if (methodHandleCache.containsKey(cacheKey)) {
            return methodHandleCache.get(cacheKey);
        }

        MethodHandles.Lookup lookup = MethodHandles.lookup();
        Method method = null;

        if (parameterTypes.isEmpty()) {
            try {
                method = caller.getClass().getMethod(methodName);
            } catch (NoSuchMethodException e) {
                // 引数ありを期待
            }
        }

        if (method == null) {
            method = caller.getClass().getMethod(methodName, parameterTypes.toArray(new Class[0]));
        }

        MethodHandle handle = lookup.unreflect(method);
        methodHandleCache.put(cacheKey, handle);
        return handle;
    }

    private List<Class<?>> resolveParameterTypes(List<String> argumentStrings) {
        List<Class<?>> types = new ArrayList<>();
        for (String argString : argumentStrings) {
            argString = argString.trim();
            if ((argString.startsWith("'") && argString.endsWith("'")) || (argString.startsWith("\"") && argString.endsWith("\""))) {
                types.add(String.class);
            } else if (argString.matches("-?\\d+")) {
                types.add(int.class);
            } else if (argString.equalsIgnoreCase("true") || argString.equalsIgnoreCase("false")) {
                types.add(boolean.class);
            } else {
                types.add(String.class);
            }
        }
        return types;
    }

    // onClickハンドラ文字列を解析するヘルパーメソッド
    private MethodInvocation parseClickHandler(String handlerString, JFX callerInstance) throws NoSuchMethodException, IllegalAccessException {
        // ラムダ式のような "{...}" 形式をチェック
        if (handlerString.startsWith("{") && handlerString.endsWith("}")) {
            throw new UnsupportedOperationException("Lambda expressions are not yet supported for onClick: " + handlerString);
        }

        // "=>" 形式の解析 (もしあれば)
        String cleanedHandlerString = handlerString.trim();
        if (cleanedHandlerString.startsWith("=>")) {
            cleanedHandlerString = cleanedHandlerString.substring(2).trim();
        }

        Matcher matcher = HANDLER_PATTERN.matcher(cleanedHandlerString);

        if (matcher.matches()) {
            String methodName = matcher.group(1);
            String argsString = matcher.group(2);
            List<String> arguments = new ArrayList<>();
            List<Class<?>> parameterTypes = new ArrayList<>();

            if (argsString != null && !argsString.trim().isEmpty()) {
                Matcher argMatcher = ARG_PATTERN.matcher(argsString);
                while (argMatcher.find()) {
                    arguments.add(argMatcher.group().trim());
                }
                parameterTypes = resolveParameterTypes(arguments);
            }
            MethodHandle handle = findMethodHandle(methodName, parameterTypes);
            return new MethodInvocation(handle, arguments, callerInstance);
        } else {
            MethodHandle handle = findMethodHandle(cleanedHandlerString, Collections.emptyList());
            return new MethodInvocation(handle, Collections.emptyList(), callerInstance);
        }
    }

    private void processClickHandlers(List<YogaLayoutEngine.RenderNode> renderNodes) {
        clickableElements.clear();

        for (YogaLayoutEngine.RenderNode node : renderNodes) {
            if (node.element == null) continue;

            String onClickHandlerString = node.element.getAttributeValue("onClick");
            if (onClickHandlerString != null) {
                try {
                    MethodInvocation invocation = parseClickHandler(onClickHandlerString, caller);
                    ClickableRegion region = new ClickableRegion(node.x, node.y, node.width, node.height);
                    clickableElements.put(region, invocation);
                } catch (UnsupportedOperationException e) {
                    System.err.println(e.getMessage());
                } catch (Exception e) { // parseClickHandler内で発生する可能性のある例外をキャッチ
                    System.err.println("Error parsing onClick handler '" + onClickHandlerString + "': " + e.getMessage());
                }
            }
        }
    }

    private String preprocessCode(String code) {
        Pattern pattern = Pattern.compile("\\{(.*?)}");
        Matcher matcher = pattern.matcher(code);

        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String innerContent = matcher.group(1);
            String replacement;
            if (innerContent.contains("&&") || innerContent.contains("?")) {
                replacement = "<![CDATA[{" + innerContent + "}]]>";
            } else {
                try {
                    replacement = caller.getClass().getDeclaredField(innerContent).toString();
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException(e);
                }
            }

            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private List<Content> expandConditionals(List<Content> dom) {
        List<Content> expandedDom = new ArrayList<>();

        for (Content content : dom) {
            if (content instanceof Element) {
                expandedDom.add(expandElement((Element) content));
            } else if (content instanceof CDATA) {
                List<Content> expanded = expandCDATA((CDATA) content);
                expandedDom.addAll(expanded);
            } else {
                expandedDom.add(content);
            }
        }

        return expandedDom;
    }

    private Element expandElement(Element element) {
        Element clonedElement = element.clone();
        clonedElement.removeContent();

        for (Content child : element.getContent()) {
            if (child instanceof Element) {
                clonedElement.addContent(expandElement((Element) child));
            } else if (child instanceof CDATA) {
                List<Content> expanded = expandCDATA((CDATA) child);
                for (Content expandedContent : expanded) {
                    clonedElement.addContent(expandedContent);
                }
            } else {
                clonedElement.addContent(child.clone());
            }
        }

        return clonedElement;
    }

    private List<Content> expandCDATA(CDATA cdata) {
        List<Content> result = new ArrayList<>();
        String code = cdata.getText();
        code = code.trim().replaceAll("^\\{|}$", "");

        Parent parent = cdata.getParent();
        String className = null;
        if (parent instanceof Element) {
            className = ((Element) parent).getAttributeValue("className");
        } else if (parent instanceof Document) {
            className = ((Document) parent).getRootElement().getAttributeValue("className");
        }

        if (code.contains("&&")) {
            try {
                String[] parts = code.split("\\s+&&\\s+");
                String fieldName = parts[0];
                String contentString = parts[1];

                Field booleanField = caller.getClass().getDeclaredField(fieldName);
                booleanField.setAccessible(true);
                boolean isVisible = (boolean) booleanField.get(caller);

                if (isVisible) {
                    SAXBuilder saxBuilder = new SAXBuilder();
                    if (className != null) {
                        contentString = "<text className=\"" + className + "\">" + contentString + "</text>";
                    } else {
                        contentString = "<text>" + contentString + "</text>";
                    }
                    result.addAll(saxBuilder.build(new StringReader(contentString)).getContent());
                }
            } catch (NoSuchFieldException | IllegalAccessException | IOException | JDOMException e) {
                throw new RuntimeException(e);
            }
        } else if (code.contains("?") && code.contains(":")) {
            Pattern pattern = Pattern.compile("(\\w+)\\s*\\?\\s*(.*?)\\s*:\\s*(.*)");
            Matcher matcher = pattern.matcher(code);

            if (matcher.matches()) {
                try {
                    String fieldName = matcher.group(1);

                    Field booleanField = caller.getClass().getDeclaredField(fieldName);
                    booleanField.setAccessible(true);
                    boolean isVisible = (boolean) booleanField.get(caller);

                    SAXBuilder saxBuilder = new SAXBuilder();
                    String contentString;

                    String contentToWrap = isVisible ? matcher.group(2) : matcher.group(3);

                    if (className != null) {
                        contentString = "<text className=\"" + className + "\">" + contentToWrap + "</text>";
                    } else {
                        contentString = "<text>" + contentToWrap + "</text>";
                    }

                    result.addAll(saxBuilder.build(new StringReader(contentString)).getContent());
                } catch (NoSuchFieldException | IllegalAccessException | IOException | JDOMException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return result;
    }

    private ArrayList<Long> times = new ArrayList<>();

    private void loop() throws Throwable {
        // メインループ
        while (!glfwWindowShouldClose(window)) {
            long startTime = System.nanoTime();

            String code = caller.render();

            // 変数展開と条件付きレンダリングの前処理
            String preprocessedCode = preprocessCode(code);

            List<Content> dom;
            try {
                dom = saxBuilder.build(new StringReader(preprocessedCode)).getContent();
            } catch (IOException | JDOMException e) {
                throw new RuntimeException(e);
            }

            // 条件付きコンテンツの展開
            dom = expandConditionals(dom);

            // フレームバッファサイズの取得
            int[] fw = new int[1];
            int[] fh = new int[1];
            glfwGetFramebufferSize(window, fw, fh);
            fbWidth = fw[0];
            fbHeight = fh[0];

            int[] w = new int[1];
            int[] h = new int[1];
            glfwGetWindowSize(window, w, h);
            int winWidth = w[0];
            int winHeight = h[0];
            float pixelRatio = (float) fbWidth / (float) winWidth;

            // Yoga Layout Engineの初期化または更新
            layoutEngine = new YogaLayoutEngine(rem, fbWidth, fbHeight, colorPalette, vg, FONT_NAME);

            // レイアウトツリーの構築
            layoutEngine.buildLayoutTree(dom);

            // レイアウト計算
            layoutEngine.calculateLayout();

            // レンダリングノードの取得
            List<YogaLayoutEngine.RenderNode> renderNodes = layoutEngine.getRenderNodes();

            // クリックハンドラーの処理
            processClickHandlers(renderNodes);

            // --- 描画前の初期設定 ---
            glViewport(0, 0, fbWidth, fbHeight);
            glClearColor(0.1f, 0.12f, 0.14f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);

            // --- NanoVG描画開始 ---
            nvgBeginFrame(vg, winWidth, winHeight, pixelRatio);

            // 背景描画
            NVGColor color = create();
            nvgBeginPath(vg);
            nvgRect(vg, 0, 0, fbWidth, fbHeight);
            nvgFillColor(vg, nvgRGBAf((float) defaultColor.get(0) / 255, (float) defaultColor.get(1) / 255, (float) defaultColor.get(2) / 255, 1.0f, color));
            nvgFill(vg);

            // レンダリングノードの描画
            for (YogaLayoutEngine.RenderNode node : renderNodes) {
                if (node.isDiv()) {
                    renderDiv(node);
                }
                if (node.hasText()) {
                    renderText(node);
                }
            }

            // クリックイベントの処理
            if (isClicked) {
                for (Map.Entry<ClickableRegion, MethodInvocation> entry : clickableElements.entrySet()) {
                    if (entry.getKey().contains((float) clickX, (float) clickY)) {
                        try {
                            entry.getValue().invoke();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                isClicked = false;
            }

            // --- NanoVG描画終了 ---
            nvgEndFrame(vg);

            // バッファ入れ替え
            glfwSwapBuffers(window);
            // イベント処理
            glfwPollEvents();

            long endTime = System.nanoTime();
            times.add(endTime - startTime);
        }
    }

    private ByteBuffer ioResourceToByteBuffer(String resource, int bufferSize) throws IOException {
        ByteBuffer buffer;
        try (
                InputStream source = Application.class.getClassLoader().getResourceAsStream(resource);
                ReadableByteChannel rbc = Channels.newChannel(source)
        ) {
            if (source == null) throw new IOException("Resource not found: " + resource);
            buffer = MemoryUtil.memAlloc(bufferSize);
            while (true) {
                int bytes = rbc.read(buffer);
                if (bytes == -1) {
                    break;
                }
                if (buffer.remaining() == 0) {
                    buffer = MemoryUtil.memRealloc(buffer, buffer.capacity() * 2);
                }
            }
        }
        buffer.flip();
        return buffer;
    }

    public ArrayList<Integer> hex2rgb(String hex) {
        if (hex.equals("defaultColor")) {
            return defaultColor;
        }

        ArrayList<Integer> rgb = new ArrayList<>();
        Color color = Color.decode(hex);
        rgb.add(color.getRed());
        rgb.add(color.getGreen());
        rgb.add(color.getBlue());
        return rgb;
    }
}
