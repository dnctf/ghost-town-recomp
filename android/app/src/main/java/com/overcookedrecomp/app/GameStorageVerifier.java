package com.overcookedrecomp.app;

import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public final class GameStorageVerifier {
    private static final String TAG = "GameStorageVerifier";

    public enum GameTitle {
        UNKNOWN(0, "Unknown Game", ""),
        OVERCOOKED_1(1, "Overcooked 1", "Overcooked_Data"),
        OVERCOOKED_2(2, "Overcooked 2", "Overcooked2_Data");

        public final int id;
        public final String displayName;
        public final String defaultFolderName;

        GameTitle(int id, String displayName, String defaultFolderName) {
            this.id = id;
            this.displayName = displayName;
            this.defaultFolderName = defaultFolderName;
        }
    }

    public enum RuntimeBackendType {
        UNKNOWN,
        MONO_CIL,
        IL2CPP_AOT
    }

    public static final class VerificationResult {
        public final boolean valid;
        public final GameTitle gameTitle;
        public final RuntimeBackendType backendType;
        public final String message;
        public final String primaryBinarySha256;
        public final File dataDir;

        public VerificationResult(boolean valid, GameTitle gameTitle, RuntimeBackendType backendType,
                                  String message, String sha256, File dataDir) {
            this.valid = valid;
            this.gameTitle = gameTitle;
            this.backendType = backendType;
            this.message = message;
            this.primaryBinarySha256 = sha256;
            this.dataDir = dataDir;
        }

        @Override
        public String toString() {
            return String.format("[%s] Valid: %b | Backend: %s | Pesan: %s",
                    gameTitle.displayName, valid, backendType, message);
        }
    }

    /**
     * Memindai direktori penyimpanan untuk mendeteksi instalasi Overcooked 1 dan Overcooked 2.
     */
    public static List<VerificationResult> detectInstalledGames(File baseDir) {
        List<VerificationResult> results = new ArrayList<>();
        if (baseDir == null || !baseDir.exists()) {
            return results;
        }

        // Cek Overcooked 1 (Overcooked_Data)
        File oc1Dir = new File(baseDir, GameTitle.OVERCOOKED_1.defaultFolderName);
        if (oc1Dir.exists() && oc1Dir.isDirectory()) {
            VerificationResult res = verifyDataDir(oc1Dir, GameTitle.OVERCOOKED_1);
            if (res.valid) results.add(res);
        }

        // Cek Overcooked 2 (Overcooked2_Data)
        File oc2Dir = new File(baseDir, GameTitle.OVERCOOKED_2.defaultFolderName);
        if (oc2Dir.exists() && oc2Dir.isDirectory()) {
            VerificationResult res = verifyDataDir(oc2Dir, GameTitle.OVERCOOKED_2);
            if (res.valid) results.add(res);
        }

        // Cek alternatif huruf kecil jika ada
        if (results.isEmpty()) {
            File oc1Lower = new File(baseDir, "overcooked_data");
            if (oc1Lower.exists()) {
                VerificationResult res = verifyDataDir(oc1Lower, GameTitle.OVERCOOKED_1);
                if (res.valid) results.add(res);
            }
            File oc2Lower = new File(baseDir, "overcooked2_data");
            if (oc2Lower.exists()) {
                VerificationResult res = verifyDataDir(oc2Lower, GameTitle.OVERCOOKED_2);
                if (res.valid) results.add(res);
            }
        }

        return results;
    }

    /**
     * Memvalidasi direktori data game untuk Overcooked 1 atau Overcooked 2.
     */
    public static VerificationResult verifyDataDir(File dataDir, GameTitle hint) {
        if (dataDir == null || !dataDir.exists() || !dataDir.isDirectory()) {
            return new VerificationResult(false, hint, RuntimeBackendType.UNKNOWN,
                    "Direktori data game tidak ditemukan.", "", dataDir);
        }

        GameTitle resolvedTitle = hint;
        String dirNameLower = dataDir.getName().toLowerCase();
        if (dirNameLower.contains("overcooked2") || dirNameLower.contains("overcooked_2")) {
            resolvedTitle = GameTitle.OVERCOOKED_2;
        } else if (dirNameLower.contains("overcooked")) {
            resolvedTitle = (hint == GameTitle.OVERCOOKED_2) ? GameTitle.OVERCOOKED_2 : GameTitle.OVERCOOKED_1;
        }

        File globalGameManagers = new File(dataDir, "globalgamemanagers");
        File dataUnity3d = new File(dataDir, "data.unity3d");
        File level0 = new File(dataDir, "level0");

        if (!globalGameManagers.exists() && !dataUnity3d.exists() && !level0.exists()) {
            // Cek apakah berkas berada di dalam subdirektori (misal: ekstraksi ZIP membuat folder pembungkus)
            File sub1 = new File(dataDir, "Overcooked_Data");
            if (sub1.exists() && sub1.isDirectory()) {
                return verifyDataDir(sub1, GameTitle.OVERCOOKED_1);
            }
            File sub2 = new File(dataDir, "Overcooked2_Data");
            if (sub2.exists() && sub2.isDirectory()) {
                return verifyDataDir(sub2, GameTitle.OVERCOOKED_2);
            }
            File[] subs = dataDir.listFiles(File::isDirectory);
            if (subs != null) {
                for (File s : subs) {
                    if (new File(s, "globalgamemanagers").exists() || new File(s, "data.unity3d").exists() || new File(s, "level0").exists()) {
                        return verifyDataDir(s, s.getName().toLowerCase().contains("2") ? GameTitle.OVERCOOKED_2 : hint);
                    }
                }
            }
            return new VerificationResult(false, resolvedTitle, RuntimeBackendType.UNKNOWN,
                    "Berkas inti Unity (globalgamemanagers/data.unity3d) tidak ditemukan di " + dataDir.getName(), "", dataDir);
        }

        // Cek C# bytecode (Assembly-CSharp.dll)
        File monoAssembly = new File(dataDir, "Managed/Assembly-CSharp.dll");
        if (monoAssembly.exists()) {
            String hash = computeSha256(monoAssembly);
            return new VerificationResult(true, resolvedTitle, RuntimeBackendType.MONO_CIL,
                    String.format("Terdeteksi %s Unity Mono CIL. Siap menjalankan Assembly-CSharp.dll.", resolvedTitle.displayName),
                    hash, dataDir);
        }

        // Cek IL2CPP metadata
        File il2cppMetadata = new File(dataDir, "il2cpp_data/Metadata/global-metadata.dat");
        if (il2cppMetadata.exists()) {
            String hash = computeSha256(il2cppMetadata);
            return new VerificationResult(true, resolvedTitle, RuntimeBackendType.IL2CPP_AOT,
                    String.format("Terdeteksi %s Unity IL2CPP AOT. Siap mem-parsing global-metadata.dat.", resolvedTitle.displayName),
                    hash, dataDir);
        }

        return new VerificationResult(false, resolvedTitle, RuntimeBackendType.UNKNOWN,
                "Tidak ditemukan Assembly-CSharp.dll maupun global-metadata.dat di " + dataDir.getName(), "", dataDir);
    }

    /**
     * Kompatibilitas mundur dengan pemanggilan Overcooked 1 tunggal
     */
    public static VerificationResult verifyOvercookedDataDir(File dataDir) {
        return verifyDataDir(dataDir, GameTitle.OVERCOOKED_1);
    }

    private static String computeSha256(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : digest.digest()) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (IOException | NoSuchAlgorithmException e) {
            Log.e(TAG, "Gagal menghitung SHA256 berkas: " + file.getAbsolutePath(), e);
            return "ERROR_HASH";
        }
    }
}
