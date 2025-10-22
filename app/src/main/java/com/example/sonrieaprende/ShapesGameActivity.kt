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

    // Datos de las formas
    private val shapes = listOf(
        Shape("círculo", "⭕", listOf("círculo", "redonda", "bola")),
        Shape("cuadrado", "⬜", listOf("cuadrado", "cuadrada", "caja")),
        Shape("triángulo", "🔺", listOf("triángulo", "triangular", "pirámide")),
        Shape("rectángulo", "📏", listOf("rectángulo", "rectangular", "tabla")),
        Shape("estrella", "⭐", listOf("estrella", "estrellada", "brillante")),
        Shape("corazón", "❤️", listOf("corazón", "amor", "corazoncito")),
        Shape("rombo", "💠", listOf("rombo", "diamante", "rombito")),
        Shape("óvalo", "🥚", listOf("óvalo", "ovalada", "huevo"))
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

        // Configurar parámetros del GridLayout para 2x2
        val rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        val colSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)

        // Crear cartas de formas
        shapeOptions.forEachIndexed { index, shape ->
            val shapeCard = LayoutInflater.from(this).inflate(R.layout.shape_card, shapesGrid, false)
            val shapeIcon = shapeCard.findViewById<TextView>(R.id.shapeIcon)
            val cardView = shapeCard.findViewById<CardView>(R.id.shapeCard)

            shapeIcon.text = shape.display

            // Configurar parámetros del GridLayout
            val params = GridLayout.LayoutParams(rowSpec, colSpec)
            params.width = 0
            params.height = GridLayout.LayoutParams.WRAP_CONTENT
            params.setMargins(8, 8, 8, 8)
            shapeCard.layoutParams = params

            // Configurar click listener
            cardView.setOnClickListener {
                checkAnswer(shape, cardView)
            }

            shapesGrid.addView(shapeCard)

        }

        // Actualizar pregunta
        val questionTypes = listOf(
            "Toca el ${currentTargetShape!!.name}",
            "¿Dónde está el ${currentTargetShape!!.name}?",
            "Encuentra la forma ${currentTargetShape!!.name}",
            "Selecciona el ${currentTargetShape!!.name}",
            "¿Cuál es el ${currentTargetShape!!.name}?"
        )

        questionDisplayText.text = questionTypes.random()

    }

    private fun checkAnswer(selectedShape: Shape, cardView: CardView) {
        if (selectedShape.name == currentTargetShape!!.name) {
            // Respuesta correcta
            score += 10
            showCorrectAnimation(cardView)
            showCorrectModal()
            createConfettiEffect()

        } else {
            // Respuesta incorrecta
            lives--
            showIncorrectAnimation(cardView)
            showIncorrectModal()
        }
        updateStats()

        // Siguiente pregunta después de un delay
        Handler(Looper.getMainLooper()).postDelayed({
            nextQuestion()
        }, 1500)
    }

    private fun nextQuestion() {
        if (lives <= 0) {
            showGameOverDialog()
            return
        }

        if (currentQuestion < totalQuestions) {
            currentQuestion++
            generateQuestion()
        } else {
            showRoundCompleteDialog()
        }
    }

    private fun showCorrectAnimation(cardView: CardView) {
        val anim = ObjectAnimator.ofFloat(cardView, "scaleX", 1f, 1.2f, 1f)
        anim.duration = 600
        anim.start()

        // Cambiar color de fondo temporalmente
        cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.correct_color))
        Handler(Looper.getMainLooper()).postDelayed({
            cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_background))
        }, 600)
    }

    private fun showIncorrectAnimation(cardView: CardView) {
        val anim = ObjectAnimator.ofFloat(cardView, "translationX", 0f, -10f, 10f, -10f, 10f, 0f)
        anim.duration = 500
        anim.start()

        // Cambiar color de fondo temporalmente
        cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.incorrect_color))
        Handler(Looper.getMainLooper()).postDelayed({
            cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_background))
        }, 500)
    }

    private fun showCorrectModal() {
        AlertDialog.Builder(this)
            .setTitle("🎉 ¡Correcto!")
            .setMessage("¡Excelente! Encontraste el ${currentTargetShape!!.name}")
            .setPositiveButton("¡Siguiente!") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
        updateProgress()

    }

    private fun showIncorrectModal() {
        AlertDialog.Builder(this)
            .setTitle("💪 ¡Sigue intentando!")
            .setMessage("Esa no es la forma correcta")
            .setPositiveButton("Siguiente Pregunta") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun showRoundCompleteDialog() {
        AlertDialog.Builder(this)
            .setTitle("🌟 ¡Ronda Completada!")
            .setMessage("Has completado todas las preguntas\nPuntos ganados: $score")
            .setPositiveButton("Nueva Ronda") { dialog, _ ->
                dialog.dismiss()
                currentQuestion = 1
                generateQuestion()
                updateProgress()
            }
            .setNegativeButton("Salir") { dialog, _ ->
                dialog.dismiss()
                saveBestScore()
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showGameOverDialog() {
        if (score > bestScore) {
            bestScore = score
            saveBestScore()
        }

        AlertDialog.Builder(this)
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
            .show()
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
        AlertDialog.Builder(this)
            .setTitle("🚪 ¿Salir del juego?")
            .setMessage("Tu progreso se guardará automáticamente")
            .setPositiveButton("Seguir jugando") { dialog, _ -> dialog.dismiss() }
            .setNegativeButton("Salir") { dialog, _ ->
                saveBestScore()
                finish()
            }
            .show()
    }

    private fun toggleNightMode() {
        isNightMode = !isNightMode

        if (isNightMode) {
            // Modo Noche

            mainContainer.setBackgroundColor(Color.parseColor("#2C3E50"))
            val textColor = Color.WHITE
            val textViews = listOf(scoreText, bestScoreText, livesText, questionCounterText, questionDisplayText)
            textViews.forEach { it.setTextColor(textColor) }
        } else {
            // Modo Día
            mainContainer.setBackgroundResource(R.drawable.gradient_background)
            val textColor = Color.BLACK
            val textViews = listOf(scoreText, bestScoreText, livesText, questionCounterText, questionDisplayText)
            textViews.forEach { it.setTextColor(textColor) }
        }

        // Actualizar texto del botón
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