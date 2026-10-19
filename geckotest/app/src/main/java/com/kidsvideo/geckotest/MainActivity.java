package com.kidsvideo.geckotest;

import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.mozilla.geckoview.AllowOrDeny;
import org.mozilla.geckoview.GeckoPreferenceController;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;

public final class MainActivity extends Activity {
    private static final String VIDEO_ID = "M7lc1UVf-VE";
    private static final String VIDEO_URL = "https://m.youtube.com/watch?v=" + VIDEO_ID + "&autoplay=0";
    private static GeckoRuntime runtime;

    private GeckoSession session;
    private GeckoView geckoView;
    private TextView status;
    private ProgressBar progress;
    private boolean watchLoaded;
    private boolean fullScreen;
    private String currentTopUrl = "";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        immersive(true);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        geckoView = new GeckoView(this);
        root.addView(geckoView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        progress = new ProgressBar(this);
        FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(64, 64, Gravity.CENTER);
        root.addView(progress, pp);

        status = new TextView(this);
        status.setText("QUIC-OFF TEST — disabling HTTP/3…");
        status.setTextColor(Color.WHITE);
        status.setTextSize(16f);
        status.setBackgroundColor(0xCC000000);
        status.setPadding(18, 10, 18, 10);
        FrameLayout.LayoutParams sp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        sp.topMargin = 20;
        root.addView(status, sp);

        setContentView(root);

        if (runtime == null) {
            runtime = GeckoRuntime.create(getApplicationContext());
        }

        session = new GeckoSession();
        session.setNavigationDelegate(new GeckoSession.NavigationDelegate() {
            @Override public GeckoResult<AllowOrDeny> onLoadRequest(
                    GeckoSession s, GeckoSession.NavigationDelegate.LoadRequest request) {
                if (!watchLoaded) {
                    return GeckoResult.fromValue(AllowOrDeny.ALLOW);
                }
                return GeckoResult.fromValue(
                        isSameVideoWatchUrl(request.uri) ? AllowOrDeny.ALLOW : AllowOrDeny.DENY);
            }

            @Override public GeckoResult<GeckoSession> onNewSession(GeckoSession s, String uri) {
                return null;
            }
        });

        session.setProgressDelegate(new GeckoSession.ProgressDelegate() {
            @Override public void onPageStart(GeckoSession s, String url) {
                currentTopUrl = url == null ? "" : url;
                if (isSameVideoWatchUrl(currentTopUrl)) {
                    status.setVisibility(View.VISIBLE);
                    status.setText("QUIC OFF — loading video over HTTP/2/TCP…");
                }
            }

            @Override public void onPageStop(GeckoSession s, boolean success) {
                if (success && isSameVideoWatchUrl(currentTopUrl)) {
                    watchLoaded = true;
                    status.setVisibility(View.GONE);
                    progress.setVisibility(View.GONE);
                } else if (!success) {
                    status.setVisibility(View.VISIBLE);
                    progress.setVisibility(View.GONE);
                    status.setText("Page load failed. QUIC is OFF in this test.");
                }
            }
        });

        session.setContentDelegate(new GeckoSession.ContentDelegate() {
            @Override public void onFullScreen(GeckoSession s, boolean enabled) {
                fullScreen = enabled;
                immersive(true);
            }
        });

        session.open(runtime);
        geckoView.setSession(session);

        // This diagnostic intentionally changes only ONE networking variable:
        // disable Gecko's HTTP/3 (QUIC) support. Everything else remains the
        // same as the previous locked-browser test.
        GeckoPreferenceController.setGeckoPref(
                "network.http.http3.enable",
                false,
                GeckoPreferenceController.PREF_BRANCH_USER)
                .accept(
                        ignored -> {
                            status.setVisibility(View.VISIBLE);
                            status.setText("QUIC OFF confirmed — loading locked YouTube page…");
                            installLockAndLoad();
                        },
                        error -> {
                            progress.setVisibility(View.GONE);
                            status.setVisibility(View.VISIBLE);
                            status.setText("TEST INVALID: could not disable QUIC/HTTP3.");
                        });
    }

    private void installLockAndLoad() {
        runtime.getWebExtensionController()
                .ensureBuiltIn("resource://android/assets/kidslock/", "kidslock@example.com")
                .accept(
                        extension -> session.loadUri(VIDEO_URL),
                        error -> {
                            status.setText("QUIC OFF; lock extension failed — not loading test video.");
                            progress.setVisibility(View.GONE);
                        });
    }

    private boolean isSameVideoWatchUrl(String raw) {
        if (raw == null) return false;
        try {
            Uri u = Uri.parse(raw);
            String host = u.getHost();
            if (host == null || !(host.equals("youtube.com") || host.endsWith(".youtube.com"))) return false;
            if (!"/watch".equals(u.getPath())) return false;
            return VIDEO_ID.equals(u.getQueryParameter("v"));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void immersive(boolean ignored) {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override public void onBackPressed() {
        if (fullScreen && session != null) {
            session.exitFullScreen();
            return;
        }
        finish();
    }

    @Override protected void onDestroy() {
        if (session != null) {
            try { session.close(); } catch (Throwable ignored) {}
        }
        super.onDestroy();
    }
}
