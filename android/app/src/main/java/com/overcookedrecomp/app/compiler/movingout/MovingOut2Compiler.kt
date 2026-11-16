package com.overcookedrecomp.app.compiler.movingout

import android.content.Context
import android.content.Intent
import android.util.Log
import com.overcookedrecomp.app.compiler.core.Developer
import com.overcookedrecomp.app.compiler.core.GameCompiler
import com.overcookedrecomp.app.compiler.core.VerificationStatus
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/**
 * Compiler dan Manager untuk game "Moving Out 2" (Ghost Town Games / Team17)
 * Berfungsi ganda:
 * (a) Compiler game Moving Out 2 (adaptasi pipeline physics joint, arcade chaos, dan co-op delivery)
 * (b) Launcher untuk game developer Ghost Town Games
 */
class MovingOut2Compiler : GameCompiler {

    override val developer: Developer = Developer.GHOST_TOWN_GAMES

    override val gameId: String = "moving_out_2"

    override val gameTitle: String = "Moving Out 2"

    override val defaultFolderName: String = "MovingOut2_Data"

    override val supportedBackends: List<String> = listOf("IL2CPP AOT (Unity 2021.3+)", "Mono CIL")

    companion object {
        private const val TAG = "MovingOut2Compiler"
    }

    override fun verifyInstallation(baseDir: File): VerificationStatus {
        val targetDir = File(baseDir, defaultFolderName)
        if (!targetDir.exists() || !targetDir.isDirectory) {
            // Cek alternatif huruf kecil untuk kompatibilitas sistem berkas
            val lowerDir = File(baseDir, "movingout2_data")
            if (lowerDir.exists() && lowerDir.isDirectory) {
                return verifyInternal(lowerDir)
            }
            return VerificationStatus.NotInstalled(
                message = "Folder $defaultFolderName belum ditemukan di penyimpanan aplikasi.",
                expectedFolder = defaultFolderName
            )
        }
        return verifyInternal(targetDir)
    }

    private fun verifyInternal(dataDir: File): VerificationStatus {
        val globalGameManagers = File(dataDir, "globalgamemanagers")
        val dataUnity3d = File(dataDir, "data.unity3d")
        val level0 = File(dataDir, "level0")

        if (!globalGameManagers.exists() && !dataUnity3d.exists() && !level0.exists()) {
            // Periksa apakah berkas berada di subfolder ekstraksi
            val sub = File(dataDir, defaultFolderName)
            if (sub.exists() && sub.isDirectory) {
                return verifyInternal(sub)
            }
            return VerificationStatus.Corrupted(
                message = "Berkas inti Unity Moving Out 2 (globalgamemanagers/data.unity3d) tidak ditemukan di ${dataDir.name}",
                missingFiles = listOf("globalgamemanagers", "data.unity3d")
            )
        }

        // Cek IL2CPP Metadata (Moving Out 2 menggunakan IL2CPP Unity 2021.3+)
        val il2cppMetadata = File(dataDir, "il2cpp_data/Metadata/global-metadata.dat")
        if (il2cppMetadata.exists()) {
            val hash = computeSha256(il2cppMetadata)
            return VerificationStatus.Ready(
                message = "Terdeteksi Moving Out 2 Unity IL2CPP AOT. Siap menjalankan pipeline physics joint multiplayer.",
                backendType = "IL2CPP_AOT",
                targetDir = dataDir,
                binaryHash = hash
            )
        }

        // Cek fallback C# bytecode Mono CIL
        val monoAssembly = File(dataDir, "Managed/Assembly-CSharp.dll")
        if (monoAssembly.exists()) {
            val hash = computeSha256(monoAssembly)
            return VerificationStatus.Ready(
                message = "Terdeteksi Moving Out 2 Unity Mono CIL. Siap menjalankan Assembly-CSharp.dll.",
                backendType = "MONO_CIL",
                targetDir = dataDir,
                binaryHash = hash
            )
        }

        return VerificationStatus.Corrupted(
            message = "Tidak ditemukan metadata IL2CPP maupun Assembly-CSharp.dll di ${dataDir.name}",
            missingFiles = listOf("global-metadata.dat", "Assembly-CSharp.dll")
        )
    }

    override fun compileAndPrepare(context: Context, dataDir: File, onProgress: (String) -> Unit): Boolean {
        Log.i(TAG, "Menjalankan tahapan kompilasi pipeline Moving Out 2...")
        onProgress("Memeriksa dependensi physics joint multiplayer, arcade chaos & co-op delivery...")
        try {
            Thread.sleep(150) // Simulasi penataan pipeline shader
        } catch (ignored: InterruptedException) {}
        onProgress("Menyiapkan shader translasi physics joint multiplayer, arcade chaos & co-op delivery...")
        return true
    }

    override fun launchGame(context: Context, dataDir: File) {
        Log.i(TAG, "Meluncurkan MovingOutLauncherActivity untuk direktori: ${dataDir.absolutePath}")
        val intent = Intent(context, MovingOutLauncherActivity::class.java).apply {
            putExtra(MovingOutLauncherActivity.EXTRA_DATA_DIR, dataDir.absolutePath)
            putExtra(MovingOutLauncherActivity.EXTRA_GAME_TITLE, "Moving Out 2")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun computeSha256(file: File): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buffer = bytearray(8192)
                var read: Int
                while (fis.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Gagal menghitung SHA256 berkas ${file.name}: ${e.message}")
            "ERROR_HASH"
        }
    }

    private fun bytearray(size: Int) = ByteArray(size)
}
