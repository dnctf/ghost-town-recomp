package com.overcookedrecomp.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class GameStorageVerifier {
    public enum RuntimeBackendType {
        UNKNOWN,
        MONO_CIL,
        IL2CPP_AOT
    }

    public static final class VerificationResult {
        public final boolean valid;
        public final RuntimeBackendType backendType;
        public final String message;
        public final String primaryBinarySha256;

        public VerificationResult(boolean valid, RuntimeBackendType backendType, String message, String sha256) {
            this.valid = valid;
            this.backendType = backendType;
            this.message = message;
            this.primaryBinarySha256 = sha256;
        }
    }

    public static VerificationResult verifyOvercookedDataDir(File dataDir) {
        if (dataDir == null || !dataDir.exists() || !dataDir.isDirectory()) {
            return new VerificationResult(false, RuntimeBackendType.UNKNOWN,
                    "Direktori Overcooked_Data tidak ditemukan.", "");
        }

        File globalGameManagers = new File(dataDir, "globalgamemanagers");
        File level0 = new File(dataDir, "level0");
        if (!globalGameManagers.exists() || !level0.exists()) {
            return new VerificationResult(false, RuntimeBackendType.UNKNOWN,
                    "Berkas inti Unity (globalgamemanagers / level0) hilang.", "");
        }

        File monoAssembly = new File(dataDir, "Managed/Assembly-CSharp.dll");
        if (monoAssembly.exists()) {
            String hash = computeSha256(monoAssembly);
            return new VerificationResult(true, RuntimeBackendType.MONO_CIL,
                    "Terdeteksi build Unity Mono (Jalur B). Siap menjalankan Assembly-CSharp.dll.", hash);
        }

        File il2cppMetadata = new File(dataDir, "il2cpp_data/Metadata/global-metadata.dat");
        if (il2cppMetadata.exists()) {
            String hash = computeSha256(il2cppMetadata);
            return new VerificationResult(true, RuntimeBackendType.IL2CPP_AOT,
                    "Terdeteksi build Unity IL2CPP (Jalur A). Siap mem-parsing global-metadata.dat.", hash);
        }

        return new VerificationResult(false, RuntimeBackendType.UNKNOWN,
                "Tidak ditemukan Assembly-CSharp.dll maupun global-metadata.dat.", "");
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
            return "ERROR_HASH";
        }
    }
}
