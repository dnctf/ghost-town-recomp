package com.overcookedrecomp.app.compiler.movingout

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.overcookedrecomp.app.R
import com.overcookedrecomp.app.compiler.core.CompilerRegistry
import com.overcookedrecomp.app.compiler.core.Developer
import com.overcookedrecomp.app.compiler.core.VerificationStatus
import java.io.File

/**
 * Activity UI Launcher khusus untuk developer "SMG Studio" dan game "Moving Out".
 * Menyediakan kontrol kompilasi asset pipeline, physics alignment, dan peluncur game.
 */
class MovingOutLauncherActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DATA_DIR = "EXTRA_DATA_DIR"
        const val EXTRA_GAME_TITLE = "EXTRA_GAME_TITLE"
        private const val TAG = "MovingOutLauncher"
    }

    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var compileBtn: Button
    private lateinit var launchBtn: Button

    private var gameDataDir: File? = null
    private val compiler = MovingOutCompiler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val dataDirPath = intent.getStringExtra(EXTRA_DATA_DIR)
        gameDataDir = if (dataDirPath != null) File(dataDirPath) else getExternalFilesDir(null)?.let { File(it, compiler.defaultFolderName) }

        val root = createLayout()
        setContentView(root)

        refreshStatus()
    }

    private fun createLayout(): View {
        val scrollView = ScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#001824"))
            isFillViewport = true
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 36, 48, 48)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // Header Developer SMG Studio
        val devBadge = TextView(this).apply {
            text = "🚚 SMG STUDIO • LAUNCHER"
            textSize = 12f
            setTextColor(Color.parseColor("#06D6A0"))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 8)
        }
        container.addView(devBadge)

        // Game Title
        val titleView = TextView(this).apply {
            text = "📦 Moving Out Recomp"
            textSize = 24f
            setTextColor(Color.parseColor("#00A8E8"))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        container.addView(titleView)

        val subTitle = TextView(this).apply {
            text = "Kompilasi & Launcher Game Fisika Pindahan Rumah PC ke Android (Snapdragon Vulkan)"
            textSize = 13f
            setTextColor(Color.parseColor("#CCCCCC"))
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 24)
        }
        container.addView(subTitle)

        // Card Info & Status
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createCardDrawable("#003459", "#007EA7")
            setPadding(32, 24, 32, 24)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val cardTitle = TextView(this).apply {
            text = "Status Berkas Game PC"
            textSize = 16f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }
        card.addView(cardTitle)

        statusText = TextView(this).apply {
            text = "Memeriksa status instalasi..."
            textSize = 13f
            setTextColor(Color.parseColor("#E0E0E0"))
            setPadding(0, 8, 0, 16)
        }
        card.addView(statusText)

        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            isIndeterminate = true
            visibility = View.GONE
        }
        card.addView(progressBar)

        container.addView(card)

        // Action Buttons
        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 24)
        }
        container.addView(spacer)

        compileBtn = Button(this).apply {
            text = "⚙️ Kompilasi & Persiapkan Asset Moving Out"
            textSize = 14f
            setTextColor(Color.WHITE)
            background = createButtonDrawable("#007EA7")
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 12, 0, 12)
            }
            setOnClickListener { runCompilation() }
        }
        container.addView(compileBtn)

        launchBtn = Button(this).apply {
            text = "🎮 Mulai Moving Out"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = createButtonDrawable("#06D6A0")
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 12)
            }
            setOnClickListener {
                val dir = gameDataDir
                if (dir != null && dir.exists()) {
                    Toast.makeText(this@MovingOutLauncherActivity, "Memulai Moving Out Engine...", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@MovingOutLauncherActivity, "Data Moving Out belum tersedia.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        container.addView(launchBtn)

        val backBtn = Button(this).apply {
            text = "⬅️ Kembali ke Menu Multi-Developer"
            textSize = 13f
            setTextColor(Color.parseColor("#CCCCCC"))
            background = createButtonDrawable("#002233")
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener { finish() }
        }
        container.addView(backBtn)

        scrollView.addView(container)
        return scrollView
    }

    private fun refreshStatus() {
        val baseDir = getExternalFilesDir(null) ?: return
        val status = compiler.verifyInstallation(baseDir)

        when (status) {
            is VerificationStatus.Ready -> {
                statusText.text = "✅ Status: Siap Dimainkan\nBackend: ${status.backendType}\nLokasi: ${status.targetDir.name}"
                statusText.setTextColor(Color.parseColor("#06D6A0"))
                compileBtn.isEnabled = true
                launchBtn.isEnabled = true
                launchBtn.alpha = 1.0f
            }
            is VerificationStatus.NotInstalled -> {
                statusText.text = "❌ Status: Belum Terpasang\nHarap salin folder '${status.expectedFolder}' dari game PC Anda ke direktori aplikasi."
                statusText.setTextColor(Color.parseColor("#E71D36"))
                compileBtn.isEnabled = false
                launchBtn.isEnabled = false
                launchBtn.alpha = 0.5f
            }
            is VerificationStatus.Corrupted -> {
                statusText.text = "⚠️ Status: Berkas Tidak Lengkap\n${status.message}"
                statusText.setTextColor(Color.parseColor("#FFD166"))
                compileBtn.isEnabled = false
                launchBtn.isEnabled = false
                launchBtn.alpha = 0.5f
            }
        }
    }

    private fun runCompilation() {
        val dir = gameDataDir ?: return
        progressBar.visibility = View.VISIBLE
        compileBtn.isEnabled = false

        Thread {
            compiler.compileAndPrepare(this, dir) { msg ->
                runOnUiThread {
                    statusText.text = "⚡ $msg"
                }
            }
            runOnUiThread {
                progressBar.visibility = View.GONE
                compileBtn.isEnabled = true
                Toast.makeText(this, "Kompilasi asset Moving Out selesai!", Toast.LENGTH_SHORT).show()
                refreshStatus()
            }
        }.start()
    }

    private fun createButtonDrawable(hexColor: String): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(hexColor))
            cornerRadius = 14f
        }
    }

    private fun createCardDrawable(bgHex: String, borderHex: String): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(bgHex))
            cornerRadius = 18f
            setStroke(2, Color.parseColor(borderHex))
        }
    }
}
