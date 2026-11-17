package com.overcookedrecomp.app;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.overcookedrecomp.app.compiler.movingout.MovingOutLauncherActivity;
import com.overcookedrecomp.app.input.GamepadManager;
import com.overcookedrecomp.app.input.VirtualGamepadOverlay;
import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    private static final String TAG = "OvercookedRecomp";

    private static boolean nativeLoaded = false;
    private static String nativeLoadError = null;

    static {
        try {
            System.loadLibrary("overcooked_native");
            nativeLoaded = true;
            Log.i(TAG, "Pustaka native overcooked_native berhasil dimuat.");
        } catch (Throwable t) {
            nativeLoaded = false;
            nativeLoadError = t.getMessage();
            Log.e(TAG, "Gagal memuat pustaka native overcooked_native: " + t.getMessage(), t);
        }
    }

    private SurfaceView gameSurfaceView;
    private FrameLayout rootLayout;
    private View setupOverlayView;
    private TextView oc1StatusText;
    private TextView oc2StatusText;
    private TextView moStatusText;
    private TextView mo2StatusText;
    private GamepadManager gamepadManager;
    private VirtualGamepadOverlay virtualOverlay;
    private boolean virtualControlsEnabled = true;
    private TextView importProgressText;
    private ProgressBar importProgressBar;
    private View firstGameCard;

    private boolean engineInitialized = false;
    private GameStorageVerifier.GameTitle activeGameTitle = GameStorageVerifier.GameTitle.UNKNOWN;
    private GameStorageVerifier.GameTitle pendingImportTitle = GameStorageVerifier.GameTitle.OVERCOOKED_1;

    // File pickers untuk ZIP dan Folder PC
    private final ActivityResultLauncher<String[]> openZipLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) handleImportZipUri(uri);
            });

    private final ActivityResultLauncher<Uri> openFolderLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), uri -> {
                if (uri != null) handleImportFolderUri(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        DiagnosticLog.info(this, "APP", "Ghost Town Recomp started; ABI=" + java.util.Arrays.toString(Build.SUPPORTED_ABIS));

        // Deteksi spesifikasi layar ponsel / tablet HarmonyOS secara aman
        detectAndApplyDeviceDisplaySettings();

        // Buat root view dengan SurfaceView di belakang dan Overlay UI di depan
        rootLayout = new FrameLayout(this);
        rootLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        gameSurfaceView = new SurfaceView(this);
        gameSurfaceView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        gameSurfaceView.getHolder().addCallback(this);
        rootLayout.addView(gameSurfaceView);

        // Inisialisasi Gamepad Manager untuk DS4 & Xbox
        gamepadManager = new GamepadManager(this);
        gamepadManager.setInputCallback(new GamepadManager.InputCallback() {
            @Override
            public void onAxisEvent(int deviceId, float lx, float ly, float rx, float ry, float hatX, float hatY) {
                if (nativeLoaded) {
                    try {
                        nativeOnGamepadAxis(deviceId, lx, ly, rx, ry, hatX, hatY);
                    } catch (Throwable ignored) {}
                }
            }

            @Override
            public void onButtonEvent(int deviceId, int keyCode, boolean pressed) {
                if (nativeLoaded) {
                    try {
                        nativeOnGamepadButton(deviceId, keyCode, pressed);
                    } catch (Throwable ignored) {}
                }
            }
        });

        // Kontrol Layar Sentuh Virtual (Virtual Touch Gamepad)
        virtualOverlay = new VirtualGamepadOverlay(this);
        virtualOverlay.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        virtualOverlay.setInputListener(new VirtualGamepadOverlay.VirtualInputListener() {
            @Override
            public void onVirtualAxis(float lx, float ly) {
                if (nativeLoaded) {
                    try {
                        nativeOnGamepadAxis(999, lx, ly, 0f, 0f, 0f, 0f);
                    } catch (Throwable ignored) {}
                }
            }

            @Override
            public void onVirtualButton(int keyCode, boolean pressed) {
                if (nativeLoaded) {
                    try {
                        nativeOnGamepadButton(999, keyCode, pressed);
                    } catch (Throwable ignored) {}
                }
            }

            @Override
            public void onToggleMenuRequested() {
                if (setupOverlayView != null) {
                    if (setupOverlayView.getVisibility() == View.VISIBLE) {
                        setupOverlayView.setVisibility(View.GONE);
                    } else {
                        setupOverlayView.setVisibility(View.VISIBLE);
                        refreshInstalledGamesStatus();
                    }
                }
            }
        });
        virtualOverlay.setVisibility(View.GONE); // Gamepad sentuh hanya tampil saat runtime game, bukan di dashboard.
        rootLayout.addView(virtualOverlay);

        // Buat UI Importer & Setup
        setupOverlayView = createSetupOverlay();
        rootLayout.addView(setupOverlayView);

        // Tombol Mengambang (Floating Action Button) untuk Buka Menu & Pengaturan
        Button menuBtn = createFloatingMenuButton();
        rootLayout.addView(menuBtn);

        setContentView(rootLayout);

        // Peringatan jika pustaka native gagal dimuat (misal kendala ABI)
        if (!nativeLoaded) {
            new AlertDialog.Builder(this)
                    .setTitle("Peringatan Sistem HarmonyOS")
                    .setMessage("Pustaka native C++ (" + nativeLoadError + ") tidak dapat dimuat secara dinamis. Silakan pastikan arsitektur arm64-v8a didukung.")
                    .setPositiveButton("OK", null)
                    .show();
        }

        // Provision the runtime from APK assets on first launch; never overwrite an existing install.
        ensureBundledRuntimeInstalled();

        // Refresh status game yang terpasang
        refreshInstalledGamesStatus();
    }

    private View createSetupOverlay() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        scrollView.setBackgroundColor(Color.parseColor("#080B12"));
        scrollView.setFillViewport(true);
        scrollView.setClipToPadding(false);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(34), dp(26), dp(34), dp(30));
        container.setGravity(Gravity.TOP);
        container.setFocusableInTouchMode(true);

        // PS4-inspired system header.
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(22));

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        TextView titleView = new TextView(this);
        titleView.setText("GHOST TOWN");
        titleView.setTextSize(25);
        titleView.setTextColor(Color.WHITE);
        titleView.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        brand.addView(titleView);

        TextView subTitle = new TextView(this);
        subTitle.setText("RECOMP  /  CONSOLE LIBRARY");
        subTitle.setTextSize(10);
        subTitle.setLetterSpacing(0.16f);
        subTitle.setTextColor(Color.parseColor("#66B9FF"));
        subTitle.setPadding(1, dp(3), 0, 0);
        brand.addView(subTitle);
        header.addView(brand, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView systemStatus = new TextView(this);
        systemStatus.setText("●  ARM64  •  EXPERIMENTAL");
        systemStatus.setTextSize(11);
        systemStatus.setTextColor(Color.parseColor("#9AA9BD"));
        systemStatus.setPadding(dp(14), dp(9), dp(14), dp(9));
        systemStatus.setBackground(createCardDrawable("#111B2A", "#233B58"));
        header.addView(systemStatus);
        container.addView(header);

        // Featured area, deliberately separate from game execution state.
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(24), dp(20), dp(24), dp(20));
        hero.setBackground(createCardDrawable("#101B2B", "#1C4772"));
        TextView heroEyebrow = new TextView(this);
        heroEyebrow.setText("YOUR GAME SPACE");
        heroEyebrow.setTextSize(10);
        heroEyebrow.setLetterSpacing(0.18f);
        heroEyebrow.setTextColor(Color.parseColor("#5CB7FF"));
        hero.addView(heroEyebrow);

        TextView heroTitle = new TextView(this);
        heroTitle.setText("Play your library.");
        heroTitle.setTextSize(27);
        heroTitle.setTextColor(Color.WHITE);
        heroTitle.setTypeface(null, Typeface.BOLD);
        heroTitle.setPadding(0, dp(5), 0, dp(4));
        hero.addView(heroTitle);

        TextView heroBody = new TextView(this);
        heroBody.setText("Pilih game, impor data milikmu, lalu periksa kesiapan runtime. Dashboard ini adalah launcher eksperimental, bukan emulator Windows.");
        heroBody.setTextSize(12);
        heroBody.setTextColor(Color.parseColor("#B5C4D8"));
        heroBody.setMaxWidth(dp(760));
        hero.addView(heroBody);
        LinearLayout.LayoutParams heroParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        heroParams.bottomMargin = dp(26);
        container.addView(hero, heroParams);

        TextView libraryTitle = new TextView(this);
        libraryTitle.setText("GAME LIBRARY");
        libraryTitle.setTextSize(13);
        libraryTitle.setLetterSpacing(0.12f);
        libraryTitle.setTextColor(Color.WHITE);
        libraryTitle.setTypeface(null, Typeface.BOLD);
        libraryTitle.setPadding(0, 0, 0, dp(12));
        container.addView(libraryTitle);

        HorizontalScrollView gameScroll = new HorizontalScrollView(this);
        gameScroll.setHorizontalScrollBarEnabled(false);
        gameScroll.setClipToPadding(false);
        LinearLayout gameRow = new LinearLayout(this);
        gameRow.setOrientation(LinearLayout.HORIZONTAL);
        gameRow.setGravity(Gravity.TOP);
        gameRow.addView(createGameCard(
                GameStorageVerifier.GameTitle.OVERCOOKED_1,
                "Overcooked!",
                "PC  •  Unity",
                text -> oc1StatusText = text));
        gameRow.addView(createGameCard(
                GameStorageVerifier.GameTitle.OVERCOOKED_2,
                "Overcooked! 2",
                "PC  •  Unity",
                text -> oc2StatusText = text));
        gameRow.addView(createGameCard(
                GameStorageVerifier.GameTitle.MOVING_OUT,
                "Moving Out",
                "PC  •  SMG Studio",
                text -> moStatusText = text));
        gameRow.addView(createGameCard(
                GameStorageVerifier.GameTitle.MOVING_OUT_2,
                "Moving Out 2",
                "PC  •  SMG Studio",
                text -> mo2StatusText = text));
        gameScroll.addView(gameRow);
        container.addView(gameScroll);

        TextView importTitle = new TextView(this);
        importTitle.setText("LIBRARY TOOLS");
        importTitle.setTextSize(11);
        importTitle.setLetterSpacing(0.14f);
        importTitle.setTextColor(Color.parseColor("#8FA2BA"));
        importTitle.setTypeface(null, Typeface.BOLD);
        importTitle.setPadding(0, dp(24), 0, dp(10));
        container.addView(importTitle);

        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setGravity(Gravity.CENTER_VERTICAL);

        Button refreshBtn = createDashboardButton("REFRESH LIBRARY", "#0878D1");
        refreshBtn.setOnClickListener(v -> refreshInstalledGamesStatus());
        LinearLayout.LayoutParams refreshParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
        refreshParams.setMarginEnd(dp(10));
        actionRow.addView(refreshBtn, refreshParams);

        Button autoScanBtn = createDashboardButton("SCAN DOWNLOADS", "#1A2B40");
        autoScanBtn.setOnClickListener(v -> scanDownloadFolder());
        actionRow.addView(autoScanBtn, new LinearLayout.LayoutParams(0, dp(48), 1f));
        container.addView(actionRow);

        LinearLayout settingsRow = new LinearLayout(this);
        settingsRow.setOrientation(LinearLayout.HORIZONTAL);
        settingsRow.setGravity(Gravity.CENTER_VERTICAL);
        settingsRow.setPadding(0, dp(10), 0, 0);

        Button toggleVirtualBtn = createDashboardButton("TOUCH CONTROLS: " + (virtualControlsEnabled ? "ON" : "OFF"), "#111B2A");
        toggleVirtualBtn.setOnClickListener(v -> {
            virtualControlsEnabled = !virtualControlsEnabled;
            toggleVirtualBtn.setText("TOUCH CONTROLS: " + (virtualControlsEnabled ? "ON" : "OFF"));
            if (virtualOverlay != null && engineInitialized) {
                virtualOverlay.setVisibility(virtualControlsEnabled ? View.VISIBLE : View.GONE);
            }
        });
        LinearLayout.LayoutParams toggleParams = new LinearLayout.LayoutParams(0, dp(44), 1f);
        toggleParams.setMarginEnd(dp(10));
        settingsRow.addView(toggleVirtualBtn, toggleParams);

        Button gfxBtn = createDashboardButton("GRAPHICS & DISPLAY", "#111B2A");
        gfxBtn.setOnClickListener(v -> showGraphicsSettingsDialog());
        settingsRow.addView(gfxBtn, new LinearLayout.LayoutParams(0, dp(44), 1f));
        container.addView(settingsRow);

        Button wineRuntimeBtn = createDashboardButton("WINE / BOX64 STATUS", "#172B42");
        wineRuntimeBtn.setOnClickListener(v -> showWineRuntimeStatus());
        LinearLayout.LayoutParams wineRuntimeParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(42));
        wineRuntimeParams.topMargin = dp(8);
        container.addView(wineRuntimeBtn, wineRuntimeParams);

        Button runtimeRepairBtn = createDashboardButton("INSTALL / REPAIR BUILT-IN RUNTIME", "#24513D");
        runtimeRepairBtn.setOnClickListener(v -> installBundledRuntime(true));
        LinearLayout.LayoutParams runtimeRepairParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(42));
        runtimeRepairParams.topMargin = dp(8);
        container.addView(runtimeRepairBtn, runtimeRepairParams);

        Button diagnosticsBtn = createDashboardButton("BAGIKAN LOG DIAGNOSTIK", "#493018");
        diagnosticsBtn.setOnClickListener(v -> shareDiagnosticReport());
        LinearLayout.LayoutParams diagnosticsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        diagnosticsParams.topMargin = dp(8);
        container.addView(diagnosticsBtn, diagnosticsParams);

        Button graphicsDiagnosticsBtn = createDashboardButton("GPU / TURNIP / DXVK LOG", "#263B55");
        graphicsDiagnosticsBtn.setOnClickListener(v -> showGraphicsDiagnostics());
        LinearLayout.LayoutParams graphicsDiagnosticsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        graphicsDiagnosticsParams.topMargin = dp(8);
        container.addView(graphicsDiagnosticsBtn, graphicsDiagnosticsParams);

        importProgressText = new TextView(this);
        importProgressText.setText("");
        importProgressText.setTextSize(11);
        importProgressText.setTextColor(Color.parseColor("#70C4FF"));
        importProgressText.setPadding(0, dp(12), 0, dp(4));
        importProgressText.setVisibility(View.GONE);
        container.addView(importProgressText);

        importProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        importProgressBar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(3)));
        importProgressBar.setIndeterminate(true);
        importProgressBar.setVisibility(View.GONE);
        container.addView(importProgressBar);

        TextView footer = new TextView(this);
        footer.setText("GHOST TOWN RECOMP  •  BUILD 0.5.0-ALPHA.1  •  EXPERIMENTAL RUNTIME / BYOG");
        footer.setTextSize(9);
        footer.setTextColor(Color.parseColor("#63748B"));
        footer.setLetterSpacing(0.08f);
        footer.setPadding(0, dp(22), 0, 0);
        container.addView(footer);

        scrollView.addView(container);
        scrollView.post(() -> {
            if (firstGameCard != null) firstGameCard.requestFocus();
        });
        return scrollView;
    }

    private interface StatusTextHolder {
        void hold(TextView tv);
    }

    private View createGameCard(GameStorageVerifier.GameTitle title, String cardTitle, String desc, StatusTextHolder holder) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setFocusable(true);
        card.setFocusableInTouchMode(true);
        card.setClickable(true);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(createCardDrawable("#111A28", "#26364C"));
        card.setMinimumHeight(dp(276));

        LinearLayout art = new LinearLayout(this);
        art.setGravity(Gravity.CENTER);
        art.setBackground(createCardDrawable("#172A42", "#284C70"));
        ImageView icon = new ImageView(this);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        icon.setImageResource(title == GameStorageVerifier.GameTitle.MOVING_OUT
                || title == GameStorageVerifier.GameTitle.MOVING_OUT_2
                ? R.drawable.ic_moving_out : R.drawable.ic_overcooked);
        art.addView(icon, new LinearLayout.LayoutParams(dp(76), dp(76)));
        card.addView(art, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(112)));

        TextView name = new TextView(this);
        name.setText(cardTitle);
        name.setTextSize(17);
        name.setTextColor(Color.WHITE);
        name.setTypeface(null, Typeface.BOLD);
        name.setPadding(0, dp(14), 0, dp(3));
        card.addView(name);

        TextView descView = new TextView(this);
        descView.setText(desc);
        descView.setTextSize(10);
        descView.setTextColor(Color.parseColor("#8FA2BA"));
        card.addView(descView);

        TextView status = new TextView(this);
        status.setText("CHECKING LIBRARY...");
        status.setTextSize(10);
        status.setTypeface(null, Typeface.BOLD);
        status.setTextColor(Color.parseColor("#8FA2BA"));
        status.setPadding(0, dp(12), 0, dp(10));
        card.addView(status);
        holder.hold(status);

        TextView actionHint = new TextView(this);
        actionHint.setText("SELECT GAME  ›");
        actionHint.setTextSize(10);
        actionHint.setTextColor(Color.parseColor("#66B9FF"));
        actionHint.setTypeface(null, Typeface.BOLD);
        actionHint.setPadding(0, dp(12), 0, 0);
        card.addView(actionHint);

        // Selecting a tile opens its own game action dialog (Play / Import).
        card.setOnClickListener(v -> showGameActionDialog(title, cardTitle));
        card.setOnFocusChangeListener((v, hasFocus) ->
                card.setBackground(createCardDrawable(
                        hasFocus ? "#14253A" : "#111A28",
                        hasFocus ? "#20A0FF" : "#26364C")));
        if (firstGameCard == null) firstGameCard = card;

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(dp(244), dp(300));
        cardParams.setMarginEnd(dp(14));
        card.setLayoutParams(cardParams);
        return card;
    }

    private void showGameActionDialog(GameStorageVerifier.GameTitle title, String cardTitle) {
        pendingImportTitle = title;
        GameStorageVerifier.VerificationResult installed = null;
        File extDir = getExternalFilesDir(null);
        if (extDir != null) {
            for (GameStorageVerifier.VerificationResult result : GameStorageVerifier.detectInstalledGames(extDir)) {
                if (result.gameTitle == title) {
                    installed = result;
                    break;
                }
            }
        }

        final GameStorageVerifier.VerificationResult selectedGame = installed;
        String statusMessage = selectedGame == null
                ? "DATA NOT FOUND\nImpor data game ini terlebih dahulu."
                : "DATA FOUND\n" + selectedGame.message
                    + "\n\nEXECUTION BLOCKED: build ini sudah memiliki instalasi runtime Wine/Box64 opsional dan probe headless, tetapi belum memiliki X11/Wayland display server, Android Vulkan presentation bridge, audio/input bridge, atau peluncur PE game. Folder _Data dan metadata saja tidak dapat menjalankan game PC.\n\nImpor lengkap game PC (EXE, UnityPlayer.dll, folder _Data) tetap diperlukan; runtime Windows dipasang terpisah.";
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(cardTitle)
                .setMessage(statusMessage)
                .setPositiveButton("EXECUTION STATUS", (d, which) -> {
                    if (selectedGame != null) showExecutionRequirements(selectedGame);
                })
                .setNeutralButton("IMPORT FULL PC ZIP", (d, which) -> {
                    pendingImportTitle = title;
                    openZipLauncher.launch(new String[]{"application/zip", "application/x-zip-compressed", "application/octet-stream", "*/*"});
                })
                .setNegativeButton("PILIH FOLDER INSTALASI PC", (d, which) -> {
                    pendingImportTitle = title;
                    openFolderLauncher.launch(null);
                })
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setEnabled(selectedGame != null));
        dialog.show();
    }

    private void ensureBundledRuntimeInstalled() {
        if (RuntimeBundleInstaller.isInstalled(this)) {
            Log.i(TAG, "Bundled runtime provisioning skipped: validated runtime already exists.");
            return;
        }
        installBundledRuntime(false);
    }

    /** Installs or repairs the one runtime shipped inside this APK; no network or file picker. */
    private void installBundledRuntime(boolean userRequested) {
        if (importProgressText != null) {
            importProgressText.setText("Memasang runtime bawaan APK...");
            importProgressText.setVisibility(View.VISIBLE);
        }
        if (importProgressBar != null) importProgressBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                DiagnosticLog.info(this, "RUNTIME", "Provisioning bundled Wine/Box64 runtime from APK assets.");
                RuntimeBundleInstaller.installBundled(this, "runtime/ghost-town-wine-runtime-arm64.tar.gz",
                        message -> runOnUiThread(() -> {
                            if (importProgressText != null) importProgressText.setText(message);
                        }));
                DiagnosticLog.info(this, "RUNTIME", "Bundled runtime installed and checksum-validated.");
                runOnUiThread(() -> {
                    showImportProgress(false, "Runtime bawaan berhasil dipasang dan diverifikasi.");
                    refreshInstalledGamesStatus();
                    new AlertDialog.Builder(this)
                            .setTitle("RUNTIME SIAP")
                            .setMessage("Runtime bawaan APK berhasil diekstrak dan checksum diverifikasi. Ini baru validasi instalasi; pengujian Wine, Vulkan, dan peluncuran game masih diperlukan.")
                            .setPositiveButton("PERIKSA STATUS", (dialog, which) -> showWineRuntimeStatus())
                            .setNegativeButton("TUTUP", null)
                            .show();
                });
            } catch (Throwable error) {
                DiagnosticLog.error(this, "RUNTIME", "Bundled runtime installation failed", error);
                runOnUiThread(() -> {
                    showImportProgress(false, "Runtime gagal dipasang. Periksa detail diagnostik.");
                    new AlertDialog.Builder(this)
                            .setTitle("INSTALASI RUNTIME GAGAL")
                            .setMessage((error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage())
                                    + "\n\nSilakan bagikan log diagnostik jika masalah berulang.")
                            .setPositiveButton("BAGIKAN LOG", (dialog, which) -> shareDiagnosticReport())
                            .setNegativeButton("TUTUP", null)
                            .show();
                });
            }
        }, userRequested ? "runtime-repair" : "bundled-runtime-provisioner").start();
    }

    private void showGraphicsDiagnostics() {
        String hostReport = "Native library unavailable; Vulkan host probe cannot run.";
        if (nativeLoaded) {
            try { hostReport = nativeGetVulkanDiagnostics(); }
            catch (Throwable error) { hostReport = "Vulkan probe error: " + error.getClass().getSimpleName() + ": " + error.getMessage(); }
        }
        String guestReport = WineRuntimeBridge.graphicsDiagnostics(this);
        DiagnosticLog.info(this, "GRAPHICS", "Host Vulkan probe completed. " + hostReport.replace('\n', ' '));
        DiagnosticLog.info(this, "DXVK", guestReport.replace('\n', ' '));
        String combined = "ANDROID HOST VULKAN\n" + hostReport
                + "\n\nWINE GUEST / DXVK\n" + guestReport
                + "\n\nCatatan: probe host tidak membuktikan Vulkan guest DXVK bekerja. Turnip baru dianggap aktif jika driver terpilih benar-benar dimuat oleh proses renderer. Jalur presentasi Wine ke SurfaceView juga belum diaktifkan.";
        TextView content = new TextView(this);
        content.setText(combined + "\n\nAPP LOG (tail)\n" + DiagnosticLog.readTail(this, 18000));
        content.setTextIsSelectable(true);
        content.setTextSize(12);
        content.setPadding(dp(18), dp(12), dp(18), dp(12));
        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        new AlertDialog.Builder(this).setTitle("GPU / TURNIP / DXVK DIAGNOSTICS")
                .setView(scroll)
                .setPositiveButton("BAGIKAN LOG", (dialog, which) -> shareDiagnosticReport())
                .setNeutralButton("REFRESH", (dialog, which) -> showGraphicsDiagnostics())
                .setNegativeButton("TUTUP", null).show();
    }

    private void showWineRuntimeStatus() {
        WineRuntimeBridge.RuntimeStatus status = WineRuntimeBridge.probe(this);
        StringBuilder message = new StringBuilder(status.getMessage());
        message.append("\n\nRuntime path: ").append(status.getRuntimeRoot());
        if (!status.getMissing().isEmpty()) {
            message.append("\n\nKomponen yang belum tersedia:");
            for (String item : status.getMissing()) message.append("\n• ").append(item);
        }
        message.append("\n\nCatatan: DXVK membutuhkan Vulkan bridge yang kompatibel. Status ini hanya memeriksa struktur file, bukan kompatibilitas GPU atau kemampuan menjalankan game.");
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("WINE / BOX64 RUNTIME")
                .setMessage(message.toString())
                .setPositiveButton("TUTUP", null);
        if (status.getState() == WineRuntimeBridge.State.READY_FOR_HEADLESS_PROBE) {
            builder.setNeutralButton("TEST WINE VERSION", (dialog, which) -> {
                Toast.makeText(this, "Menjalankan probe Wine...", Toast.LENGTH_SHORT).show();
                new Thread(() -> {
                    WineRuntimeBridge.ProbeResult result = WineRuntimeBridge.runWineVersionProbe(this);
                    runOnUiThread(() -> new AlertDialog.Builder(this)
                            .setTitle(result.getSuccess() ? "WINE PROBE BERHASIL" : "WINE PROBE GAGAL")
                            .setMessage((result.getOutput().isEmpty() ? "Tidak ada output." : result.getOutput())
                                    + (result.getExitCode() == null ? "" : "\nExit code: " + result.getExitCode()))
                            .setPositiveButton("OK", null)
                            .show());
                }).start();
            });
        }
        builder.show();
    }

    /**
     * Builds a shareable device report containing system details, runtime status,
     * game-data detection, and recent logcat output filtered to this app process.
     */
    private void shareDiagnosticReport() {
        Log.i(TAG, "Tester requested diagnostic report export.");
        StringBuilder report = new StringBuilder(16000);
        report.append("GHOST TOWN RECOMP - DEVICE TEST REPORT\n");
        report.append("Generated: ").append(new java.util.Date()).append("\n\n");
        report.append("DEVICE\n");
        report.append("Manufacturer: ").append(Build.MANUFACTURER).append("\n");
        report.append("Model: ").append(Build.MODEL).append("\n");
        report.append("Device: ").append(Build.DEVICE).append("\n");
        report.append("OS: ").append(Build.VERSION.RELEASE).append(" (SDK ").append(Build.VERSION.SDK_INT).append(")\n");
        report.append("Build fingerprint: ").append(Build.FINGERPRINT).append("\n");
        report.append("Supported ABIs: ").append(java.util.Arrays.toString(Build.SUPPORTED_ABIS)).append("\n");
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        report.append("Display: ").append(metrics.widthPixels).append("x").append(metrics.heightPixels)
                .append(" @ ").append(metrics.densityDpi).append(" dpi\n");
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            ActivityManager.MemoryInfo memory = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(memory);
            report.append("RAM total: ").append(memory.totalMem / (1024 * 1024)).append(" MiB\n");
            report.append("RAM available: ").append(memory.availMem / (1024 * 1024)).append(" MiB\n");
        }
        report.append("App storage available: ").append(getFilesDir().getUsableSpace() / (1024 * 1024)).append(" MiB\n");

        report.append("\nAPPLICATION\n");
        try {
            android.content.pm.PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            report.append("Package: ").append(getPackageName()).append("\n");
            report.append("Version: ").append(info.versionName).append(" (").append(info.getLongVersionCode()).append(")\n");
        } catch (Throwable error) {
            report.append("Version unavailable: ").append(error.getClass().getSimpleName()).append("\n");
        }
        report.append("Native library loaded: ").append(nativeLoaded).append("\n");
        if (nativeLoadError != null) report.append("Native load error: ").append(nativeLoadError).append("\n");

        report.append("\nGAME DATA DETECTION\n");
        File externalFiles = getExternalFilesDir(null);
        if (externalFiles == null) {
            report.append("App external files directory unavailable.\n");
        } else {
            try {
                List<GameStorageVerifier.VerificationResult> games = GameStorageVerifier.detectInstalledGames(externalFiles);
                if (games.isEmpty()) report.append("No supported game data detected.\n");
                for (GameStorageVerifier.VerificationResult game : games) {
                    report.append("- ").append(game.gameTitle.name()).append(": detected\n");
                }
            } catch (Throwable error) {
                report.append("Game scan error: ").append(error.getClass().getSimpleName()).append(": ").append(error.getMessage()).append("\n");
            }
        }

        report.append("\nWINE / BOX64 RUNTIME\n");
        try {
            WineRuntimeBridge.RuntimeStatus runtime = WineRuntimeBridge.probe(this);
            report.append("State: ").append(runtime.getState()).append("\n");
            report.append("Message: ").append(runtime.getMessage()).append("\n");
            for (String missing : runtime.getMissing()) report.append("Missing: ").append(missing).append("\n");
        } catch (Throwable error) {
            report.append("Runtime probe error: ").append(error.getClass().getSimpleName()).append(": ").append(error.getMessage()).append("\n");
        }

        report.append("\nPERSISTENT APP DIAGNOSTIC LOG\n");
        report.append(DiagnosticLog.readTail(this, 50000)).append("\n");
        report.append("\nGRAPHICS / DXVK STATUS\n");
        try {
            report.append(WineRuntimeBridge.graphicsDiagnostics(this)).append("\n");
            if (nativeLoaded) report.append(nativeGetVulkanDiagnostics()).append("\n");
        } catch (Throwable error) {
            report.append("Graphics diagnostics error: ").append(error.getMessage()).append("\n");
        }

        report.append("\nWINE VERSION PROBE OUTPUT\n");
        File wineProbeLog = new File(getFilesDir(), "runtime/wine/wine-version-probe.log");
        if (wineProbeLog.isFile()) {
            try {
                String probeOutput = new String(java.nio.file.Files.readAllBytes(
                        wineProbeLog.toPath()), java.nio.charset.StandardCharsets.UTF_8);
                if (probeOutput.length() > 30000) probeOutput = probeOutput.substring(probeOutput.length() - 30000);
                report.append(probeOutput).append("\n");
            } catch (Throwable error) {
                report.append("Unable to read Wine probe log: ").append(error.getMessage()).append("\n");
            }
        } else {
            report.append("Wine version probe has not produced a log yet.\n");
        }
        File runtimeChecksums = new File(getFilesDir(), "runtime/wine/provenance/RUNTIME-SHA256SUMS");
        if (runtimeChecksums.isFile()) {
            report.append("\nRUNTIME PAYLOAD CHECKSUM MANIFEST\n");
            try {
                report.append(new String(java.nio.file.Files.readAllBytes(
                        runtimeChecksums.toPath()), java.nio.charset.StandardCharsets.UTF_8));
            } catch (Throwable error) {
                report.append("Unable to read checksum manifest: ").append(error.getMessage()).append("\n");
            }
        }

        report.append("\nAPP LOGCAT (last 1200 lines, this app process only)\n");
        report.append(collectAppLogcat());
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Ghost Town Recomp - Device Test Report");
        shareIntent.putExtra(Intent.EXTRA_TEXT, report.toString());
        try {
            startActivity(Intent.createChooser(shareIntent, "Bagikan laporan diagnostik"));
        } catch (Throwable error) {
            Toast.makeText(this, "Tidak dapat membuka menu berbagi: " + error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String collectAppLogcat() {
        File output = new File(getCacheDir(), "device-diagnostic-logcat.txt");
        try {
            Process process = new ProcessBuilder("logcat", "-d", "-v", "threadtime",
                    "--pid=" + android.os.Process.myPid(), "-t", "1200")
                    .redirectErrorStream(true).redirectOutput(output).start();
            if (!process.waitFor(8, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return "logcat collection timed out after 8 seconds.\n";
            }
            String content = output.isFile()
                    ? new String(java.nio.file.Files.readAllBytes(output.toPath()), java.nio.charset.StandardCharsets.UTF_8) : "";
            if (content.length() > 60000) content = content.substring(content.length() - 60000);
            return "logcat exit code: " + process.exitValue() + "\n"
                    + (content.isEmpty() ? "(No app logcat output captured.)\n" : content);
        } catch (Throwable error) {
            return "Unable to collect app logcat: " + error.getClass().getSimpleName()
                    + ": " + error.getMessage() + "\n";
        }
    }

    private Button createDashboardButton(String label, String backgroundColor) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(11);
        button.setTypeface(null, Typeface.BOLD);
        button.setTextColor(Color.WHITE);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(10), 0, dp(10), 0);
        button.setMinHeight(dp(40));
        button.setBackground(createButtonDrawable(backgroundColor));
        button.setFocusable(true);
        return button;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private Button createFloatingMenuButton() {
        Button btn = new Button(this);
        btn.setText("⚙️ Menu");
        btn.setTextSize(12);
        btn.setTextColor(Color.WHITE);
        btn.setBackground(createButtonDrawable("#CC1E2229"));
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.gravity = Gravity.TOP | Gravity.END;
        p.setMargins(0, 16, 24, 0);
        btn.setLayoutParams(p);
        btn.setOnClickListener(v -> {
            if (setupOverlayView.getVisibility() == View.VISIBLE) {
                setupOverlayView.setVisibility(View.GONE);
            } else {
                refreshInstalledGamesStatus();
                setupOverlayView.setVisibility(View.VISIBLE);
            }
        });
        return btn;
    }

    private GradientDrawable createButtonDrawable(String hexColor) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor(hexColor));
        gd.setCornerRadius(dp(10));
        return gd;
    }

    private GradientDrawable createCardDrawable(String bgHex, String borderHex) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor(bgHex));
        gd.setCornerRadius(dp(14));
        gd.setStroke(dp(1), Color.parseColor(borderHex));
        return gd;
    }

    private void refreshInstalledGamesStatus() {
        File extDir = getExternalFilesDir(null);
        if (extDir == null) return;

        new Thread(() -> {
            List<GameStorageVerifier.VerificationResult> games = GameStorageVerifier.detectInstalledGames(extDir);
            boolean hasOC1 = false;
            boolean hasOC2 = false;
            boolean hasMO = false;
            boolean hasMO2 = false;

            for (GameStorageVerifier.VerificationResult g : games) {
                if (g.gameTitle == GameStorageVerifier.GameTitle.OVERCOOKED_1) hasOC1 = true;
                if (g.gameTitle == GameStorageVerifier.GameTitle.OVERCOOKED_2) hasOC2 = true;
                if (g.gameTitle == GameStorageVerifier.GameTitle.MOVING_OUT) hasMO = true;
                if (g.gameTitle == GameStorageVerifier.GameTitle.MOVING_OUT_2) hasMO2 = true;
            }

            final boolean fHasOC1 = hasOC1;
            final boolean fHasOC2 = hasOC2;
            final boolean fHasMO = hasMO;
            final boolean fHasMO2 = hasMO2;

            runOnUiThread(() -> {
                if (oc1StatusText != null) {
                    if (fHasOC1) {
                        oc1StatusText.setText("DATA FOUND  •  NOT EXECUTABLE");
                        oc1StatusText.setTextColor(Color.parseColor("#2EC4B6"));
                    } else {
                        oc1StatusText.setText("NOT INSTALLED  •  IMPORT DATA");
                        oc1StatusText.setTextColor(Color.parseColor("#E71D36"));
                    }
                }

                if (oc2StatusText != null) {
                    if (fHasOC2) {
                        oc2StatusText.setText("DATA FOUND  •  RUNTIME REQUIRED");
                        oc2StatusText.setTextColor(Color.parseColor("#2EC4B6"));
                    } else {
                        oc2StatusText.setText("NOT INSTALLED  •  IMPORT DATA");
                        oc2StatusText.setTextColor(Color.parseColor("#E71D36"));
                    }
                }

                if (moStatusText != null) {
                    if (fHasMO) {
                        moStatusText.setText("DATA DETECTED  •  RUNTIME UNVERIFIED");
                        moStatusText.setTextColor(Color.parseColor("#06D6A0"));
                    } else {
                        moStatusText.setText("NOT INSTALLED  •  IMPORT DATA");
                        moStatusText.setTextColor(Color.parseColor("#E71D36"));
                    }
                }

                if (mo2StatusText != null) {
                    if (fHasMO2) {
                        mo2StatusText.setText("DATA DETECTED  •  RUNTIME UNVERIFIED");
                        mo2StatusText.setTextColor(Color.parseColor("#06D6A0"));
                    } else {
                        mo2StatusText.setText("NOT INSTALLED  •  IMPORT DATA");
                        mo2StatusText.setTextColor(Color.parseColor("#E71D36"));
                    }
                }

            });
        }).start();
    }

    private void handleImportZipUri(Uri uri) {
        File extDir = getExternalFilesDir(null);
        if (extDir == null) return;

        File targetDir = new File(extDir, pendingImportTitle.name() + "_PC");
        showImportProgress(true, "Mengekstrak berkas game " + pendingImportTitle.displayName + "...");

        GameFileImporter.extractZipUri(this, uri, targetDir, new GameFileImporter.ProgressListener() {
            @Override
            public void onProgress(int percent, String currentFileName) {
                runOnUiThread(() -> {
                    if (importProgressText != null) {
                        importProgressText.setText("Mengekstrak: " + currentFileName);
                    }
                });
            }

            @Override
            public void onComplete(boolean success, String message) {
                runOnUiThread(() -> {
                    showImportProgress(false, "");
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                    refreshInstalledGamesStatus();
                });
            }
        });
    }

    private void handleImportFolderUri(Uri uri) {
        File extDir = getExternalFilesDir(null);
        if (extDir == null) return;

        File targetDir = new File(extDir, pendingImportTitle.defaultFolderName);
        showImportProgress(true, "Menyalin folder game " + pendingImportTitle.displayName + "...");

        GameFileImporter.copyFolderUri(this, uri, targetDir, new GameFileImporter.ProgressListener() {
            @Override
            public void onProgress(int percent, String currentFileName) {
                runOnUiThread(() -> {
                    if (importProgressText != null) {
                        importProgressText.setText("Menyalin: " + currentFileName);
                    }
                });
            }

            @Override
            public void onComplete(boolean success, String message) {
                runOnUiThread(() -> {
                    showImportProgress(false, "");
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                    refreshInstalledGamesStatus();
                });
            }
        });
    }

    private void scanDownloadFolder() {
        List<File> zips = GameFileImporter.scanDownloadFolderForGameZips();
        if (zips.isEmpty()) {
            Toast.makeText(this, "Tidak ada file ZIP Overcooked ditemukan di folder Download.", Toast.LENGTH_LONG).show();
            return;
        }

        String[] zipNames = new String[zips.size()];
        for (int i = 0; i < zips.size(); i++) {
            zipNames[i] = zips.get(i).getName() + " (" + (zips.get(i).length() / (1024 * 1024)) + " MB)";
        }

        new AlertDialog.Builder(this)
                .setTitle("Pilih File ZIP dari Folder Download")
                .setItems(zipNames, (dialog, which) -> {
                    File selected = zips.get(which);
                    String selName = selected.getName().toLowerCase();
                    GameStorageVerifier.GameTitle title;
                    if (selName.contains("movingout2") || selName.contains("moving_out_2") || (selName.contains("moving") && selName.contains("2"))) {
                        title = GameStorageVerifier.GameTitle.MOVING_OUT_2;
                    } else if (selName.contains("moving")) {
                        title = GameStorageVerifier.GameTitle.MOVING_OUT;
                    } else if (selName.contains("2")) {
                        title = GameStorageVerifier.GameTitle.OVERCOOKED_2;
                    } else {
                        title = GameStorageVerifier.GameTitle.OVERCOOKED_1;
                    }
                    pendingImportTitle = title;
                    handleImportZipUri(Uri.fromFile(selected));
                })
                .show();
    }

    private void showImportProgress(boolean show, String text) {
        if (importProgressBar != null) importProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (importProgressText != null) {
            importProgressText.setVisibility(show ? View.VISIBLE : View.GONE);
            importProgressText.setText(text);
        }
    }

    private void showExecutionRequirements(GameStorageVerifier.VerificationResult game) {
        String message = "Selected: " + game.gameTitle.displayName
                + "\nData folder: " + game.dataDir.getAbsolutePath()
                + "\nDetected backend: " + game.backendType
                + "\n\nCurrent status: DATA DETECTED, GAME NOT EXECUTABLE."
                + "\n\nWhy Play is unavailable:"
                + "\n• UnityPlayer.dll and game assets are title/build-specific and must come from the user's legally obtained PC installation."
                + "\n• Mono CIL validation is only a PE-header check; it does not host or execute managed Unity code."
                + "\n• IL2CPP metadata parsing is not native recompilation."
                + "\n• Wine/Box64 currently provides runtime provisioning and a headless version probe only; PE game execution is not yet verified."
                + "\n• DXVK DLL files alone do not provide an Android Vulkan driver, X11/Wayland display server, or presentation path."
                + "\n• Audio, touch, keyboard, mouse, gamepad, and process lifecycle bridges are not integrated."
                + "\n\nNext integration target: verify Wine startup on device, then integrate a display/Vulkan backend and validate a minimal Direct3D test before enabling game launch.";
        new AlertDialog.Builder(this)
                .setTitle("Execution Readiness")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showGraphicsSettingsDialog() {
        if (!nativeLoaded) {
            Toast.makeText(this, "Pustaka native belum dimuat", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] options = {
                "Preset: Rendah (Low - SD845 / 30 FPS)",
                "Preset: Sedang (Medium - Seimbang)",
                "Preset: Tinggi (High - Layar Asli HP / 60 FPS)",
                "Preset: Ultra (Flagship MSAA 4x)",
                "Mode Resolusi Layar...",
                "Ringkasan Profil Grafik"
        };

        new AlertDialog.Builder(this)
                .setTitle("Pengaturan Grafik & Resolusi HP")
                .setItems(options, (dialog, which) -> {
                    try {
                        switch (which) {
                            case 0:
                                nativeSetGraphicsPreset(0);
                                Toast.makeText(this, "Preset Rendah diterapkan", Toast.LENGTH_SHORT).show();
                                break;
                            case 1:
                                nativeSetGraphicsPreset(1);
                                Toast.makeText(this, "Preset Sedang diterapkan", Toast.LENGTH_SHORT).show();
                                break;
                            case 2:
                                nativeSetGraphicsPreset(2);
                                Toast.makeText(this, "Preset Tinggi diterapkan", Toast.LENGTH_SHORT).show();
                                break;
                            case 3:
                                nativeSetGraphicsPreset(3);
                                Toast.makeText(this, "Preset Ultra diterapkan", Toast.LENGTH_SHORT).show();
                                break;
                            case 4:
                                showResolutionModeDialog();
                                break;
                            case 5:
                                new AlertDialog.Builder(this)
                                        .setTitle("Profil Grafik Aktif")
                                        .setMessage(nativeGetGraphicsSummary())
                                        .setPositiveButton("OK", null)
                                        .show();
                                break;
                        }
                    } catch (Throwable t) {
                        Log.e(TAG, "Gagal mengubah pengaturan grafik: " + t.getMessage());
                    }
                })
                .show();
    }

    private void showResolutionModeDialog() {
        String[] modes = {
                "Resolusi Asli Layar HP (100% Native)",
                "Adaptif (Otomatis sesuai suhu & termal Snapdragon)",
                "Skala 1080p (FHD)",
                "Skala 720p (Hemat Daya)",
                "Skala 540p (Performa Tinggi)"
        };

        new AlertDialog.Builder(this)
                .setTitle("Pilih Mode Resolusi Layar")
                .setItems(modes, (dialog, which) -> {
                    try {
                        nativeSetResolutionMode(which, 1.0f);
                        Toast.makeText(this, "Mode resolusi diubah", Toast.LENGTH_SHORT).show();
                    } catch (Throwable t) {
                        Log.e(TAG, "Gagal mengubah resolusi: " + t.getMessage());
                    }
                })
                .show();
    }

    private void detectAndApplyDeviceDisplaySettings() {
        int width = 1920;
        int height = 1080;
        float dpi = 320.0f;
        float refreshRate = 60.0f;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowMetrics wm = getWindowManager().getCurrentWindowMetrics();
                Rect bounds = wm.getBounds();
                width = bounds.width();
                height = bounds.height();
                Display display = getDisplay();
                if (display != null) {
                    refreshRate = display.getRefreshRate();
                }
                dpi = getResources().getDisplayMetrics().densityDpi;
            } else {
                DisplayMetrics metrics = new DisplayMetrics();
                Display display = getWindowManager().getDefaultDisplay();
                display.getRealMetrics(metrics);
                width = metrics.widthPixels;
                height = metrics.heightPixels;
                dpi = metrics.densityDpi;
                refreshRate = display.getRefreshRate();
            }
        } catch (Throwable t) {
            Log.w(TAG, "Gagal membaca display metrics secara penuh: " + t.getMessage());
            DisplayMetrics dm = getResources().getDisplayMetrics();
            width = dm.widthPixels;
            height = dm.heightPixels;
            dpi = dm.densityDpi;
        }

        ActivityManager actManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        long totalRamMb = 4096;
        if (actManager != null) {
            actManager.getMemoryInfo(memInfo);
            totalRamMb = memInfo.totalMem / (1024 * 1024);
        }

        if (nativeLoaded) {
            try {
                nativeInitDisplayInfo(width, height, dpi, refreshRate, (int) totalRamMb);
            } catch (Throwable t) {
                Log.e(TAG, "Gagal memanggil nativeInitDisplayInfo: " + t.getMessage());
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (engineInitialized && nativeLoaded) {
            try { nativePauseEngine(); } catch (Throwable ignored) {}
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!engineInitialized && setupOverlayView != null) {
            setupOverlayView.setVisibility(View.VISIBLE);
            if (virtualOverlay != null) virtualOverlay.setVisibility(View.GONE);
        }
        if (engineInitialized && nativeLoaded) {
            try { nativeResumeEngine(); } catch (Throwable ignored) {}
        }
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        if (nativeLoaded) {
            DiagnosticLog.info(this, "SURFACE", "Surface created: " + holder.getSurfaceFrame());
            try { nativeOnSurfaceCreated(holder.getSurface()); } catch (Throwable t) {
                Log.e(TAG, "nativeOnSurfaceCreated error: " + t.getMessage());
            }
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        if (nativeLoaded) {
            DiagnosticLog.info(this, "SURFACE", "Surface changed: " + width + "x" + height);
            try { nativeOnSurfaceChanged(width, height); } catch (Throwable ignored) {}
        }
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        if (nativeLoaded) {
            DiagnosticLog.info(this, "SURFACE", "Surface destroyed.");
            try { nativeOnSurfaceDestroyed(); } catch (Throwable ignored) {}
        }
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        if (gamepadManager != null && (event.getSource() & (InputDevice.SOURCE_JOYSTICK | InputDevice.SOURCE_GAMEPAD)) != 0
                && event.getAction() == MotionEvent.ACTION_MOVE) {
            gamepadManager.processMotionEvent(event);
            return true;
        }
        return super.dispatchGenericMotionEvent(event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (setupOverlayView != null && setupOverlayView.getVisibility() != View.VISIBLE) {
                setupOverlayView.setVisibility(View.VISIBLE);
                if (virtualOverlay != null) virtualOverlay.setVisibility(View.GONE);
                refreshInstalledGamesStatus();
                return true;
            }
            return super.dispatchKeyEvent(event);
        }
        if (keyCode == KeyEvent.KEYCODE_HOME) {
            return super.dispatchKeyEvent(event);
        }

        if (setupOverlayView != null && setupOverlayView.getVisibility() == View.VISIBLE
                && (event.getSource() & (InputDevice.SOURCE_GAMEPAD | InputDevice.SOURCE_DPAD)) != 0) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                        || keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    return super.dispatchKeyEvent(event);
                }
                if (keyCode == KeyEvent.KEYCODE_BUTTON_A || keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                        || keyCode == KeyEvent.KEYCODE_ENTER) {
                    View focused = getCurrentFocus();
                    if (focused != null && focused.isFocusable()) {
                        focused.performClick();
                        return true;
                    }
                }
            }
        }
        if ((event.getSource() & (InputDevice.SOURCE_GAMEPAD | InputDevice.SOURCE_JOYSTICK | InputDevice.SOURCE_DPAD)) != 0) {
            if (gamepadManager != null) {
                gamepadManager.processKeyEvent(event);
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    public int getJavaThermalStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                try {
                    return pm.getCurrentThermalStatus();
                } catch (Throwable ignored) {}
            }
        }
        return 0;
    }

    @Override
    protected void onDestroy() {
        if (gamepadManager != null) {
            gamepadManager.unregister();
        }
        if (engineInitialized && nativeLoaded) {
            try { nativeShutdownEngine(); } catch (Throwable ignored) {}
        }
        super.onDestroy();
    }

    // Native JNI functions
    private native void nativeInitEngine(String dataDirPath, int backendOrdinal, int gameVersion);
    private native void nativeInitDisplayInfo(int width, int height, float dpi, float refreshRate, int ramMb);
    private native void nativeSetGraphicsPreset(int presetOrdinal);
    private native void nativeSetResolutionMode(int modeOrdinal, float customScale);
    private native void nativeSetTargetFPS(int fps);
    private native void nativeSetVsync(boolean enabled);
    private native void nativeSetAntiAliasing(int modeOrdinal);
    private native void nativeSetShadowQuality(int qualityOrdinal);
    private native void nativeSetTextureQuality(int qualityOrdinal);
    private native String nativeGetGraphicsSummary();
    private native String nativeGetVulkanDiagnostics();
    private native void nativePauseEngine();
    private native void nativeResumeEngine();
    private native void nativeOnSurfaceCreated(Surface surface);
    private native void nativeOnSurfaceChanged(int width, int height);
    private native void nativeOnSurfaceDestroyed();
    private native void nativeOnGamepadAxis(int deviceId, float lx, float ly, float rx, float ry, float hatX, float hatY);
    private native void nativeOnGamepadButton(int deviceId, int keyCode, boolean pressed);
    private native void nativeShutdownEngine();
}
