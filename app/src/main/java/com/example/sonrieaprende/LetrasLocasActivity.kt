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

class LetrasLocasActivity : AppCompatActivity() {

    // Variables del juego
    private lateinit var scoreText: TextView
    private lateinit var bestScoreText: TextView
    private lateinit var livesText: TextView
    private lateinit var questionCounterText: TextView
    private lateinit var soundButton: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var lettersGrid: GridLayout
    private lateinit var closeBtn: ImageButton
    private lateinit var nightModeBtn: Button
    private lateinit var mainContainer: LinearLayout

    private var score = 0
    private var bestScore = 0
    private var lives = 3
    private var currentQuestion = 1
    private val totalQuestions = 10
    private var currentTargetLetter: Letter? = null
    private var isNightMode = false

    // audio
    private var currentMediaPlayer: android.media.MediaPlayer? = null
    private val audioHandler = Handler(Looper.getMainLooper())
    private var isPlayingAudio = false
    private val audioQueue = mutableListOf<Int>()

    // Datos de las letras
    private val letters = listOf(
        Letter("F", R.raw.f, R.drawable.ff),
        Letter("X", R.raw.x, R.drawable.xx),
        Letter("H", R.raw.h, R.drawable.hh),
        Letter("U", R.raw.u, R.drawable.uu),
        Letter("M", R.raw.m, R.drawable.mm),
        Letter("I", R.raw.i, R.drawable.ii),
        Letter("Q", R.raw.q, R.drawable.qq),
        Letter("E", R.raw.e, R.drawable.ee),
        Letter("T", R.raw.t, R.drawable.tt),
        Letter("W", R.raw.w, R.drawable.ww),
        Letter("Z", R.raw.z, R.drawable.zz),
        Letter("R", R.raw.r, R.drawable.rr),
        Letter("Y", R.raw.y, R.drawable.yy),
        Letter("D", R.raw.d, R.drawable.dd),
        Letter("S", R.raw.s, R.drawable.ss),
        Letter("Ñ", R.raw.n_tilde, R.drawable.nn_tilde),
        Letter("C", R.raw.c, R.drawable.cc),
        Letter("K", R.raw.k, R.drawable.kk),
        Letter("L", R.raw.l, R.drawable.ll),
        Letter("J", R.raw.j, R.drawable.jj),
        Letter("G", R.raw.g, R.drawable.gg),
        Letter("N", R.raw.n, R.drawable.nn),
        Letter("A", R.raw.a, R.drawable.aa),
        Letter("P", R.raw.p, R.drawable.pp),
        Letter("V", R.raw.v, R.drawable.vv),
        Letter("B", R.raw.b, R.drawable.bb),
        Letter("O", R.raw.o, R.drawable.oo)
    )

    data class Letter(
        val character: String,
        val soundResource: Int,
        val imageResource: Int
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_letras_locas)

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
        soundButton = findViewById(R.id.soundButton)
        progressBar = findViewById(R.id.progressBar)
        lettersGrid = findViewById(R.id.lettersGrid)
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
        lettersGrid.removeAllViews()

        // Seleccionar letra objetivo
        currentTargetLetter = letters.random()

        // Crear opciones (letra objetivo + 3 aleatorias)
        val letterOptions = mutableListOf(currentTargetLetter!!)
        while (letterOptions.size < 4) {
            val randomLetter = letters.random()
            if (!letterOptions.contains(randomLetter)) {
                letterOptions.add(randomLetter)
            }
        }

        // Mezclar opciones
        letterOptions.shuffle()

        val rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        val colSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)

        // Crear cartas de letras
        letterOptions.forEachIndexed { index, letter ->
            val letterCard = LayoutInflater.from(this).inflate(R.layout.letter_card, lettersGrid, false)
            val letterImage = letterCard.findViewById<ImageView>(R.id.letterImage)
            val cardView = letterCard.findViewById<CardView>(R.id.letterCard)

            // Establecer la imagen de la letra
            letterImage.setImageResource(letter.imageResource)

            val params = GridLayout.LayoutParams(rowSpec, colSpec)
            params.width = 0
            params.height = GridLayout.LayoutParams.WRAP_CONTENT
            params.setMargins(8, 8, 8, 8)
            letterCard.layoutParams = params

            cardView.setOnClickListener {
                checkAnswer(letter, cardView)
            }

            lettersGrid.addView(letterCard)
        }

        // Reproducir sonido de la letra después de un breve delay
        audioHandler.postDelayed({
            playLetterSound()
        }, 500)
    }

    private fun playLetterSound() {
        currentTargetLetter?.let { letter ->
            if (!isPlayingAudio) {
                playQuestionSound(letter.soundResource)
            } else {
                audioQueue.add(letter.soundResource)
            }
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

    private fun checkAnswer(selectedLetter: Letter, cardView: CardView) {
        stopAllAudio()

        if (selectedLetter.character == currentTargetLetter!!.character) {
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
        stopAllAudio()

        val dialog = AlertDialog.Builder(this)
            .setTitle("🎉 ¡Correcto!")
            .setMessage("¡Excelente! Encontraste la letra ${currentTargetLetter!!.character}")
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
            .setMessage("Esa no es la letra correcta")
            .setPositiveButton("Siguiente Pregunta") { dialog, _ ->
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

    private fun showRoundCompleteDialog() {
        stopAllAudio()

        val dialog = AlertDialog.Builder(this)
            .setTitle("🌟 ¡Ronda Completada!")
            .setMessage("Has completado todas las preguntas\nPuntos ganados: $score")
            .setPositiveButton("Nueva Ronda") { dialog, _ ->
                dialog.dismiss()
                currentQuestion = 1
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

        soundButton.setOnClickListener {
            playLetterSound()
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
            val textViews = listOf(scoreText, bestScoreText, livesText, questionCounterText)
            textViews.forEach { it.setTextColor(textColor) }
        } else {
            mainContainer.setBackgroundResource(R.drawable.gradient_background)
            val textColor = Color.BLACK
            val textViews = listOf(scoreText, bestScoreText, livesText, questionCounterText)
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