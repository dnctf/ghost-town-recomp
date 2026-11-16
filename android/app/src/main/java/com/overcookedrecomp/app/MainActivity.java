package com.overcookedrecomp.app;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    private static final String TAG = "OvercookedRecomp";

    static {
        try {
            System.loadLibrary("overcooked_native");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Gagal memuat pustaka native overcooked_native", e);
        }
    }

    private SurfaceView gameSurfaceView;
    private boolean engineInitialized = false;
    private GameStorageVerifier.GameTitle activeGameTitle = GameStorageVerifier.GameTitle.UNKNOWN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Deteksi spesifikasi layar ponsel (resolusi, dpi, refresh rate) dan RAM
        detectAndApplyDeviceDisplaySettings();

        gameSurfaceView = new SurfaceView(this);
        gameSurfaceView.getHolder().addCallback(this);
        setContentView(gameSurfaceView);

        File extDir = getExternalFilesDir(null);
        if (extDir == null) {
            Log.e(TAG, "External storage tidak dapat diakses.");
            Toast.makeText(this, "External storage tidak dapat diakses", Toast.LENGTH_LONG).show();
            return;
        }

        // Deteksi otomatis apakah pengguna memiliki Overcooked 1, Overcooked 2, atau keduanya
        List<GameStorageVerifier.VerificationResult> detectedGames =
                GameStorageVerifier.detectInstalledGames(extDir);

        if (detectedGames.size() > 1) {
            // Kedua game terpasang: tampilkan dialog pemilih game
            showGameSelectionDialog(detectedGames);
        } else if (detectedGames.size() == 1) {
            // Satu game terpasang: langsung luncurkan game tersebut
            launchGame(detectedGames.get(0));
        } else {
            // Belum ada data game terdeteksi
            Log.w(TAG, "Tidak ditemukan Overcooked 1 maupun Overcooked 2 di " + extDir.getAbsolutePath());
            Toast.makeText(this, "Salin folder 'Overcooked_Data' (OC1) atau 'Overcooked2_Data' (OC2) ke folder aplikasi", Toast.LENGTH_LONG).show();
            File defaultDir = new File(extDir, "Overcooked_Data");
            nativeInitEngine(defaultDir.getAbsolutePath(), 0, GameStorageVerifier.GameTitle.OVERCOOKED_1.id);
            engineInitialized = true;
        }
    }

    private void showGameSelectionDialog(List<GameStorageVerifier.VerificationResult> games) {
        String[] titles = new String[games.size()];
        for (int i = 0; i < games.size(); i++) {
            titles[i] = games.get(i).gameTitle.displayName + " (" + games.get(i).dataDir.getName() + ")";
        }

        new AlertDialog.Builder(this)
                .setTitle("Pilih Game untuk Dimainkan")
                .setItems(titles, (dialog, which) -> {
                    launchGame(games.get(which));
                })
                .setCancelable(false)
                .show();
    }

    private void launchGame(GameStorageVerifier.VerificationResult game) {
        activeGameTitle = game.gameTitle;
        Log.i(TAG, "Meluncurkan " + game.gameTitle.displayName + " dari: " + game.dataDir.getAbsolutePath());
        nativeInitEngine(game.dataDir.getAbsolutePath(), game.backendType.ordinal(), game.gameTitle.id);
        engineInitialized = true;
        Toast.makeText(this, "Memuat " + game.gameTitle.displayName + "...", Toast.LENGTH_SHORT).show();
        Log.i(TAG, "Native Engine aktif.\n" + nativeGetGraphicsSummary());
    }

    private void detectAndApplyDeviceDisplaySettings() {
        DisplayMetrics metrics = new DisplayMetrics();
        Display display = getWindowManager().getDefaultDisplay();
        display.getRealMetrics(metrics);

        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        float dpi = metrics.densityDpi;
        float refreshRate = display.getRefreshRate();

        // Deteksi total RAM sistem
        ActivityManager actManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        long totalRamMb = 4096;
        if (actManager != null) {
            actManager.getMemoryInfo(memInfo);
            totalRamMb = memInfo.totalMem / (1024 * 1024);
        }

        Log.i(TAG, String.format("Layar HP terdeteksi: %dx%d @ %.1fHz, %.0f dpi, RAM %d MB",
                width, height, refreshRate, dpi, totalRamMb));

        nativeInitDisplayInfo(width, height, dpi, refreshRate, (int) totalRamMb);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (engineInitialized) {
            nativePauseEngine();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (engineInitialized) {
            nativeResumeEngine();
        }
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        nativeOnSurfaceCreated(holder.getSurface());
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        nativeOnSurfaceChanged(width, height);
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        nativeOnSurfaceDestroyed();
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        if ((event.getSource() & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
                && event.getAction() == MotionEvent.ACTION_MOVE) {
            nativeOnGamepadAxis(
                    event.getDeviceId(),
                    event.getAxisValue(MotionEvent.AXIS_X),
                    event.getAxisValue(MotionEvent.AXIS_Y),
                    event.getAxisValue(MotionEvent.AXIS_Z),
                    event.getAxisValue(MotionEvent.AXIS_RZ),
                    event.getAxisValue(MotionEvent.AXIS_HAT_X),
                    event.getAxisValue(MotionEvent.AXIS_HAT_Y)
            );
            return true;
        }
        return super.dispatchGenericMotionEvent(event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME) {
            return super.dispatchKeyEvent(event);
        }

        if ((event.getSource() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) {
            boolean pressed = event.getAction() == KeyEvent.ACTION_DOWN;
            nativeOnGamepadButton(event.getDeviceId(), keyCode, pressed);
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    public int getJavaThermalStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                return pm.getCurrentThermalStatus();
            }
        }
        return 0;
    }

    @Override
    protected void onDestroy() {
        if (engineInitialized) {
            nativeShutdownEngine();
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
