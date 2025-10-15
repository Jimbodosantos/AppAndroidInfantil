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
import kotlinx.coroutines.*

class ContarAnimales : AppCompatActivity() {

    // Variables del juego
    private var score = 0
    private var bestScore = 0
    private var lives = 3
    private var currentQuestion = 1
    private val totalQuestions = 10
    private var correctAnswer = 0
    private var isNightMode = false

    // Vistas
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

    // Coroutine scope para manejar tareas asíncronas
    private val scope = MainScope()

    // Animales del juego
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

        // Inicializar vistas
        initViews()

        // Cargar mejor puntuación
        loadBestScore()

        // Configurar listeners
        setupListeners()

        // Configurar elementos visuales
        setupVisuals()

        // Iniciar juego
        startNewGame()
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

        // Configurar GridView para 4 columnas
        animalsGrid.numColumns = 4
        animalsGrid.verticalSpacing = 8.dpToPx()
        animalsGrid.horizontalSpacing = 8.dpToPx()

        // Configurar GridLayout para números
        numberPad.columnCount = 5
        numberPad.rowCount = 2
    }

    private fun setupVisuals() {
        // Configurar bordes redondeados
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

        // Configurar barra de progreso
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
        // Limpiar grids
        animalsGrid.adapter = null
        numberPad.removeAllViews()

        // Generar cantidad de animales (3-10)
        val animalCount = (3..10).random()
        correctAnswer = animalCount

        // Configurar pregunta
        val questions = listOf(
            "¿Cuántos animales hay en total?",

            "¿Cuántos amigos animales ves?",

            "¿Qué número representa la cantidad?"
        )
        questionText.text = questions.random()

        // Crear lista de animales
        val animalList = mutableListOf<Animal>()
        repeat(animalCount) {
            animalList.add(animals.random())
        }

        // Configurar GridView de animales
        setupAnimalsGrid(animalList)

        // Configurar teclado numérico
        setupNumberPad(animalCount)

        updateProgress()
    }

    private fun setupAnimalsGrid(animalList: List<Animal>) {
        val adapter = object : BaseAdapter() {
            override fun getCount(): Int = animalList.size
            override fun getItem(position: Int): Any = animalList[position]
            override fun getItemId(position: Int): Long = position.toLong()

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val animal = getItem(position) as Animal

                // Reutilizar view si existe
                val view = convertView ?: layoutInflater.inflate(R.layout.animal_card_item, parent, false)

                // Configurar la vista
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

        // Ajustar altura del GridView basado en el número de filas necesarias
        val numRows = Math.ceil(animalList.size / 4.0).toInt()
        val rowHeight = 80.dpToPx() // Altura de cada fila
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
        if (selectedNumber == correctAnswer) {
            // Respuesta correcta
            score += 10
            button.setBackgroundColor(Color.parseColor("#4ECDC4"))
            showCorrectDialog()
            createConfettiEffect()
        } else {
            // Respuesta incorrecta
            lives--
            button.setBackgroundColor(Color.parseColor("#FF6B6B"))
            showIncorrectDialog()
        }
        updateStats()

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
            // Modo Noche - Fondo oscuro sólido
            mainContainer.setBackgroundColor(Color.parseColor("#2C3E50"))

            // Cambiar color del texto para mejor contraste en modo noche
            val textColor = Color.WHITE
            val textViews = listOf(scoreText, bestScoreText, livesText, questionProgressText, questionText)
            textViews.forEach { it.setTextColor(textColor) }



        } else {
            // Modo Día - Usa gradiente
            mainContainer.setBackgroundResource(R.drawable.gradient_background)

            // Cambiar color del texto para mejor contraste en modo día
            val textColor = Color.BLACK
            val textViews = listOf(scoreText, bestScoreText, livesText, questionProgressText, questionText)
            textViews.forEach { it.setTextColor(textColor) }

        }

        // Actualizar texto del botón
        nightModeBtn.text = if (isNightMode) "☀️ Modo Día" else "🌙 Modo Noche"
    }

    // DIÁLOGOS (MODALES)
    private fun showCorrectDialog() {
        AlertDialog.Builder(this)
            .setTitle("🎉 ¡Correcto!")
            .setMessage("¡Excelente trabajo! Eran $correctAnswer animales")
            .setPositiveButton("¡Siguiente!") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun showIncorrectDialog() {
        AlertDialog.Builder(this)
            .setTitle("💪 ¡Sigue intentando!")
            .setMessage("Eran $correctAnswer animales. ¡Tú puedes!")
            .setPositiveButton("Siguiente Pregunta") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun showGameOverDialog() {
        AlertDialog.Builder(this)
            .setTitle("🎮 ¡Se acabaron las vidas!")
            .setMessage("Puntuación: $score puntos\nMejor puntuación: $bestScore")
            .setPositiveButton("Jugar otra vez") { dialog, _ ->
                dialog.dismiss()
                startNewGame()
            }
            .setNegativeButton("Salir") { dialog, _ ->
                dialog.dismiss()
                saveBestScore()
                finish()
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
            }
            .setNegativeButton("Salir") { dialog, _ ->
                dialog.dismiss()
                saveBestScore()
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showExitDialog() {
        AlertDialog.Builder(this)
            .setTitle("🚪 ¿Salir del juego?")
            .setMessage("Tu progreso se guardará automáticamente")
            .setPositiveButton("Seguir jugando") { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton("Salir") { dialog, _ ->
                dialog.dismiss()
                saveBestScore()
                finish()
            }
            .setCancelable(true)
            .show()
    }

    private fun createConfettiEffect() {
        Toast.makeText(this, "🎉 ¡Correcto! +10 puntos", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    // Extensión para convertir dp a px
    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}