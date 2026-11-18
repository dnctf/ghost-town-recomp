package com.overcookedrecomp.app;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.os.Environment;
import android.util.Log;
import androidx.documentfile.provider.DocumentFile;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class GameFileImporter {
    private static final String TAG = "GameFileImporter";

    public interface ProgressListener {
        void onProgress(int percent, String currentFileName);
        void onComplete(boolean success, String message);
    }


    private static final long MAX_EXECUTABLE_BYTES = 2L * 1024L * 1024L * 1024L;

    /** Returns the provider-supplied display name for a SAF URI. */
    public static String getDisplayName(Context context, Uri uri) {
        if (uri == null) return "";
        try (Cursor cursor = context.getContentResolver().query(
                uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    String name = cursor.getString(index);
                    if (name != null && !name.trim().isEmpty()) return name;
                }
            }
        } catch (Exception error) {
            Log.w(TAG, "Tidak dapat membaca nama file SAF: " + error.getMessage());
        }
        String fallback = uri.getLastPathSegment();
        return fallback == null ? "" : fallback;
    }

    /** Imports and parses a Windows EXE as data; this method never executes it. */
    public static void importExecutableUri(Context context, Uri exeUri, File targetDir,
                                           GameStorageVerifier.GameTitle title, ProgressListener listener) {
        new Thread(() -> {
            File temporary = null;
            try {
                String name = getDisplayName(context, exeUri);
                if (!name.toLowerCase(Locale.ROOT).endsWith(".exe") || name.contains("/") ||
                        name.contains("\\") || name.equals(".exe") || name.equals("..") ||
                        name.trim().isEmpty()) {
                    throw new IOException("Pilih satu file executable Windows (.exe) dengan nama file valid.");
                }
                if (!targetDir.exists() && !targetDir.mkdirs()) {
                    throw new IOException("Tidak dapat membuat folder instalasi game.");
                }
                File destination = new File(targetDir, name);
                temporary = new File(targetDir, "." + name + ".importing.exe");
                if (temporary.exists() && !temporary.delete()) {
                    throw new IOException("Tidak dapat membersihkan file impor EXE sementara.");
                }
                InputStream raw = context.getContentResolver().openInputStream(exeUri);
                if (raw == null) throw new IOException("Tidak dapat membuka file EXE yang dipilih.");
                long total = 0L;
                try (InputStream input = new BufferedInputStream(raw);
                     FileOutputStream output = new FileOutputStream(temporary)) {
                    byte[] buffer = new byte[64 * 1024];
                    int read;
                    while ((read = input.read(buffer)) != -1) {
                        total += read;
                        if (total > MAX_EXECUTABLE_BYTES) throw new IOException("Ukuran executable melebihi batas impor 2 GiB.");
                        output.write(buffer, 0, read);
                        if (listener != null && total / (16L * 1024L * 1024L) != (total - read) / (16L * 1024L * 1024L)) {
                            listener.onProgress(-1, name);
                        }
                    }
                    output.getFD().sync();
                }
                if (total == 0L) throw new IOException("File EXE kosong.");
                GameStorageVerifier.ExecutableVerificationResult parsed =
                        GameStorageVerifier.inspectGameExecutable(temporary, title);
                if (!parsed.valid || !parsed.supportedByBox64) throw new IOException(parsed.message);
                if (destination.exists() && !destination.delete()) {
                    throw new IOException("Tidak dapat mengganti executable lama.");
                }
                if (!temporary.renameTo(destination)) {
                    throw new IOException("Tidak dapat mengaktifkan file EXE hasil impor.");
                }
                temporary = null;
                if (listener != null) listener.onComplete(true,
                        "PE32+ AMD64 terdeteksi dan SHA-256 dihitung. File belum dijalankan; UnityPlayer.dll dan folder *_Data juga diperlukan.\n" +
                                destination.getName() + " • " + parsed.sha256);
            } catch (Exception error) {
                if (temporary != null && temporary.exists()) temporary.delete();
                Log.e(TAG, "Gagal mengimpor executable game.", error);
                if (listener != null) listener.onComplete(false, "Gagal mengimpor EXE: " + error.getLocalizedMessage());
            }
        }, "game-exe-import").start();
    }

    /**
     * Mengekstrak file ZIP dari Uri (Storage Access Framework) ke direktori tujuan aplikasi.
     */
    public static void extractZipUri(Context context, Uri zipUri, File targetDir, ProgressListener listener) {
        new Thread(() -> {
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }

            InputStream rawStream;
            try {
                if ("file".equalsIgnoreCase(zipUri.getScheme()) || zipUri.getScheme() == null) {
                    rawStream = new FileInputStream(zipUri.getPath());
                } else {
                    rawStream = context.getContentResolver().openInputStream(zipUri);
                }
            } catch (Exception e) {
                Log.e(TAG, "Gagal membuka stream berkas ZIP: " + e.getMessage());
                if (listener != null) listener.onComplete(false, "Gagal membuka berkas ZIP: " + e.getLocalizedMessage());
                return;
            }

            try (InputStream is = rawStream;
                 BufferedInputStream bis = new BufferedInputStream(is);
                 ZipInputStream zis = new ZipInputStream(bis)) {

                byte[] buffer = new byte[64 * 1024]; // 64 KB buffer untuk kecepatan I/O
                ZipEntry entry;
                int count = 0;

                while ((entry = zis.getNextEntry()) != null) {
                    count++;
                    String entryName = entry.getName();

                    // Proteksi dari celah keamanan Zip Slip
                    File outputFile = new File(targetDir, entryName);
                    String canonicalDestPath = targetDir.getCanonicalPath();
                    String canonicalOutPath = outputFile.getCanonicalPath();
                    if (!canonicalOutPath.startsWith(canonicalDestPath + File.separator)) {
                        zis.closeEntry();
                        continue;
                    }

                    if (entry.isDirectory()) {
                        outputFile.mkdirs();
                    } else {
                        File parent = outputFile.getParentFile();
                        if (parent != null && !parent.exists()) {
                            parent.mkdirs();
                        }
                        try (FileOutputStream fos = new FileOutputStream(outputFile);
                             BufferedOutputStream bos = new BufferedOutputStream(fos, buffer.length)) {
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                bos.write(buffer, 0, len);
                            }
                        }
                    }
                    zis.closeEntry();

                    if (listener != null && count % 5 == 0) {
                        listener.onProgress(-1, entryName);
                    }
                }

                Log.i(TAG, "Ekstraksi ZIP selesai. Total entri: " + count);
                if (listener != null) {
                    listener.onComplete(true, "Berhasil mengekstrak " + count + " berkas game PC.");
                }

            } catch (Exception e) {
                Log.e(TAG, "Gagal mengekstrak ZIP: " + e.getMessage(), e);
                if (listener != null) {
                    listener.onComplete(false, "Gagal mengekstrak berkas ZIP: " + e.getLocalizedMessage());
                }
            }
        }).start();
    }

    /**
     * Menyalin seluruh folder dari Uri Tree (Folder SAF) ke direktori tujuan aplikasi.
     */
    public static void copyFolderUri(Context context, Uri treeUri, File targetDir, ProgressListener listener) {
        new Thread(() -> {
            try {
                DocumentFile docDir = DocumentFile.fromTreeUri(context, treeUri);
                if (docDir == null || !docDir.isDirectory()) {
                    if (listener != null) listener.onComplete(false, "Folder yang dipilih tidak valid.");
                    return;
                }

                if (!targetDir.exists()) targetDir.mkdirs();
                int totalCopied = copyDocumentFileRecursive(context, docDir, targetDir, listener, new int[]{0});

                if (listener != null) {
                    listener.onComplete(true, "Berhasil menyalin " + totalCopied + " berkas game PC.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Gagal menyalin folder: " + e.getMessage(), e);
                if (listener != null) {
                    listener.onComplete(false, "Gagal menyalin folder: " + e.getLocalizedMessage());
                }
            }
        }).start();
    }

    private static int copyDocumentFileRecursive(Context context, DocumentFile src, File dst,
                                                 ProgressListener listener, int[] counter) throws IOException {
        DocumentFile[] files = src.listFiles();
        for (DocumentFile f : files) {
            File outFile = new File(dst, f.getName());
            if (f.isDirectory()) {
                outFile.mkdirs();
                copyDocumentFileRecursive(context, f, outFile, listener, counter);
            } else {
                try (InputStream in = context.getContentResolver().openInputStream(f.getUri());
                     OutputStream out = new FileOutputStream(outFile)) {
                    byte[] buf = new byte[64 * 1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                }
                counter[0]++;
                if (listener != null && counter[0] % 5 == 0) {
                    listener.onProgress(-1, f.getName());
                }
            }
        }
        return counter[0];
    }

    /**
     * Memindai folder unduhan (/sdcard/Download) untuk mencari arsip ZIP game PC secara otomatis.
     */
    public static List<File> scanDownloadFolderForGameZips() {
        List<File> candidates = new ArrayList<>();
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (downloadDir != null && downloadDir.exists() && downloadDir.isDirectory()) {
            File[] files = downloadDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    String name = f.getName().toLowerCase();
                    if ((name.contains("overcooked") || name.contains("moving")) && name.endsWith(".zip")) {
                        candidates.add(f);
                    }
                }
            }
        }
        return candidates;
    }
}
