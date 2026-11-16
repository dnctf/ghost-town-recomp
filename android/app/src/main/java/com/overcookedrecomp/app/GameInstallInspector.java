package com.overcookedrecomp.app;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Read-only preflight for a user-owned Windows game installation.
 * This class does not execute PE files and does not modify game data.
 */
public final class GameInstallInspector {
    private GameInstallInspector() {}

    public static final class ExecutableInfo {
        public final String name;
        public final long size;
        public final String architecture;
        public final String detail;

        ExecutableInfo(File file, String architecture, String detail) {
            this.name = file.getName();
            this.size = file.length();
            this.architecture = architecture;
            this.detail = detail;
        }
    }

    public static final class Inspection {
        public final boolean hasUnityData;
        public final List<ExecutableInfo> executables;
        public final String report;

        Inspection(boolean hasUnityData, List<ExecutableInfo> executables, String report) {
            this.hasUnityData = hasUnityData;
            this.executables = Collections.unmodifiableList(executables);
            this.report = report;
        }
    }

    public static Inspection inspect(File importedRoot, GameStorageVerifier.GameTitle title) {
        List<File> exeFiles = new ArrayList<>();
        boolean[] hasData = new boolean[] { false };
        collect(importedRoot, title.defaultFolderName, 0, exeFiles, hasData);

        List<ExecutableInfo> infos = new ArrayList<>();
        StringBuilder report = new StringBuilder();
        report.append("Folder impor: ").append(importedRoot == null ? "(null)" : importedRoot.getAbsolutePath()).append('\n');
        report.append("Folder data Unity: ").append(hasData[0] ? "ditemukan" : "tidak ditemukan").append('\n');
        report.append("Executable Windows (.exe): ").append(exeFiles.size()).append('\n');

        for (File exe : exeFiles) {
            String[] pe = readPeArchitecture(exe);
            ExecutableInfo info = new ExecutableInfo(exe, pe[0], pe[1]);
            infos.add(info);
            report.append("\n• ").append(info.name)
                    .append(" — ").append(info.architecture)
                    .append(" — ").append(info.size / (1024 * 1024)).append(" MB")
                    .append("\n  ").append(info.detail);
        }

        if (exeFiles.isEmpty()) {
            report.append("\n\nTidak ada .exe ditemukan. Pastikan yang diimpor adalah folder instalasi PC lengkap, bukan hanya *_Data.");
        } else {
            report.append("\n\nCatatan: pemeriksaan PE hanya membaca header executable. Ini belum membuktikan Wine/Box64, DRM, grafis, atau gameplay kompatibel.");
        }
        return new Inspection(hasData[0], infos, report.toString());
    }

    private static void collect(File dir, String expectedDataFolder, int depth,
                                List<File> exeFiles, boolean[] hasData) {
        if (dir == null || !dir.isDirectory() || depth > 6) return;
        String name = dir.getName().toLowerCase(Locale.ROOT);
        if ((depth > 0 && name.equals(expectedDataFolder.toLowerCase(Locale.ROOT)))
                || new File(dir, "globalgamemanagers").isFile()
                || new File(dir, "data.unity3d").isFile()) {
            hasData[0] = true;
        }
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (!child.isFile() && !child.isDirectory()) continue;
            if (child.isFile() && child.getName().toLowerCase(Locale.ROOT).endsWith(".exe")) {
                exeFiles.add(child);
            } else if (child.isDirectory() && depth < 6) {
                collect(child, expectedDataFolder, depth + 1, exeFiles, hasData);
            }
        }
    }

    private static String[] readPeArchitecture(File file) {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            if (raf.length() < 64 || raf.readUnsignedByte() != 'M' || raf.readUnsignedByte() != 'Z') {
                return new String[] {"Unknown", "Header DOS/MZ tidak valid."};
            }
            raf.seek(0x3c);
            long peOffset = readUInt32LE(raf);
            if (peOffset < 0 || peOffset + 6 > raf.length()) {
                return new String[] {"Unknown", "Offset PE di luar ukuran file."};
            }
            raf.seek(peOffset);
            if (raf.readUnsignedByte() != 'P' || raf.readUnsignedByte() != 'E'
                    || raf.readUnsignedByte() != 0 || raf.readUnsignedByte() != 0) {
                return new String[] {"Unknown", "Signature PE tidak ditemukan."};
            }
            int machine = readUInt16LE(raf);
            switch (machine) {
                case 0x014c: return new String[] {"x86 / 32-bit", "PE machine 0x014C. Membutuhkan jalur Wine 32-bit/WoW64 yang sesuai."};
                case 0x8664: return new String[] {"x86-64 / 64-bit", "PE machine 0x8664. Kandidat untuk Wine + Box64, belum berarti kompatibel."};
                case 0xAA64: return new String[] {"ARM64", "PE machine 0xAA64. Bukan executable x86-64 biasa; jalur translasi perlu ditinjau."};
                case 0x01c4: return new String[] {"ARM32", "PE machine 0x01C4. Perlu runtime Windows ARM32 yang sesuai."};
                default: return new String[] {String.format(Locale.ROOT, "Unknown (0x%04X)", machine), "Arsitektur PE belum dikenali oleh pemeriksa."};
            }
        } catch (IOException | RuntimeException e) {
            return new String[] {"Unknown", "Gagal membaca header PE: " + e.getMessage()};
        }
    }

    private static int readUInt16LE(RandomAccessFile raf) throws IOException {
        return raf.readUnsignedByte() | (raf.readUnsignedByte() << 8);
    }

    private static long readUInt32LE(RandomAccessFile raf) throws IOException {
        return ((long) raf.readUnsignedByte())
                | ((long) raf.readUnsignedByte() << 8)
                | ((long) raf.readUnsignedByte() << 16)
                | ((long) raf.readUnsignedByte() << 24);
    }
}
