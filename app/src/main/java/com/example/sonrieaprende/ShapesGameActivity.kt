package com.example.sonrieaprende

import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat

class ShapesGameActivity : AppCompatActivity() {

    // Variables del juego
    private lateinit var scoreText: TextView
    private lateinit var bestScoreText: TextView
    private lateinit var livesText: TextView
    private lateinit var questionCounterText: TextView
    private lateinit var questionDisplayText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var shapesGrid: GridLayout
    private lateinit var closeBtn: ImageButton
    private lateinit var nightModeBtn: Button
    private lateinit var mainContainer: LinearLayout

    private var score = 0
    private var bestScore = 0
    private var lives = 3
    private var currentQuestion = 1
    private val totalQuestions = 10
    private var currentTargetShape: Shape? = null
    private var isNightMode = false

    // gestión de audio
    private var currentMediaPlayer: android.media.MediaPlayer? = null
    private val audioHandler = Handler(Looper.getMainLooper())
    private var isPlayingAudio = false
    private val audioQueue = mutableListOf<Int>()

    // Datos de las formas
    private val shapes = listOf(
        Shape("círculo", "⭕", listOf("círculo", "redonda", "bola", "rueda")),
        Shape("cuadrado", "⬜", listOf("cuadrado", "cuadrada", "caja", "bloque")),
        Shape("triángulo", "🔺", listOf("triángulo", "triangular", "pirámide", "tejado")),
        Shape("rectángulo", "📏", listOf("rectángulo", "rectangular", "tabla", "puerta")),
        Shape("estrella", "⭐", listOf("estrella", "estrellada", "brillante", "lucero")),
        Shape("corazón", "❤️", listOf("corazón", "amor", "corazoncito", "amoroso")),
        Shape("rombo", "💠", listOf("rombo", "diamante", "rombito", "cometa")),
        Shape("óvalo", "🥚", listOf("óvalo", "ovalada", "huevo", "elipse")),
        Shape("pentágono", "⬟", listOf("pentágono", "cinco lados", "casa", "forma de casa")),
        Shape("hexágono", "⬢", listOf("hexágono", "seis lados", "panal", "colmena")),
        Shape("octágono", "🛑", listOf("octágono", "ocho lados", "señal", "stop")),
        Shape("crescente", "🌙", listOf("luna", "creciente", "media luna", "nocturna")),
        Shape("espiral", "🌀", listOf("espiral", "caracol", "remolino", "torbellino")),
        Shape("cruz", "➕", listOf("cruz", "cruce", "más", "intersección")),
        Shape("flecha", "➡️", listOf("flecha", "punta", "dirección", "señal")),
        Shape("campana", "🔔", listOf("campana", "sonido", "timbre", "llamada")),
    )

    data class Shape(
        val name: String,
        val display: String,
        val descriptions: List<String>
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shapes_game)

