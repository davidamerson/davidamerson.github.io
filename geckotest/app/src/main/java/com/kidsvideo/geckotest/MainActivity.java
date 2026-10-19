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
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
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
    private static final String VIDEO_URL =
            "https://m.youtube.com/watch?v=" + VIDEO_ID + "&autoplay=0&kidslock=1";
    private static final String SIGN_IN_URL =
            "https://accounts.google.com/ServiceLogin?service=youtube&continue=https%3A%2F%2Fm.youtube.com%2F";

    private static GeckoRuntime runtime;

    private GeckoSession session;
    private GeckoView geckoView;
    private TextView status;
    private ProgressBar progress;
    private LinearLayout parentBar;
    private Button signInButton;
    private Button testButton;

    private boolean parentMode = true;
    private boolean watchLoaded;
    private boolean fullScreen;
    private boolean lockInstalled;
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
        progress.setVisibility(View.GONE);

        status = new TextView(this);
        status.setText("PARENT SIGN-IN TEST — ZIVPN stays ON. QUIC/HTTP3 will be OFF.");
        status.setTextColor(Color.WHITE);
        status.setTextSize(15f);
        status.setBackgroundColor(0xDD000000);
        status.setPadding(20, 12, 20, 12);
        FrameLayout.LayoutParams sp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        sp.topMargin = 18;
        root.addView(status, sp);

        parentBar = new LinearLayout(this);
        parentBar.setOrientation(LinearLayout.HORIZONTAL);
        parentBar.setGravity(Gravity.CENTER);
        parentBar.setPadding(12, 10, 12, 10);
        parentBar.setBackgroundColor(0xEE111111);

        signInButton = new Button(this);
        signInButton.setText("1. Parent Sign-In");
        parentBar.addView(signInButton, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        testButton = new Button(this);
        testButton.setText("2. Test Locked Video");
        LinearLayout.LayoutParams testParams = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        testParams.leftMargin = 8;
        parentBar.addView(testButton, testParams);

        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM);
        root.addView(parentBar, bp);

        setContentView(root);

        if (runtime == null) {
            runtime = GeckoRuntime.create(getApplicationContext());
        }

        session = new GeckoSession();
        session.setNavigationDelegate(new GeckoSession.NavigationDelegate() {
            @Override public GeckoResult<AllowOrDeny> onLoadRequest(
                    GeckoSession s, GeckoSession.NavigationDelegate.LoadRequest request) {
                // In parent setup mode, allow the Google/YouTube sign-in flow to complete.
                if (parentMode) {
                    return GeckoResult.fromValue(AllowOrDeny.ALLOW);
                }

                // In child/test mode, only the exact selected YouTube watch page is allowed.
                return GeckoResult.fromValue(
                        isSameVideoWatchUrl(request.uri) ? AllowOrDeny.ALLOW : AllowOrDeny.DENY);
            }

            @Override public GeckoResult<GeckoSession> onNewSession(GeckoSession s, String uri) {
                // No popups/new tabs. Google sign-in normally completes in the same tab.
                return null;
            }
        });

        session.setProgressDelegate(new GeckoSession.ProgressDelegate() {
            @Override public void onPageStart(GeckoSession s, String url) {
                currentTopUrl = url == null ? "" : url;
                if (parentMode) {
                    status.setVisibility(View.VISIBLE);
                    status.setText("Parent mode: finish Google/YouTube sign-in, then tap ‘Test Locked Video’.\nYour password is entered on Google’s page, not read by this app.");
                } else if (isSameVideoWatchUrl(currentTopUrl)) {
                    status.setVisibility(View.VISIBLE);
                    status.setText("SIGNED-SESSION TEST — QUIC OFF — loading selected video…");
                }
            }

            @Override public void onPageStop(GeckoSession s, boolean success) {
                if (!success) {
                    progress.setVisibility(View.GONE);
                    status.setVisibility(View.VISIBLE);
                    status.setText(parentMode
                            ? "Parent sign-in page failed to load. Keep ZIVPN ON and try again."
                            : "Video page failed to load. QUIC is OFF in this test.");
                    return;
                }

                if (!parentMode && isSameVideoWatchUrl(currentTopUrl)) {
                    watchLoaded = true;
                    status.setVisibility(View.GONE);
                    progress.setVisibility(View.GONE);
                } else if (parentMode) {
                    progress.setVisibility(View.GONE);
                    status.setVisibility(View.VISIBLE);
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

        signInButton.setOnClickListener(v -> openParentSignIn());
        testButton.setOnClickListener(v -> startLockedVideoTest());

        // Keep the same networking condition as the previous diagnostic:
        // HTTP/3/QUIC disabled, everything else unchanged.
        GeckoPreferenceController.setGeckoPref(
                "network.http.http3.enable",
                false,
                GeckoPreferenceController.PREF_BRANCH_USER)
                .accept(
                        ignored -> status.setText(
                                "QUIC OFF confirmed. Tap ‘1. Parent Sign-In’. After sign-in, tap ‘2. Test Locked Video’."),
                        error -> {
                            parentBar.setVisibility(View.GONE);
                            status.setVisibility(View.VISIBLE);
                            status.setText("TEST INVALID: could not disable QUIC/HTTP3.");
                        });
    }

    private void openParentSignIn() {
        parentMode = true;
        watchLoaded = false;
        parentBar.setVisibility(View.VISIBLE);
        progress.setVisibility(View.VISIBLE);
        status.setVisibility(View.VISIBLE);
        status.setText("Opening Google sign-in inside this test browser…\nComplete sign-in, then tap ‘2. Test Locked Video’. ");
        session.loadUri(SIGN_IN_URL);
    }

    private void startLockedVideoTest() {
        parentMode = false;
        watchLoaded = false;
        parentBar.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
        status.setVisibility(View.VISIBLE);
        status.setText("Keeping the signed-in cookies. QUIC OFF. Installing child lock…");

        if (lockInstalled) {
            session.loadUri(VIDEO_URL);
            return;
        }

        runtime.getWebExtensionController()
                .ensureBuiltIn("resource://android/assets/kidslock/", "kidslock@example.com")
                .accept(
                        extension -> {
                            lockInstalled = true;
                            session.loadUri(VIDEO_URL);
                        },
                        error -> {
                            progress.setVisibility(View.GONE);
                            status.setText("Lock extension failed — test stopped rather than opening unrestricted YouTube.");
                        });
    }

    private boolean isSameVideoWatchUrl(String raw) {
        if (raw == null) return false;
        try {
            Uri u = Uri.parse(raw);
            String host = u.getHost();
            if (host == null || !(host.equals("youtube.com") || host.endsWith(".youtube.com"))) {
                return false;
            }
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

        if (!parentMode) {
            // Leaving the locked player exits the test instead of returning to YouTube browsing.
            finish();
            return;
        }

        // Parent sign-in mode may use Back normally within the authentication flow.
        if (session != null && session.canGoBack()) {
            session.goBack();
        } else {
            finish();
        }
    }

    @Override protected void onDestroy() {
        if (session != null) {
            try { session.close(); } catch (Throwable ignored) {}
        }
        super.onDestroy();
    }
}
