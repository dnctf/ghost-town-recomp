package com.overcookedrecomp.app.compiler.core

import android.content.Context
import java.io.File

/**
 * Interface dasar untuk setiap compiler/launcher game dalam ekosistem multi-developer.
 * Berfungsi ganda:
 * 1. Memeriksa, mengekstrak, dan memvalidasi kompatibilitas berkas game PC (CIL/IL2CPP/Shaders)
 * 2. Meluncurkan game dengan native environment yang sesuai untuk developer tersebut
 */
interface GameCompiler {
    val developer: Developer
    val gameId: String
    val gameTitle: String
    val defaultFolderName: String
    val supportedBackends: List<String>

    /**
     * Memeriksa apakah berkas game PC terpasang dengan struktur yang valid
     */
    fun verifyInstallation(baseDir: File): VerificationStatus

    /**
     * Menjalankan tahapan kompilasi shim / adaptasi shader / penyesuaian aset sebelum booting
     */
    fun compileAndPrepare(context: Context, dataDir: File, onProgress: (String) -> Unit): Boolean

    /**
     * Meluncurkan Activity UI launcher atau langsung ke NativeEngine surface
     */
    fun launchGame(context: Context, dataDir: File)
}
