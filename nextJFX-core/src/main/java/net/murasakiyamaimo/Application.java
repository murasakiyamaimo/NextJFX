package net.murasakiyamaimo;

import org.jdom2.*;
import org.jdom2.filter.ContentFilter;
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

    private ArrayList<Integer> defaultColor = new ArrayList<>();
    private long window;
    private long vg;
    private final ColorPalette colorPalette = new ColorPalette();
    private final Sizer sizer = new Sizer();
    private final Stack<Float> currentYstack = new Stack<>();
    private float currentY;
    private int fbWidth, fbHeight;
    private int rem = 16;

    private static final String FONT_NAME = "mplus-1p";

    private JFX caller;
    private final Map<ClickableRegion, Method> clickableElements = new HashMap<>();

    private double clickX, clickY;
    private boolean isClicked;

    private final SAXBuilder saxBuilder = new SAXBuilder();

    public void launch(JFX layout, Settings settings) {
        new Application().run(layout, settings);
    }

    public void run(JFX caller, Settings settings) {
        this.caller = caller;

        System.out.println("LWJGL " + Version.getVersion() + "!");

        this.settings = settings;

        defaultColor = this.settings.getDefaultColor();

        currentYstack.push(0f);

        init();
        loop();

        // 終了処理
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // ウィンドウ表示
        glfwShowWindow(window);
    }

    private void div(ArrayList<Integer> rgb, float size, float width, Map<String, Float> radius) {
        float height = size * rem;
        NVGColor color = create();
        nvgBeginPath(vg);

        final float[] radTopLeft = {0f};
        final float[] radTopRight = {0f};
        final float[] radBotRight = {0f};
        final float[] radBotLeft = {0f};

        radius.forEach((key, value) -> {
            switch (key) {
                case "all" -> {
                    radTopLeft[0] = value;
                    radTopRight[0] = value;
                    radBotRight[0] = value;
                    radBotLeft[0] = value;
                }
                case "t" -> {
                    radTopLeft[0] = value;
                    radTopRight[0] = value;
                }
                case "r" -> {
                    radTopRight[0] = value;
                    radBotRight[0] = value;
                }
                case "b" -> {
                    radBotLeft[0] = value;
                    radBotRight[0] = value;
                }
                case "l" -> {
                    radBotLeft[0] = value;
                    radTopLeft[0] = value;
                }
                case "tl" -> radTopLeft[0] = value;
                case "tr" -> radTopRight[0] = value;
                case "br" -> radBotRight[0] = value;
                case "bl" -> radBotLeft[0] = value;
            }
        });

        nvgRoundedRectVarying(vg, 0, currentYstack.peek() * rem, width * rem, height, radTopLeft[0] * rem, radTopRight[0] * rem, radBotRight[0] * rem, radBotLeft[0] * rem);
        nvgFillColor(vg, nvgRGBAf((float) rgb.get(0) / 255, (float) rgb.get(1) / 255, (float) rgb.get(2) / 255, 1.0f, color));
        nvgFill(vg);
    }

    private void text(ArrayList<Integer> rgb, float size, String literal) {
        NVGColor color = create();
        nvgFontSize(vg, size * rem);
        nvgFontFace(vg, FONT_NAME);
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_TOP);
        nvgFillColor(vg, nvgRGBAf((float) rgb.get(0) / 255, (float) rgb.get(1) / 255, (float) rgb.get(2) / 255, 1.0f, color));
        nvgText(vg, 0, currentYstack.peek() * rem + 9, literal);
    }

    private void analyze(Content content, float size, String[] textColor) {
        text(hex2rgb(colorPalette.get(textColor[1], textColor[2])), size, ((Text) content).getText());
    }

    private void analyze(Content content) {
        if (content instanceof Element) {
            Element element = (Element) content;
            float h = 0,w = (float) fbWidth / rem;
            String[] parts;
            String[] bgColor = {"bg", "none", "none"};
            String[] textColor = {"text", "gray", "900"};
            Map<String, Float> radius = new HashMap<>();
            float textSize = 1;
            if (element.getAttributeValue("className") != null) {
                for (String css : element.getAttributeValue("className").split("\\s+")) {
                    if (css.startsWith("bg-")) {
                        bgColor = css.split("-");
                    } else if (css.startsWith("text-")) {
                        parts = css.split("-");
                        if (parts[1].contains("xl")) {
                            textSize = sizer.xl2px(parts[1].charAt(0));
                        } else {
                            textColor = css.split("-");
                        }
                    } else if (css.startsWith("h-")) {
                        parts = css.split("-");
                        if (parts[1].equals("screen")) {
                            h = fbHeight;
                        } else {
                            try {
                                h = Float.parseFloat(parts[1]) / 4;
                            } catch (NumberFormatException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    } else if (css.startsWith("w-")) {
                        parts = css.split("-");
                        if (parts[1].equals("screen")) {
                            w = (float) fbWidth / rem;
                        } else {
                            try {
                                w = Float.parseFloat(parts[1]) / 4;
                            } catch (NumberFormatException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    } else if (css.startsWith("rounded-")) {
                        parts = css.split("-");
                        switch (parts[1]) {
                            case "t", "r", "b", "l", "tl", "tr", "br", "bl" -> radius.put(parts[1], sizer.rounded(parts[2]));
                            default -> radius.put("all", sizer.rounded(parts[1]));
                        }
                    }
                }
            }

            if (h == 0 && textSize != 1) h = textSize;

            String onClickHandler = element.getAttributeValue("onClick");
            if (onClickHandler != null) {
                try {
                    Method handlerMethod = caller.getClass().getMethod(onClickHandler);

                    ClickableRegion region = new ClickableRegion(0, currentYstack.peek() * rem, w * rem, h * rem);
                    clickableElements.put(region, handlerMethod);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }
            if (Objects.equals(element.getName(), "div")) {
                div(hex2rgb(colorPalette.get(bgColor[1], bgColor[2])), h, w, radius);
            }
            if (!element.getContent(new ContentFilter(ContentFilter.TEXT)).isEmpty()) {
                for (Content text : element.getContent(new ContentFilter(ContentFilter.TEXT))) {
                    analyze(text, textSize, textColor);
                }
            } else if (!element.getContent(new ContentFilter(ContentFilter.CDATA)).isEmpty()) {
                for (Content cdata : element.getContent(new ContentFilter(ContentFilter.CDATA))) {
                    analyze(cdata);
                }
            }
            currentY = currentYstack.peek() + h;

            currentYstack.push(0f);
            for (Content child : element.getContent(new ContentFilter(ContentFilter.ELEMENT))) {
                analyze(child);
            }
            currentY += currentYstack.peek();
            currentYstack.pop();
        } else if (content instanceof CDATA) {
            String code = ((CDATA) content).getText();
            code = code.trim().replaceAll("^\\{|}$", "");
            String className = null;

            Parent parent = content.getParent();
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
                        for (Content literal : saxBuilder.build(new StringReader(contentString)).getContent()) {
                            analyze(literal);
                        }
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

                        System.out.println(contentString);

                        for (Content literal : saxBuilder.build(new StringReader(contentString)).getContent()) {
                            analyze(literal);
                        }
                    } catch (NoSuchFieldException | IllegalAccessException | IOException | JDOMException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    private void loop() {

        // メインループ
        while (!glfwWindowShouldClose(window)) {
            String code = caller.render();

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

            List<Content> dom;
            try {
                dom = saxBuilder.build(new StringReader(result.toString())).getContent();
            } catch (IOException | JDOMException e) {
                throw new RuntimeException(e);
            }

            clickableElements.clear();

            int[] w = new int[1];
            int[] h = new int[1];
            int[] fw = new int[1];
            int[] fh = new int[1];

            glfwGetWindowSize(window, w, h);
            int winWidth = w[0];
            int winHeight = h[0];

            glfwGetFramebufferSize(window, fw, fh);
            fbWidth = fw[0];
            fbHeight = fh[0];

            float pixelRatio = (float) fbWidth / (float) winWidth;

            // --- 描画前の初期設定 ---
            glViewport(0, 0, fbWidth, fbHeight);
            glClearColor(0.1f, 0.12f, 0.14f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);

            // --- NanoVG描画開始 ---
            nvgBeginFrame(vg, winWidth, winHeight, pixelRatio);

            NVGColor color = create();
            nvgBeginPath(vg);
            nvgRect(vg, 0, 0, fbWidth, fbHeight);
            nvgFillColor(vg, nvgRGBAf((float) defaultColor.get(0) / 255, (float) defaultColor.get(1) / 255, (float) defaultColor.get(2) / 255, 1.0f, color));
            nvgFill(vg);

            for (Content child : dom) {
                analyze(child);
            }

            if (isClicked) {
                double[] xpos = new double[1], ypos = new double[1];
                glfwGetCursorPos(window, xpos, ypos);
                for (Map.Entry<ClickableRegion, Method> entry : clickableElements.entrySet()) {
                    if (entry.getKey().contains((float) clickX, (float) clickY)) {
                        try {
                            entry.getValue().invoke(caller);
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