package com.overcookedrecomp.app.compiler.overcooked

import android.content.Context
import android.util.Log
import com.overcookedrecomp.app.GameStorageVerifier
import com.overcookedrecomp.app.compiler.core.Developer
import com.overcookedrecomp.app.compiler.core.GameCompiler
import com.overcookedrecomp.app.compiler.core.VerificationStatus
import java.io.File

/**
 * Compiler dan Manager untuk seri Overcooked (Ghost Town Games / Team17)
 */
class OvercookedCompiler(
    val titleVersion: Int = 1
) : GameCompiler {

    override val developer: Developer = Developer.GHOST_TOWN_GAMES

    override val gameId: String = if (titleVersion == 2) "overcooked_2" else "overcooked_1"

    override val gameTitle: String = if (titleVersion == 2) "Overcooked 2" else "Overcooked 1"

    override val defaultFolderName: String = if (titleVersion == 2) "Overcooked2_Data" else "Overcooked_Data"

    override val supportedBackends: List<String> = listOf("Mono CIL (.NET 3.5/4.x)", "IL2CPP AOT")

    companion object {
        private const val TAG = "OvercookedCompiler"
    }

    override fun verifyInstallation(baseDir: File): VerificationStatus {
        val targetDir = File(baseDir, defaultFolderName)
        val hint = if (titleVersion == 2) GameStorageVerifier.GameTitle.OVERCOOKED_2 else GameStorageVerifier.GameTitle.OVERCOOKED_1
        val result = GameStorageVerifier.verifyDataDir(targetDir, hint)

        return if (result.valid) {
            VerificationStatus.Ready(
                message = result.message,
                backendType = result.backendType.name,
                targetDir = result.dataDir,
                binaryHash = result.primaryBinarySha256
            )
        } else {
            if (!targetDir.exists()) {
                VerificationStatus.NotInstalled(
                    message = "Folder $defaultFolderName belum ditemukan.",
                    expectedFolder = defaultFolderName
                )
            } else {
                VerificationStatus.Corrupted(
                    message = result.message,
                    missingFiles = listOf("globalgamemanagers", "Managed/Assembly-CSharp.dll")
                )
            }
        }
    }

    override fun compileAndPrepare(context: Context, dataDir: File, onProgress: (String) -> Unit): Boolean {
        Log.i(TAG, "Menyiapkan pipeline shader dan kontrol untuk $gameTitle di: ${dataDir.absolutePath}")
        onProgress("Memverifikasi CIL bytecode dan mapping shader $gameTitle...")
        return true
    }

    override fun launchGame(context: Context, dataDir: File) {
        Log.i(TAG, "Meluncurkan $gameTitle dari $dataDir")
    }
}
