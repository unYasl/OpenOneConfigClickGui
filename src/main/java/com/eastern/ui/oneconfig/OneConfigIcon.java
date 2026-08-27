package com.eastern.ui.oneconfig;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Data;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.svg.SVGDOM;
import io.github.humbleui.types.Rect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OneConfigIcon {
    private static final Logger LOGGER = LoggerFactory.getLogger("OneConfigIcon");

    private static final Pattern VIEWBOX = Pattern.compile("viewBox=\"0 0 (\\d+(?:\\.\\d+)?) (\\d+(?:\\.\\d+)?)\"");

    private static final Pattern ROOT_SIZE = Pattern.compile(
            "<svg[^>]*?\\swidth=\"(\\d+(?:\\.\\d+)?)(?:px)?\"[^>]*?\\sheight=\"(\\d+(?:\\.\\d+)?)(?:px)?\"");

    private static final Pattern HEX_COLOR_ATTR =
            Pattern.compile("(fill|stroke)=\"#[0-9a-fA-F]{3,8}\"");

    private static String stripRootSize(String svg) {
        int open = svg.indexOf("<svg");
        if (open < 0) return svg;
        int close = svg.indexOf('>', open);
        if (close < 0) return svg;
        String head = svg.substring(open, close)
                .replaceAll("\\s(width|height)\\s*=\\s*\"[^\"]*\"", "");
        return svg.substring(0, open) + head + svg.substring(close);
    }

    public static final String LOGO = "oc/icons/OneConfigFullDark";
    public static final String COPYRIGHT_FILL = "oc/icons/CopyrightFill";
    public static final String FADERS = "oc1/hud";
    public static final String USERS = "oc/icons/users-02";
    public static final String BRUSH = "oc1/paintbrush";
    public static final String SETTINGS = "oc/icons/settings-02";
    public static final String LAYOUT = "oc1/hud";
    public static final String X_CLOSE = "oc1/close";
    public static final String ARROW_LEFT = "oc1/left-arrow";
    public static final String ARROW_RIGHT = "oc1/right-arrow";
    public static final String CARET_RIGHT = "oc1/right-arrow";
    public static final String SEARCH = "oc1/search";
    public static final String HEART_FILL = "oc1/star-filled";
    public static final String HEART_OUTLINE = "oc1/star";
    public static final String DROPDOWN = "oc/old-icons/DropdownList";
    public static final String CHECKBOX_TICK = "oc/old-icons/CheckboxTick";
    public static final String CHEVRON_UP = "oc1/up";
    public static final String CHEVRON_DOWN = "oc1/down";
    public static final String KEYSTROKE = "oc1/keyboard";
    public static final String COG = "oc1/cog";

    public static final String INFO_ARROW = "oc/icons/InfoArrow";
    public static final String CHECK_CIRCLE = "oc/old-icons/CheckCircle";
    public static final String WARNING = "oc/old-icons/Warning";
    public static final String ERROR = "oc/old-icons/Error";

    public static final String CAT_CLIENT = "oc1/console";

    public static final String MUSIC = "oc1/music";
    public static final String PLAY = "oc1/play";
    public static final String PAUSE = "oc1/pause";
    public static final String SKIP_NEXT = "oc1/skip-next";
    public static final String SKIP_PREV = "oc1/skip-prev";
    public static final String VOLUME = "oc1/volume";
    public static final String SHUFFLE = "oc1/shuffle";
    public static final String REFRESH = "oc1/refresh";
    public static final String CAT_COMBAT = "oc1/combat";
    public static final String CAT_MISC = "oc1/qol";
    public static final String CAT_MOVEMENT = "oc/icons/movement";
    public static final String CAT_PLAYER = "oc/icons/player";
    public static final String CAT_RENDER = "oc1/eye";
    public static final String CAT_WORLD = "oc/icons/world";
    public static final String CONFIGS = "oc/icons/configsicon";

    private static final Set<String> MULTICOLOR = Set.of(LOGO);

    private record IconEntry(SVGDOM dom, float vbW, float vbH) {}

    private static final Map<String, byte[]> RAW_CACHE = new HashMap<>();
    private static final Map<Long, IconEntry> DOM_CACHE = new HashMap<>();
    private static final Set<Long> FAILED = new HashSet<>();

    private OneConfigIcon() {}

    private static byte[] load(String name) {
        return RAW_CACHE.computeIfAbsent(name, n -> {
            String path = "/assets/eastern/" + n + ".svg";
            try (java.io.InputStream is = OneConfigIcon.class.getResourceAsStream(path)) {
                if (is == null) {
                    LOGGER.warn("Missing OneConfig icon: {}", path);
                    return new byte[0];
                }
                return is.readAllBytes();
            } catch (Exception e) {
                LOGGER.warn("Failed to load OneConfig icon: {}", path, e);
                return new byte[0];
            }
        });
    }

    private static String toHex(int color) {
        return String.format("#%02X%02X%02X", (color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF);
    }

    private static IconEntry makeDom(String name, int color) {
        byte[] raw = load(name);
        if (raw.length == 0) return null;
        try {
            String original = new String(raw, StandardCharsets.UTF_8);
            String svg = stripRootSize(original);
            if (!MULTICOLOR.contains(name)) {

                svg = svg.replace("currentColor", toHex(color))
                        .replace("fill=\"white\"", "fill=\"" + toHex(color) + "\"")
                        .replace("fill=\"#fff\"", "fill=\"" + toHex(color) + "\"")
                        .replace("fill=\"#FFFFFF\"", "fill=\"" + toHex(color) + "\"")
                        .replace("stroke=\"white\"", "stroke=\"" + toHex(color) + "\"")
                        .replace("stroke=\"#fff\"", "stroke=\"" + toHex(color) + "\"")
                        .replace("stroke=\"#FFFFFF\"", "stroke=\"" + toHex(color) + "\"");
                svg = HEX_COLOR_ATTR.matcher(svg).replaceAll("$1=\"" + toHex(color) + "\"");
            }

            float vbW = 24f, vbH = 24f;
            Matcher m = VIEWBOX.matcher(svg);
            if (m.find()) {
                vbW = Float.parseFloat(m.group(1));
                vbH = Float.parseFloat(m.group(2));
            } else {
                Matcher rs = ROOT_SIZE.matcher(original);
                if (rs.find()) {
                    vbW = Float.parseFloat(rs.group(1));
                    vbH = Float.parseFloat(rs.group(2));
                }
            }

            try (Data data = Data.makeFromBytes(svg.getBytes(StandardCharsets.UTF_8))) {
                SVGDOM dom = new SVGDOM(data);
                dom.setContainerSize(vbW, vbH);
                return new IconEntry(dom, vbW, vbH);
            }
        } catch (Throwable t) {
            LOGGER.warn("Failed to parse OneConfig icon: {}", name, t);
            return null;
        }
    }

    private static IconEntry domFor(String name, int color) {
        long key = ((long) name.hashCode() << 32) | (color & 0xFFFFFFFFL);
        if (FAILED.contains(key)) return null;
        IconEntry entry = DOM_CACHE.get(key);
        if (entry == null) {
            entry = makeDom(name, color);
            if (entry == null) {
                FAILED.add(key);
                return null;
            }
            DOM_CACHE.put(key, entry);
        }
        return entry;
    }

    public static void draw(Canvas canvas, String name, float x, float y, float w, float h, int color) {
        IconEntry entry = domFor(name, color);
        if (entry == null) return;

        float alpha = ((color >>> 24) & 0xFF) / 255f;

        float scale = Math.min(w / entry.vbW(), h / entry.vbH());
        float dw = entry.vbW() * scale, dh = entry.vbH() * scale;
        float dx = x + (w - dw) / 2f, dy = y + (h - dh) / 2f;

        canvas.save();
        canvas.translate(dx, dy);
        canvas.scale(scale, scale);
        if (alpha < 1f) {
            try (Paint p = new Paint().setAlphaf(alpha)) {
                canvas.saveLayer(Rect.makeWH(entry.vbW(), entry.vbH()), p);
                entry.dom().render(canvas);
                canvas.restore();
            }
        } else {
            entry.dom().render(canvas);
        }
        canvas.restore();
    }

    public static void drawCentered(Canvas canvas, String name, float cx, float cy, float size, int color) {
        draw(canvas, name, cx - size / 2f, cy - size / 2f, size, size, color);
    }

    private static final Map<String, io.github.humbleui.skija.Image> PNG_CACHE = new HashMap<>();
    private static final Set<String> PNG_FAILED = new HashSet<>();

    public static void drawPng(Canvas canvas, String path, float x, float y, float w, float h) {
        io.github.humbleui.skija.Image img = PNG_CACHE.get(path);
        if (img == null) {
            if (PNG_FAILED.contains(path)) return;
            String full = "/assets/eastern/" + path;
            try (java.io.InputStream is = OneConfigIcon.class.getResourceAsStream(full)) {
                if (is == null) {
                    LOGGER.warn("Missing png: {}", full);
                    PNG_FAILED.add(path);
                    return;
                }
                img = io.github.humbleui.skija.Image.makeFromEncoded(is.readAllBytes());
                PNG_CACHE.put(path, img);
            } catch (Exception e) {
                LOGGER.warn("Failed to load png: {}", full, e);
                PNG_FAILED.add(path);
                return;
            }
        }
        float iw = img.getWidth(), ih = img.getHeight();
        float scale = Math.min(w / iw, h / ih);
        float dw = iw * scale, dh = ih * scale;
        canvas.save();
        canvas.translate(x + (w - dw) / 2f, y + (h - dh) / 2f);
        canvas.scale(scale, scale);
        canvas.drawImage(img, 0f, 0f);
        canvas.restore();
    }
}
