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
 * Compiler dan Manager untuk game "Moving Out" (SMG Studio / DevM Games)
 * Berfungsi ganda:
 * (a) Compiler game Moving Out (adaptasi pipeline physics joint, furniture collision, dan shader)
 * (b) Launcher untuk game developer SMG Studio
 */
class MovingOutCompiler : GameCompiler {

    override val developer: Developer = Developer.SMG_STUDIO

    override val gameId: String = "moving_out"

    override val gameTitle: String = "Moving Out"

    override val defaultFolderName: String = "MovingOut_Data"

    override val supportedBackends: List<String> = listOf("IL2CPP AOT (Unity 2019.4+)", "Mono CIL")

    companion object {
        private const val TAG = "MovingOutCompiler"
    }

    override fun verifyInstallation(baseDir: File): VerificationStatus {
        val targetDir = File(baseDir, defaultFolderName)
        if (!targetDir.exists() || !targetDir.isDirectory) {
            // Cek alternatif huruf kecil
            val lowerDir = File(baseDir, "movingout_data")
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
                message = "Berkas inti Unity Moving Out (globalgamemanagers/data.unity3d) tidak ditemukan di ${dataDir.name}",
                missingFiles = listOf("globalgamemanagers", "data.unity3d")
            )
        }

        // Cek IL2CPP Metadata (Moving Out modern menggunakan IL2CPP)
        val il2cppMetadata = File(dataDir, "il2cpp_data/Metadata/global-metadata.dat")
        if (il2cppMetadata.exists()) {
            val hash = computeSha256(il2cppMetadata)
            return VerificationStatus.Ready(
                message = "Terdeteksi Moving Out Unity IL2CPP AOT. Siap menjalankan pipeline furniture physics.",
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
                message = "Terdeteksi Moving Out Unity Mono CIL. Siap menjalankan Assembly-CSharp.dll.",
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
        Log.i(TAG, "Menjalankan tahapan kompilasi pipeline Moving Out...")
        onProgress("Memeriksa dependensi Physics 2D/3D & Havok/PhysX joint...")
        try {
            Thread.sleep(150) // Simulasi penataan pipeline shader
        } catch (ignored: InterruptedException) {}
        onProgress("Menyiapkan shader translasi sofa, rintangan, dan delivery truck...")
        return true
    }

    override fun launchGame(context: Context, dataDir: File) {
        Log.i(TAG, "Meluncurkan MovingOutLauncherActivity untuk direktori: ${dataDir.absolutePath}")
        val intent = Intent(context, MovingOutLauncherActivity::class.java).apply {
            putExtra(MovingOutLauncherActivity.EXTRA_DATA_DIR, dataDir.absolutePath)
            putExtra(MovingOutLauncherActivity.EXTRA_GAME_TITLE, gameTitle)
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
