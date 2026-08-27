package com.eastern.ui.oneconfig.music;

import com.eastern.music.MusicController;
import com.eastern.music.MusicLibrary;
import com.eastern.music.MusicPlayerEngine;
import com.eastern.ui.oneconfig.OneConfigIcon;
import com.eastern.util.font.SkiaFontManager;
import com.eastern.util.skia.SkiaRenderer;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.Image;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MusicPlayerPage {

    private static final float LEFT_X = 240f, LEFT_W = 320f;
    private static final float RIGHT_X = 600f, RIGHT_W = 664f;
    private static final float TOP_Y = 104f;
    private static final float COVER = 320f;
    private static final float ROW_H = 44f;
    private static final float SEARCH_H = 40f;

    private static final int GRAY_900 = 0xFF1A1B1E;
    private static final int GRAY_850 = 0xFF202127;
    private static final int GRAY_700 = 0xFF2B2D31;
    private static final int GRAY_500 = 0xFF333639;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int WHITE_90 = 0xE6FFFFFF;
    private static final int WHITE_60 = 0x99FFFFFF;
    private static final int WHITE_50 = 0x80FFFFFF;
    private static final int WHITE_40 = 0x66FFFFFF;

    private static boolean searchFocus;
    private static float listScroll;
    private static float listScrollTarget;
    private static boolean draggingSeek;
    private static boolean draggingVolume;

    private sealed interface Hit {
        record SearchBox() implements Hit {}
        record SongRow(int index) implements Hit {}
        record PlayPause() implements Hit {}
        record Next() implements Hit {}
        record Prev() implements Hit {}
        record Shuffle() implements Hit {}
        record Loop() implements Hit {}
        record SeekBar(float x, float w) implements Hit {}
        record VolumeBar(float x, float w) implements Hit {}
        record LoginEntry() implements Hit {}
        record QrRefresh() implements Hit {}
        record QrClose() implements Hit {}
        record Logout() implements Hit {}
    }

    private record Zone(Hit hit, float x, float y, float w, float h) {
        boolean contains(double mx, double my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
    }

    private static final List<Zone> ZONES = new ArrayList<>();
    private static float seekX, seekW, volX, volW;

    private MusicPlayerPage() {}

    public static void render(Canvas canvas, double dmx, double dmy, int primary600, int primary700) {
        MusicController.ensureLoaded();

        ZONES.clear();

        listScroll += (listScrollTarget - listScroll) * 0.35f;
        if (Math.abs(listScrollTarget - listScroll) < 0.5f) listScroll = listScrollTarget;

        drawNowPlaying(canvas, dmx, dmy, primary600, primary700);
        drawSearchAndList(canvas, dmx, dmy, primary600);
    }

    private static void drawNowPlaying(Canvas canvas, double dmx, double dmy,
                                       int primary600, int primary700) {
        MusicLibrary.Song song = MusicController.session.current();
        MusicPlayerEngine e = MusicPlayerEngine.INSTANCE;

        MusicController.LoginState ls = MusicController.loginState;
        boolean loginView = ls == MusicController.LoginState.LOADING
                || ls == MusicController.LoginState.WAIT_SCAN
                || ls == MusicController.LoginState.WAIT_CONFIRM
                || ls == MusicController.LoginState.EXPIRED;
        if (loginView) {
            drawLoginCard(canvas, ls);
        } else {
            SkiaRenderer.drawRRect(canvas, LEFT_X, TOP_Y, COVER, COVER, 12f, GRAY_700);
            Image cover = song != null ? MusicLibrary.cover(song.coverUrl()) : null;
            if (cover != null) {
                canvas.save();
                SkiaRenderer.clipRRect(canvas, LEFT_X, TOP_Y, COVER, COVER, 12f, io.github.humbleui.skija.ClipMode.INTERSECT);
                canvas.drawImageRect(cover,
                        Rect.makeWH(cover.getWidth(), cover.getHeight()),
                        Rect.makeXYWH(LEFT_X, TOP_Y, COVER, COVER),
                        io.github.humbleui.skija.SamplingMode.LINEAR, null, true);
                canvas.restore();
            } else {
                OneConfigIcon.draw(canvas, OneConfigIcon.MUSIC, LEFT_X + COVER / 2f - 18f, TOP_Y + COVER / 2f - 18f,
                        36f, 36f, WHITE_40);
            }
        }

        Font tf = SkiaFontManager.getMedium(18f);
        Font af = SkiaFontManager.getRegular(13f);
        float infoY = TOP_Y + COVER + 34f;
        if (tf != null) {
            String title = song != null ? song.title() : "Nothing playing";
            SkiaRenderer.drawString(canvas, ellipsize(title, tf, LEFT_W), LEFT_X, infoY, tf, WHITE);
        }
        if (af != null) {
            String artist = song != null ? song.artist() : "Search a song to start";
            SkiaRenderer.drawString(canvas, ellipsize(artist, af, LEFT_W), LEFT_X, infoY + 22f, af, WHITE_60);
        }

        long dur = e.getDurationMs() > 0 ? e.getDurationMs() : (song != null ? song.durationMs() : 0);
        long pos = Math.min(e.getPositionMs(), dur > 0 ? dur : e.getPositionMs());
        Font cf = SkiaFontManager.getRegular(10f);
        float timeY = infoY + 52f;
        float barY = timeY + 12f;
        seekX = LEFT_X;
        seekW = LEFT_W;
        if (cf != null) {
            SkiaRenderer.drawString(canvas, fmtTime(pos), LEFT_X, timeY, cf, WHITE_50);
            String total = fmtTime(dur);
            SkiaRenderer.drawString(canvas, total, LEFT_X + LEFT_W - SkiaRenderer.getStringWidth(total, cf),
                    timeY, cf, WHITE_50);
        }
        float ratio = dur > 0 ? Math.min(1f, pos / (float) dur) : 0f;
        SkiaRenderer.drawRRect(canvas, seekX, barY, seekW, 4f, 2f, GRAY_500);
        if (ratio > 0.001f) {
            SkiaRenderer.drawRRect(canvas, seekX, barY, Math.max(4f, seekW * ratio), 4f, 2f, primary600);
        }
        SkiaRenderer.drawCircle(canvas, seekX + seekW * ratio, barY + 2f, 5f, WHITE);
        boolean seekHover = dmx >= seekX - 4f && dmx <= seekX + seekW + 4f && dmy >= barY - 8f && dmy <= barY + 12f;
        ZONES.add(new Zone(new Hit.SeekBar(seekX, seekW), seekX - 4f, barY - 10f, seekW + 8f, 24f));
        if (seekHover || draggingSeek) {
            SkiaRenderer.drawRRect(canvas, seekX, barY - 1f, seekW, 6f, 3f, withA(primary600, 0.2f));
        }

        float cy = barY + 48f;
        float prevX = LEFT_X + 8f;
        float playX = LEFT_X + 82f;
        float nextX = LEFT_X + 164f;
        boolean playing = e.getState() == MusicPlayerEngine.State.PLAYING;

        iconButton(canvas, OneConfigIcon.SKIP_PREV, prevX, cy - 18f, 36f, dmx, dmy, WHITE_60, WHITE);
        ZONES.add(new Zone(new Hit.Prev(), prevX, cy - 18f, 36f, 36f));

        boolean playHover = dmx >= playX && dmx <= playX + 48f && dmy >= cy - 24f && dmy <= cy + 24f;
        SkiaRenderer.drawCircle(canvas, playX + 24f, cy, 24f, playHover ? primary700 : primary600);
        String playIcon = playing ? OneConfigIcon.PAUSE : OneConfigIcon.PLAY;

        OneConfigIcon.draw(canvas, playIcon, playX + 24f - 9f + (playing ? 0f : 1.5f), cy - 9f, 18f, 18f, WHITE);
        ZONES.add(new Zone(new Hit.PlayPause(), playX, cy - 24f, 48f, 48f));

        iconButton(canvas, OneConfigIcon.SKIP_NEXT, nextX, cy - 18f, 36f, dmx, dmy, WHITE_60, WHITE);
        ZONES.add(new Zone(new Hit.Next(), nextX, cy - 18f, 36f, 36f));

        boolean shuffle = MusicController.shuffle;
        boolean loop = MusicController.loop;
        OneConfigIcon.draw(canvas, OneConfigIcon.SHUFFLE, LEFT_X + LEFT_W - 56f, cy - 9f, 18f, 18f,
                shuffle ? primary600 : WHITE_50);
        OneConfigIcon.draw(canvas, OneConfigIcon.REFRESH, LEFT_X + LEFT_W - 24f, cy - 9f, 18f, 18f,
                loop ? primary600 : WHITE_50);
        ZONES.add(new Zone(new Hit.Shuffle(), LEFT_X + LEFT_W - 64f, cy - 12f, 28f, 24f));
        ZONES.add(new Zone(new Hit.Loop(), LEFT_X + LEFT_W - 32f, cy - 12f, 28f, 24f));

        float vy = cy + 56f;
        volX = LEFT_X + 28f;
        volW = 192f;
        OneConfigIcon.draw(canvas, OneConfigIcon.VOLUME, LEFT_X, vy - 8f, 16f, 16f, WHITE_50);
        float vr = MusicController.volume / 100f;
        SkiaRenderer.drawRRect(canvas, volX, vy - 2f, volW, 4f, 2f, GRAY_500);
        SkiaRenderer.drawRRect(canvas, volX, vy - 2f, Math.max(4f, volW * vr), 4f, 2f, primary600);
        SkiaRenderer.drawCircle(canvas, volX + volW * vr, vy, 5f, WHITE);
        ZONES.add(new Zone(new Hit.VolumeBar(volX, volW), volX - 6f, vy - 12f, volW + 12f, 24f));

        if (e.getState() == MusicPlayerEngine.State.ERROR && e.getError() != null && cf != null) {
            SkiaRenderer.drawString(canvas, ellipsize("Error: " + e.getError(), cf, LEFT_W),
                    LEFT_X, vy + 24f, cf, 0xFFCC4444);
        }

        if (MusicController.loginState == MusicController.LoginState.LOGGED_IN) {
            float uy = vy + 46f;
            SkiaRenderer.drawRRect(canvas, LEFT_X, uy, 20f, 20f, 10f, GRAY_500);
            Image av = MusicController.avatarUrl != null ? MusicLibrary.cover(MusicController.avatarUrl) : null;
            if (av != null) {
                canvas.save();
                SkiaRenderer.clipRRect(canvas, LEFT_X, uy, 20f, 20f, 10f, io.github.humbleui.skija.ClipMode.INTERSECT);
                canvas.drawImageRect(av, Rect.makeWH(av.getWidth(), av.getHeight()),
                        Rect.makeXYWH(LEFT_X, uy, 20f, 20f), io.github.humbleui.skija.SamplingMode.LINEAR, null, true);
                canvas.restore();
            }
            Font nf = SkiaFontManager.getMedium(12f);
            if (nf != null) {
                String nick = MusicController.nickname != null ? MusicController.nickname : "Signed in";
                SkiaRenderer.drawString(canvas, ellipsize(nick, nf, LEFT_W - 90f), LEFT_X + 28f,
                        uy + 10f + nf.getMetrics().getCapHeight() / 2f, nf, WHITE);
            }
            Font lf = SkiaFontManager.getRegular(11f);
            if (lf != null) {
                String out = "Logout";
                float outW = SkiaRenderer.getStringWidth(out, lf);
                float outX = LEFT_X + LEFT_W - outW;
                boolean outHover = dmx >= outX - 6f && dmx <= LEFT_X + LEFT_W && dmy >= uy && dmy <= uy + 20f;
                SkiaRenderer.drawString(canvas, out, outX, uy + 10f + lf.getMetrics().getCapHeight() / 2f,
                        lf, outHover ? WHITE_90 : WHITE_50);
                ZONES.add(new Zone(new Hit.Logout(), outX - 6f, uy, outW + 6f, 20f));
            }
        } else if (MusicController.loginState == MusicController.LoginState.OFF) {
            float uy = vy + 46f;
            boolean hover = dmx >= LEFT_X && dmx <= LEFT_X + LEFT_W && dmy >= uy && dmy <= uy + 20f;
            OneConfigIcon.draw(canvas, OneConfigIcon.USERS, LEFT_X, uy + 2f, 16f, 16f,
                    hover ? WHITE_90 : WHITE_50);
            Font lf = SkiaFontManager.getRegular(12f);
            if (lf != null) {
                SkiaRenderer.drawString(canvas, "Sign in with QR to play VIP songs", LEFT_X + 24f,
                        uy + 10f + lf.getMetrics().getCapHeight() / 2f, lf, hover ? WHITE_90 : WHITE_50);
            }
            ZONES.add(new Zone(new Hit.LoginEntry(), LEFT_X, uy, LEFT_W, 20f));
        }
    }

    private static void drawLoginCard(Canvas canvas, MusicController.LoginState ls) {
        SkiaRenderer.drawRRect(canvas, LEFT_X, TOP_Y, COVER, COVER, 12f, GRAY_700);

        Font hf = SkiaFontManager.getMedium(15f);
        if (hf != null) {
            String title = "Scan to sign in";
            float tw = SkiaRenderer.getStringWidth(title, hf);
            SkiaRenderer.drawString(canvas, title, LEFT_X + (COVER - tw) / 2f, TOP_Y + 36f, hf, WHITE);
        }

        OneConfigIcon.draw(canvas, OneConfigIcon.X_CLOSE, LEFT_X + COVER - 30f, TOP_Y + 12f,
                16f, 16f, WHITE_50);
        ZONES.add(new Zone(new Hit.QrClose(), LEFT_X + COVER - 34f, TOP_Y + 8f, 24f, 24f));

        float qs = 176f;
        float qx = LEFT_X + (COVER - qs) / 2f;
        float qy = TOP_Y + 64f;
        SkiaRenderer.drawRRect(canvas, qx - 4f, qy - 4f, qs + 8f, qs + 8f, 8f, WHITE);
        Image qr = MusicController.qrImage;
        if (qr != null) {
            canvas.save();
            SkiaRenderer.clipRRect(canvas, qx, qy, qs, qs, 4f, io.github.humbleui.skija.ClipMode.INTERSECT);
            canvas.drawImageRect(qr, Rect.makeWH(qr.getWidth(), qr.getHeight()),
                    Rect.makeXYWH(qx, qy, qs, qs), io.github.humbleui.skija.SamplingMode.LINEAR, null, true);
            canvas.restore();
        } else {
            OneConfigIcon.draw(canvas, OneConfigIcon.MUSIC, qx + qs / 2f - 12f, qy + qs / 2f - 12f,
                    24f, 24f, WHITE_40);
        }

        Font sf = SkiaFontManager.getRegular(12f);
        if (sf != null) {
            String status = switch (ls) {
                case LOADING -> "Loading QR code...";
                case WAIT_SCAN -> "Waiting for scan...";
                case WAIT_CONFIRM -> "Scanned - confirm on phone";
                case EXPIRED -> "QR expired - click to refresh";
                default -> "";
            };
            int color = ls == MusicController.LoginState.EXPIRED ? 0xFFCC6666 : WHITE_60;
            float sw = SkiaRenderer.getStringWidth(status, sf);
            SkiaRenderer.drawString(canvas, status, LEFT_X + (COVER - sw) / 2f, TOP_Y + 280f, sf, color);
        }

        if (ls == MusicController.LoginState.EXPIRED) {
            ZONES.add(new Zone(new Hit.QrRefresh(), qx, qy, qs, qs));
        }
    }

    private static void drawSearchAndList(Canvas canvas, double dmx, double dmy, int primary600) {

        float sy = TOP_Y;
        SkiaRenderer.drawRRect(canvas, RIGHT_X + 1f, sy + 1f, RIGHT_W - 2f, SEARCH_H - 2f, 12f, GRAY_850);
        strokeRRect(canvas, RIGHT_X, sy, RIGHT_W - 0.5f, SEARCH_H - 0.5f, 12f,
                searchFocus ? primary600 : GRAY_700, 1f);
        OneConfigIcon.draw(canvas, OneConfigIcon.SEARCH, RIGHT_X + 12f, sy + 11f, 18f, 18f, WHITE_50);

        Font sf = SkiaFontManager.getRegular(14f);
        if (sf != null) {
            String q = MusicController.session.keywords;
            String shown = q.isEmpty() ? "Search NetEase Cloud Music..." : q;
            SkiaRenderer.drawString(canvas, ellipsize(shown, sf, RIGHT_W - 120f), RIGHT_X + 40f,
                    sy + SEARCH_H / 2f + sf.getMetrics().getCapHeight() / 2f, sf,
                    q.isEmpty() ? WHITE_50 : WHITE);
            if (searchFocus && !q.isEmpty() && (System.currentTimeMillis() / 500L) % 2 == 0) {
                float cw = SkiaRenderer.getStringWidth(ellipsize(q, sf, RIGHT_W - 120f), sf);
                SkiaRenderer.drawRect(canvas, RIGHT_X + 41f + cw, sy + 10f, 1f, 20f, WHITE);
            }
        }
        ZONES.add(new Zone(new Hit.SearchBox(), RIGHT_X, sy, RIGHT_W, SEARCH_H));

        float listY = sy + SEARCH_H + 16f;
        List<MusicLibrary.Song> results = MusicController.session.results;

        Font tf = SkiaFontManager.getMedium(13f);
        Font af = SkiaFontManager.getRegular(11f);
        Font df = SkiaFontManager.getRegular(10f);

        if (MusicController.session.state == MusicLibrary.LoadState.LOADING) {
            if (tf != null) SkiaRenderer.drawString(canvas, "Loading...", RIGHT_X, listY + 4f, tf, WHITE_50);
        } else if (MusicController.session.state == MusicLibrary.LoadState.ERROR) {
            if (tf != null) SkiaRenderer.drawString(canvas,
                    ellipsize("Error: " + MusicController.session.error, tf, RIGHT_W), RIGHT_X, listY + 4f, tf, 0xFFCC6666);
        } else if (results.isEmpty()) {
            if (tf != null) SkiaRenderer.drawString(canvas,
                    "No results found",
                    RIGHT_X, listY + 4f, tf, WHITE_50);
        }

        float rowsY = listY + 20f;
        float contentH = results.size() * ROW_H;
        listScrollTarget = Math.max(Math.min(0f, listScrollTarget), Math.min(0f, LIST_VIEW_H - contentH));

        canvas.save();
        canvas.clipRect(Rect.makeXYWH(RIGHT_X - 8f, rowsY - 4f, RIGHT_W + 16f, LIST_VIEW_H + 8f));
        MusicPlayerEngine e = MusicPlayerEngine.INSTANCE;
        String curId = e.getCurrentId();
        float ry = rowsY + listScroll;
        for (int i = 0; i < results.size(); i++, ry += ROW_H) {
            MusicLibrary.Song s = results.get(i);
            boolean current = s.id().equals(curId);
            boolean hover = dmx >= RIGHT_X && dmx <= RIGHT_X + RIGHT_W && dmy >= ry && dmy <= ry + ROW_H - 4f;

            if (current) {
                SkiaRenderer.drawRRect(canvas, RIGHT_X - 8f, ry, RIGHT_W + 8f, ROW_H - 4f, 8f, GRAY_700);
                SkiaRenderer.drawRRect(canvas, RIGHT_X - 8f, ry + 6f, 3f, ROW_H - 16f, 1.5f, primary600);
            } else if (hover) {
                SkiaRenderer.drawRRect(canvas, RIGHT_X - 8f, ry, RIGHT_W + 8f, ROW_H - 4f, 8f, GRAY_850);
            }

            float iy = ry + 2f;
            SkiaRenderer.drawRRect(canvas, RIGHT_X, iy, 32f, 32f, 4f, GRAY_500);
            Image c = MusicLibrary.cover(s.coverUrl());
            if (c != null) {
                canvas.save();
                SkiaRenderer.clipRRect(canvas, RIGHT_X, iy, 32f, 32f, 4f, io.github.humbleui.skija.ClipMode.INTERSECT);
                canvas.drawImageRect(c, Rect.makeWH(c.getWidth(), c.getHeight()),
                        Rect.makeXYWH(RIGHT_X, iy, 32f, 32f), io.github.humbleui.skija.SamplingMode.LINEAR, null, true);
                canvas.restore();
            }

            if (tf != null) {
                SkiaRenderer.drawString(canvas, ellipsize(s.title(), tf, RIGHT_W - 140f),
                        RIGHT_X + 44f, iy + 9f + tf.getMetrics().getCapHeight() / 2f - 2f, tf,
                        current ? primary600 : WHITE_90);
            }
            if (af != null) {
                SkiaRenderer.drawString(canvas, ellipsize(s.artist(), af, RIGHT_W - 140f),
                        RIGHT_X + 44f, iy + 26f, af, WHITE_50);
            }
            if (df != null) {
                String dur = fmtTime(s.durationMs());
                SkiaRenderer.drawString(canvas, dur, RIGHT_X + RIGHT_W - SkiaRenderer.getStringWidth(dur, df),
                        iy + 16f, df, WHITE_40);
            }

            ZONES.add(new Zone(new Hit.SongRow(i), RIGHT_X - 8f, ry, RIGHT_W + 8f, ROW_H - 4f));
        }
        canvas.restore();

        if (contentH > LIST_VIEW_H) {
            float barH = LIST_VIEW_H * (LIST_VIEW_H / contentH);
            float barY = rowsY + (-listScroll) / contentH * LIST_VIEW_H;
            SkiaRenderer.drawRRect(canvas, RIGHT_X + RIGHT_W + 6f, barY, 4f, barH, 2f, GRAY_500);
        }
    }

    private static final float CONTENT_BOTTOM = 784f;
    private static final float LIST_VIEW_H = CONTENT_BOTTOM - (TOP_Y + SEARCH_H + 16f + 20f);

    public static boolean mouseClicked(double dmx, double dmy, int button) {
        if (button != 0) return false;
        for (int i = ZONES.size() - 1; i >= 0; i--) {
            Zone z = ZONES.get(i);
            if (!z.contains(dmx, dmy)) continue;

            if (z.hit() instanceof Hit.SearchBox) {
                searchFocus = true;
                return true;
            }
            searchFocus = false;

            switch (z.hit()) {
                case Hit.SongRow sr -> MusicController.playIndex(sr.index());
                case Hit.PlayPause ignored -> MusicController.togglePause();
                case Hit.Next ignored -> MusicController.playNext();
                case Hit.Prev ignored -> MusicController.playPrev();
                case Hit.Shuffle ignored -> MusicController.shuffle = !MusicController.shuffle;
                case Hit.Loop ignored -> MusicController.loop = !MusicController.loop;
                case Hit.SeekBar sb -> {
                    draggingSeek = true;
                    applySeek(dmx, sb.x(), sb.w());
                }
                case Hit.VolumeBar vb -> {
                    draggingVolume = true;
                    applyVolume(dmx, vb.x(), vb.w());
                }
                case Hit.LoginEntry ignored -> MusicController.openLogin();
                case Hit.QrRefresh ignored -> MusicController.openLogin();
                case Hit.QrClose ignored -> MusicController.closeLogin();
                case Hit.Logout ignored -> MusicController.logout();
                default -> {}
            }
            return true;
        }
        searchFocus = false;
        return false;
    }

    public static boolean mouseDragged(double dmx, double dmy) {
        if (draggingSeek) {
            applySeek(dmx, seekX, seekW);
            return true;
        }
        if (draggingVolume) {
            applyVolume(dmx, volX, volW);
            return true;
        }
        return false;
    }

    public static void mouseReleased() {
        draggingSeek = false;
        draggingVolume = false;
    }

    public static boolean mouseScrolled(double dmx, double dmy, double verticalAmount) {
        List<MusicLibrary.Song> results = MusicController.session.results;
        float contentH = results.size() * ROW_H;
        if (contentH <= LIST_VIEW_H) return false;
        if (dmx < RIGHT_X - 16f || dmx > RIGHT_X + RIGHT_W + 16f) return false;

        float delta = (float) verticalAmount * ROW_H;
        float max = LIST_VIEW_H - contentH;
        listScrollTarget = Math.max(max, Math.min(0f, listScrollTarget + delta));
        return true;
    }

    public static boolean keyPressed(int key) {
        if (!searchFocus) return false;

        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            searchFocus = false;
            return true;
        }
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || key == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
            MusicController.search(MusicController.session.keywords.trim());
            listScrollTarget = 0f;
            listScroll = 0f;
            return true;
        }
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) {
            String q = MusicController.session.keywords;
            if (!q.isEmpty()) MusicController.session.keywords = q.substring(0, q.length() - 1);
            return true;
        }
        return true;
    }

    public static boolean charTyped(int codepoint) {
        if (!searchFocus) return false;
        MusicController.session.keywords += Character.toString(codepoint);
        return true;
    }

    private static void applySeek(double dmx, float x, float w) {
        MusicLibrary.Song song = MusicController.session.current();
        long dur = MusicPlayerEngine.INSTANCE.getDurationMs() > 0
                ? MusicPlayerEngine.INSTANCE.getDurationMs()
                : (song != null ? song.durationMs() : 0);
        if (dur <= 0) return;
        float ratio = (float) Math.min(1d, Math.max(0d, (dmx - x) / w));
        MusicPlayerEngine.INSTANCE.seek((long) (ratio * dur));
    }

    private static void applyVolume(double dmx, float x, float w) {
        float ratio = (float) Math.min(1d, Math.max(0d, (dmx - x) / w));
        MusicController.volume = Math.round(ratio * 100f);
        MusicController.applyVolume();
    }

    private static void iconButton(Canvas canvas, String icon, float x, float y, float size,
                                   double dmx, double dmy, int idle, int hoverColor) {
        boolean hover = dmx >= x && dmx <= x + size && dmy >= y && dmy <= y + size;
        OneConfigIcon.draw(canvas, icon, x + size / 2f - 9f, y + size / 2f - 9f, 18f, 18f,
                hover ? hoverColor : idle);
    }

    private static void strokeRRect(Canvas canvas, float x, float y, float w, float h, float r, int color, float stroke) {
        try (io.github.humbleui.skija.Paint p = new io.github.humbleui.skija.Paint()) {
            p.setAntiAlias(true);
            p.setMode(io.github.humbleui.skija.PaintMode.STROKE);
            p.setStrokeWidth(stroke);
            p.setColor(color);
            canvas.drawRRect(RRect.makeXYWH(x, y, w, h, r), p);
        }
    }

    private static int withA(int color, float alpha) {
        int a = (int) (((color >>> 24) & 0xFF) * Math.min(1f, Math.max(0f, alpha)));
        return (a << 24) | (color & 0x00FFFFFF);
    }

    private static String ellipsize(String text, Font font, float maxW) {
        if (SkiaRenderer.getStringWidth(text, font) <= maxW) return text;
        while (text.length() > 1 && SkiaRenderer.getStringWidth(text + "...", font) > maxW) {
            text = text.substring(0, text.length() - 1);
        }
        return text + "...";
    }

    private static String fmtTime(long ms) {
        if (ms <= 0) return "0:00";
        long s = ms / 1000;
        return s >= 3600
                ? String.format(Locale.ROOT, "%d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
                : String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60);
    }
}
