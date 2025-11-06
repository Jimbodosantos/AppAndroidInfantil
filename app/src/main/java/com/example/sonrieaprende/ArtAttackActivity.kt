package com.example.sonrieaprende

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ArtAttackActivity : AppCompatActivity() {

    private lateinit var mainContainer: LinearLayout
    private lateinit var closeBtn: ImageButton
    private lateinit var baseImage: ImageView
    private lateinit var drawingCanvas: DrawingCanvasView
    private lateinit var zoomLayout: ZoomLayout
    private lateinit var categorySpinner: Spinner
    private lateinit var colorSpinner: Spinner
    private lateinit var brushSpinner: Spinner
    private lateinit var clearBtn: Button
    private lateinit var saveBtn: Button
    private lateinit var nightModeBtn: Button
    private lateinit var undoBtn: Button
    private lateinit var redoBtn: Button

    private var isNightMode = false
    private var currentSound: MediaPlayer? = null
    private var backgroundMusic: MediaPlayer? = null
    private var currentBaseImageRes: Int = 0

    private companion object {
        private const val STORAGE_PERMISSION_CODE = 100
        private const val HISTORY_DIR_NAME = "art_history"
        private const val HISTORY_FILE = "history.json"
        private const val SESSION_FLAG = "session_active"
    }

    private val categories = listOf(
        Category("Animales", listOf(R.raw.animal1, R.raw.animal2, R.raw.animal3)),
        Category("Naturaleza", listOf(R.raw.nat1, R.raw.nat2, R.raw.nat3)),
        Category("Personajes", listOf(R.raw.per1, R.raw.per2, R.raw.per3)),
        Category("Vehículos", listOf(R.raw.vei1, R.raw.vei2, R.raw.vei3)),
        Category("Fantasía", listOf(R.raw.fan1, R.raw.fan2, R.raw.fan3)),
        Category("Objetos", listOf(R.raw.obj1, R.raw.obj2, R.raw.obj3))
    )

    private val colors = listOf(
        ColorItem("Rojo 🔴", Color.parseColor("#FF6B6B")),
        ColorItem("Azul 🔵", Color.parseColor("#4ECDC4")),
        ColorItem("Amarillo 🟡", Color.parseColor("#FFD93D")),
        ColorItem("Verde 🟢", Color.parseColor("#96CEB4")),
        ColorItem("Morado 🟣", Color.parseColor("#A363D9")),
        ColorItem("Naranja 🟠", Color.parseColor("#FF9A76")),
        ColorItem("Rosa 💖", Color.parseColor("#FF9FF3")),
        ColorItem("Cian 💠", Color.parseColor("#45B7D1")),
        ColorItem("Negro ⚫", Color.BLACK),
        ColorItem("Blanco ⚪", Color.WHITE)
    )

    private val brushSizes = listOf(
        BrushSize("Fino ●", 3f),
        BrushSize("Mediano ●", 8f),
        BrushSize("Grueso ●", 15f),
        BrushSize("Extra ●", 25f)
    )

    data class Category(val name: String, val images: List<Int>)
    data class ColorItem(val name: String, val color: Int)
    data class BrushSize(val name: String, val size: Float)

    private var progressDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_art_attack)
        detectAndClearStaleHistory()
        createSessionFlag()
        initViews()
        setupListeners()
        setupSpinners()
        startBackgroundMusic()
        checkStoragePermissions()
        selectRandomImageFromCategory(0)
        Toast.makeText(this, "🎨 Un dedo: Dibujar\n🔍 Dos dedos: Zoom y Mover", Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeSessionFlagAndClearHistory()
        stopAllAudio()
        stopBackgroundMusic()
    }

    private fun initViews() {
        mainContainer = findViewById(R.id.mainContainer)
        closeBtn = findViewById(R.id.closeBtn)
        baseImage = findViewById(R.id.baseImage)
        drawingCanvas = findViewById(R.id.drawingCanvas)
        zoomLayout = findViewById(R.id.zoomLayout)
        categorySpinner = findViewById(R.id.categorySpinner)
        colorSpinner = findViewById(R.id.colorSpinner)
        brushSpinner = findViewById(R.id.brushSpinner)
        clearBtn = findViewById(R.id.clearBtn)
        saveBtn = findViewById(R.id.saveBtn)
        nightModeBtn = findViewById(R.id.nightModeBtn)
        undoBtn = findViewById(R.id.undoBtn)
        redoBtn = findViewById(R.id.redoBtn)

        setupButtonAppearance()
        connectZoomWithCanvas()
        drawingCanvas.onHistoryChanged = { runOnUiThread { updateUndoRedoButtons() } }
    }

    private fun setupButtonAppearance() {
        listOf(clearBtn, saveBtn, nightModeBtn, undoBtn, redoBtn).forEach { button ->
            val background = GradientDrawable().apply { cornerRadius = 20f }
            button.background = background
        }
    }

    private fun setupListeners() {
        closeBtn.setOnClickListener { showExitDialog() }
        clearBtn.setOnClickListener { clearDrawing() }
        saveBtn.setOnClickListener {
            if (hasStoragePermissions()) saveDrawing() else requestStoragePermissions()
        }
        nightModeBtn.setOnClickListener { toggleNightMode() }
        undoBtn.setOnClickListener {
            val did = drawingCanvas.undo()
            if (did) playSound(R.raw.correct_sound) else Toast.makeText(this, "Nada que deshacer", Toast.LENGTH_SHORT).show()
            updateUndoRedoButtons()
        }
        redoBtn.setOnClickListener {
            val did = drawingCanvas.redo()
            if (did) playSound(R.raw.correct_sound) else Toast.makeText(this, "Nada que rehacer", Toast.LENGTH_SHORT).show()
            updateUndoRedoButtons()
        }
    }

    private fun updateUndoRedoButtons() {
        undoBtn.isEnabled = drawingCanvas.canUndo()
        redoBtn.isEnabled = drawingCanvas.canRedo()
        undoBtn.alpha = if (undoBtn.isEnabled) 1f else 0.5f
        redoBtn.alpha = if (redoBtn.isEnabled) 1f else 0.5f
    }

    private fun setupSpinners() {
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories.map { it.name })
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter
        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { selectRandomImageFromCategory(position) }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val colorAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, colors.map { it.name })
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        colorSpinner.adapter = colorAdapter
        colorSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { drawingCanvas.setBrushColor(colors[position].color) }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val brushAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, brushSizes.map { it.name })
        brushAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        brushSpinner.adapter = brushAdapter
        brushSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { drawingCanvas.setBrushSize(brushSizes[position].size) }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun selectRandomImageFromCategory(categoryIndex: Int) {
        val category = categories[categoryIndex]
        val randomImage = category.images.random()
        currentBaseImageRes = randomImage
        clearDrawing()
        baseImage.setImageResource(randomImage)
        baseImage.post { zoomLayout.adjustImageToFit() }
        val bitmap = BitmapFactory.decodeResource(resources, randomImage)
        drawingCanvas.setBaseImageBitmap(bitmap)
        playSound(R.raw.correct_sound)
        updateUndoRedoButtons()
        Toast.makeText(this, "Categoría: ${category.name}", Toast.LENGTH_SHORT).show()
    }

    private fun clearDrawing() {
        drawingCanvas.clearCanvas()
        updateUndoRedoButtons()
    }

    private fun saveDrawing() {
        lifecycleScope.launch {
            showLoading("Guardando imagen...")
            val combined = withContext(Dispatchers.Default) { drawingCanvas.getCombinedBitmap() }
            hideLoading()
            combined?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) saveToMediaStore(it) else saveToExternal(it)
            } ?: run { Toast.makeText(this@ArtAttackActivity, "Nada para guardar", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun saveToMediaStore(bitmap: android.graphics.Bitmap) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "ArtAttack_$timestamp.png"
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SonrieAprende")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let { fileUri ->
                contentResolver.openOutputStream(fileUri)?.use { out -> bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out) }
                contentValues.clear(); contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(fileUri, contentValues, null, null)
                playSound(R.raw.level_complete_sound)
                showSaveSuccessDialog(fileName, uri)
            } ?: run { Toast.makeText(this, "❌ Error al crear el archivo", Toast.LENGTH_SHORT).show() }
        } catch (e: Exception) { Toast.makeText(this, "❌ Error al guardar", Toast.LENGTH_SHORT).show() }
    }

    private fun saveToExternal(bitmap: android.graphics.Bitmap) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "ArtAttack_$timestamp.png"
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val imageFile = File(storageDir, fileName)
            FileOutputStream(imageFile).use { out -> bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out) }
            playSound(R.raw.level_complete_sound)
            showSaveSuccessDialog(fileName, Uri.fromFile(imageFile))
        } catch (e: Exception) { Toast.makeText(this, "❌ Error al guardar", Toast.LENGTH_SHORT).show() }
    }

    // guardar historia en background
    override fun onPause() {
        super.onPause()
        lifecycleScope.launch(Dispatchers.IO) {
            val dir = File(cacheDir, HISTORY_DIR_NAME)
            dir.mkdirs()
            val f = File(dir, HISTORY_FILE)
            drawingCanvas.saveHistoryToFile(f)
        }
        pauseBackgroundMusic()
    }

    // restaurar historia en background
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            showLoading("Restaurando historial...")
            withContext(Dispatchers.IO) {
                val dir = File(cacheDir, HISTORY_DIR_NAME)
                val f = File(dir, HISTORY_FILE)
                drawingCanvas.restoreHistoryFromFile(f)
            }
            hideLoading()
            updateUndoRedoButtons()
        }
        resumeBackgroundMusic()
    }

    // SESSION FLAG
    private fun detectAndClearStaleHistory() {
        val sessionFile = File(cacheDir, SESSION_FLAG)
        if (sessionFile.exists()) {
            val dir = File(cacheDir, HISTORY_DIR_NAME)
            if (dir.exists()) dir.deleteRecursively()
            sessionFile.delete()
        }
    }

    private fun createSessionFlag() {
        try { File(cacheDir, SESSION_FLAG).writeText("active") } catch (_: Exception) {}
    }

    private fun removeSessionFlagAndClearHistory() {
        try {
            val sf = File(cacheDir, SESSION_FLAG); if (sf.exists()) sf.delete()
            val dir = File(cacheDir, HISTORY_DIR_NAME)
            if (dir.exists()) dir.deleteRecursively()
        } catch (_: Exception) {}
    }

    private fun showLoading(text: String) {
        runOnUiThread {
            if (progressDialog?.isShowing == true) return@runOnUiThread
            val pb = ProgressBar(this).apply { isIndeterminate = true }
            val tv = TextView(this).apply { this.text = text; setPadding(20, 0, 0, 0) }
            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(30, 30, 30, 30); addView(pb); addView(tv)
            }
            progressDialog = AlertDialog.Builder(this).setView(layout).setCancelable(false).create()
            progressDialog?.show()
        }
    }

    private fun hideLoading() {
        runOnUiThread { progressDialog?.dismiss(); progressDialog = null }
    }

    private fun toggleNightMode() {
        isNightMode = !isNightMode
        if (isNightMode) { mainContainer.setBackgroundColor(ContextCompat.getColor(this, R.color.text_dark)); nightModeBtn.text = "☀️ Día" }
        else { mainContainer.setBackgroundResource(R.drawable.gradient_background); nightModeBtn.text = "🌙 Modo" }
        playSound(R.raw.correct_sound)
    }

    private fun startBackgroundMusic() {
        try { backgroundMusic = MediaPlayer.create(this, R.raw.sonidodibujo); backgroundMusic?.isLooping = true; backgroundMusic?.setVolume(0.3f, 0.3f); backgroundMusic?.start() } catch (e: Exception) { e.printStackTrace() }
    }

    private fun stopBackgroundMusic() { backgroundMusic?.let { if (it.isPlaying) it.stop(); it.release(); backgroundMusic = null } }
    private fun pauseBackgroundMusic() { backgroundMusic?.pause() }
    private fun resumeBackgroundMusic() { backgroundMusic?.takeIf { !it.isPlaying }?.start() }

    private fun playSound(res: Int) {
        try { currentSound?.release(); currentSound = MediaPlayer.create(this, res); currentSound?.setOnCompletionListener { it.release(); currentSound = null }; currentSound?.start() } catch (e: Exception) { e.printStackTrace() }
    }

    private fun stopAllAudio() { currentSound?.let { if (it.isPlaying) it.stop(); it.release(); currentSound = null } }

    private fun connectZoomWithCanvas() { zoomLayout.onMatrixChanged = { drawingCanvas.setTransformMatrix(it) } }

    private fun showSaveSuccessDialog(fileName: String, uri: Uri? = null) {
        AlertDialog.Builder(this).setTitle("🎉 ¡Dibujo Guardado!").setMessage("Tu obra de arte se ha guardado como:\n$fileName\n\n✅ Incluye la imagen de fondo")
            .setPositiveButton("¡Continuar Dibujando!") { dialog, _ -> dialog.dismiss() }.setCancelable(false).show()
    }

    private fun showExitDialog() {
        AlertDialog.Builder(this).setTitle("🚪 ¿Salir del juego?").setMessage("Tu dibujo actual se perderá")
            .setPositiveButton("Seguir dibujando") { dialog, _ -> dialog.dismiss() }
            .setNegativeButton("Salir") { dialog, _ -> dialog.dismiss(); finish() }.setCancelable(true).show()
    }

    private fun checkStoragePermissions() { if (!hasStoragePermissions()) requestStoragePermissions() }

    private fun hasStoragePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) true else ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) Toast.makeText(this, "Permisos concedidos", Toast.LENGTH_SHORT).show()
    }
}
