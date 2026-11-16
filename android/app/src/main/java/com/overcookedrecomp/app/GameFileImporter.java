package com.overcookedrecomp.app;

import android.content.Context;
import android.net.Uri;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class GameFileImporter {
    private static final String TAG = "GameFileImporter";

    public interface ProgressListener {
        void onProgress(int percent, String currentFileName);
        void onComplete(boolean success, String message);
    }

    /**
     * Mengekstrak file ZIP dari Uri (Storage Access Framework) ke direktori tujuan aplikasi.
     */
    public static void extractZipUri(Context context, Uri zipUri, File targetDir, ProgressListener listener) {
        new Thread(() -> {
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }

            try (InputStream is = context.getContentResolver().openInputStream(zipUri);
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
                    if (name.contains("overcooked") && name.endsWith(".zip")) {
                        candidates.add(f);
                    }
                }
            }
        }
        return candidates;
    }
}
