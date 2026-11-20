package com.overcookedrecomp.app.compiler.movingout

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

/**
 * Moving Out 2-only status screen.
 * It intentionally does not claim compilation, runtime readiness, or launch success.
 */
class MovingOutLauncherActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_DATA_DIR = "EXTRA_DATA_DIR"
        const val EXTRA_GAME_TITLE = "EXTRA_GAME_TITLE"
        private const val TAG = "MovingOut2Status"
    }

    private var gameDataDir: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gameDataDir = intent.getStringExtra(EXTRA_DATA_DIR)?.let(::File)
            ?: getExternalFilesDir(null)?.let { File(it, "MovingOut2_Data") }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 28, 32, 28)
            setBackgroundColor(Color.rgb(17, 31, 43))
        }
        val title = TextView(this).apply {
            text = "MOVING OUT 2"
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(246, 199, 107))
            gravity = Gravity.CENTER
        }
        root.addView(title)
        val subtitle = TextView(this).apply {
            text = "Dedicated Android compatibility session"
            textSize = 14f
            setTextColor(Color.LTGRAY
            )
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 20)
        }
        root.addView(subtitle)

        val dataDir = gameDataDir
        val hasUnityCore = dataDir?.let { dir ->
            listOf("globalgamemanagers", "data.unity3d", "level0").any { File(dir, it).isFile }
        } ?: false
        val hasIl2CppMetadata = dataDir?.let { File(it, "il2cpp_data/Metadata/global-metadata.dat").isFile } ?: false
        val status = TextView(this).apply {
            text = buildString {
                append("Game data path: ")
                append(dataDir?.absolutePath ?: "not selected")
                append("\n\nUnity core files: ")
                append(if (hasUnityCore) "detected" else "not detected")
                append("\nIL2CPP metadata: ")
                append(if (hasIl2CppMetadata) "detected" else "not detected")
                append("\n\nRuntime compatibility: NOT VERIFIED")
                append("\nGame boot / first frame / gameplay: NOT VERIFIED")
            }
            textSize = 13f
            setTextColor(Color.WHITE)
            setPadding(20, 18, 20, 18)
            setBackgroundColor(Color.rgb(38, 57, 73))
        }
        root.addView(status)

        val launch = Button(this).apply {
            text = "Runtime belum tersedia — launch dinonaktifkan"
            isEnabled = false
            setPadding(8, 16, 8, 16)
        }
        root.addView(launch)

        val back = Button(this).apply {
            text = "Kembali"
            setOnClickListener { finish() }
        }
        root.addView(back)
        setContentView(root)
    }
}
