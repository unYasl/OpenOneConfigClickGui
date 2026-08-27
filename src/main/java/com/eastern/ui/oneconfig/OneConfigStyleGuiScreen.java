package com.eastern.ui.oneconfig;

import com.eastern.module.Category;
import com.eastern.module.Module;
import com.eastern.module.impl.hud.HUDModule;
import com.eastern.module.impl.render.ClickGui;
import com.eastern.module.value.Value;
import com.eastern.module.value.impl.*;
import com.eastern.config.impl.ModuleConfig;
import com.eastern.ui.hud.HUDDesignerScreen;
import com.eastern.util.IMinecraft;
import com.eastern.util.animation.anime.Animation;
import com.eastern.util.animation.anime.Easings;
import com.eastern.util.font.SkiaFontManager;
import com.eastern.util.skia.BlurUtil;
import com.eastern.util.skia.SkiaRenderer;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.ClipMode;
import io.github.humbleui.skija.FilterTileMode;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.ImageFilter;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.Shader;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OneConfigStyleGuiScreen extends Screen implements IMinecraft {

    private static final int GRAY_900 = 0x99232D32;
    private static final int GRAY_850 = 0xFF151719;
    private static final int GRAY_800 = 0xC411171C;
    private static final int GRAY_800_95 = 0xB3151C22;
    private static final int GRAY_700 = 0xFF222326;
    private static final int GRAY_600 = 0xFF2A2C30;
    private static final int GRAY_500 = 0xFF313338;
    private static final int GRAY_400 = 0xFF373B45;
    private static final int GRAY_400_80 = 0xCC373B45;
    private static final int GRAY_400_60 = 0x99373B45;
    private static final int GRAY_300 = 0xFF494F5C;

    private int primary700 = 0xFF1247B2, primary700_80 = 0xCC1247B2;
    private int primary600 = 0xFF1452CC, primary500 = 0xFF1967FF;

    private void updateTheme() {
        ClickGui cg = (ClickGui) instance.getModuleManager().getModule(ClickGui.class);
        int rgb = cg != null ? cg.getThemeColor() : 0xFF1967FF;
        float[] hsb = Color.RGBtoHSB((rgb >>> 16) & 0xFF, (rgb >>> 8) & 0xFF, rgb & 0xFF, null);
        primary500 = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
        primary600 = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2] * 0.85f);
        primary700 = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2] * 0.68f);
        primary700_80 = (primary700 & 0x00FFFFFF) | 0xCC000000;
    }

    private static final int WHITE_50 = 0x80FFFFFF;
    private static final int WHITE_60 = 0x99FFFFFF;
    private static final int WHITE_80 = 0xCCFFFFFF;
    private static final int WHITE_90 = 0xE5FFFFFF;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int ERROR_600_80 = 0xCCD92020;

    private static final float WIN_W = 1280f, WIN_H = 800f;
    private static final float SIDEBAR_W = 244f, SPLIT_X = 224f, HEADER_H = 72f;
    private static final float CONTENT_W = 1056f, CONTENT_H = 728f;
    private static final float NAV_BTN_W = 192f, NAV_BTN_H = 36f;
    private static final float CARD_W = 244f, CARD_TOP_H = 87f, CARD_H = 119f;
    private static final float CARD_STEP_X = 260f, CARD_STEP_Y = 135f;
    private static final float OPT_X_OFF = 30f;
    private static final float PANEL_X_OFF = 14f;
    private static final float PANEL_W = 1024f;
    private static final float CTRL_X = 352f;
    private static final float SLIDER_W = 512f, DROPDOWN_W = 640f;

    private static final float SB_CREDITS_Y = 80f;
    private static final float SB_TITLE1_BASE = 142f;
    private static final float SB_CAT_Y0 = 152f;
    private static final float SB_TITLE2_BASE = 456f;
    private static final float SB_PREFS_Y = 466f;
    private static final float SB_CONFIGS_Y = 502f;
    private static final float SB_MUSIC_Y = 538f;
    private static final float SB_HUD_Y = 704f;
    private static final float SB_CLOSE_Y = 748f;

    private final Animation openAnimation = new Animation(Easings.EXPO_OUT, 500);
    private final Animation pageAnimation = new Animation(Easings.EXPO_OUT, 300);
    private boolean closing = false;
    private Module prevModule;
    private NavPage prevNavPage = NavPage.NONE;
    private boolean pageAnimBack;

    private static int selectedCat = 0;

    private enum NavPage { NONE, PREFS, CREDITS, CONFIGS, MUSIC }
    private static NavPage navPage = NavPage.NONE;
    private Module openModule;
    private String searchQuery = "";
    private boolean searchFocused = false;

    private final Animation gridScrollAnim = new Animation(Easings.QUAD_OUT, 150);
    private final Animation settingsScrollAnim = new Animation(Easings.QUAD_OUT, 150);
    private float gridScroll, settingsScroll;
    private float modsContentH = 0f, settingsContentH = 0f;

    private final Animation sidebarMove = new Animation(Easings.EXPO_OUT, 300);
    private float sidebarCurY = SB_CAT_Y0 + selectedCat * 36f;

    private final Map<Object, Animation> switchAnims = new HashMap<>();

    private NumberValue dragNumber;
    private ColorValue dragColor;
    private int dragColorChannel;
    private boolean dragSV;
    private ModeValue openDropdown;
    private String dropdownQuery = "";
    private float dropdownScroll;
    private ColorValue openColor;
    private StringValue focusText;
    private Module bindModule;

    private final java.util.Set<String> favoriteMods = new java.util.HashSet<>();

    private String configsInput = "";
    private boolean configsInputFocus = false;
    private static java.io.File profilesDir() {
        return new java.io.File(com.eastern.Eastern.name, "configs");
    }
    private static List<String> listProfiles() {
        List<String> names = new ArrayList<>();
        java.io.File[] files = profilesDir().listFiles((dir, n) -> n.toLowerCase(Locale.ROOT).endsWith(".json"));
        if (files != null) {
            for (java.io.File f : files) {
                String n = f.getName();
                names.add(n.substring(0, n.length() - 5));
            }
        }
        names.sort(String::compareToIgnoreCase);
        return names;
    }
    private void saveProfileFromInput() {
        String name = configsInput.trim().replaceAll("[\\\\/:*?\"<>|]", "");
        if (name.isEmpty()) return;
        new ModuleConfig(name).save();
        configsInput = "";
    }
    private void deleteProfile(String name) {
        java.io.File f = new java.io.File(profilesDir(), name.toLowerCase(Locale.ROOT) + ".json");
        if (f.exists() && !f.delete()) {
            com.eastern.Eastern.logger.debug("Failed to delete profile: {}", name);
        }
    }

    private final List<HitZone> hits = new ArrayList<>();

    private sealed interface Hit {
        record CatButton(int index) implements Hit {}
        record NavPreferences() implements Hit {}
        record NavCredits() implements Hit {}
        record NavConfigs() implements Hit {}
        record NavMusic() implements Hit {}
        record ConfigInput() implements Hit {}
        record ConfigSave() implements Hit {}
        record ConfigLoad(String name) implements Hit {}
        record ConfigDelete(String name) implements Hit {}
        record EditHud() implements Hit {}
        record Close() implements Hit {}
        record Back() implements Hit {}
        record SearchBox() implements Hit {}
        record Card(Module module) implements Hit {}
        record CardToggle(Module module) implements Hit {}
        record Favorite(Module module) implements Hit {}
        record Keybind(Module module) implements Hit {}
        record Switch(BoolValue value) implements Hit {}
        record ModuleSwitch(Module module) implements Hit {}
        record Slider(NumberValue value) implements Hit {}
        record StepperUp(NumberValue value) implements Hit {}
        record StepperDown(NumberValue value) implements Hit {}
        record Dropdown(ModeValue value) implements Hit {}
        record DropdownItem(ModeValue value, String option) implements Hit {}
        record ColorWell(ColorValue value) implements Hit {}
        record ColorSV(ColorValue value) implements Hit {}
        record ColorSlider(ColorValue value, int channel) implements Hit {}
        record TextField(StringValue value) implements Hit {}
    }

    private record HitZone(Hit hit, float x, float y, float w, float h) {
        boolean contains(double mx, double my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
    }

    public OneConfigStyleGuiScreen() {
        super(Text.of("OneConfig"));

        pageAnimation.setValue(1f);

        if (navPage == NavPage.PREFS) {
            ClickGui cg = (ClickGui) instance.getModuleManager().getModule(ClickGui.class);
            if (cg != null) openModule = cg;
            else navPage = NavPage.NONE;
        }

        float initY = switch (navPage) {
            case PREFS -> SB_PREFS_Y;
            case CREDITS -> SB_CREDITS_Y;
            case CONFIGS -> SB_CONFIGS_Y;
            case MUSIC -> SB_MUSIC_Y;
            case NONE -> SB_CAT_Y0 + selectedCat * 36f;
        };
        sidebarCurY = initY;
        sidebarMove.setValue(initY);
    }

    @Override
    protected void init() {
        if (openAnimation.getValue() < 0.01f && !closing) {
            openAnimation.setValue(0f);
            openAnimation.animate(1.0f);
        }
        ClickGui clickGui = (ClickGui) instance.getModuleManager().getModule(ClickGui.class);
        if (clickGui != null) clickGui.setEnabled(true);
        SkiaRenderer.updateSize(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());
    }

    @Override
    public void removed() {
        ClickGui clickGui = (ClickGui) instance.getModuleManager().getModule(ClickGui.class);
        if (clickGui != null) clickGui.setEnabled(false);

        instance.getConfigManager().saveAll();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        if (closing && !openAnimation.isRunning() && openAnimation.getValue() <= 0.01f) {
            super.close();
        }
    }

    @Override
    public void close() {
        if (!closing) {
            closing = true;
            openAnimation.animate(0f);
        }
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    private void drawBlurBackground(Canvas canvas, int radius) {
        Surface surface = SkiaRenderer.getSurface();
        if (surface == null) return;

        float normalized = Math.min(1f, Math.max(0f, (radius - 1f) / 129f));
        float sigma = 0.9f + (float) (Math.pow(normalized, 1.28f) * 14f);

        BlurUtil.drawBlurredBackbuffer(canvas, surface.getWidth(), surface.getHeight(), sigma, 1f);
    }

    private static void drawDropShadow(Canvas canvas, float x, float y, float w, float h, float r, int strength) {
        float t = Math.min(1f, Math.max(0f, strength / 100f));
        float blur = 32f * (0.25f + 0.75f * t);
        int alpha = (int) (255 * t * 0.7f) & 0xFF;
        final float spread = 8f;
        try (Paint paint = new Paint()) {
            paint.setColor((alpha << 24) | 0x000000);
            paint.setImageFilter(ImageFilter.makeBlur(blur, blur, FilterTileMode.DECAL));
            canvas.save();
            SkiaRenderer.clipRRect(canvas, x - spread, y - spread,
                    w + 2f * spread, h + 2f * spread, r + spread, ClipMode.DIFFERENCE);
            canvas.drawRRect(RRect.makeXYWH(x - spread, y - spread,
                    w + 2f * spread, h + 2f * spread, r + spread), paint);
            canvas.restore();
        }
    }

    public void renderSkia(Canvas canvas) {
        float progress = openAnimation.getValue();
        if (progress <= 0.001f && closing) return;

        updateTheme();

        float sw = mc.getWindow().getFramebufferWidth();
        float sh = mc.getWindow().getFramebufferHeight();
        float mcScale = (float) mc.getWindow().getScaleFactor();
        double mx = mc.mouse.getX() / mcScale;
        double my = mc.mouse.getY() / mcScale;

        canvas.save();
        canvas.resetMatrix();
        ClickGui blurCfg = (ClickGui) instance.getModuleManager().getModule(ClickGui.class);
        if (blurCfg != null && blurCfg.isBlurEnabled()) {
            drawBlurBackground(canvas, blurCfg.getBlurRadius());
        }

        SkiaRenderer.drawRect(canvas, 0f, 0f, sw, sh, 0x30000000);
        canvas.restore();

        uiScale = Math.min(sw / 1920f, sh / 1080f);
        if (uiScale < 1f) uiScale = Math.min(Math.min(1f, sw / WIN_W), Math.min(1f, sh / WIN_H));
        uiScale = (float) (Math.floor(uiScale / 0.05f) * 0.05f);

        float alpha = Math.min(1f, progress * 10f);
        float winScale = 0.9f + 0.1f * progress;
        float total = uiScale * winScale;

        winX = sw / 2f;
        winY = sh / 2f;

        canvas.save();
        canvas.translate(winX, winY);
        canvas.scale(total, total);
        canvas.translate(-WIN_W / 2f, -WIN_H / 2f);
        if (alpha < 1f) {
            try (Paint alphaPaint = new Paint().setAlphaf(alpha)) {
                canvas.saveLayer(Rect.makeWH(WIN_W, WIN_H), alphaPaint);
            }
        }

        double dmx = toDesignX(mx);
        double dmy = toDesignY(my);
        mouseXDesign = dmx;
        mouseYDesign = dmy;
        boolean lmb = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        hits.clear();

        if (blurCfg != null && blurCfg.isShadowEnabled()) {
            drawDropShadow(canvas, 0f, 0f, WIN_W, WIN_H, 20f, blurCfg.getShadowStrength());
        }

        canvas.save();
        canvas.clipRect(Rect.makeXYWH(0, 0, SPLIT_X, WIN_H));
        SkiaRenderer.drawRRect(canvas, 0, 0, SIDEBAR_W, WIN_H, 20f, GRAY_800_95);
        canvas.restore();
        canvas.save();
        canvas.clipRect(Rect.makeXYWH(SPLIT_X, 0, WIN_W - SPLIT_X, WIN_H));
        SkiaRenderer.drawRRect(canvas, SPLIT_X - 20f, 0, CONTENT_W + 20f, WIN_H, 20f, GRAY_800);
        canvas.restore();

        SkiaRenderer.drawRect(canvas, SPLIT_X - 0.5f, 0, 1f, WIN_H, GRAY_700);
        SkiaRenderer.drawRect(canvas, SPLIT_X, HEADER_H - 0.5f, WIN_W - SPLIT_X, 1f, GRAY_700);

        OneConfigIcon.drawPng(canvas, "icon.png", 27f, 18f, 40f, 40f);
        Font logoF = SkiaFontManager.getInterBold(15f);
        if (logoF != null) drawStringVC(canvas, "EASTERN", 75f, 40f, logoF, WHITE);

        drawSearchBar(canvas, dmx, dmy);

        drawNavArrows(canvas, dmx, dmy);

        drawSideBar(canvas, dmx, dmy);

        canvas.save();
        canvas.clipRect(Rect.makeXYWH(SPLIT_X, HEADER_H, CONTENT_W, CONTENT_H));
        float p = pageAnimation.getValue();
        float out = p * 1904f;
        float in = (1f - p) * 1904f;
        if (p < 1f && prevModule != null) {
            drawPage(canvas, prevModule, pageAnimBack ? -out : out, dmx, dmy, true);
        }
        drawPage(canvas, openModule, pageAnimBack ? in : -in, dmx, dmy, false);
        canvas.restore();

        drawBreadcrumb(canvas, dmx, dmy);

        drawPopups(canvas, dmx, dmy);

        if (alpha < 1f) canvas.restore();
        canvas.restore();
    }

    private void drawSearchBar(Canvas canvas, double dmx, double dmy) {
        float x = 1020f, y = 16f, w = 248f, h = 40f;
        boolean focus = searchFocused;
        SkiaRenderer.drawRRect(canvas, x + 1f, y + 1f, w - 2f, h - 2f, 12f, GRAY_850);
        strokeRRect(canvas, x, y, w - 0.5f, h - 0.5f, 12f, focus ? primary600 : GRAY_700, 1f);
        hits.add(new HitZone(new Hit.SearchBox(), x, y, w, h));

        int iconColor = focus ? WHITE : WHITE_60;
        OneConfigIcon.draw(canvas, OneConfigIcon.SEARCH, x + 10f, y + h / 2f - 10f, 20f, 20f, iconColor);

        Font f = SkiaFontManager.getInterRegular(12f);
        if (f == null) return;
        float tx = x + 36f, ty = y + h / 2f;
        String shown = searchQuery;
        int color = WHITE;
        if (shown.isEmpty()) {
            shown = "Search...";
            color = WHITE_60;
        }
        float maxW = w - 36f - 12f;
        while (SkiaRenderer.getStringWidth(shown, f) > maxW && shown.length() > 1) {
            shown = shown.substring(0, shown.length() - 1);
        }
        drawStringVC(canvas, shown, tx, ty, f, color);

        if (focus && !searchQuery.isEmpty() && (System.currentTimeMillis() / 500L) % 2 == 0) {
            float cw = SkiaRenderer.getStringWidth(searchQuery, f);
            SkiaRenderer.drawRect(canvas, tx + cw + 1f, ty - 10f, 1f, 20f, WHITE);
        }
    }

    private void drawNavArrows(Canvas canvas, double dmx, double dmy) {
        boolean canBack = openModule != null || navPage != NavPage.NONE;
        boolean backHover = canBack && dmx >= 240f && dmx <= 280f && dmy >= 16f && dmy <= 56f;
        if (canBack) hits.add(new HitZone(new Hit.Back(), 240f, 16f, 40f, 40f));
        OneConfigIcon.draw(canvas, OneConfigIcon.ARROW_LEFT, 250f, 26f, 20f, 20f,
                withAlpha(backHover ? WHITE : WHITE_80, canBack ? 1f : 0.5f));
        OneConfigIcon.draw(canvas, OneConfigIcon.ARROW_RIGHT, 290f, 26f, 20f, 20f, withAlpha(WHITE_80, 0.5f));
    }

    private void drawBreadcrumb(Canvas canvas, double dmx, double dmy) {
        Font f = SkiaFontManager.getInterSemiBold(24f);
        if (f == null) return;
        float bx = 336f;

        List<String> crumbs = new ArrayList<>();
        crumbs.add(switch (navPage) {
            case PREFS -> "Preferences";
            case CREDITS -> "Credits";
            case CONFIGS -> "Configs";
            case MUSIC -> "Music";
            case NONE -> Category.values()[selectedCat].getName();
        });
        if (openModule != null) crumbs.add(openModule.getName());

        float[] widths = new float[crumbs.size()];
        for (int i = 0; i < crumbs.size(); i++) {
            widths[i] = SkiaRenderer.getStringWidth(crumbs.get(i), f);
        }
        for (int i = 0; i < crumbs.size(); i++) {
            String title = crumbs.get(i);
            boolean last = i == crumbs.size() - 1;
            boolean hovered = !last && dmx >= bx && dmx <= bx + widths[i] && dmy >= 24f && dmy <= 60f;
            int color = last ? WHITE : (hovered ? WHITE_80 : WHITE_60);
            if (i != 0) {
                OneConfigIcon.draw(canvas, OneConfigIcon.CARET_RIGHT, bx - 28f, 25f, 24f, 24f, color);
            }
            if (!last) hits.add(new HitZone(new Hit.Back(), bx, 24f, widths[i], 36f));
            drawStringVC(canvas, title, bx, 38f, f, color);
            bx += widths[i] + 32f;
        }
    }

    private static String catIcon(Category cat) {
        return switch (cat) {
            case Combat -> OneConfigIcon.CAT_COMBAT;
            case Exploits -> OneConfigIcon.CAT_MISC;
            case Move -> OneConfigIcon.CAT_MOVEMENT;
            case Player -> OneConfigIcon.CAT_PLAYER;
            case World -> OneConfigIcon.CAT_WORLD;
            case Render -> OneConfigIcon.CAT_RENDER;
            case HUD -> OneConfigIcon.FADERS;
        };
    }

    private void drawSideBar(Canvas canvas, double dmx, double dmy) {
        Font nf = SkiaFontManager.getInterMedium(14f);
        Font gf = SkiaFontManager.getInterSemiBold(12f);
        Category[] cats = Category.values();

        float targetY = switch (navPage) {
            case PREFS -> SB_PREFS_Y;
            case CREDITS -> SB_CREDITS_Y;
            case CONFIGS -> SB_CONFIGS_Y;
            case MUSIC -> SB_MUSIC_Y;
            case NONE -> SB_CAT_Y0 + selectedCat * 36f;
        };
        if (!sidebarMove.isRunning() && Math.abs(sidebarCurY - targetY) > 0.5f) {
            sidebarMove.setValue(sidebarCurY);
            sidebarMove.animate(targetY);
        }
        sidebarCurY = sidebarMove.getValue();
        SkiaRenderer.drawRRect(canvas, 16f, sidebarCurY, NAV_BTN_W, NAV_BTN_H, 12f, primary600);

        navButton(canvas, nf, OneConfigIcon.COPYRIGHT_FILL, "Credits", SB_CREDITS_Y, true, dmx, dmy);

        drawStringVC(canvas, "MOD CONFIG", 16f, SB_TITLE1_BASE, gf, WHITE_50);

        for (int i = 0; i < cats.length; i++) {
            navButton(canvas, nf, catIcon(cats[i]), cats[i].getName(), SB_CAT_Y0 + i * 36f, true, dmx, dmy);
        }
        drawStringVC(canvas, "PERSONALIZATION", 16f, SB_TITLE2_BASE, gf, WHITE_50);
        navButton(canvas, nf, OneConfigIcon.SETTINGS, "Preferences", SB_PREFS_Y, true, dmx, dmy);
        navButton(canvas, nf, OneConfigIcon.CONFIGS, "Configs", SB_CONFIGS_Y, true, dmx, dmy);
        navButton(canvas, nf, OneConfigIcon.MUSIC, "Music", SB_MUSIC_Y, true, dmx, dmy);
        navButton(canvas, nf, OneConfigIcon.LAYOUT, "Edit HUD", SB_HUD_Y, true, dmx, dmy);
        navButton(canvas, nf, OneConfigIcon.X_CLOSE, "Close", SB_CLOSE_Y, true, dmx, dmy);
    }

    private void navButton(Canvas canvas, Font f, String icon, String text, float y,
                           boolean enabled, double dmx, double dmy) {
        boolean hover = enabled && dmx >= 16f && dmx <= 16f + NAV_BTN_W && dmy >= y && dmy <= y + NAV_BTN_H;
        int textColor;
        if (text.equals("Close")) {
            textColor = hover ? ERROR_600_80 : WHITE_90;
        } else {
            textColor = hover ? WHITE : WHITE_80;
        }
        if (!enabled) textColor = withAlpha(WHITE_80, 0.5f);

        OneConfigIcon.draw(canvas, icon, 32f, y + 9f, 18f, 18f, textColor);
        drawStringVC(canvas, text, 58f, y + NAV_BTN_H / 2f + 1.75f, f, textColor);

        if (!enabled) return;
        Hit hit = switch (text) {
            case "Preferences" -> new Hit.NavPreferences();
            case "Credits" -> new Hit.NavCredits();
            case "Configs" -> new Hit.NavConfigs();
            case "Music" -> new Hit.NavMusic();
            case "Edit HUD" -> new Hit.EditHud();
            case "Close" -> new Hit.Close();
            default -> {
                Category[] cats = Category.values();
                for (int i = 0; i < cats.length; i++) {
                    if (cats[i].getName().equals(text)) yield new Hit.CatButton(i);
                }
                yield null;
            }
        };
        if (hit != null) hits.add(new HitZone(hit, 16f, y, NAV_BTN_W, NAV_BTN_H));
    }

    private void drawPage(Canvas canvas, Module module, float offsetX, double dmx, double dmy, boolean isPrev) {
        canvas.save();
        canvas.translate(offsetX, 0);
        NavPage pn = isPrev ? prevNavPage : navPage;
        if (module == null && pn == NavPage.CREDITS) {
            drawCreditsPage(canvas, dmx, dmy, isPrev);
        } else if (module == null && pn == NavPage.CONFIGS) {
            drawConfigsPage(canvas, dmx, dmy, isPrev);
        } else if (module == null && pn == NavPage.MUSIC) {
            com.eastern.ui.oneconfig.music.MusicPlayerPage.render(canvas, dmx, dmy, primary600, primary700);
        } else if (module == null && pn == NavPage.NONE) {
            drawModsPage(canvas, dmx, dmy, isPrev);
        } else {
            drawConfigPage(canvas, module, dmx, dmy, isPrev);
        }
        canvas.restore();
    }

    private void drawModsPage(Canvas canvas, double dmx, double dmy, boolean isPrev) {

        gridScroll = gridScrollAnim.getValue();
        float pageY = HEADER_H + gridScroll;

        List<Module> modules = visibleModules(searchQuery.trim().toLowerCase(Locale.ROOT));
        Font mf = SkiaFontManager.getFont("minecraft-bold", 16f);
        Font tf = SkiaFontManager.getInterMedium(14f);

        float iX = SPLIT_X + 16f;
        float iY = pageY + 16f;
        boolean lmb = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        for (Module module : modules) {
            if (iY + CARD_H >= HEADER_H && iY <= HEADER_H + CONTENT_H) {
                drawModCard(canvas, module, iX, iY, mf, tf, dmx, dmy, lmb, isPrev);
            }
            iX += CARD_STEP_X;
            if (iX > SPLIT_X + 796f) {
                iX = SPLIT_X + 16f;
                iY += CARD_STEP_Y;
            }
        }
        modsContentH = modules.isEmpty() ? 0f : 16f + ((modules.size() + 3) / 4) * CARD_STEP_Y;

        if (modules.isEmpty()) {
            drawStringVC(canvas, "Looks like there is nothing here. Try another category?",
                    SPLIT_X + 16f, pageY + 72f, tf, WHITE_60);
        }

        drawScrollBar(canvas, gridScroll, modsContentH);
    }

    private void drawModCard(Canvas canvas, Module module, float x, float y, Font mf, Font tf,
                             double dmx, double dmy, boolean lmb, boolean isPrev) {
        boolean enabled = module.isEnabled();
        boolean hoverTop = dmx >= x && dmx <= x + CARD_W && dmy >= y && dmy <= y + CARD_TOP_H;
        boolean hoverBottom = dmx >= x && dmx <= x + CARD_W - 32f && dmy >= y + CARD_TOP_H && dmy <= y + CARD_H;

        int topColor = hoverTop ? (lmb ? GRAY_400_80 : GRAY_400) : GRAY_500;
        SkiaRenderer.drawTopRoundedSquircle(canvas, x, y, CARD_W, CARD_TOP_H, 12f, topColor);

        if (mf != null) {
            String name = module.getName();
            while (SkiaRenderer.getStringWidth(name, mf) > CARD_W - 16f && name.length() > 2) {
                name = name.substring(0, name.length() - 2) + "…";
            }
            SkiaRenderer.drawCenteredString(canvas, name, x + CARD_W / 2f, y + 44f, mf,
                    withAlpha(WHITE, hoverTop && lmb ? 0.8f : 1f));
        }

        SkiaRenderer.drawRect(canvas, x, y + 86f, CARD_W, 2f, GRAY_300);

        int botColor = enabled
                ? (hoverBottom ? (lmb ? primary700_80 : primary700) : primary600)
                : (hoverBottom ? (lmb ? GRAY_400_80 : GRAY_400) : GRAY_500);
        SkiaRenderer.drawBottomRoundedSquircle(canvas, x, y + CARD_TOP_H, CARD_W, 32f, 12f, botColor);
        if (tf != null) {
            String name = module.getName();
            while (SkiaRenderer.getStringWidth(name, tf) > CARD_W - 32f - 12f && name.length() > 2) {
                name = name.substring(0, name.length() - 2) + "…";
            }
            drawStringVC(canvas, name, x + 12f, y + 103f, tf,
                    withAlpha(WHITE, hoverBottom && lmb ? 0.8f : 1f));
        }

        boolean favorite = favoriteMods.contains(module.getName());
        boolean favHover = dmx >= x + 212f && dmx <= x + CARD_W && dmy >= y + CARD_TOP_H && dmy <= y + CARD_H;
        OneConfigIcon.draw(canvas, favorite ? OneConfigIcon.HEART_FILL : OneConfigIcon.HEART_OUTLINE,
                x + 216f, y + 95f, 16f, 16f, favHover ? WHITE : WHITE_80);

        if (isPrev) return;
        hits.add(new HitZone(new Hit.Card(module), x, y, CARD_W, CARD_TOP_H));
        hits.add(new HitZone(new Hit.CardToggle(module), x, y + CARD_TOP_H, CARD_W - 32f, 32f));
        hits.add(new HitZone(new Hit.Favorite(module), x + 212f, y + CARD_TOP_H, 32f, 32f));
    }

    private void drawCreditsPage(Canvas canvas, double dmx, double dmy, boolean isPrev) {
        settingsScroll = settingsScrollAnim.getValue();
        float x = SPLIT_X + OPT_X_OFF;
        float y = HEADER_H + settingsScroll + 28f;
        float maxW = PANEL_W - OPT_X_OFF - PANEL_X_OFF - 16f;

        Font h = SkiaFontManager.getInterSemiBold(24f);
        Font hc = SkiaFontManager.getMedium(24f);

        drawH(canvas, "EASTERN", x, y, h);
        y += 15f;
        drawB(canvas, "Eastern Client - OneConfig 风格 ClickGUI 移植", x, y, maxW, WHITE_60);
        y += 45f;

        drawH(canvas, "OneConfig Development Team", x, y, h);
        y += 15f;
        String[] team = {
                " - Wyvest - OG Team - Gradle, NanoVGHelper, VCAL, Utilities, GUI Frontend",
                " - Caledonian - Designer",
                " - nextdaydelivery - OG Team - GUI Frontend, NanoVGHelper, Utilities",
                " - Pauline - Utilities",
                " - DeDiamondPro - OG Team - Config Backend, GUI Frontend, HUD",
                " - xtrm - Multiversion support, GUI Frontend",
                " - MoonTidez - OG Team - Designer",
        };
        for (String line : team) { drawB(canvas, line, x, y, maxW, WHITE_60); y += 15f; }
        y += 30f;

        drawH(canvas, "版权说明", x, y, hc != null ? hc : h);
        y += 15f;
        drawB(canvas, "OneConfig 以 LGPL 协议开源于：", x, y, maxW, WHITE_60); y += 15f;
        drawB(canvas, "https://github.com/Polyfrost/OneConfig", x, y, maxW, WHITE_60); y += 15f;
        drawB(canvas, "本客户端仅按 LGPL 标准使用了 OneConfig 的界面设计规格，界面为独立实现（Skia 渲染）。", x, y, maxW, WHITE_60); y += 30f;

        drawH(canvas, "Libraries", x, y, h);
        y += 15f;
        String[] libs = {
                " - Skija (HumbleUI) - Skia bindings for Java",
                " - Gson (Google) - JSON serialization",
                " - LWJGL (lwjgl.org) - OpenGL / GLFW",
                " - https://easings.net/ - Easing functions",
                " - Inter (rsms.me/inter) - UI font, SIL OFL",
                " - Phosphor Icons (phosphoricons.com) - Icon set, MIT",
        };
        for (String line : libs) { drawB(canvas, line, x, y, maxW, WHITE_60); y += 15f; }

        settingsContentH = (y - HEADER_H - settingsScroll) + 40f;
        drawScrollBar(canvas, settingsScroll, settingsContentH);
    }

    private void drawH(Canvas canvas, String text, float x, float cy, Font latin) {
        if (latin == null) return;
        if (text.codePoints().anyMatch(OneConfigStyleGuiScreen::isCJK)) {
            drawMixed(canvas, text, x, cy, 24f, latin, SkiaFontManager.getMedium(24f), WHITE);
        } else {
            drawStringVC(canvas, text, x, cy, latin, WHITE);
        }
    }

    private void drawB(Canvas canvas, String text, float x, float cy, float maxW, int color) {
        drawMixed(canvas, ellipsizeMixed(text, maxW), x, cy, 12f,
                SkiaFontManager.getInterRegular(12f), SkiaFontManager.getRegular(12f), color);
    }

    private static void drawMixed(Canvas canvas, String text, float x, float cy, float size,
                                  Font latin, Font cjk, int color) {
        if (latin == null && cjk == null) return;
        float cx = x;
        int i = 0, n = text.length();
        while (i < n) {
            boolean cjkRun = isCJK(text.codePointAt(i));
            int j = i;
            while (j < n && isCJK(text.codePointAt(j)) == cjkRun) {
                j += Character.charCount(text.codePointAt(j));
            }
            String seg = text.substring(i, j);
            Font f = cjkRun ? (cjk != null ? cjk : latin) : (latin != null ? latin : cjk);
            if (f != null) {
                drawStringVC(canvas, seg, cx, cy, f, color);
                cx += SkiaRenderer.getStringWidth(seg, f);
            }
            i = j;
        }
    }

    private static boolean isCJK(int cp) {
        return cp >= 0x2E80 && cp <= 0x9FFF
                || cp >= 0xF900 && cp <= 0xFAFF
                || cp >= 0xFF00 && cp <= 0xFFEF;
    }

    private static float widthMixed(String text, Font latin, Font cjk) {
        float w = 0f;
        int i = 0, n = text.length();
        while (i < n) {
            boolean cjkRun = isCJK(text.codePointAt(i));
            int j = i;
            while (j < n && isCJK(text.codePointAt(j)) == cjkRun) {
                j += Character.charCount(text.codePointAt(j));
            }
            String seg = text.substring(i, j);
            Font f = cjkRun ? (cjk != null ? cjk : latin) : (latin != null ? latin : cjk);
            if (f != null) w += SkiaRenderer.getStringWidth(seg, f);
            i = j;
        }
        return w;
    }

    private static String ellipsizeMixed(String s, float maxW) {
        Font latin = SkiaFontManager.getInterRegular(12f);
        Font cjk = SkiaFontManager.getRegular(12f);
        if (latin == null && cjk == null) return s;
        if (widthMixed(s, latin, cjk) <= maxW) return s;
        while (s.length() > 1 && widthMixed(s + "...", latin, cjk) > maxW) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "...";
    }

    private void drawConfigsPage(Canvas canvas, double dmx, double dmy, boolean isPrev) {
        settingsScroll = settingsScrollAnim.getValue();
        float pageY = HEADER_H + settingsScroll;
        float panelX = SPLIT_X + PANEL_X_OFF;
        float optX = SPLIT_X + OPT_X_OFF;

        List<String> profiles = listProfiles();

        Font h = SkiaFontManager.getInterSemiBold(24f);
        if (h != null) drawStringVC(canvas, "Configs", optX, pageY + 28f, h, WHITE);

        float py = pageY + 52f;
        float panelH = 16f + 48f + Math.max(1, profiles.size()) * 48f;
        SkiaRenderer.drawRRect(canvas, panelX, py, PANEL_W, panelH, 20f, GRAY_900);

        float iy = py + 16f;
        boolean focus = configsInputFocus;
        SkiaRenderer.drawRRect(canvas, optX + 1f, iy + 1f, 640f - 2f, 30f, 12f, GRAY_850);
        strokeRRect(canvas, optX, iy, 639.5f, 31.5f, 12f, focus ? primary600 : GRAY_700, 1f);
        Font sf = SkiaFontManager.getInterRegular(12f);
        if (sf != null) {
            String shown = configsInput.isEmpty() ? "New config name..." : configsInput;
            drawStringVC(canvas, shown, optX + 12f, iy + 17f, sf, configsInput.isEmpty() ? WHITE_60 : WHITE);
            if (focus && !configsInput.isEmpty() && (System.currentTimeMillis() / 500L) % 2 == 0) {
                float cw = SkiaRenderer.getStringWidth(configsInput, sf);
                SkiaRenderer.drawRect(canvas, optX + 13f + cw, iy + 7f, 1f, 18f, WHITE);
            }
        }
        boolean saveHover = dmx >= optX + 656f && dmx <= optX + 740f && dmy >= iy && dmy <= iy + 32f;
        SkiaRenderer.drawRRect(canvas, optX + 656f, iy, 84f, 32f, 12f, saveHover ? primary700 : primary600);
        Font bf = SkiaFontManager.getInterMedium(12f);
        if (bf != null) {
            float tw = SkiaRenderer.getStringWidth("Save", bf);
            drawStringVC(canvas, "Save", optX + 656f + 42f - tw / 2f, iy + 17.5f, bf, WHITE);
        }
        if (!isPrev) {
            hits.add(new HitZone(new Hit.ConfigInput(), optX, iy, 640f, 32f));
            hits.add(new HitZone(new Hit.ConfigSave(), optX + 656f, iy, 84f, 32f));
        }

        float ly = py + 16f + 48f;
        Font nf = SkiaFontManager.getInterMedium(14f);
        for (String name : profiles) {
            if (nf != null) drawStringVC(canvas, name, optX, ly + 17f, nf, WHITE_90);
            float loadX = panelX + PANEL_W - 16f - 84f - 8f - 84f;
            float delX = panelX + PANEL_W - 16f - 84f;
            boolean loadHover = dmx >= loadX && dmx <= loadX + 84f && dmy >= ly && dmy <= ly + 32f;
            SkiaRenderer.drawRRect(canvas, loadX, ly, 84f, 32f, 12f, loadHover ? GRAY_400 : GRAY_500);
            boolean delHover = dmx >= delX && dmx <= delX + 84f && dmy >= ly && dmy <= ly + 32f;
            if (bf != null) {
                float ltw = SkiaRenderer.getStringWidth("Load", bf);
                drawStringVC(canvas, "Load", loadX + 42f - ltw / 2f, ly + 17.5f, bf, loadHover ? WHITE : WHITE_80);
                float dtw = SkiaRenderer.getStringWidth("Delete", bf);
                drawStringVC(canvas, "Delete", delX + 42f - dtw / 2f, ly + 17.5f, bf,
                        delHover ? ERROR_600_80 : WHITE_90);
            }
            if (!isPrev) {
                hits.add(new HitZone(new Hit.ConfigLoad(name), loadX, ly, 84f, 32f));
                hits.add(new HitZone(new Hit.ConfigDelete(name), delX, ly, 84f, 32f));
            }
            ly += 48f;
        }
        if (profiles.isEmpty()) {
            Font ef = SkiaFontManager.getInterRegular(12f);
            if (ef != null) {
                drawStringVC(canvas, "No configs yet. Type a name and press Save.", optX, ly + 17f, ef, WHITE_60);
            }
        }

        settingsContentH = 52f + panelH;
        drawScrollBar(canvas, settingsScroll, settingsContentH);
    }

    private void drawConfigPage(Canvas canvas, Module module, double dmx, double dmy, boolean isPrev) {
        settingsScroll = settingsScrollAnim.getValue();
        float pageY = HEADER_H + settingsScroll;

        float panelX = SPLIT_X + PANEL_X_OFF;
        float optX = SPLIT_X + OPT_X_OFF;
        float col2 = optX + 512f;
        Font nameF = SkiaFontManager.getInterMedium(14f);

        List<Value> values = new ArrayList<>();
        for (Value v : module.getValues()) {
            if (v.isVisible()) values.add(v);
        }

        Font groupF = SkiaFontManager.getInterMedium(24f);
        drawStringVC(canvas, "General", optX, pageY + 28f, groupF, WHITE_90);

        List<Row> rows = buildRows(module, values);
        int lineCount = 0;
        for (Row row : rows) if (!row.second) lineCount++;
        float panelH = 16f + lineCount * 48f;
        float py = pageY + 52f;
        SkiaRenderer.drawRRect(canvas, panelX, py, PANEL_W, panelH, 20f, GRAY_900);

        float ry = py + 16f;
        boolean lmb = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        float lastFirstY = ry;
        for (Row row : rows) {
            float bx = row.second ? col2 : optX;
            float dy = row.second ? lastFirstY : ry;
            switch (row.value) {
                case BoolValue bv -> {
                    drawSwitch(canvas, bx, dy, bv.getValue(), bv);
                    if (nameF != null) drawOptionName(canvas, bv.getName(), bx + 50f, dy, nameF);
                    if (!isPrev) hits.add(new HitZone(new Hit.Switch(bv), bx, dy, 42f, 32f));
                }
                case ModuleSwitchValue ignored -> {
                    drawSwitch(canvas, bx, dy, module.isEnabled(), module);
                    if (nameF != null) drawOptionName(canvas, "Enabled", bx + 50f, dy, nameF);
                    if (!isPrev) hits.add(new HitZone(new Hit.ModuleSwitch(module), bx, dy, 42f, 32f));
                }
                case NumberValue nv -> {
                    drawSliderRow(canvas, bx, dy, nv, nameF, dmx, isPrev);
                }
                case ModeValue mv -> {
                    drawDropdownRow(canvas, bx, dy, mv, nameF, isPrev);
                }
                case ColorValue cv -> {
                    if (nameF != null) drawOptionName(canvas, cv.getName(), bx, dy, nameF);
                    float wellX = bx + CTRL_X;
                    int rgb = cv.getValue().getRGB() | 0xFF000000;
                    SkiaRenderer.drawRRect(canvas, wellX, dy, 512f, 32f, 12f, GRAY_500);
                    SkiaRenderer.drawRRect(canvas, wellX + 4f, dy + 4f, 32f, 24f, 8f, rgb);
                    if (!isPrev) hits.add(new HitZone(new Hit.ColorWell(cv), wellX, dy, 512f, 32f));
                }
                case StringValue sv -> {
                    drawTextRow(canvas, bx, dy, sv, nameF, isPrev);
                }
                case KeybindValue ignored -> {
                    if (nameF != null) drawOptionName(canvas, "Keybind", bx, dy, nameF);
                    drawKeybind(canvas, bx + 224f, dy, module, lmb, isPrev);
                }
                case MultiBoolValue mbv -> {

                }
                default -> {}
            }
            if (!row.second) { lastFirstY = ry; ry += 32f + 16f; }
        }

        settingsContentH = 52f + panelH;
        drawScrollBar(canvas, settingsScroll, settingsContentH);
    }

    private static final class Row {
        final Value value; final boolean second;
        Row(Value value, boolean second) { this.value = value; this.second = second; }
    }

    private List<Row> buildRows(Module module, List<Value> values) {
        List<Row> rows = new ArrayList<>();
        rows.add(new Row(new ModuleSwitchValue(), false));
        rows.add(new Row(new KeybindValue(), true));
        boolean halfOpen = false;
        for (Value v : values) {
            if (v instanceof MultiBoolValue mbv) {

                for (BoolValue b : mbv.getValues()) {
                    rows.add(new Row(b, halfOpen));
                    halfOpen = !halfOpen;
                }
                continue;
            }
            if (v instanceof BoolValue) {
                rows.add(new Row(v, halfOpen));
                halfOpen = !halfOpen;
            } else {
                if (halfOpen) {  }
                rows.add(new Row(v, false));
                halfOpen = false;
            }
        }
        return rows;
    }

    private static final class ModuleSwitchValue extends Value {
        ModuleSwitchValue() { super("Enabled", () -> true); }
    }

    private static final class KeybindValue extends Value {
        KeybindValue() { super("Keybind", () -> true); }
    }

    private void drawOptionName(Canvas canvas, String name, float x, float y, Font f) {
        String n = name;
        while (SkiaRenderer.getStringWidth(n, f) > 340f && n.length() > 2) n = n.substring(0, n.length() - 2) + "…";
        drawStringVC(canvas, n, x, y + 17f, f, WHITE_90);
    }

    private void drawSwitch(Canvas canvas, float x, float y, boolean on, Object animKey) {
        float t = on ? 1f : 0f;
        if (animKey != null) {
            Animation anim = switchAnims.get(animKey);
            if (anim == null) {
                anim = new Animation(Easings.IN_OUT_QUAD, 150);
                anim.setValue(t);
                switchAnims.put(animKey, anim);
            }
            anim.animate(on ? 1f : 0f);
            t = anim.getValue();
        }
        boolean hover = mouseXDesign >= x && mouseXDesign <= x + 42f
                && mouseYDesign >= y && mouseYDesign <= y + 32f;
        int bg = on ? (hover ? primary700 : primary600) : (hover ? GRAY_400 : GRAY_500);
        SkiaRenderer.drawRRect(canvas, x, y + 4f, 42f, 24f, 12f, bg);
        SkiaRenderer.drawRRect(canvas, x + 3f + t * 18f, y + 7f, 18f, 18f, 9f, WHITE);
    }

    private void drawSliderRow(Canvas canvas, float x, float y, NumberValue value, Font nameF,
                               double dmx, boolean isPrev) {
        float min = value.getMin(), max = value.getMax();
        float ratio = (value.getValue() - min) / Math.max(1e-6f, max - min);
        ratio = Math.min(1f, Math.max(0f, ratio));

        if (nameF != null) drawOptionName(canvas, value.getName(), x, y, nameF);

        float sx = x + CTRL_X;
        float xc = sx + ratio * SLIDER_W;

        SkiaRenderer.drawRRect(canvas, sx, y + 13f, SLIDER_W, 6f, 3f, GRAY_300);
        SkiaRenderer.drawRRect(canvas, sx, y + 13f, Math.max(6f, xc - sx), 6f, 3f, primary500);
        SkiaRenderer.drawRRect(canvas, xc - 7f, y + 9f, 14f, 14f, 7f, WHITE);

        float nbX = x + 892f;
        SkiaRenderer.drawRRect(canvas, nbX + 1f, y + 1f, 68f - 2f, 32f - 2f, 12f, GRAY_850);
        strokeRRect(canvas, nbX, y, 68f - 0.5f, 32f - 0.5f, 12f, GRAY_700, 1f);
        Font vf = SkiaFontManager.getInterRegular(12f);
        if (vf != null) {
            SkiaRenderer.drawCenteredString(canvas, formatNumber(value.getValue()), nbX + 34f, y + 17f, vf, WHITE);
        }

        boolean atMax = value.getValue() >= max - 1e-6f;
        boolean atMin = value.getValue() <= min + 1e-6f;
        boolean upHover = dmx >= nbX + 72f && dmx <= nbX + 84f && mouseYDesign >= y && mouseYDesign <= y + 14f;
        boolean dnHover = dmx >= nbX + 72f && dmx <= nbX + 84f && mouseYDesign >= y + 14f && mouseYDesign <= y + 28f;
        SkiaRenderer.drawTopRoundedSquircle(canvas, nbX + 72f, y, 12f, 14f, 6f, upHover ? GRAY_400 : GRAY_500);
        SkiaRenderer.drawBottomRoundedSquircle(canvas, nbX + 72f, y + 14f, 12f, 14f, 6f, dnHover ? GRAY_400 : GRAY_500);
        OneConfigIcon.draw(canvas, OneConfigIcon.CHEVRON_UP, nbX + 73f, y + 2f, 10f, 10f,
                atMax ? withAlpha(WHITE_80, 0.3f) : WHITE_80);
        OneConfigIcon.draw(canvas, OneConfigIcon.CHEVRON_DOWN, nbX + 73f, y + 16f, 10f, 10f,
                atMin ? withAlpha(WHITE_80, 0.3f) : WHITE_80);

        if (isPrev) return;
        hits.add(new HitZone(new Hit.Slider(value), sx, y, SLIDER_W, 32f));
        hits.add(new HitZone(new Hit.StepperUp(value), nbX + 72f, y, 12f, 14f));
        hits.add(new HitZone(new Hit.StepperDown(value), nbX + 72f, y + 14f, 12f, 14f));
    }

    private void drawDropdownRow(Canvas canvas, float x, float y, ModeValue value, Font nameF, boolean isPrev) {
        if (nameF != null) drawOptionName(canvas, value.getName(), x, y, nameF);
        float dx = x + CTRL_X;
        boolean open = openDropdown == value;
        boolean hover = !open && mouseXDesign >= dx && mouseXDesign <= dx + DROPDOWN_W
                && mouseYDesign >= y && mouseYDesign <= y + 32f;

        if (open) {

            SkiaRenderer.drawRRect(canvas, dx + 1f, y + 1f, DROPDOWN_W - 2f, 30f, 12f, GRAY_850);
            strokeRRect(canvas, dx, y, DROPDOWN_W - 0.5f, 31.5f, 12f, primary600, 1f);
            OneConfigIcon.draw(canvas, OneConfigIcon.SEARCH, dx + 10f, y + 6f, 20f, 20f, WHITE);
            Font sf = SkiaFontManager.getInterRegular(12f);
            if (sf != null) {
                String shown = dropdownQuery.isEmpty() ? "Search..." : dropdownQuery;
                drawStringVC(canvas, shown, dx + 46f, y + 17f, sf,
                        dropdownQuery.isEmpty() ? WHITE_60 : WHITE);
            }
        } else {
            SkiaRenderer.drawRRect(canvas, dx, y, DROPDOWN_W, 32f, 12f, hover ? GRAY_400 : GRAY_500);
            Font vf = SkiaFontManager.getInterMedium(14f);
            if (vf != null) {
                String text = value.getValue();
                while (SkiaRenderer.getStringWidth(text, vf) > DROPDOWN_W - 72f && text.length() > 2) {
                    text = text.substring(0, text.length() - 2) + "…";
                }
                drawStringVC(canvas, text, dx + 12f, y + 17f, vf, WHITE_80);
            }
        }

        float ax = dx + DROPDOWN_W - 28f;
        boolean atomHover = mouseXDesign >= dx && mouseXDesign <= dx + DROPDOWN_W
                && mouseYDesign >= y && mouseYDesign <= y + 32f;
        SkiaRenderer.drawRRect(canvas, ax, y + 4f, 24f, 24f, 8f, atomHover ? primary500 : primary600);
        OneConfigIcon.draw(canvas, OneConfigIcon.DROPDOWN, ax, y + 4f, 24f, 24f, WHITE);

        if (!isPrev) hits.add(new HitZone(new Hit.Dropdown(value), dx, y, DROPDOWN_W, 32f));
    }

    private void drawTextRow(Canvas canvas, float x, float y, StringValue value, Font nameF, boolean isPrev) {
        if (nameF != null) drawOptionName(canvas, value.getName(), x, y, nameF);
        float tx = x + CTRL_X;
        boolean focus = focusText == value;
        SkiaRenderer.drawRRect(canvas, tx + 1f, y + 1f, 640f - 2f, 32f - 2f, 12f, GRAY_850);
        strokeRRect(canvas, tx, y, 640f - 0.5f, 32f - 0.5f, 12f, focus ? primary600 : GRAY_700, 1f);
        Font vf = SkiaFontManager.getInterRegular(12f);
        if (vf != null) {
            String shown = value.getValue();
            if (shown.isEmpty() && !focus) shown = "...";
            while (SkiaRenderer.getStringWidth(shown, vf) > 640f - 24f && shown.length() > 1) {
                shown = shown.substring(0, shown.length() - 1);
            }
            drawStringVC(canvas, shown, tx + 12f, y + 17f, vf, shown.equals("...") ? WHITE_60 : WHITE);
            if (focus && (System.currentTimeMillis() / 500L) % 2 == 0) {
                float cw = SkiaRenderer.getStringWidth(value.getValue(), vf);
                SkiaRenderer.drawRect(canvas, tx + 12f + cw + 1f, y + 7f, 1f, 18f, WHITE);
            }
        }
        if (!isPrev) hits.add(new HitZone(new Hit.TextField(value), tx, y, 640f, 32f));
    }

    private void drawKeybind(Canvas canvas, float x, float y, Module module, boolean lmb, boolean isPrev) {
        boolean hover = dmx0(x, y);
        boolean binding = bindModule == module;
        int bg = binding ? primary600 : (hover && !lmb ? GRAY_400 : (hover ? GRAY_400_80 : GRAY_500));
        SkiaRenderer.drawRRect(canvas, x, y, 256f, 32f, 10f, bg);
        Font vf = SkiaFontManager.getInterMedium(12f);
        String label = binding ? "Recording..." : keyName(module);
        if (vf != null) {
            float tw = SkiaRenderer.getStringWidth(label, vf);

            drawStringVC(canvas, label, x + 128f - tw / 2f, y + 17.5f, vf, WHITE_80);
        }
        OneConfigIcon.draw(canvas, OneConfigIcon.KEYSTROKE, x + 12f, y + 8f, 16f, 16f, WHITE_80);
        if (!isPrev) hits.add(new HitZone(new Hit.Keybind(module), x, y, 256f, 32f));
    }

    private double mouseXDesign = -1, mouseYDesign = -1;

    private boolean dmx0(float x, float y) {
        return mouseXDesign >= x && mouseXDesign <= x + 256f && mouseYDesign >= y && mouseYDesign <= y + 32f;
    }

    private void drawPopups(Canvas canvas, double dmx, double dmy) {
        mouseXDesign = dmx;
        mouseYDesign = dmy;
        Font vf = SkiaFontManager.getInterMedium(14f);

        if (openDropdown != null) {
            HitZone zone = findZone(new Hit.Dropdown(openDropdown));
            if (zone != null) {

                List<String> filtered = new ArrayList<>();
                String q = dropdownQuery.toLowerCase(Locale.ROOT);
                for (String mode : openDropdown.getModes()) {
                    if (q.isEmpty() || mode.toLowerCase(Locale.ROOT).contains(q)) filtered.add(mode);
                }
                String[] modes = filtered.toArray(new String[0]);
                float listH = Math.min(modes.length, 10) * 32f + 8f;
                float px = zone.x(), py = zone.y() + 40f, pw = zone.w();

                SkiaRenderer.drawRRect(canvas, px, py, pw, listH, 12f, GRAY_700);
                strokeRRect(canvas, px - 1f, py - 1f, pw + 2f, listH + 2f, 12f, 0x4DCCCCCC, 1f);
                float maxScroll = Math.max(0f, modes.length * 32f + 8f - 328f);
                dropdownScroll = Math.min(dropdownScroll, maxScroll);
                float oy = py + 4f - dropdownScroll;
                boolean lmb = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
                for (String option : modes) {
                    boolean hover = dmx >= px && dmx <= px + pw && dmy >= oy && dmy <= oy + 32f;
                    if (hover) {
                        SkiaRenderer.drawRRect(canvas, px + 4f, oy + 2f, pw - 8f, 28f, 8f,
                                lmb ? primary700_80 : primary700);
                    }
                    drawStringVC(canvas, option, px + 16f, oy + 18f, vf, hover ? WHITE : WHITE_80);
                    hits.add(new HitZone(new Hit.DropdownItem(openDropdown, option), px, oy, pw, 32f));
                    oy += 32f;
                }
                if (modes.length > 10) {
                    drawMiniScrollBar(canvas, px + pw - 8f, py + 4f, 328f, dropdownScroll, maxScroll);
                }
            }
        }

        if (openColor != null) {
            HitZone zone = findZone(new Hit.ColorWell(openColor));
            if (zone != null) {
                float px = Math.min(zone.x(), WIN_W - 292f);
                float py = zone.y() + 40f;
                float pw = 284f, ph = 250f;
                SkiaRenderer.drawRRect(canvas, px, py, pw, ph, 12f, GRAY_700);
                strokeRRect(canvas, px - 1f, py - 1f, pw + 2f, ph + 2f, 12f, 0x4DCCCCCC, 1f);

                float pad = 20f;
                float svW = pw - pad * 2f, svH = 140f;
                float svX = px + pad, svY = py + pad;
                float[] hsb = {openColor.getHue(), openColor.getSaturation(), openColor.getBrightness()};
                int hueRGB = java.awt.Color.HSBtoRGB(hsb[0], 1f, 1f);
                SkiaRenderer.drawRRect(canvas, svX, svY, svW, svH, 8f, hueRGB | 0xFF000000);
                fillGradientRect(canvas, svX, svY, svW, svH, 0xFFFFFFFF, 0x00FFFFFF, true, 8f);
                fillGradientRect(canvas, svX, svY, svW, svH, 0x00000000, 0xFF000000, false, 8f);
                float stx = svX + hsb[1] * svW, sty = svY + (1f - hsb[2]) * svH;
                SkiaRenderer.drawCircle(canvas, stx, sty, 6f, WHITE);
                hits.add(new HitZone(new Hit.ColorSV(openColor), svX, svY, svW, svH));

                float hy = svY + svH + 16f;
                drawHueBar(canvas, px + pad, hy, svW, 12f);
                float hx = px + pad + hsb[0] * svW;
                SkiaRenderer.drawRect(canvas, hx - 2f, hy - 2f, 4f, 16f, WHITE);
                hits.add(new HitZone(new Hit.ColorSlider(openColor, 0), px + pad, hy - 4f, svW, 20f));
            }
        }
    }

    private void drawHueBar(Canvas canvas, float x, float y, float w, float h) {
        try (Shader shader = Shader.makeLinearGradient(x, y, x + w, y, new int[]{
                0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000});
             Paint p = new Paint()) {
            p.setAntiAlias(true);
            p.setShader(shader);
            canvas.drawRRect(io.github.humbleui.types.RRect.makeXYWH(x, y, w, h, 6f), p);
        }
    }

    private static void fillGradientRect(Canvas canvas, float x, float y, float w, float h,
                                         int from, int to, boolean horizontal, float r) {
        try (Shader shader = horizontal
                ? Shader.makeLinearGradient(x, y, x + w, y, new int[]{from, to})
                : Shader.makeLinearGradient(x, y, x, y + h, new int[]{from, to});
             Paint p = new Paint()) {
            p.setAntiAlias(true);
            p.setShader(shader);
            canvas.drawRRect(io.github.humbleui.types.RRect.makeXYWH(x, y, w, h, r), p);
        }
    }

    private void drawScrollBar(Canvas canvas, float scroll, float contentH) {
        if (contentH <= CONTENT_H) return;
        float maxScroll = contentH - CONTENT_H;
        float sc = Math.min(0f, Math.max(-maxScroll, scroll));
        float thumbLen = (CONTENT_H / contentH) * CONTENT_H;
        float thumbY = HEADER_H + (sc / maxScroll) * (CONTENT_H - 10f);
        SkiaRenderer.drawRRect(canvas, SPLIT_X + 1048f, thumbY - 5f, 4f, thumbLen, 2f, GRAY_400_60);
    }

    private void drawMiniScrollBar(Canvas canvas, float x, float y, float h, float scroll, float maxScroll) {
        if (maxScroll <= 0f) return;
        float thumbLen = Math.max(24f, (328f / (maxScroll + 328f)) * 328f);
        float thumbY = y + (scroll / maxScroll) * (h - thumbLen);
        SkiaRenderer.drawRRect(canvas, x, thumbY, 4f, thumbLen, 2f, GRAY_400_60);
    }

    private List<Module> visibleModules(String query) {
        List<Module> result = new ArrayList<>();
        Category cat = Category.values()[selectedCat];
        for (Module module : instance.getModuleManager().getModuleMap().values()) {
            if (module.isHidden()) continue;
            if (!query.isEmpty()) {
                boolean matchModule = module.getName().toLowerCase(Locale.ROOT).contains(query);
                boolean matchValue = module.getValues().stream()
                        .anyMatch(v -> v.getName().toLowerCase(Locale.ROOT).contains(query));
                if (!matchModule && !matchValue) continue;
            } else if (module.getCategory() != cat) {
                continue;
            }
            result.add(module);
        }

        result.sort((a, b) -> {
            boolean fa = favoriteMods.contains(a.getName());
            boolean fb = favoriteMods.contains(b.getName());
            if (fa != fb) return fa ? -1 : 1;
            return a.getName().compareToIgnoreCase(b.getName());
        });
        return result;
    }

    private static String keyName(Module module) {
        int key = module.getKey();
        if (key <= 0) return "NONE";
        String name = GLFW.glfwGetKeyName(key, 0);
        if (name != null) return name.toUpperCase(Locale.ROOT);
        return switch (key) {
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL";
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            default -> "KEY_" + key;
        };
    }

    private static String formatNumber(float v) {
        if (Math.abs(v - Math.round(v)) < 0.001f) return String.valueOf(Math.round(v));
        return String.format(Locale.ROOT, "%.1f", v);
    }

    private static int withAlpha(int color, float alpha) {
        return (color & 0x00FFFFFF) | ((int) (Math.min(1f, Math.max(0f, alpha)) * 255f) << 24);
    }

    private static void drawStringVC(Canvas canvas, String text, float x, float cy, Font f, int color) {
        if (f == null) return;
        SkiaRenderer.drawString(canvas, text, x, cy + f.getMetrics().getCapHeight() / 2f, f, color);
    }

    private HitZone findZone(Hit hit) {
        for (HitZone zone : hits) {
            if (zone.hit().equals(hit)) return zone;
        }
        return null;
    }

    private static void strokeRRect(Canvas canvas, float x, float y, float w, float h, float r, int color, float stroke) {
        try (Paint p = new Paint()) {
            p.setAntiAlias(true);
            p.setColor(color);
            p.setMode(io.github.humbleui.skija.PaintMode.STROKE);
            p.setStrokeWidth(stroke);
            canvas.drawRRect(io.github.humbleui.types.RRect.makeXYWH(x, y, w, h, r), p);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        double dmx = toDesignX(click.x());
        double dmy = toDesignY(click.y());
        int button = click.button();

        if (button == 0 && navPage == NavPage.MUSIC && openModule == null
                && com.eastern.ui.oneconfig.music.MusicPlayerPage.mouseClicked(dmx, dmy, button)) {
            return true;
        }

        if (button == 0) {

            if (openDropdown != null || openColor != null) {
                for (int i = hits.size() - 1; i >= 0; i--) {
                    HitZone zone = hits.get(i);
                    if (!zone.contains(dmx, dmy)) continue;
                    if (zone.hit() instanceof Hit.DropdownItem di) {
                        di.value().setValue(di.option());
                        openDropdown = null;
                        dropdownScroll = 0f;
                        return true;
                    }
                    if (zone.hit() instanceof Hit.ColorSlider cs) {
                        dragColor = cs.value();
                        dragColorChannel = cs.channel();
                        applyColorSlider(dmx, cs);
                        return true;
                    }
                    if (zone.hit() instanceof Hit.ColorSV sv) {
                        dragColor = sv.value();
                        dragSV = true;
                        applyColorSV(dmx, dmy, sv.value());
                        return true;
                    }
                }
                openDropdown = null;
                openColor = null;
                return true;
            }

            for (int i = hits.size() - 1; i >= 0; i--) {
                HitZone zone = hits.get(i);
                if (!zone.contains(dmx, dmy)) continue;
                Hit hit = zone.hit();

                if (hit instanceof Hit.Close) {
                    close();
                    return true;
                } else if (hit instanceof Hit.Back) {
                    navigateBack();
                    return true;
                } else if (hit instanceof Hit.CatButton cb) {
                    selectedCat = cb.index();
                    if (openModule != null || navPage != NavPage.NONE) {
                        navPage = NavPage.NONE;
                        switchPage(null);
                    }
                    resetGridScroll();
                    return true;
                } else if (hit instanceof Hit.NavPreferences) {
                    ClickGui clickGui = (ClickGui) instance.getModuleManager().getModule(ClickGui.class);
                    if (clickGui != null) {
                        switchPage(clickGui);
                        navPage = NavPage.PREFS;
                        resetSettingsScroll();
                    }
                    return true;
                } else if (hit instanceof Hit.NavCredits) {
                    if (navPage != NavPage.CREDITS) {
                        navPage = NavPage.CREDITS;
                        switchPage(null);
                        resetSettingsScroll();
                    }
                    return true;
                } else if (hit instanceof Hit.NavConfigs) {
                    if (navPage != NavPage.CONFIGS) {
                        navPage = NavPage.CONFIGS;
                        switchPage(null);
                        resetSettingsScroll();
                    }
                    return true;
                } else if (hit instanceof Hit.NavMusic) {
                    if (navPage != NavPage.MUSIC) {
                        navPage = NavPage.MUSIC;
                        switchPage(null);
                        resetSettingsScroll();
                    }
                    return true;
                } else if (hit instanceof Hit.ConfigInput) {
                    configsInputFocus = true;
                    searchFocused = false;
                    focusText = null;
                    return true;
                } else if (hit instanceof Hit.ConfigSave) {
                    saveProfileFromInput();
                    return true;
                } else if (hit instanceof Hit.ConfigLoad cl) {
                    new ModuleConfig(cl.name()).load();
                    return true;
                } else if (hit instanceof Hit.ConfigDelete cd) {
                    deleteProfile(cd.name());
                    return true;
                } else if (hit instanceof Hit.EditHud) {
                    mc.setScreen(new HUDDesignerScreen());
                    return true;
                } else if (hit instanceof Hit.SearchBox) {
                    searchFocused = true;
                    focusText = null;
                    return true;
                } else if (hit instanceof Hit.Card card) {

                    if (card.module() instanceof HUDModule) {
                        mc.setScreen(new HUDDesignerScreen());
                        return true;
                    }
                    switchPage(card.module());
                    resetSettingsScroll();
                    return true;
                } else if (hit instanceof Hit.CardToggle mt) {
                    mt.module().toggle();
                    return true;
                } else if (hit instanceof Hit.Favorite fav) {
                    String name = fav.module().getName();
                    if (!favoriteMods.remove(name)) favoriteMods.add(name);
                    return true;
                } else if (hit instanceof Hit.Keybind kb) {
                    bindModule = bindModule == kb.module() ? null : kb.module();
                    return true;
                } else if (hit instanceof Hit.Switch sw) {
                    sw.value().toggle();
                    return true;
                } else if (hit instanceof Hit.ModuleSwitch ms) {
                    ms.module().toggle();
                    return true;
                } else if (hit instanceof Hit.Slider sl) {
                    dragNumber = sl.value();
                    applySlider(dmx, sl.value());
                    return true;
                } else if (hit instanceof Hit.StepperUp su) {
                    su.value().setValue(Math.min(su.value().getMax(), su.value().getValue() + su.value().getInc()));
                    return true;
                } else if (hit instanceof Hit.StepperDown sd) {
                    sd.value().setValue(Math.max(sd.value().getMin(), sd.value().getValue() - sd.value().getInc()));
                    return true;
                } else if (hit instanceof Hit.Dropdown dd) {
                    openDropdown = openDropdown == dd.value() ? null : dd.value();
                    dropdownQuery = "";
                    dropdownScroll = 0f;
                    return true;
                } else if (hit instanceof Hit.ColorWell cw) {
                    openColor = openColor == cw.value() ? null : cw.value();
                    return true;
                } else if (hit instanceof Hit.TextField tf) {
                    focusText = tf.value();
                    searchFocused = false;
                    return true;
                }
            }

            searchFocused = false;
            focusText = null;
            bindModule = null;
            configsInputFocus = false;
        }

        return super.mouseClicked(click, bl);
    }

    private void switchPage(Module target) {
        prevModule = openModule;
        prevNavPage = navPage;
        openModule = target;
        pageAnimBack = target == null && navPage == NavPage.NONE;
        pageAnimation.setValue(0f);
        pageAnimation.animate(1f);
    }

    private void navigateBack() {
        if (openModule != null || navPage != NavPage.NONE) {
            navPage = NavPage.NONE;
            switchPage(null);
        }
    }

    private void resetGridScroll() {
        gridScrollAnim.setValue(0f);
        gridScroll = 0f;
    }

    private void resetSettingsScroll() {
        settingsScrollAnim.setValue(0f);
        settingsScroll = 0f;
    }

    @Override
    public boolean mouseReleased(Click click) {
        dragNumber = null;
        dragColor = null;
        dragSV = false;
        com.eastern.ui.oneconfig.music.MusicPlayerPage.mouseReleased();
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float delta = (float) verticalAmount * 40f;
        if (openDropdown != null) {
            float max = Math.max(0f, openDropdown.getModes().length * 32f + 8f - 328f);
            dropdownScroll = Math.max(0f, Math.min(max, dropdownScroll - delta));
            return true;
        }

        if (navPage == NavPage.MUSIC && openModule == null) {
            double mdx = toDesignX(mouseX);
            double mdy = toDesignY(mouseY);
            if (com.eastern.ui.oneconfig.music.MusicPlayerPage.mouseScrolled(mdx, mdy, verticalAmount)) {
                return true;
            }
        }
        if (openModule != null) {
            float max = Math.max(0f, settingsContentH - CONTENT_H);
            float target = clampScroll(settingsScrollAnim.getValue() + delta, max);
            settingsScrollAnim.animate(target);
        } else {
            float max = Math.max(0f, modsContentH - CONTENT_H);
            float target = clampScroll(gridScrollAnim.getValue() + delta, max);
            gridScrollAnim.animate(target);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private float clampScroll(float value, float max) {
        if (value > 0f) return 0f;
        if (value < -max) return -max;
        return value;
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (click.button() == 0) {
            double dmx = toDesignX(click.x());
            double dmy = toDesignY(click.y());

            if (com.eastern.ui.oneconfig.music.MusicPlayerPage.mouseDragged(dmx, dmy)) {
                return true;
            }
            if (dragNumber != null) {
                applySlider(dmx, dragNumber);
                return true;
            }
            if (dragSV && dragColor != null) {
                applyColorSV(dmx, dmy, dragColor);
                return true;
            }
            if (dragColor != null) {
                applyColorSlider(dmx, new Hit.ColorSlider(dragColor, dragColorChannel));
                return true;
            }
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    private void applySlider(double dmx, NumberValue value) {
        HitZone zone = findZone(new Hit.Slider(value));
        if (zone == null) return;
        float ratio = (float) ((dmx - zone.x()) / SLIDER_W);
        ratio = Math.min(1f, Math.max(0f, ratio));
        float raw = value.getMin() + ratio * (value.getMax() - value.getMin());
        float snapped = Math.round(raw / value.getInc()) * value.getInc();
        value.setValue(Math.min(value.getMax(), Math.max(value.getMin(), snapped)));
    }

    private void applyColorSlider(double dmx, Hit.ColorSlider cs) {
        HitZone zone = findZone(cs);
        if (zone == null) return;
        float ratio = (float) ((dmx - zone.x()) / Math.max(1f, zone.w()));
        ratio = Math.min(1f, Math.max(0f, ratio));
        float[] hsb = {cs.value().getHue(), cs.value().getSaturation(), cs.value().getBrightness()};
        hsb[cs.channel()] = ratio;
        cs.value().setHSB(hsb[0], hsb[1], hsb[2]);
    }

    private void applyColorSV(double dmx, double dmy, ColorValue value) {
        HitZone zone = findZone(new Hit.ColorSV(value));
        if (zone == null) return;
        float s = (float) ((dmx - zone.x()) / zone.w());
        float b = 1f - (float) ((dmy - zone.y()) / zone.h());
        value.setHSB(value.getHue(), Math.min(1f, Math.max(0f, s)), Math.min(1f, Math.max(0f, b)));
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.key();

        if (com.eastern.ui.oneconfig.music.MusicPlayerPage.keyPressed(key)) {
            return true;
        }

        if (bindModule != null) {

            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE) {
                bindModule.setKey(-1);
            } else {
                bindModule.setKey(key);
            }
            bindModule = null;
            return true;
        }

        if (searchFocused) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                if (!searchQuery.isEmpty()) {
                    searchQuery = "";
                    return true;
                }
                searchFocused = false;
                return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (!searchQuery.isEmpty()) {
                    searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                }
                return true;
            }
        } else if (openDropdown != null) {

            if (key == GLFW.GLFW_KEY_ESCAPE) {
                openDropdown = null;
                return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (!dropdownQuery.isEmpty()) {
                    dropdownQuery = dropdownQuery.substring(0, dropdownQuery.length() - 1);
                }
                return true;
            }
        } else if (focusText != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                focusText = null;
                return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                String v = focusText.getValue();
                if (!v.isEmpty()) focusText.setValue(v.substring(0, v.length() - 1));
                return true;
            }
        } else if (configsInputFocus) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                configsInputFocus = false;
                return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (!configsInput.isEmpty()) {
                    configsInput = configsInput.substring(0, configsInput.length() - 1);
                }
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                saveProfileFromInput();
                return true;
            }
        } else if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (openModule != null || navPage != NavPage.NONE) {
                navigateBack();
                return true;
            }
            close();
            return true;
        }

        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        int codepoint = input.codepoint();
        if (codepoint == 0 || !input.isValidChar()) return super.charTyped(input);
        if (codepoint >= Character.MIN_SURROGATE && codepoint <= Character.MAX_SURROGATE) return super.charTyped(input);
        String ch = Character.toString(codepoint);

        if (com.eastern.ui.oneconfig.music.MusicPlayerPage.charTyped(codepoint)) {
            return true;
        }

        if (searchFocused) {
            searchQuery += ch;
            return true;
        }
        if (openDropdown != null) {
            dropdownQuery += ch;
            dropdownScroll = 0f;
            return true;
        }
        if (focusText != null) {
            focusText.setValue(focusText.getValue() + ch);
            return true;
        }
        if (configsInputFocus) {
            configsInput += ch;
            return true;
        }
        return super.charTyped(input);
    }

    private float winX, winY, uiScale = 1f;

    private double toDesignX(double mxScaled) {
        float total = uiScale * (0.9f + 0.1f * openAnimation.getValue());
        double mx = mxScaled * (float) mc.getWindow().getScaleFactor();
        return (mx - winX) / Math.max(0.0001f, total) + WIN_W / 2f;
    }

    private double toDesignY(double myScaled) {
        float total = uiScale * (0.9f + 0.1f * openAnimation.getValue());
        double my = myScaled * (float) mc.getWindow().getScaleFactor();
        return (my - winY) / Math.max(0.0001f, total) + WIN_H / 2f;
    }
}
