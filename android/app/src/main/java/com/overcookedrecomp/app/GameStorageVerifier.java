package com.overcookedrecomp.app;

import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class GameStorageVerifier {
    private static final String TAG = "GameStorageVerifier";

    public enum GameTitle {
        UNKNOWN(0, "Unknown Game", ""),
        OVERCOOKED_1(1, "Overcooked 1", "Overcooked_Data"),
        OVERCOOKED_2(2, "Overcooked 2", "Overcooked2_Data"),
        MOVING_OUT(3, "Moving Out", "MovingOut_Data"),
        MOVING_OUT_2(4, "Moving Out 2", "MovingOut2_Data");

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


    /** Parsed PE/COFF metadata for a user-supplied Windows executable. */
    public static final class ExecutableVerificationResult {
        public final boolean valid;
        public final boolean supportedByBox64;
        public final GameTitle gameTitle;
        public final String architecture;
        public final int machine;
        public final int optionalHeaderMagic;
        public final String message;
        public final String sha256;
        public final File executable;

        public ExecutableVerificationResult(boolean valid, boolean supportedByBox64, GameTitle gameTitle,
                                             String architecture, int machine, int optionalHeaderMagic,
                                             String message, String sha256, File executable) {
            this.valid = valid;
            this.supportedByBox64 = supportedByBox64;
            this.gameTitle = gameTitle;
            this.architecture = architecture;
            this.machine = machine;
            this.optionalHeaderMagic = optionalHeaderMagic;
            this.message = message;
            this.sha256 = sha256;
            this.executable = executable;
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
        File oc1Dir = importedRoot(baseDir, GameTitle.OVERCOOKED_1);
        if (oc1Dir.exists() && oc1Dir.isDirectory()) {
            VerificationResult res = verifyDataDir(oc1Dir, GameTitle.OVERCOOKED_1);
            if (res.valid) results.add(res);
        }

        // Cek Overcooked 2 (Overcooked2_Data)
        File oc2Dir = importedRoot(baseDir, GameTitle.OVERCOOKED_2);
        if (oc2Dir.exists() && oc2Dir.isDirectory()) {
            VerificationResult res = verifyDataDir(oc2Dir, GameTitle.OVERCOOKED_2);
            if (res.valid) results.add(res);
        }

        // Cek Moving Out (MovingOut_Data)
        File moDir = importedRoot(baseDir, GameTitle.MOVING_OUT);
        if (moDir.exists() && moDir.isDirectory()) {
            VerificationResult res = verifyDataDir(moDir, GameTitle.MOVING_OUT);
            if (res.valid) results.add(res);
        }

        // Cek Moving Out 2 (MovingOut2_Data)
        File mo2Dir = importedRoot(baseDir, GameTitle.MOVING_OUT_2);
        if (mo2Dir.exists() && mo2Dir.isDirectory()) {
            VerificationResult res = verifyDataDir(mo2Dir, GameTitle.MOVING_OUT_2);
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
            File moLower = new File(baseDir, "movingout_data");
            if (moLower.exists()) {
                VerificationResult res = verifyDataDir(moLower, GameTitle.MOVING_OUT);
                if (res.valid) results.add(res);
            }
            File mo2Lower = new File(baseDir, "movingout2_data");
            if (mo2Lower.exists()) {
                VerificationResult res = verifyDataDir(mo2Lower, GameTitle.MOVING_OUT_2);
                if (res.valid) results.add(res);
            }
        }

        return results;
    }

    /**
     * Memvalidasi direktori data game untuk Overcooked 1 atau Overcooked 2.
     */
    private static File importedRoot(File baseDir, GameTitle title) {
        File completeInstall = new File(baseDir, title.name() + "_PC");
        if (completeInstall.isDirectory()) return completeInstall;
        return new File(baseDir, title.defaultFolderName);
    }

    public static VerificationResult verifyDataDir(File dataDir, GameTitle hint) {
        if (dataDir == null || !dataDir.exists() || !dataDir.isDirectory()) {
            return new VerificationResult(false, hint, RuntimeBackendType.UNKNOWN,
                    "Direktori data game tidak ditemukan.", "", dataDir);
        }

        GameTitle resolvedTitle = hint;
        String dirNameLower = dataDir.getName().toLowerCase();
        if (dirNameLower.contains("movingout2") || dirNameLower.contains("moving_out_2")) {
            resolvedTitle = GameTitle.MOVING_OUT_2;
        } else if (dirNameLower.contains("movingout") || dirNameLower.contains("moving_out")) {
            resolvedTitle = GameTitle.MOVING_OUT;
        } else if (dirNameLower.contains("overcooked2") || dirNameLower.contains("overcooked_2")) {
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
            File sub3 = new File(dataDir, "MovingOut_Data");
            if (sub3.exists() && sub3.isDirectory()) {
                return verifyDataDir(sub3, GameTitle.MOVING_OUT);
            }
            File sub4 = new File(dataDir, "MovingOut2_Data");
            if (sub4.exists() && sub4.isDirectory()) {
                return verifyDataDir(sub4, GameTitle.MOVING_OUT_2);
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
                    String.format("Terdeteksi folder data %s dengan Assembly-CSharp.dll. Eksekusi Mono belum tersedia.", resolvedTitle.displayName),
                    hash, dataDir);
        }

        // Cek IL2CPP metadata
        File il2cppMetadata = new File(dataDir, "il2cpp_data/Metadata/global-metadata.dat");
        if (il2cppMetadata.exists()) {
            String hash = computeSha256(il2cppMetadata);
            return new VerificationResult(true, resolvedTitle, RuntimeBackendType.IL2CPP_AOT,
                    String.format("Terdeteksi folder data %s dengan metadata IL2CPP. Recompilation dan eksekusi belum tersedia.", resolvedTitle.displayName),
                    hash, dataDir);
        }

        return new VerificationResult(false, resolvedTitle, RuntimeBackendType.UNKNOWN,
                "Tidak ditemukan Assembly-CSharp.dll maupun global-metadata.dat di " + dataDir.getName(), "", dataDir);
    }


    /** Reads a Windows PE header without executing the file. Only PE32+ AMD64 is supported. */
    public static ExecutableVerificationResult inspectGameExecutable(File executable, GameTitle hint) {
        GameTitle resolved = resolveExecutableTitle(executable, hint);
        if (executable == null || !executable.isFile()) {
            return new ExecutableVerificationResult(false, false, resolved, "unknown", 0, 0,
                    "File executable tidak ditemukan.", "", executable);
        }
        if (!executable.getName().toLowerCase(Locale.ROOT).endsWith(".exe")) {
            return new ExecutableVerificationResult(false, false, resolved, "unknown", 0, 0,
                    "File yang dipilih bukan berkas .exe.", "", executable);
        }
        try (RandomAccessFile input = new RandomAccessFile(executable, "r")) {
            if (input.length() < 128 || readU16LE(input) != 0x5A4D) {
                return new ExecutableVerificationResult(false, false, resolved, "unknown", 0, 0,
                        "Header DOS MZ tidak valid atau file terlalu kecil.", "", executable);
            }
            input.seek(0x3C);
            long peOffset = readU32LE(input);
            if (peOffset < 64 || peOffset + 26L > input.length()) {
                return new ExecutableVerificationResult(false, false, resolved, "unknown", 0, 0,
                        "Offset header PE tidak valid.", "", executable);
            }
            input.seek(peOffset);
            if (input.readUnsignedByte() != 'P' || input.readUnsignedByte() != 'E'
                    || input.readUnsignedByte() != 0 || input.readUnsignedByte() != 0) {
                return new ExecutableVerificationResult(false, false, resolved, "unknown", 0, 0,
                        "Signature PE tidak ditemukan.", "", executable);
            }
            int machine = readU16LE(input);
            int sections = readU16LE(input);
            input.seek(peOffset + 20L);
            int optionalHeaderSize = readU16LE(input);
            input.seek(peOffset + 24L);
            if (optionalHeaderSize < 2 || input.getFilePointer() + optionalHeaderSize > input.length()) {
                return new ExecutableVerificationResult(false, false, resolved, "unknown", machine, 0,
                        "Optional header PE terpotong atau tidak valid.", "", executable);
            }
            int optionalMagic = readU16LE(input);
            boolean magicMatches = (machine == 0x8664 || machine == 0xAA64)
                    ? optionalMagic == 0x020B
                    : machine == 0x014C ? optionalMagic == 0x010B
                    : (optionalMagic == 0x010B || optionalMagic == 0x020B);
            if (sections <= 0 || !magicMatches) {
                return new ExecutableVerificationResult(false, false, resolved, "unknown", machine, optionalMagic,
                        "Struktur PE/COFF tidak konsisten.", "", executable);
            }
            String architecture;
            switch (machine) {
                case 0x8664: architecture = "x86_64"; break;
                case 0x014C: architecture = "x86"; break;
                case 0xAA64: architecture = "ARM64"; break;
                default: architecture = String.format(Locale.ROOT, "unknown-0x%04x", machine);
            }
            boolean supported = machine == 0x8664 && optionalMagic == 0x020B;
            String message = supported
                    ? "PE32+ AMD64 valid; header executable terbaca. File belum dijalankan."
                    : "PE valid (" + architecture + "), tetapi runtime saat ini hanya mendukung PE32+ AMD64 melalui Box64/Wine.";
            return new ExecutableVerificationResult(true, supported, resolved, architecture, machine,
                    optionalMagic, message, computeSha256(executable), executable);
        } catch (IOException error) {
            Log.e(TAG, "Gagal membaca header PE: " + executable.getAbsolutePath(), error);
            return new ExecutableVerificationResult(false, false, resolved, "unknown", 0, 0,
                    "Gagal membaca header PE: " + error.getMessage(), "", executable);
        }
    }

    /** Finds the title-matching .exe in an imported ZIP/folder tree without launching it. */
    public static File findGameExecutable(File root, GameTitle title) {
        if (root == null || !root.exists() || !root.isDirectory()) return null;
        List<File> candidates = new ArrayList<>();
        collectExecutables(root, 0, new HashSet<>(), candidates);
        File best = null;
        int bestScore = 0;
        for (File candidate : candidates) {
            int score = executableNameScore(candidate, title);
            if (score > bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }

    private static void collectExecutables(File directory, int depth, Set<String> visited, List<File> output) {
        if (depth > 8 || output.size() >= 5000) return;
        try {
            if (!visited.add(directory.getCanonicalPath())) return;
        } catch (IOException ignored) {
            return;
        }
        File[] children = directory.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (output.size() >= 5000) return;
            if (child.isFile() && child.getName().toLowerCase(Locale.ROOT).endsWith(".exe")) {
                output.add(child);
            } else if (child.isDirectory() && !child.isHidden()) {
                collectExecutables(child, depth + 1, visited, output);
            }
        }
    }

    private static int executableNameScore(File executable, GameTitle title) {
        String name = executable.getName().toLowerCase(Locale.ROOT).replace("_", "").replace(" ", "");
        switch (title) {
            case OVERCOOKED_2: return name.equals("overcooked2.exe") ? 100 : name.contains("overcooked2") ? 80 : 0;
            case OVERCOOKED_1: return name.equals("overcooked.exe") ? 100 : (name.contains("overcooked") && !name.contains("2")) ? 80 : 0;
            case MOVING_OUT_2: return name.equals("movingout2.exe") ? 100 : name.contains("movingout2") ? 80 : 0;
            case MOVING_OUT: return name.equals("movingout.exe") ? 100 : (name.contains("movingout") && !name.contains("2")) ? 80 : 0;
            default: return 1;
        }
    }

    private static GameTitle resolveExecutableTitle(File executable, GameTitle hint) {
        if (executable != null) {
            String name = executable.getName().toLowerCase(Locale.ROOT).replace("_", "").replace(" ", "");
            if (name.contains("movingout2")) return GameTitle.MOVING_OUT_2;
            if (name.contains("movingout")) return GameTitle.MOVING_OUT;
            if (name.contains("overcooked2")) return GameTitle.OVERCOOKED_2;
            if (name.contains("overcooked")) return GameTitle.OVERCOOKED_1;
        }
        return hint == null ? GameTitle.UNKNOWN : hint;
    }

    private static int readU16LE(RandomAccessFile input) throws IOException {
        int low = input.readUnsignedByte();
        int high = input.readUnsignedByte();
        return low | (high << 8);
    }

    private static long readU32LE(RandomAccessFile input) throws IOException {
        long b0 = input.readUnsignedByte();
        long b1 = input.readUnsignedByte();
        long b2 = input.readUnsignedByte();
        long b3 = input.readUnsignedByte();
        return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
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
