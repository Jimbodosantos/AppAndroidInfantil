package com.example.sonrieaprende

import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat

class EnglishGameActivity : AppCompatActivity() {

    // Variables del juego
    private lateinit var scoreText: TextView
    private lateinit var bestScoreText: TextView
    private lateinit var livesText: TextView
    private lateinit var questionCounterText: TextView
    private lateinit var questionDisplayText: TextView
    private lateinit var gameTitle: TextView
    private lateinit var gameSubtitle: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var optionsGrid: GridLayout
    private lateinit var closeBtn: ImageButton
    private lateinit var nightModeBtn: Button
    private lateinit var categoryBtn: Button
    private lateinit var mainContainer: LinearLayout

    private var score = 0
    private var bestScore = 0
    private var lives = 3
    private var currentQuestion = 1
    private val totalQuestions = 10
    private var currentCorrectAnswer = ""
    private var currentSpanishWord = ""
    private var isNightMode = false
    private var currentCategory = "animals"

    // Base de datos de vocabulario
    private val vocabulary = mapOf(

            "animals" to listOf(
                Word("perro", "Dog", "🐶"),
                Word("gato", "Cat", "🐱"),
                Word("pájaro", "Bird", "🐦"),
                Word("pez", "Fish", "🐠"),
                Word("conejo", "Rabbit", "🐰"),
                Word("vaca", "Cow", "🐮"),
                Word("cerdo", "Pig", "🐷"),
                Word("oveja", "Sheep", "🐑"),
                Word("caballo", "Horse", "🐴"),
                Word("gallina", "Chicken", "🐔"),
                Word("pato", "Duck", "🦆"),
                Word("mariposa", "Butterfly", "🦋"),
                Word("elefante", "Elephant", "🐘"),
                Word("león", "Lion", "🦁"),
                Word("tigre", "Tiger", "🐯"),
                Word("jirafa", "Giraffe", "🦒"),
                Word("mono", "Monkey", "🐵"),
                Word("oso", "Bear", "🐻"),
                Word("zorro", "Fox", "🦊"),
                Word("lobo", "Wolf", "🐺")
            ),
    "colors" to listOf(
    Word("rojo", "Red", "🔴"),
    Word("azul", "Blue", "🔵"),
    Word("verde", "Green", "🟢"),
    Word("amarillo", "Yellow", "🟡"),
    Word("naranja", "Orange", "🟠"),
    Word("morado", "Purple", "🟣"),
    Word("rosa", "Pink", "💗"),
    Word("marrón", "Brown", "🟤"),
    Word("negro", "Black", "⚫"),
    Word("blanco", "White", "⚪"),
    Word("gris", "Gray", "🔘"),
    Word("dorado", "Gold", "🌟"),
    Word("plateado", "Silver", "💿"),
    Word("celeste", "Sky Blue", "🌤️"),
    Word("violeta", "Violet", "🔮"),
    Word("turquesa", "Turquoise", "🧊"),
    Word("beige", "Beige", "🟫"),
    Word("azul marino", "Navy Blue", "🌊"),
    Word("verde lima", "Lime Green", "🍈"),
    Word("rojo oscuro", "Dark Red", "🍎")
    ),
    "numbers" to listOf(
    Word("uno", "One", "1️⃣"),
    Word("dos", "Two", "2️⃣"),
    Word("tres", "Three", "3️⃣"),
    Word("cuatro", "Four", "4️⃣"),
    Word("cinco", "Five", "5️⃣"),
    Word("seis", "Six", "6️⃣"),
    Word("siete", "Seven", "7️⃣"),
    Word("ocho", "Eight", "8️⃣"),
    Word("nueve", "Nine", "9️⃣"),
    Word("diez", "Ten", "🔟"),
    Word("once", "Eleven", "11"),
    Word("doce", "Twelve", "12"),
    Word("trece", "Thirteen", "13"),
    Word("catorce", "Fourteen", "14"),
    Word("quince", "Fifteen", "15"),
    Word("veinte", "Twenty", "20"),
    Word("cincuenta", "Fifty", "50"),
    Word("cien", "One Hundred", "💯"),
    Word("mil", "One Thousand", "1️⃣0️⃣0️⃣0️⃣"),
    Word("millón", "One Million", "💰")
    ),
    "food" to listOf(
    Word("manzana", "Apple", "🍎"),
    Word("plátano", "Banana", "🍌"),
    Word("naranja", "Orange", "🍊"),
    Word("leche", "Milk", "🥛"),
    Word("pan", "Bread", "🍞"),
    Word("queso", "Cheese", "🧀"),
    Word("agua", "Water", "💧"),
    Word("jugo", "Juice", "🧃"),
    Word("huevo", "Egg", "🥚"),
    Word("arroz", "Rice", "🍚"),
    Word("pollo", "Chicken", "🍗"),
    Word("pescado", "Fish", "🐟"),
    Word("carne", "Meat", "🥩"),
    Word("ensalada", "Salad", "🥗"),
    Word("sopa", "Soup", "🍲"),
    Word("pizza", "Pizza", "🍕"),
    Word("hamburguesa", "Hamburger", "🍔"),
    Word("helado", "Ice Cream", "🍦"),
    Word("pastel", "Cake", "🍰"),
    Word("chocolate", "Chocolate", "🍫")
    ),
    "family" to listOf(
    Word("mamá", "Mom", "👩"),
    Word("papá", "Dad", "👨"),
    Word("hermano", "Brother", "👦"),
    Word("hermana", "Sister", "👧"),
    Word("abuelo", "Grandpa", "👴"),
    Word("abuela", "Grandma", "👵"),
    Word("bebé", "Baby", "👶"),
    Word("familia", "Family", "👪"),
    Word("tío", "Uncle", "👨‍💼"),
    Word("tía", "Aunt", "👩‍💼"),
    Word("primo", "Cousin", "👦"),
    Word("prima", "Cousin", "👧"),
    Word("sobrino", "Nephew", "🧒"),
    Word("sobrina", "Niece", "👧"),
    Word("hijo", "Son", "👦"),
    Word("hija", "Daughter", "👧"),
    Word("esposo", "Husband", "👨"),
    Word("esposa", "Wife", "👩"),
    Word("padres", "Parents", "👨‍👩‍👧"),
    Word("hermanos", "Siblings", "👨‍👧‍👦")
    ),
    "transport" to listOf(
    Word("coche", "Car", "🚗"),
    Word("autobús", "Bus", "🚌"),
    Word("bicicleta", "Bicycle", "🚲"),
    Word("tren", "Train", "🚆"),
    Word("avión", "Airplane", "✈️"),
    Word("barco", "Boat", "🚢"),
    Word("motocicleta", "Motorcycle", "🏍️"),
    Word("helicóptero", "Helicopter", "🚁"),
    Word("camión", "Truck", "🚚"),
    Word("taxi", "Taxi", "🚕"),
    Word("ambulancia", "Ambulance", "🚑"),
    Word("bomberos", "Fire Truck", "🚒"),
    Word("policía", "Police Car", "🚓"),
    Word("metro", "Subway", "🚇"),
    Word("tranvía", "Tram", "🚊"),
    Word("globo", "Balloon", "🎈"),
    Word("cohete", "Rocket", "🚀"),
    Word("submarino", "Submarine", "🛸"),
    Word("yate", "Yacht", "⛵"),
    Word("carreta", "Cart", "🛺")
    ),
    "school" to listOf(
    Word("escuela", "School", "🏫"),
    Word("maestro", "Teacher", "👨‍🏫"),
    Word("estudiante", "Student", "👩‍🎓"),
    Word("libro", "Book", "📚"),
    Word("lápiz", "Pencil", "✏️"),
    Word("bolígrafo", "Pen", "🖊️"),
    Word("cuaderno", "Notebook", "📓"),
    Word("mochila", "Backpack", "🎒"),
    Word("pizarra", "Blackboard", "📋"),
    Word("tijeras", "Scissors", "✂️"),
    Word("goma", "Eraser", "🧼"),
    Word("regla", "Ruler", "📏"),
    Word("calculadora", "Calculator", "🧮"),
    Word("computadora", "Computer", "💻"),
    Word("papel", "Paper", "📄"),
    Word("clase", "Class", "👨‍🏫"),
    Word("examen", "Exam", "📝"),
    Word("tarea", "Homework", "📖"),
    Word("recreo", "Recess", "⚽"),
    Word("biblioteca", "Library", "📚")
    ),
    "body" to listOf(
    Word("cabeza", "Head", "👦"),
    Word("mano", "Hand", "✋"),
    Word("pie", "Foot", "🦶"),
    Word("ojo", "Eye", "👁️"),
    Word("nariz", "Nose", "👃"),
    Word("boca", "Mouth", "👄"),
    Word("oreja", "Ear", "👂"),
    Word("brazo", "Arm", "💪"),
    Word("pierna", "Leg", "🦵"),
    Word("dedo", "Finger", "👆"),
    Word("cabello", "Hair", "💇"),
    Word("cara", "Face", "😀"),
    Word("corazón", "Heart", "❤️"),
    Word("estómago", "Stomach", "🩹"),
    Word("espalda", "Back", "👤"),
    Word("rodilla", "Knee", "🦵"),
    Word("codo", "Elbow", "🦾"),
    Word("hombro", "Shoulder", "💪"),
    Word("cuello", "Neck", "👔"),
    Word("diente", "Tooth", "🦷")
    ),
    "clothes" to listOf(
    Word("camisa", "Shirt", "👕"),
    Word("pantalón", "Pants", "👖"),
    Word("vestido", "Dress", "👗"),
    Word("zapatos", "Shoes", "👟"),
    Word("sombrero", "Hat", "🧢"),
    Word("chaqueta", "Jacket", "🧥"),
    Word("calcetines", "Socks", "🧦"),
    Word("falda", "Skirt", "👗"),
    Word("suéter", "Sweater", "🥼"),
    Word("bufanda", "Scarf", "🧣"),
    Word("guantes", "Gloves", "🧤"),
    Word("abrigo", "Coat", "🧥"),
    Word("pijama", "Pajamas", "🛌"),
    Word("traje", "Suit", "👔"),
    Word("corbata", "Tie", "👔"),
    Word("botas", "Boots", "👢"),
    Word("sandalia", "Sandals", "👡"),
    Word("gafas", "Glasses", "👓"),
    Word("reloj", "Watch", "⌚"),
    Word("bolso", "Bag", "👜")
    ),
    "house" to listOf(
    Word("casa", "House", "🏠"),
    Word("puerta", "Door", "🚪"),
    Word("ventana", "Window", "🪟"),
    Word("cocina", "Kitchen", "👨‍🍳"),
    Word("baño", "Bathroom", "🚽"),
    Word("dormitorio", "Bedroom", "🛏️"),
    Word("sala", "Living Room", "🛋️"),
    Word("mesa", "Table", "🪑"),
    Word("silla", "Chair", "💺"),
    Word("cama", "Bed", "🛏️"),
    Word("sofá", "Sofa", "🛋️"),
    Word("televisión", "Television", "📺"),
    Word("refrigerador", "Refrigerator", "❄️"),
    Word("estufa", "Stove", "🔥"),
    Word("ducha", "Shower", "🚿"),
    Word("espejo", "Mirror", "🪞"),
    Word("lámpara", "Lamp", "💡"),
    Word("pared", "Wall", "🧱"),
    Word("techo", "Ceiling", "🏠"),
    Word("piso", "Floor", "🟫")
    )
    )

