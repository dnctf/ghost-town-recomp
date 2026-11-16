package com.overcookedrecomp.app.compiler.core

import java.io.File

/**
 * Status hasil audit dan verifikasi integritas data game PC pada penyimpanan perangkat.
 */
sealed class VerificationStatus {
    abstract val message: String

    data class Ready(
        override val message: String,
        val backendType: String,
        val targetDir: File,
        val binaryHash: String = ""
    ) : VerificationStatus()

    data class NotInstalled(
        override val message: String,
        val expectedFolder: String
    ) : VerificationStatus()

    data class Corrupted(
        override val message: String,
        val missingFiles: List<String> = emptyList()
    ) : VerificationStatus()

    val isReady: Boolean get() = this is Ready
}