        initViews()
        loadBestScore()
        startGame()
        setupClickListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAllAudio()
        audioHandler.removeCallbacksAndMessages(null)
    }

    private fun initViews() {
        scoreText = findViewById(R.id.scoreText)
        bestScoreText = findViewById(R.id.bestScoreText)
        livesText = findViewById(R.id.livesText)
        questionCounterText = findViewById(R.id.questionCounterText)
        questionDisplayText = findViewById(R.id.questionDisplayText)
        progressBar = findViewById(R.id.progressBar)
        shapesGrid = findViewById(R.id.shapesGrid)
        closeBtn = findViewById(R.id.closeBtn)
        nightModeBtn = findViewById(R.id.nightModeBtn)
        mainContainer = findViewById(R.id.mainContainer)
    }

    private fun loadBestScore() {
        val prefs = getPreferences(MODE_PRIVATE)
        bestScore = prefs.getInt("best_score", 0)
        bestScoreText.text = bestScore.toString()
    }

    private fun saveBestScore() {
        val prefs = getPreferences(MODE_PRIVATE)
        with(prefs.edit()) {
            putInt("best_score", bestScore)
            apply()
        }
    }

    private fun startGame() {
        score = 0
        lives = 3
        currentQuestion = 1
        updateStats()
        generateQuestion()
    }

    private fun generateQuestion() {
        shapesGrid.removeAllViews()

        // Seleccionar forma objetivo
        currentTargetShape = shapes.random()

        // Crear opciones (forma objetivo + 3 aleatorias)
        val shapeOptions = mutableListOf(currentTargetShape!!)
        while (shapeOptions.size < 4) {
            val randomShape = shapes.random()
            if (!shapeOptions.contains(randomShape)) {
                shapeOptions.add(randomShape)
            }
        }

        // Mezclar opciones
        shapeOptions.shuffle()


        val rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        val colSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)

        // Crear cartas de formas
        shapeOptions.forEachIndexed { index, shape ->
            val shapeCard = LayoutInflater.from(this).inflate(R.layout.shape_card, shapesGrid, false)
            val shapeIcon = shapeCard.findViewById<TextView>(R.id.shapeIcon)
            val cardView = shapeCard.findViewById<CardView>(R.id.shapeCard)

            shapeIcon.text = shape.display


            val params = GridLayout.LayoutParams(rowSpec, colSpec)
            params.width = 0
            params.height = GridLayout.LayoutParams.WRAP_CONTENT
            params.setMargins(8, 8, 8, 8)
            shapeCard.layoutParams = params


            cardView.setOnClickListener {
                checkAnswer(shape, cardView)
            }

            shapesGrid.addView(shapeCard)
        }


        audioHandler.postDelayed({
            updateQuestionWithSound()
        }, 500)
    }

    private fun updateQuestionWithSound() {

        val availableQuestionTypes = mutableListOf<Pair<String, Int>>()

        // Verificar cada tipo de pregunta para esta forma específica
        val possibleQuestions = listOf(
            Pair("Toca el ${currentTargetShape!!.name}", getQuestionSound("toca", currentTargetShape!!.name)),
            Pair("¿Dónde está el ${currentTargetShape!!.name}?", getQuestionSound("donde", currentTargetShape!!.name)),
            Pair("Encuentra la forma ${currentTargetShape!!.name}", getQuestionSound("encuentra", currentTargetShape!!.name)),
            Pair("Selecciona el ${currentTargetShape!!.name}", getQuestionSound("selecciona", currentTargetShape!!.name)),
            Pair("¿Cuál es el ${currentTargetShape!!.name}?", getQuestionSound("cual", currentTargetShape!!.name))
        )

        // Filtrar solo las preguntas que tienen sonido
        possibleQuestions.forEach { question ->
            if (question.second != 0) {
                availableQuestionTypes.add(question)
            }
        }

        // Si hay preguntas con sonido disponibles, elegir una al azar
        if (availableQuestionTypes.isNotEmpty()) {
            val selectedQuestion = availableQuestionTypes.random()
            questionDisplayText.text = selectedQuestion.first
            playQuestionSound(selectedQuestion.second)
        } else {
            // Si no hay sonidos para esta forma, usar pregunta genérica SIN sonido
            questionDisplayText.text = "Encuentra el ${currentTargetShape!!.name}"

        }
    }

    private fun getQuestionSound(questionType: String, shapeName: String): Int {
        return when (questionType) {
            "toca" -> getTouchSound(shapeName)
            "donde" -> getWhereSound(shapeName)
            "encuentra" -> getFindSound(shapeName)
            "selecciona" -> getSelectSound(shapeName)
            "cual" -> getWhichSound(shapeName)
            else -> 0
        }
    }

    private fun getTouchSound(shapeName: String): Int {
        return when (shapeName) {
            "corazón" -> R.raw.tocaelcorazon
            "espiral" -> R.raw.tocaelespiral
            "rombo" -> R.raw.tocaelrombo
            "campana" -> R.raw.tocalacampana
            "flecha" -> R.raw.tocalaflecha
            else -> 0
        }
    }

    private fun getWhereSound(shapeName: String): Int {
        return when (shapeName) {
            "hexágono" -> R.raw.dondeestaelexagono
            "octágono" -> R.raw.dondeestaeloctagono
            else -> 0
        }
    }

    private fun getFindSound(shapeName: String): Int {
        return when (shapeName) {
            "círculo" -> R.raw.encuentralaformacirculo
            "cuadrado" -> R.raw.fan1
            "espiral" -> R.raw.encuentralaformaespiral
            "octágono" -> R.raw.encuentralaformaoctagono
            else -> 0
        }
    }

    private fun getSelectSound(shapeName: String): Int {
        return when (shapeName) {
            "círculo" -> R.raw.seleccionaelcirculo
            "crescente" -> R.raw.seleccionaelcreciente
            else -> 0
        }
    }

    private fun getWhichSound(shapeName: String): Int {
        return when (shapeName) {
            "crescente" -> R.raw.cualeselcrecinete
            "hexágono" -> R.raw.cualeseloxagono
            "rectángulo" -> R.raw.cualeselrectangulo
            "cruz" -> R.raw.cualeslacruz
            "estrella" -> R.raw.cualeslaestrella
            else -> 0
        }
    }

    private fun playQuestionSound(soundResource: Int) {
        if (soundResource != 0 && !isPlayingAudio) {
            try {
                isPlayingAudio = true
                currentMediaPlayer = android.media.MediaPlayer.create(this, soundResource)
                currentMediaPlayer?.setOnCompletionListener {
                    it.release()
                    currentMediaPlayer = null
                    isPlayingAudio = false
                    processAudioQueue()
                }
                currentMediaPlayer?.start()
            } catch (e: Exception) {
                e.printStackTrace()
                isPlayingAudio = false
                processAudioQueue()
            }
        } else if (soundResource != 0) {
            // Si ya hay audio reproduciéndose, poner en cola
            audioQueue.add(soundResource)
        }
    }

    private fun processAudioQueue() {
        if (audioQueue.isNotEmpty() && !isPlayingAudio) {
            val nextSound = audioQueue.removeAt(0)
            playQuestionSound(nextSound)
        }
    }

    private fun stopAllAudio() {
        currentMediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
            currentMediaPlayer = null
        }
        isPlayingAudio = false
        audioQueue.clear()
    }

    private fun checkAnswer(selectedShape: Shape, cardView: CardView) {
        // Detener cualquier audio actual antes de mostrar el diálogo
        stopAllAudio()

        if (selectedShape.name == currentTargetShape!!.name) {
            // Respuesta correcta
            score += 10
            showCorrectAnimation(cardView)
            showCorrectModal()
            createConfettiEffect()

            audioHandler.postDelayed({
                playCorrectSound()
            }, 300)
        } else {
            // Respuesta incorrecta
            lives--
            showIncorrectAnimation(cardView)
            showIncorrectModal()

            audioHandler.postDelayed({
                playIncorrectSound()
            }, 300)
        }
        updateStats()
    }

    private fun playCorrectSound() {
        if (!isPlayingAudio) {
            playQuestionSound(R.raw.correct_sound)
        } else {
            audioQueue.add(R.raw.correct_sound)
        }
    }

    private fun playIncorrectSound() {
        if (!isPlayingAudio) {
            playQuestionSound(R.raw.incorrect_sound)
        } else {
            audioQueue.add(R.raw.incorrect_sound)
        }
    }

    private fun nextQuestion() {
        if (lives <= 0) {
            showGameOverDialog()
            return
        }

        if (currentQuestion < totalQuestions) {
            currentQuestion++
            generateQuestion()
            updateProgress()
        } else {
            showRoundCompleteDialog()
        }
    }

    private fun showCorrectAnimation(cardView: CardView) {
        val anim = ObjectAnimator.ofFloat(cardView, "scaleX", 1f, 1.2f, 1f)
        anim.duration = 600
        anim.start()

        cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.correct_color))
        Handler(Looper.getMainLooper()).postDelayed({
            cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_background))
        }, 600)
    }

    private fun showIncorrectAnimation(cardView: CardView) {
        val anim = ObjectAnimator.ofFloat(cardView, "translationX", 0f, -10f, 10f, -10f, 10f, 0f)
        anim.duration = 500
        anim.start()

        cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.incorrect_color))
        Handler(Looper.getMainLooper()).postDelayed({
            cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_background))
        }, 500)
    }

    private fun showCorrectModal() {
        // Detener audio antes de mostrar el diálogo
        stopAllAudio()

        val dialog = AlertDialog.Builder(this)
            .setTitle("🎉 ¡Correcto!")
            .setMessage("¡Excelente! Encontraste el ${currentTargetShape!!.name}")
            .setPositiveButton("¡Siguiente!") { dialog, _ ->
                dialog.dismiss()

                audioHandler.postDelayed({
                    nextQuestion()
                }, 500)
            }
            .setCancelable(false)
            .create()

        dialog.setOnDismissListener {

            stopAllAudio()
        }
        dialog.show()
    }

    private fun showIncorrectModal() {

        stopAllAudio()

        val dialog = AlertDialog.Builder(this)
            .setTitle("💪 ¡Sigue intentando!")
            .setMessage("Esa no es la forma correcta")
            .setPositiveButton("Siguiente Pregunta") { dialog, _ ->
                dialog.dismiss()
                // Esperar a que termine el diálogo antes de pasar a la siguiente pregunta
                audioHandler.postDelayed({
                    nextQuestion()
                }, 500)
            }
            .setCancelable(false)
            .create()

        dialog.setOnDismissListener {

            stopAllAudio()
        }
        dialog.show()
    }

    private fun showRoundCompleteDialog() {
        stopAllAudio()

        val dialog = AlertDialog.Builder(this)
            .setTitle("🌟 ¡Ronda Completada!")
            .setMessage("Has completado todas las preguntas\nPuntos ganados: $score")
            .setPositiveButton("Nueva Ronda") { dialog, _ ->
                dialog.dismiss()
                currentQuestion = 1
                // Delay para asegurar que el diálogo se cierre completamente
                audioHandler.postDelayed({
                    generateQuestion()
                    updateProgress()
                }, 300)
            }
            .setNegativeButton("Salir") { dialog, _ ->
                dialog.dismiss()
                saveBestScore()
                finish()
            }
            .setCancelable(false)
            .create()

        dialog.setOnDismissListener {
            stopAllAudio()
        }


        audioHandler.postDelayed({
            playLevelCompleteSound()
        }, 300)

        dialog.show()
    }

    private fun playLevelCompleteSound() {
        if (!isPlayingAudio) {
            playQuestionSound(R.raw.level_complete_sound)
        } else {
            audioQueue.add(R.raw.level_complete_sound)
        }
    }

    private fun showGameOverDialog() {
        stopAllAudio()

        val dialog = AlertDialog.Builder(this)
            .setTitle("🎮 ¡Se acabaron las vidas!")
            .setMessage("Puntuación: $score puntos\nMejor puntuación: $bestScore")
            .setPositiveButton("Jugar otra vez") { dialog, _ ->
                dialog.dismiss()
                startGame()
                updateProgress()
            }
            .setNegativeButton("Salir") { dialog, _ ->
                dialog.dismiss()
                saveBestScore()
                finish()
            }
            .setCancelable(false)
            .create()

        dialog.setOnDismissListener {
            stopAllAudio()
        }


        audioHandler.postDelayed({
            playGameOverSound()
        }, 300)

        dialog.show()

        if (score > bestScore) {
            bestScore = score
            saveBestScore()
        }
    }

    private fun playGameOverSound() {
        if (!isPlayingAudio) {
            playQuestionSound(R.raw.game_over_sound)
        } else {
            audioQueue.add(R.raw.game_over_sound)
        }
    }

    private fun updateStats() {
        scoreText.text = score.toString()
        livesText.text = lives.toString()
        questionCounterText.text = "$currentQuestion/$totalQuestions"

        if (score > bestScore) {
            bestScore = score
            bestScoreText.text = bestScore.toString()
            saveBestScore()
        }
    }

    private fun updateProgress() {
        val progress = (currentQuestion.toFloat() / totalQuestions) * 100
        progressBar.progress = progress.toInt()
    }

    private fun setupClickListeners() {
        closeBtn.setOnClickListener {
            showExitModal()
        }

        nightModeBtn.setOnClickListener {
            toggleNightMode()
        }
    }

    private fun showExitModal() {
        stopAllAudio()

        val dialog = AlertDialog.Builder(this)
            .setTitle("🚪 ¿Salir del juego?")
            .setMessage("Tu progreso se guardará automáticamente")
            .setPositiveButton("Seguir jugando") { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton("Salir") { dialog, _ ->
                saveBestScore()
                finish()
            }
            .create()

        dialog.setOnDismissListener {
            stopAllAudio()
        }
        dialog.show()
    }

    private fun toggleNightMode() {
        isNightMode = !isNightMode

        if (isNightMode) {
            mainContainer.setBackgroundColor(Color.parseColor("#2C3E50"))
            val textColor = Color.WHITE
            val textViews = listOf(scoreText, bestScoreText, livesText, questionCounterText, questionDisplayText)
            textViews.forEach { it.setTextColor(textColor) }
        } else {
            mainContainer.setBackgroundResource(R.drawable.gradient_background)
            val textColor = Color.BLACK
            val textViews = listOf(scoreText, bestScoreText, livesText, questionCounterText, questionDisplayText)
            textViews.forEach { it.setTextColor(textColor) }
        }

        nightModeBtn.text = if (isNightMode) "☀️ Modo Día" else "🌙 Modo Noche"

        Toast.makeText(this,
            if (isNightMode) "🌙 Modo nocturno activado" else "☀️ Modo diurno activado",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun createConfettiEffect() {
        Toast.makeText(this, "🎉 ¡Correcto! +10 puntos", Toast.LENGTH_SHORT).show()
    }
}