package com.example.sonrieaprende

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class ContarAnimales : AppCompatActivity() {

    private var score = 0
    private var bestScore = 0
    private var lives = 3
    private var currentQuestion = 1
    private val totalQuestions = 10
    private var correctAnswer = 0
    private var isNightMode = false

    private lateinit var scoreText: TextView
    private lateinit var bestScoreText: TextView
    private lateinit var livesText: TextView
    private lateinit var questionProgressText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var questionText: TextView
    private lateinit var animalsGrid: GridView
    private lateinit var numberPad: GridLayout
    private lateinit var closeBtn: ImageButton
    private lateinit var nightModeBtn: Button
    private lateinit var statsBar: LinearLayout
    private lateinit var mainContainer: LinearLayout

    private var currentMediaPlayer: android.media.MediaPlayer? = null
    private val audioHandler = Handler(Looper.getMainLooper())
    private var currentDialog: AlertDialog? = null

    private val animals = listOf(
        Animal("Vaca", "🐮", "Muuu!"),
        Animal("Cerdo", "🐷", "Oink!"),
        Animal("Gallina", "🐔", "Kikiriki!"),
        Animal("Oveja", "🐑", "Beee!"),
        Animal("Caballo", "🐴", "Relincho!"),
        Animal("Pato", "🦆", "Cuac!"),
        Animal("Conejo", "🐰", "Sniff!"),
        Animal("Gato", "🐱", "Miau!"),
        Animal("Perro", "🐶", "Guau!"),
        Animal("Cabrita", "🐐", "Meee!")
    )

    data class Animal(val name: String, val icon: String, val sound: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.contaranimales)
        initViews()
        loadBestScore()
        setupListeners()
        setupVisuals()
        startNewGame()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAllAudio()
        audioHandler.removeCallbacksAndMessages(null)
        currentDialog?.dismiss()
    }

    private fun initViews() {
        scoreText = findViewById(R.id.scoreText)
        bestScoreText = findViewById(R.id.bestScoreText)
        livesText = findViewById(R.id.livesText)
        questionProgressText = findViewById(R.id.questionProgressText)
        progressBar = findViewById(R.id.progressBar)
        questionText = findViewById(R.id.questionText)
        animalsGrid = findViewById(R.id.animalsGrid)
        numberPad = findViewById(R.id.numberPad)
        closeBtn = findViewById(R.id.closeBtn)
        nightModeBtn = findViewById(R.id.nightModeBtn)
        statsBar = findViewById(R.id.statsBar)
        mainContainer = findViewById(R.id.mainContainer)

        animalsGrid.numColumns = 4
        animalsGrid.verticalSpacing = 8.dpToPx()
        animalsGrid.horizontalSpacing = 8.dpToPx()
        numberPad.columnCount = 5
        numberPad.rowCount = 2
    }

    private fun setupVisuals() {
        val statsBg = GradientDrawable().apply {
            setColor(Color.parseColor("#FFD700"))
            cornerRadius = 20f
        }
        statsBar.background = statsBg

        val questionBg = GradientDrawable().apply {
            setColor(Color.parseColor("#FF6B6B"))
            cornerRadius = 15f
        }
        questionText.background = questionBg

        val nightModeBg = GradientDrawable().apply {
            setColor(Color.parseColor("#FF6B6B"))
            cornerRadius = 25f
        }
        nightModeBtn.background = nightModeBg

        progressBar.progressDrawable?.setColorFilter(Color.parseColor("#FF5722"), android.graphics.PorterDuff.Mode.SRC_IN)
    }

    private fun loadBestScore() {
        val sharedPref = getPreferences(MODE_PRIVATE)
        bestScore = sharedPref.getInt("best_score", 0)
        bestScoreText.text = bestScore.toString()
    }

    private fun saveBestScore() {
        val sharedPref = getPreferences(MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("best_score", bestScore)
            apply()
        }
    }

    private fun setupListeners() {
        closeBtn.setOnClickListener { showExitDialog() }
        nightModeBtn.setOnClickListener { toggleNightMode() }
    }

    private fun startNewGame() {
        score = 0
        lives = 3
        currentQuestion = 1
        updateStats()
        generateQuestion()
    }

    private fun generateQuestion() {
        animalsGrid.adapter = null
        numberPad.removeAllViews()

        val animalCount = (3..10).random()
        correctAnswer = animalCount

        val availableQuestions = getAvailableQuestions()

        val selectedQuestion = if (availableQuestions.isNotEmpty()) {
            availableQuestions.random()
        } else {
            "¿Cuántos animales hay en total?"
        }

        questionText.text = selectedQuestion

        val animalList = mutableListOf<Animal>()
        repeat(animalCount) {
            animalList.add(animals.random())
        }

        setupAnimalsGrid(animalList)
        setupNumberPad(animalCount)

        audioHandler.postDelayed({
            playQuestionSound()
        }, 500)
    }

    private fun getAvailableQuestions(): List<String> {
        val availableQuestions = mutableListOf<String>()

        val possibleQuestions = listOf(
            "¿Cuántos animales hay en total?" to R.raw.cuantosanimaleshayentotal,
            "¿Cuántos amigos animales ves?" to R.raw.cuantosamigosanimalesves,
            "¿Qué número representa la cantidad?" to R.raw.cuantosanimalesmirasaqui
        )

        possibleQuestions.forEach { (question, soundResource) ->
            if (resourceExists(soundResource)) {
                availableQuestions.add(question)
            }
        }

        return availableQuestions
    }

    private fun playQuestionSound() {
        val question = questionText.text.toString()
        val soundResource = when (question) {
            "¿Cuántos animales hay en total?" -> R.raw.cuantosanimaleshayentotal
            "¿Cuántos amigos animales ves?" -> R.raw.cuantosamigosanimalesves
            "¿Qué número representa la cantidad?" -> R.raw.cuantosanimalesmirasaqui
            else -> 0
        }

        if (soundResource != 0) {
            playSound(soundResource)
        }
    }

    private fun resourceExists(resourceId: Int): Boolean {
        return try {
            val resources = this.resources
            resources.openRawResource(resourceId).close()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun setupAnimalsGrid(animalList: List<Animal>) {
        val adapter = object : BaseAdapter() {
            override fun getCount(): Int = animalList.size
            override fun getItem(position: Int): Any = animalList[position]
            override fun getItemId(position: Int): Long = position.toLong()

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val animal = getItem(position) as Animal
                val view = convertView ?: layoutInflater.inflate(R.layout.animal_card_item, parent, false)

                val iconText = view.findViewById<TextView>(R.id.animalIcon)
                val nameText = view.findViewById<TextView>(R.id.animalName)

                iconText.text = animal.icon
                nameText.text = animal.name

                view.setOnClickListener {
                    showAnimalSound(animal.sound)
                }

                return view
            }
        }

        animalsGrid.adapter = adapter

        val numRows = Math.ceil(animalList.size / 4.0).toInt()
        val rowHeight = 80.dpToPx()
        val params = animalsGrid.layoutParams
        params.height = rowHeight * numRows
        animalsGrid.layoutParams = params
    }

    private fun setupNumberPad(correctNumber: Int) {
        val options = generateNumberOptions(correctNumber)

        for (i in 0 until 10) {
            val number = options[i]
            val button = Button(this).apply {
                text = number.toString()
                setBackgroundColor(Color.parseColor("#667eea"))
                setTextColor(Color.WHITE)
                textSize = 16f
                isAllCaps = false
                gravity = Gravity.CENTER

                val params = GridLayout.LayoutParams().apply {
                    width = 50.dpToPx()
                    height = 50.dpToPx()
                    columnSpec = GridLayout.spec(i % 5)
                    rowSpec = GridLayout.spec(i / 5)
                    setMargins(3.dpToPx(), 3.dpToPx(), 3.dpToPx(), 3.dpToPx())
                }

                layoutParams = params

                setOnClickListener {
                    checkAnswer(number, this)
                }
            }
            numberPad.addView(button)
        }

        numberPad.requestLayout()
    }

    private fun generateNumberOptions(correct: Int): List<Int> {
        val options = mutableListOf(correct)
        while (options.size < 10) {
            val randomNum = (1..15).random()
            if (randomNum != correct && randomNum !in options) {
                options.add(randomNum)
            }
        }
        return options.shuffled()
    }

    private fun checkAnswer(selectedNumber: Int, button: Button) {
        stopAllAudio()

        if (selectedNumber == correctAnswer) {
            score += 10
            button.setBackgroundColor(Color.parseColor("#4ECDC4"))
            playSoundImmediately(R.raw.correct_sound)
            showCorrectDialog()
            createConfettiEffect()
        } else {
            lives--
            button.setBackgroundColor(Color.parseColor("#FF6B6B"))
            playSoundImmediately(R.raw.incorrect_sound)
            showIncorrectDialog()
        }
        updateStats()
    }

    private fun playSoundImmediately(soundResource: Int) {
        try {
            // Detener cualquier sonido anterior
            currentMediaPlayer?.release()

            // Crear y reproducir nuevo sonido
            currentMediaPlayer = android.media.MediaPlayer.create(this, soundResource)
            currentMediaPlayer?.setOnCompletionListener {
                it.release()
                currentMediaPlayer = null
            }
            currentMediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playSound(soundResource: Int) {
        if (soundResource != 0) {
            try {
                currentMediaPlayer?.release()
                currentMediaPlayer = android.media.MediaPlayer.create(this, soundResource)
                currentMediaPlayer?.setOnCompletionListener {
                    it.release()
                    currentMediaPlayer = null
                }
                currentMediaPlayer?.start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
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

    private fun updateStats() {
        scoreText.text = score.toString()
        livesText.text = lives.toString()
        questionProgressText.text = "$currentQuestion/$totalQuestions"

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

    private fun showAnimalSound(sound: String) {
        Toast.makeText(this, sound, Toast.LENGTH_SHORT).show()
    }

    private fun toggleNightMode() {
        isNightMode = !isNightMode

        if (isNightMode) {
            mainContainer.setBackgroundColor(Color.parseColor("#2C3E50"))
            val textColor = Color.WHITE
            val textViews = listOf(scoreText, bestScoreText, livesText, questionProgressText, questionText)
            textViews.forEach { it.setTextColor(textColor) }
        } else {
            mainContainer.setBackgroundResource(R.drawable.gradient_background)
            val textColor = Color.BLACK
            val textViews = listOf(scoreText, bestScoreText, livesText, questionProgressText, questionText)
            textViews.forEach { it.setTextColor(textColor) }
        }

        nightModeBtn.text = if (isNightMode) "☀️ Modo Día" else "🌙 Modo Noche"
    }

    private fun showCorrectDialog() {
        currentDialog?.dismiss()

        val dialog = AlertDialog.Builder(this)
            .setTitle("🎉 ¡Correcto!")
            .setMessage("¡Excelente trabajo! Eran $correctAnswer animales")
            .setPositiveButton("¡Siguiente!") { dialog, _ ->
                dialog.dismiss()
                currentDialog = null
                nextQuestion()
            }
            .setCancelable(false)
            .create()

        dialog.setOnDismissListener {
            currentDialog = null
        }

        // Mostrar el diálogo después de un pequeño delay para que se escuche el sonido
        audioHandler.postDelayed({
            currentDialog = dialog
            dialog.show()
        }, 800)
    }

    private fun showIncorrectDialog() {
        currentDialog?.dismiss()

        val dialog = AlertDialog.Builder(this)
            .setTitle("💪 ¡Sigue intentando!")
            .setMessage("Eran $correctAnswer animales. ¡Tú puedes!")
            .setPositiveButton("Siguiente Pregunta") { dialog, _ ->
                dialog.dismiss()
                currentDialog = null
                nextQuestion()
            }
            .setCancelable(false)
            .create()

        dialog.setOnDismissListener {
            currentDialog = null
        }

        // Mostrar el diálogo después de un pequeño delay para que se escuche el sonido
        audioHandler.postDelayed({
            currentDialog = dialog
            dialog.show()
        }, 800)
    }

    private fun showGameOverDialog() {
        currentDialog?.dismiss()

        val dialog = AlertDialog.Builder(this)
            .setTitle("🎮 ¡Se acabaron las vidas!")
            .setMessage("Puntuación: $score puntos\nMejor puntuación: $bestScore")
            .setPositiveButton("Jugar otra vez") { dialog, _ ->
                dialog.dismiss()
                currentDialog = null
                startNewGame()
                updateProgress()
            }
            .setNegativeButton("Salir") { dialog, _ ->
                dialog.dismiss()
                currentDialog = null
                saveBestScore()
                finish()
            }
            .setCancelable(false)
            .create()

        audioHandler.postDelayed({
            playSound(R.raw.game_over_sound)
        }, 300)

        currentDialog = dialog
        dialog.show()
    }

    private fun showRoundCompleteDialog() {
        currentDialog?.dismiss()

        val dialog = AlertDialog.Builder(this)
            .setTitle("🌟 ¡Ronda Completada!")
            .setMessage("Has completado todas las preguntas\nPuntos ganados: $score")
            .setPositiveButton("Nueva Ronda") { dialog, _ ->
                dialog.dismiss()
                currentDialog = null
                currentQuestion = 1
                audioHandler.postDelayed({
                    generateQuestion()
                    updateProgress()
                }, 300)
            }
            .setNegativeButton("Salir") { dialog, _ ->
                dialog.dismiss()
                currentDialog = null
                saveBestScore()
                finish()
            }
            .setCancelable(false)
            .create()

        audioHandler.postDelayed({
            playSound(R.raw.level_complete_sound)
        }, 300)

        currentDialog = dialog
        dialog.show()
    }

    private fun showExitDialog() {
        currentDialog?.dismiss()

        val dialog = AlertDialog.Builder(this)
            .setTitle("🚪 ¿Salir del juego?")
            .setMessage("Tu progreso se guardará automáticamente")
            .setPositiveButton("Seguir jugando") { dialog, _ ->
                dialog.dismiss()
                currentDialog = null
            }
            .setNegativeButton("Salir") { dialog, _ ->
                dialog.dismiss()
                currentDialog = null
                saveBestScore()
                finish()
            }
            .create()

        currentDialog = dialog
        dialog.show()
    }

    private fun createConfettiEffect() {
        Toast.makeText(this, "🎉 ¡Correcto! +10 puntos", Toast.LENGTH_SHORT).show()
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}