    private val categoryNames = mapOf(
        "animals" to "Animales",
        "colors" to "Colores",
        "numbers" to "Números",
        "food" to "Comida",
        "family" to "Familia",
        "transport" to "Transporte",
        "school" to "Escuela",
        "body" to "Cuerpo",
        "clothes" to "Ropa",
        "house" to "Casa"
    )

    private val categoryTitles = mapOf(
        "animals" to "🐶 English Animals",
        "colors" to "🎨 English Colors",
        "numbers" to "🔢 English Numbers",
        "food" to "🍎 English Food",
        "family" to "👨‍👩‍👧‍👦 English Family",
        "transport" to "🚗 English Transport",
        "school" to "🏫 English School",
        "body" to "👦 English Body",
        "clothes" to "👕 English Clothes",
        "house" to "🏠 English House"
    )

    private val categorySubtitles = mapOf(
        "animals" to "¡Aprende los animales en inglés!",
        "colors" to "¡Aprende los colores en inglés!",
        "numbers" to "¡Aprende los números en inglés!",
        "food" to "¡Aprende la comida en inglés!",
        "family" to "¡Aprende la familia en inglés!",
        "transport" to "¡Aprende el transporte en inglés!",
        "school" to "¡Aprende vocabulario escolar!",
        "body" to "¡Aprende las partes del cuerpo!",
        "clothes" to "¡Aprende la ropa en inglés!",
        "house" to "¡Aprende las partes de la casa!"
    )
    data class Word(
        val spanish: String,
        val english: String,
        val icon: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_english_game)

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
        gameTitle = findViewById(R.id.gameTitle)
        gameSubtitle = findViewById(R.id.gameSubtitle)
        progressBar = findViewById(R.id.progressBar)
        optionsGrid = findViewById(R.id.optionsGrid)
        closeBtn = findViewById(R.id.closeBtn)
        nightModeBtn = findViewById(R.id.nightModeBtn)
        categoryBtn = findViewById(R.id.categoryBtn)
        mainContainer = findViewById(R.id.mainContainer)
    }

    private fun loadBestScore() {
        val prefs = getPreferences(MODE_PRIVATE)
        bestScore = prefs.getInt("english_best_score", 0)
        bestScoreText.text = bestScore.toString()
    }

    private fun saveBestScore() {
        val prefs = getPreferences(MODE_PRIVATE)
        with(prefs.edit()) {
            putInt("english_best_score", bestScore)
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
        optionsGrid.removeAllViews()

        val currentVocabulary = vocabulary[currentCategory]!!
        val targetWord = currentVocabulary.random()
        currentCorrectAnswer = targetWord.english
        currentSpanishWord = targetWord.spanish

        // Crear opciones (palabra correcta + 3 aleatorias)
        val wordOptions = mutableListOf(targetWord)
        while (wordOptions.size < 4) {
            val randomWord = currentVocabulary.random()
            if (!wordOptions.contains(randomWord)) {
                wordOptions.add(randomWord)
            }
        }

        // Mezclar opciones
        wordOptions.shuffle()

        // Configurar parámetros del GridLayout para 2x2
        val rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        val colSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)

        // Crear cartas de opciones
        wordOptions.forEach { word ->
            val optionCard = LayoutInflater.from(this).inflate(R.layout.english_option_card, optionsGrid, false)
            val optionIcon = optionCard.findViewById<TextView>(R.id.optionIcon)
            val optionText = optionCard.findViewById<TextView>(R.id.optionText)
            val cardView = optionCard.findViewById<CardView>(R.id.optionCard)

            optionIcon.text = word.icon
            optionText.text = word.english

            // Configurar parámetros del GridLayout
            val params = GridLayout.LayoutParams(rowSpec, colSpec)
            params.width = 0
            params.height = GridLayout.LayoutParams.WRAP_CONTENT
            params.setMargins(8, 8, 8, 8)
            optionCard.layoutParams = params

            // Configurar click listener
            cardView.setOnClickListener {
                checkAnswer(word.english, cardView)
            }

            optionsGrid.addView(optionCard)
        }

        // Actualizar pregunta
        val questionTypes = listOf(
            "¿Cómo se dice \"$currentSpanishWord\" en inglés?",
            "Selecciona la palabra en inglés para \"$currentSpanishWord\"",
            "Encuentra la traducción de \"$currentSpanishWord\"",
            "¿Qué palabra significa \"$currentSpanishWord\"?"
        )

        questionDisplayText.text = questionTypes.random()

    }

    private fun checkAnswer(selectedAnswer: String, cardView: CardView) {
        if (selectedAnswer == currentCorrectAnswer) {
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
            .setMessage("¡Excelente! \"$currentCorrectAnswer\" significa \"$currentSpanishWord\"")
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
            .setMessage("La respuesta correcta era: \"$currentCorrectAnswer\"")
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

    private fun showCategorySelectionDialog() {
        val categories = arrayOf(
            "🐶 Animales",
            "🎨 Colores",
            "🔢 Números",
            "🍎 Comida",
            "👨‍👩‍👧‍👦 Familia",
            "🚗 Transporte",
            "🏫 Escuela",
            "👦 Cuerpo",
            "👕 Ropa",
            "🏠 Casa"
        )

        AlertDialog.Builder(this)
            .setTitle("📚 Selecciona una categoría")
            .setItems(categories) { dialog, which ->
                when (which) {
                    0 -> selectCategory("animals")
                    1 -> selectCategory("colors")
                    2 -> selectCategory("numbers")
                    3 -> selectCategory("food")
                    4 -> selectCategory("family")
                    5 -> selectCategory("transport")
                    6 -> selectCategory("school")
                    7 -> selectCategory("body")
                    8 -> selectCategory("clothes")
                    9 -> selectCategory("house")

                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun selectCategory(category: String) {
        currentCategory = category
        categoryBtn.text = "📚 ${categoryNames[category]} ▼"
        gameTitle.text = categoryTitles[category]
        gameSubtitle.text = categorySubtitles[category]
        resetGame()

    }

    private fun resetGame() {
        score = 0
        lives = 3
        currentQuestion = 1
        updateStats()
        generateQuestion()
        updateProgress()
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

        categoryBtn.setOnClickListener {
            showCategorySelectionDialog()
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
            val textViews = listOf(scoreText, bestScoreText, livesText, questionCounterText,
                questionDisplayText, gameTitle, gameSubtitle)
            textViews.forEach { it.setTextColor(textColor) }
        } else {
            // Modo Día
            mainContainer.setBackgroundResource(R.drawable.gradient_background)
            val textColor = Color.BLACK
            val textViews = listOf(scoreText, bestScoreText, livesText, questionCounterText,
                questionDisplayText, gameTitle, gameSubtitle)
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