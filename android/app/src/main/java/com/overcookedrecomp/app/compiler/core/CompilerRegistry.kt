package com.overcookedrecomp.app.compiler.core

import android.util.Log
import com.overcookedrecomp.app.compiler.movingout.MovingOutCompiler
import com.overcookedrecomp.app.compiler.overcooked.OvercookedCompiler
import java.io.File

/**
 * Registry terpusat (Singleton) untuk mengelola seluruh compiler developer game.
 */
object CompilerRegistry {
    private const val TAG = "CompilerRegistry"
    private val compilers = mutableMapOf<String, GameCompiler>()

    init {
        // Registrasi compiler bawaan
        registerCompiler(OvercookedCompiler(titleVersion = 1))
        registerCompiler(OvercookedCompiler(titleVersion = 2))
        registerCompiler(MovingOutCompiler())
        Log.i(TAG, "CompilerRegistry diinisialisasi dengan ${compilers.size} compiler terdaftar.")
    }

    fun registerCompiler(compiler: GameCompiler) {
        compilers[compiler.gameId] = compiler
        Log.d(TAG, "Compiler terdaftar: [${compiler.developer.displayName}] ${compiler.gameTitle} (${compiler.gameId})")
    }

    fun getCompiler(gameId: String): GameCompiler? = compilers[gameId]

    fun getAllCompilers(): List<GameCompiler> = compilers.values.toList()

    fun getCompilersByDeveloper(developer: Developer): List<GameCompiler> =
        compilers.values.filter { it.developer == developer }

    /**
     * Memindai direktori penyimpanan untuk seluruh compiler yang terdaftar
     */
    fun scanAllInstallations(baseDir: File): Map<GameCompiler, VerificationStatus> {
        val results = mutableMapOf<GameCompiler, VerificationStatus>()
        for (compiler in compilers.values) {
            results[compiler] = compiler.verifyInstallation(baseDir)
        }
        return results
    }
}
