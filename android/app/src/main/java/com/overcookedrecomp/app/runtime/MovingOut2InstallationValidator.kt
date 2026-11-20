package com.overcookedrecomp.app.runtime

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

/**
 * Read-only SAF validator for a user-selected Moving Out 2 PC folder.
 * This checks identity/file evidence only; it does not execute files or imply playability.
 */
object MovingOut2InstallationValidator {
    data class Report(
        val folderSelected: Boolean,
        val detected: Boolean,
        val summary: String,
        val missing: List<String>,
        val evidence: List<String>
    )

    fun validate(context: Context, treeUri: Uri?): Report {
        if (treeUri == null) return Report(false, false, "Folder game belum dipilih.", listOf("Pilih folder instalasi Moving Out 2"), emptyList())
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: return Report(true, false, "Folder tidak dapat diakses.", listOf("Folder SAF tidak dapat dibuka"), emptyList())
        if (!root.isDirectory) return Report(true, false, "Pilihan bukan folder.", listOf("Folder instalasi"), emptyList())

        val exe = child(root, "mo2.exe") ?: child(root, "MovingOut2.exe")
        val gameAssembly = child(root, "GameAssembly.dll")
        val unityPlayer = child(root, "UnityPlayer.dll")
        val baselib = child(root, "baselib.dll")
        val dataDir = child(root, "MovingOut2_Data") ?: child(root, "movingout2_data")
        val globalManagers = dataDir?.let { child(it, "globalgamemanagers") }
        val metadata = dataDir?.let { findPath(it, listOf("il2cpp_data", "Metadata", "global-metadata.dat")) }

        val evidence = mutableListOf<String>()
        if (exe?.isFile == true) evidence += "Windows game executable: ${exe.name}"
        if (gameAssembly?.isFile == true) evidence += "IL2CPP GameAssembly.dll"
        if (unityPlayer?.isFile == true) evidence += "UnityPlayer.dll"
        if (baselib?.isFile == true) evidence += "baselib.dll"
        if (globalManagers?.isFile == true) evidence += "Unity game data globalgamemanagers"
        if (metadata?.isFile == true) evidence += "IL2CPP metadata global-metadata.dat"

        val missing = mutableListOf<String>()
        if (exe?.isFile != true) missing += "mo2.exe / MovingOut2.exe"
        if (gameAssembly?.isFile != true) missing += "GameAssembly.dll"
        if (unityPlayer?.isFile != true) missing += "UnityPlayer.dll"
        if (baselib?.isFile != true) missing += "baselib.dll"
        if (globalManagers?.isFile != true) missing += "MovingOut2_Data/globalgamemanagers"
        if (metadata?.isFile != true) missing += "MovingOut2_Data/il2cpp_data/Metadata/global-metadata.dat"

        val complete = missing.isEmpty()
        val summary = if (complete) {
            "Moving Out 2 file baseline detected. Runtime compatibility remains unverified."
        } else {
            "Moving Out 2 belum tervalidasi lengkap. File wajib yang belum ditemukan: ${missing.size}."
        }
        return Report(true, complete, summary, missing, evidence)
    }

    private fun child(parent: DocumentFile, expected: String): DocumentFile? =
        parent.listFiles().firstOrNull { it.name.equals(expected, ignoreCase = true) }

    private fun findPath(parent: DocumentFile, parts: List<String>): DocumentFile? {
        var current: DocumentFile? = parent
        for (part in parts) current = current?.let { child(it, part) }
        return current
    }
}
