package com.overcookedrecomp.app;

import android.app.ActivityManager;
import android.content.Context;
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
import com.overcookedrecomp.app.input.GamepadManager;
import com.overcookedrecomp.app.input.VirtualGamepadOverlay;
import java.io.File;
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

        // Refresh status game yang terpasang
        refreshInstalledGamesStatus();
    }

    private View createSetupOverlay() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        scrollView.setBackgroundColor(Color.parseColor("#E6121418"));
        scrollView.setFillViewport(true);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(48, 36, 48, 48);
        container.setGravity(Gravity.CENTER_HORIZONTAL);

        // Header Title
        TextView titleView = new TextView(this);
        titleView.setText("GHOST TOWN • GAME LIBRARY");
        titleView.setTextSize(24);
        titleView.setTextColor(Color.parseColor("#FF9F1C"));
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setGravity(Gravity.CENTER);
        container.addView(titleView);

        TextView subTitle = new TextView(this);
        subTitle.setText("GAME LIBRARY • PC DATA IMPORT • ANDROID RUNTIME STATUS");
        subTitle.setTextSize(13);
        subTitle.setTextColor(Color.parseColor("#CCCCCC"));
        subTitle.setGravity(Gravity.CENTER);
        subTitle.setPadding(0, 8, 0, 24);
        container.addView(subTitle);

        // Developer Switcher & Icon Bar
        LinearLayout devBar = new LinearLayout(this);
        devBar.setOrientation(LinearLayout.HORIZONTAL);
        devBar.setGravity(Gravity.CENTER);
        devBar.setPadding(0, 0, 0, 20);

        // Badge Ghost Town Games / Team17
        LinearLayout gtgBadge = new LinearLayout(this);
        gtgBadge.setOrientation(LinearLayout.HORIZONTAL);
        gtgBadge.setGravity(Gravity.CENTER_VERTICAL);
        gtgBadge.setBackground(createCardDrawable("#1E2229", "#FF9F1C"));
        gtgBadge.setPadding(18, 10, 22, 10);
        ImageView gtgIcon = new ImageView(this);
        gtgIcon.setImageResource(R.drawable.ic_overcooked);
        gtgIcon.setLayoutParams(new LinearLayout.LayoutParams(40, 40));
        gtgBadge.addView(gtgIcon);
        ImageView moDevIcon = new ImageView(this);
        moDevIcon.setImageResource(R.drawable.ic_moving_out);
        LinearLayout.LayoutParams moIconParams = new LinearLayout.LayoutParams(40, 40);
        moIconParams.setMarginStart(10);
        moDevIcon.setLayoutParams(moIconParams);
        gtgBadge.addView(moDevIcon);
        TextView gtgText = new TextView(this);
        gtgText.setText(" Ghost Town Games / Team17");
        gtgText.setTextSize(12);
        gtgText.setTextColor(Color.parseColor("#FF9F1C"));
        gtgText.setTypeface(null, Typeface.BOLD);
        gtgBadge.addView(gtgText);
        devBar.addView(gtgBadge);

        container.addView(devBar);

        // Kartu Overcooked 1
        container.addView(createGameCard(
                GameStorageVerifier.GameTitle.OVERCOOKED_1,
                "Overcooked 1 (PC)",
                "Impor folder instalasi PC lengkap: .exe, DLL, dan Overcooked_Data",
                text -> oc1StatusText = text
        ));

        View spacer1 = new View(this);
        spacer1.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 20));
        container.addView(spacer1);

        // Kartu Overcooked 2
        container.addView(createGameCard(
                GameStorageVerifier.GameTitle.OVERCOOKED_2,
                "Overcooked 2 (PC)",
                "Impor folder instalasi PC lengkap: .exe, DLL, dan Overcooked2_Data",
                text -> oc2StatusText = text
        ));

        View spacer2 = new View(this);
        spacer2.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 20));
        container.addView(spacer2);

        // Kartu Moving Out
        container.addView(createGameCard(
                GameStorageVerifier.GameTitle.MOVING_OUT,
                "Moving Out (PC)",
                "Impor folder instalasi PC lengkap: .exe, DLL, dan MovingOut_Data",
                text -> moStatusText = text
        ));

        View spacer3 = new View(this);
        spacer3.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 20));
        container.addView(spacer3);

        // Kartu Moving Out 2
        container.addView(createGameCard(
                GameStorageVerifier.GameTitle.MOVING_OUT_2,
                "Moving Out 2 (PC)",
                "Impor folder instalasi PC lengkap: .exe, DLL, dan MovingOut2_Data",
                text -> mo2StatusText = text
        ));

        // Tombol Toggle Virtual On-Screen Controls
        Button toggleVirtualBtn = new Button(this);
        toggleVirtualBtn.setText(virtualControlsEnabled ? "🎮 Kontrol Virtual Layar Sentuh: AKTIF" : "🎮 Kontrol Virtual Layar Sentuh: NONAKTIF");
        toggleVirtualBtn.setTextSize(13);
        toggleVirtualBtn.setTextColor(Color.WHITE);
        toggleVirtualBtn.setBackground(createButtonDrawable("#00A8E8"));
        LinearLayout.LayoutParams vParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        vParams.setMargins(0, 20, 0, 0);
        toggleVirtualBtn.setLayoutParams(vParams);
        toggleVirtualBtn.setOnClickListener(v -> {
            virtualControlsEnabled = !virtualControlsEnabled;
            if (virtualOverlay != null) {
                virtualOverlay.setVisibility(virtualControlsEnabled ? View.VISIBLE : View.GONE);
            }
            toggleVirtualBtn.setText(virtualControlsEnabled ? "🎮 Kontrol Virtual Layar Sentuh: AKTIF" : "🎮 Kontrol Virtual Layar Sentuh: NONAKTIF");
        });
        container.addView(toggleVirtualBtn);

        // Tombol Auto-Scan dari folder Download
        Button autoScanBtn = new Button(this);
        autoScanBtn.setText("⚡ Deteksi Otomatis dari Folder Download");
        autoScanBtn.setTextSize(14);
        autoScanBtn.setTextColor(Color.WHITE);
        autoScanBtn.setBackground(createButtonDrawable("#2EC4B6"));
        LinearLayout.LayoutParams scanParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        scanParams.setMargins(0, 24, 0, 16);
        autoScanBtn.setLayoutParams(scanParams);
        autoScanBtn.setOnClickListener(v -> scanDownloadFolder());
        container.addView(autoScanBtn);

        // Progress Impor
        importProgressText = new TextView(this);
        importProgressText.setText("");
        importProgressText.setTextSize(12);
        importProgressText.setTextColor(Color.parseColor("#E71D36"));
        importProgressText.setGravity(Gravity.CENTER);
        importProgressText.setVisibility(View.GONE);
        container.addView(importProgressText);

        importProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        importProgressBar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        importProgressBar.setIndeterminate(true);
        importProgressBar.setVisibility(View.GONE);
        container.addView(importProgressBar);

        // Tombol Pengaturan Grafik & Resolusi
        Button gfxBtn = new Button(this);
        gfxBtn.setText("⚙️ Pengaturan Grafik & Resolusi Layar");
        gfxBtn.setTextSize(13);
        gfxBtn.setTextColor(Color.WHITE);
        gfxBtn.setBackground(createButtonDrawable("#495057"));
        gfxBtn.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        gfxBtn.setOnClickListener(v -> showGraphicsSettingsDialog());
        container.addView(gfxBtn);

        scrollView.addView(container);
        return scrollView;
    }

    private interface StatusTextHolder {
        void hold(TextView tv);
    }

    private View createGameCard(GameStorageVerifier.GameTitle title, String cardTitle, String desc, StatusTextHolder holder) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(createCardDrawable("#151A23", "#394354"));
        card.setPadding(24, 20, 24, 20);

        ImageView gameIcon = new ImageView(this);
        gameIcon.setImageResource((title == GameStorageVerifier.GameTitle.OVERCOOKED_1
                || title == GameStorageVerifier.GameTitle.OVERCOOKED_2)
                ? R.drawable.ic_overcooked : R.drawable.ic_moving_out);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(64, 64);
        iconParams.gravity = Gravity.CENTER_HORIZONTAL;
        iconParams.bottomMargin = 12;
        gameIcon.setLayoutParams(iconParams);
        gameIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        card.addView(gameIcon);

        TextView name = new TextView(this);
        name.setText(cardTitle);
        name.setTextSize(16);
        name.setTextColor(Color.parseColor("#FFFFFF"));
        name.setTypeface(null, Typeface.BOLD);
        card.addView(name);

        TextView descView = new TextView(this);
        descView.setText(desc);
        descView.setTextSize(12);
        descView.setTextColor(Color.parseColor("#ADB5BD"));
        descView.setPadding(0, 4, 0, 8);
        card.addView(descView);

        TextView status = new TextView(this);
        status.setText("Status: Memeriksa...");
        status.setTextSize(13);
        status.setTypeface(null, Typeface.BOLD);
        card.addView(status);
        holder.hold(status);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, 12, 0, 0);

        Button zipBtn = new Button(this);
        zipBtn.setText("📁 Impor .ZIP PC");
        zipBtn.setTextSize(12);
        zipBtn.setTextColor(Color.WHITE);
        zipBtn.setBackground(createButtonDrawable("#011627"));
        LinearLayout.LayoutParams btnP1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        btnP1.setMarginEnd(8);
        zipBtn.setLayoutParams(btnP1);
        zipBtn.setOnClickListener(v -> {
            pendingImportTitle = title;
            openZipLauncher.launch(new String[]{"application/zip", "application/x-zip-compressed", "application/octet-stream", "*/*"});
        });
        btnRow.addView(zipBtn);

        Button folderBtn = new Button(this);
        folderBtn.setText("📂 Pilih Folder");
        folderBtn.setTextSize(12);
        folderBtn.setTextColor(Color.WHITE);
        folderBtn.setBackground(createButtonDrawable("#011627"));
        folderBtn.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        folderBtn.setOnClickListener(v -> {
            pendingImportTitle = title;
            openFolderLauncher.launch(null);
        });
        btnRow.addView(folderBtn);

        card.addView(btnRow);

        Button runtimeBtn = new Button(this);
        runtimeBtn.setText("▶ Periksa & Pilih Game");
        runtimeBtn.setTextSize(13);
        runtimeBtn.setTextColor(Color.WHITE);
        runtimeBtn.setBackground(createButtonDrawable("#245B86"));
        LinearLayout.LayoutParams runtimeParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        runtimeParams.topMargin = 10;
        runtimeBtn.setLayoutParams(runtimeParams);
        runtimeBtn.setOnClickListener(v -> inspectGameRuntime(title));
        card.addView(runtimeBtn);
        return card;
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
        gd.setCornerRadius(14);
        return gd;
    }

    private GradientDrawable createCardDrawable(String bgHex, String borderHex) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor(bgHex));
        gd.setCornerRadius(18);
        gd.setStroke(2, Color.parseColor(borderHex));
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
                        oc1StatusText.setText("Status: DATA TERDETEKSI • RUNTIME BELUM TERSEDIA");
                        oc1StatusText.setTextColor(Color.parseColor("#2EC4B6"));
                    } else {
                        oc1StatusText.setText("Status: ❌ Belum Terpasang (Butuh Overcooked_Data)");
                        oc1StatusText.setTextColor(Color.parseColor("#E71D36"));
                    }
                }

                if (oc2StatusText != null) {
                    if (fHasOC2) {
                        oc2StatusText.setText("Status: DATA TERDETEKSI • RUNTIME BELUM TERSEDIA");
                        oc2StatusText.setTextColor(Color.parseColor("#2EC4B6"));
                    } else {
                        oc2StatusText.setText("Status: ❌ Belum Terpasang (Butuh Overcooked2_Data)");
                        oc2StatusText.setTextColor(Color.parseColor("#E71D36"));
                    }
                }

                if (moStatusText != null) {
                    if (fHasMO) {
                        moStatusText.setText("Status: DATA TERDETEKSI • RUNTIME BELUM TERSEDIA");
                        moStatusText.setTextColor(Color.parseColor("#06D6A0"));
                    } else {
                        moStatusText.setText("Status: ❌ Belum Terpasang (Butuh MovingOut_Data)");
                        moStatusText.setTextColor(Color.parseColor("#E71D36"));
                    }
                }

                if (mo2StatusText != null) {
                    if (fHasMO2) {
                        mo2StatusText.setText("Status: DATA TERDETEKSI • RUNTIME BELUM TERSEDIA");
                        mo2StatusText.setTextColor(Color.parseColor("#06D6A0"));
                    } else {
                        mo2StatusText.setText("Status: ❌ Belum Terpasang (Butuh MovingOut2_Data)");
                        mo2StatusText.setTextColor(Color.parseColor("#E71D36"));
                    }
                }

            });
        }).start();
    }

    private void handleImportZipUri(Uri uri) {
        File extDir = getExternalFilesDir(null);
        if (extDir == null) return;

        File targetDir = new File(extDir, pendingImportTitle.defaultFolderName);
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

    /**
     * Per-game action: verifies that the selected PC data is present, but does not
     * claim that a Windows executable or Unity player can run on Android yet.
     */
    private void inspectGameRuntime(GameStorageVerifier.GameTitle title) {
        File extDir = getExternalFilesDir(null);
        if (extDir == null) {
            Toast.makeText(this, "Penyimpanan aplikasi tidak tersedia.", Toast.LENGTH_LONG).show();
            return;
        }

        // Import stores the selected complete installation under this per-game directory.
        File importedRoot = new File(extDir, title.defaultFolderName);
        if (!importedRoot.isDirectory()) {
            Toast.makeText(this, "Folder instalasi " + title.displayName + " belum diimpor.", Toast.LENGTH_LONG).show();
            return;
        }

        GameInstallInspector.Inspection inspection = GameInstallInspector.inspect(importedRoot, title);
        String runtimeStatus = "\\n\\nSTATUS EKSEKUSI: BELUM TERSEDIA. Build ini belum memuat Wine + Box64 atau renderer DirectX yang terintegrasi. Pemeriksaan ini hanya membaca struktur folder dan header PE; tidak menjalankan .exe.";
        String hardwareNote = "\\n\\nTarget pengujian: Android ARM64. Untuk kandidat x86-64, tahap berikutnya adalah validasi Wine/Box64 dan jalur grafis pada perangkat fisik.";
        new AlertDialog.Builder(this)
                .setTitle(title.displayName + " • Base Game Preflight")
                .setMessage(inspection.report + runtimeStatus + hardwareNote)
                .setPositiveButton("Mengerti", null)
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
        if (engineInitialized && nativeLoaded) {
            try { nativeResumeEngine(); } catch (Throwable ignored) {}
        }
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        if (nativeLoaded) {
            try { nativeOnSurfaceCreated(holder.getSurface()); } catch (Throwable t) {
                Log.e(TAG, "nativeOnSurfaceCreated error: " + t.getMessage());
            }
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        if (nativeLoaded) {
            try { nativeOnSurfaceChanged(width, height); } catch (Throwable ignored) {}
        }
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        if (nativeLoaded) {
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
                refreshInstalledGamesStatus();
                return true;
            }
            return super.dispatchKeyEvent(event);
        }
        if (keyCode == KeyEvent.KEYCODE_HOME) {
            return super.dispatchKeyEvent(event);
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
    private native void nativePauseEngine();
    private native void nativeResumeEngine();
    private native void nativeOnSurfaceCreated(Surface surface);
    private native void nativeOnSurfaceChanged(int width, int height);
    private native void nativeOnSurfaceDestroyed();
    private native void nativeOnGamepadAxis(int deviceId, float lx, float ly, float rx, float ry, float hatX, float hatY);
    private native void nativeOnGamepadButton(int deviceId, int keyCode, boolean pressed);
    private native void nativeShutdownEngine();
}
