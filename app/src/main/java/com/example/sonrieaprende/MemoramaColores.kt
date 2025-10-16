package com.example.sonrieaprende

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MemoramaColores : AppCompatActivity() {

    // Variables del juego
    private var score = 0
    private var bestScore = 0
    private var lives = 3
    private var pairsFound = 0
    private val totalPairs = 8

    private var flippedCards = mutableListOf<View>()
    private var matchedPairs = mutableSetOf<String>()
    private var canFlip = true
    private var isNightMode = false

    // Vistas
    private lateinit var scoreText: TextView
    private lateinit var bestScoreText: TextView
    private lateinit var livesText: TextView
    private lateinit var pairsFoundText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var memoryGrid: GridView
    private lateinit var closeBtn: ImageButton
    private lateinit var nightModeBtn: Button
    private lateinit var resetBtn: Button
    private lateinit var statsBar: LinearLayout
    private lateinit var mainContainer: LinearLayout

    // Colores del juego
    private val colors = listOf(
        ColorItem("Rojo", "🔴", Color.parseColor("#FF6B6B")),
        ColorItem("Azul", "🔵", Color.parseColor("#4ECDC4")),
        ColorItem("Verde", "🟢", Color.parseColor("#96CEB4")),
        ColorItem("Amarillo", "🟡", Color.parseColor("#FFD93D")),
        ColorItem("Morado", "🟣", Color.parseColor("#A363D9")),
        ColorItem("Naranja", "🟠", Color.parseColor("#FF9A76")),
        ColorItem("Rosa", "💖", Color.parseColor("#FF9FF3")),
        ColorItem("Cian", "💠", Color.parseColor("#45B7D1"))
    )

    private var cardsList = mutableListOf<ColorItem>()

    data class ColorItem(val name: String, val icon: String, val color: Int)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_memorama)

        initViews()
        loadBestScore()
        setupListeners()
        setupVisuals()
        startNewGame()
    }

    private fun initViews() {
        scoreText = findViewById(R.id.scoreText)
        bestScoreText = findViewById(R.id.bestScoreText)
        livesText = findViewById(R.id.livesText)
        pairsFoundText = findViewById(R.id.pairsFoundText)
        progressBar = findViewById(R.id.progressBar)
        memoryGrid = findViewById(R.id.memoryGrid)
        closeBtn = findViewById(R.id.closeBtn)
        nightModeBtn = findViewById(R.id.nightModeBtn)
        resetBtn = findViewById(R.id.resetBtn)
        statsBar = findViewById(R.id.statsBar)
        mainContainer = findViewById(R.id.mainContainer)

        // Configurar GridView
        memoryGrid.numColumns = 4
        memoryGrid.verticalSpacing = 8.dpToPx()
        memoryGrid.horizontalSpacing = 8.dpToPx()
    }

    private fun setupVisuals() {
        // Configurar bordes redondeados
        val statsBg = GradientDrawable().apply {
            setColor(Color.parseColor("#FFD700"))
            cornerRadius = 20f
        }
        statsBar.background = statsBg

        val resetBtnBg = GradientDrawable().apply {
            setColor(Color.parseColor("#FF6B6B"))
            cornerRadius = 25f
        }
        resetBtn.background = resetBtnBg

        val nightModeBg = GradientDrawable().apply {
            setColor(Color.parseColor("#4ECDC4"))
            cornerRadius = 25f
        }
        nightModeBtn.background = nightModeBg

        // Configurar barra de progreso
        progressBar.progressDrawable?.setColorFilter(Color.parseColor("#FF5722"), android.graphics.PorterDuff.Mode.SRC_IN)
    }

    private fun loadBestScore() {
        val sharedPref = getPreferences(MODE_PRIVATE)
        bestScore = sharedPref.getInt("memorama_best_score", 0)
        bestScoreText.text = bestScore.toString()
    }

    private fun saveBestScore() {
        val sharedPref = getPreferences(MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("memorama_best_score", bestScore)
            apply()
        }
    }

    private fun setupListeners() {
        closeBtn.setOnClickListener { showExitDialog() }
        nightModeBtn.setOnClickListener { toggleNightMode() }
        resetBtn.setOnClickListener { resetGame() }
    }

    private fun startNewGame() {
        score = 0
        lives = 3
        pairsFound = 0
        flippedCards.clear()
        matchedPairs.clear()
        canFlip = true
        updateStats()
        createBoard()
    }

    private fun createBoard() {
        // Duplicar y mezclar colores
        cardsList.clear()
        colors.forEach { color ->
            cardsList.add(color)
            cardsList.add(color.copy()) // Duplicar para hacer parejas
        }

        // Mezclar cartas
        cardsList.shuffle()

        val adapter = object : BaseAdapter() {
            override fun getCount(): Int = cardsList.size
            override fun getItem(position: Int): ColorItem = cardsList[position]
            override fun getItemId(position: Int): Long = position.toLong()

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val colorItem = getItem(position)
                val view = convertView ?: layoutInflater.inflate(R.layout.memory_card_item, parent, false)

                val cardFront = view.findViewById<TextView>(R.id.cardFront)
                val cardBack = view.findViewById<TextView>(R.id.cardBack)
                val cardLayout = view.findViewById<LinearLayout>(R.id.cardLayout)



                // Configurar la carta
                cardFront.text = "?"
                cardBack.text = colorItem.icon

                // Crear background con color sólido para el reverso
                val backBackground = GradientDrawable().apply {
                    setColor(colorItem.color)
                    cornerRadius = 12f
                }
                cardBack.background = backBackground

                // Resetear estado de la carta
                cardFront.visibility = View.VISIBLE
                cardBack.visibility = View.INVISIBLE
                cardLayout.isEnabled = true
                cardLayout.alpha = 1f
                cardLayout.scaleX = 1f
                cardLayout.scaleY = 1f

                // Remover cualquier estado previo
                cardLayout.setBackgroundResource(R.drawable.card_background)

                // Tag para identificar la carta
                cardLayout.tag = colorItem.name

                cardLayout.setOnClickListener {
                    if (canFlip &&
                        !flippedCards.contains(view) &&
                        !matchedPairs.contains(colorItem.name) &&
                        flippedCards.size < 2) {
                        flipCard(view, colorItem)
                    }
                }

                return view
            }
        }

        memoryGrid.adapter = adapter
        updateProgress()
    }

    private fun flipCard(cardView: View, colorItem: ColorItem) {
        val cardFront = cardView.findViewById<TextView>(R.id.cardFront)
        val cardBack = cardView.findViewById<TextView>(R.id.cardBack)
        val cardLayout = cardView.findViewById<LinearLayout>(R.id.cardLayout)



        // Animación de volteo
        cardFront.visibility = View.INVISIBLE
        cardBack.visibility = View.VISIBLE

        flippedCards.add(cardView)

        if (flippedCards.size == 2) {
            canFlip = false
            Handler(Looper.getMainLooper()).postDelayed({
                checkMatch()
            }, 800) // Tiempo para ver las cartas
        }
    }

    private fun checkMatch() {
        if (flippedCards.size != 2) {
            canFlip = true
            return
        }

        val card1 = flippedCards[0]
        val card2 = flippedCards[1]
        val color1 = card1.tag as String
        val color2 = card2.tag as String

        println("DEBUG: Comparando - $color1 vs $color2")

        if (color1 == color2) {
            // Pareja encontrada
            handleMatchFound(card1, card2, color1)
        } else {
            // No es pareja
            handleNoMatch(card1, card2)
        }
    }

    private fun handleMatchFound(card1: View, card2: View, colorName: String) {
        pairsFound++
        score += 20
        matchedPairs.add(colorName)

        // Animación de acierto
        card1.findViewById<LinearLayout>(R.id.cardLayout).animate()
            .scaleX(0.9f).scaleY(0.9f).alpha(0.7f).setDuration(300).start()
        card2.findViewById<LinearLayout>(R.id.cardLayout).animate()
            .scaleX(0.9f).scaleY(0.9f).alpha(0.7f).setDuration(300).start()

        // Deshabilitar cartas
        card1.findViewById<LinearLayout>(R.id.cardLayout).isEnabled = false
        card2.findViewById<LinearLayout>(R.id.cardLayout).isEnabled = false

        updateStats()
        updateProgress()

        // Mostrar feedback inmediato
        showPairFoundDialog()
        createConfettiEffect()

        flippedCards.clear()

        if (pairsFound == totalPairs) {
            Handler(Looper.getMainLooper()).postDelayed({
                completeLevel()
            }, 1000)
        } else {
            canFlip = true
        }
    }

    private fun handleNoMatch(card1: View, card2: View) {
        lives--
        updateStats()

        // Mostrar diálogo de error primero
        showWrongPairDialog()

        // Luego voltear las cartas de vuelta después de un delay
        Handler(Looper.getMainLooper()).postDelayed({
            resetFlippedCards()

            if (lives <= 0) {
                Handler(Looper.getMainLooper()).postDelayed({
                    endGame()
                }, 500)
            } else {
                canFlip = true
            }
        }, 1500)
    }

    private fun resetFlippedCards() {
        flippedCards.forEach { cardView ->
            val cardFront = cardView.findViewById<TextView>(R.id.cardFront)
            val cardBack = cardView.findViewById<TextView>(R.id.cardBack)

            cardFront.visibility = View.VISIBLE
            cardBack.visibility = View.INVISIBLE
        }
        flippedCards.clear()
    }

    private fun completeLevel() {
        showLevelCompleteDialog()
    }

    private fun resetGame() {
        score = 0
        lives = 3
        pairsFound = 0
        flippedCards.clear()
        matchedPairs.clear()
        canFlip = true
        updateStats()
        createBoard()
    }

    private fun updateStats() {
        scoreText.text = score.toString()
        bestScoreText.text = bestScore.toString()
        livesText.text = lives.toString()
        pairsFoundText.text = "$pairsFound/$totalPairs"
    }

    private fun updateProgress() {
        val progress = (pairsFound.toFloat() / totalPairs) * 100
        progressBar.progress = progress.toInt()
    }

    private fun toggleNightMode() {
        isNightMode = !isNightMode

        if (isNightMode) {
            // Modo Noche
            mainContainer.setBackgroundColor(Color.parseColor("#2C3E50"))
            val textColor = Color.WHITE
            val textViews = listOf(scoreText, bestScoreText, livesText, pairsFoundText)
            textViews.forEach { it.setTextColor(textColor) }
        } else {
            // Modo Día - Gradiente
            mainContainer.setBackgroundResource(R.drawable.gradient_background)
            val textColor = Color.BLACK
            val textViews = listOf(scoreText, bestScoreText, livesText, pairsFoundText)
            textViews.forEach { it.setTextColor(textColor) }
        }

        nightModeBtn.text = if (isNightMode) "☀️ Día" else "🌙 Noche"
    }

    // DIÁLOGOS
    private fun showPairFoundDialog() {
        // Diálogo  para pareja encontrada
        Toast.makeText(this, "🎉 ¡Pareja encontrada! +20 puntos", Toast.LENGTH_SHORT).show()
    }

    private fun showWrongPairDialog() {
        // Toast
        Toast.makeText(this, "💪 No son pareja - Vidas: $lives", Toast.LENGTH_SHORT).show()
    }

    private fun showLevelCompleteDialog() {
        AlertDialog.Builder(this)
            .setTitle("🏆 ¡Nivel Completado!")
            .setMessage("¡Encontraste todas las parejas!\n\nPuntuación final: $score puntos")
            .setPositiveButton("🎮 Nuevo Juego") { dialog, _ ->
                dialog.dismiss()
                resetGame()
            }
            .setNegativeButton("Salir") { dialog, _ ->
                dialog.dismiss()
                saveBestScore()
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun endGame() {
        // Actualizar mejor puntuación
        if (score > bestScore) {
            bestScore = score
            saveBestScore()
            bestScoreText.text = bestScore.toString()
        }

        AlertDialog.Builder(this)
            .setTitle("🎮 ¡Game Over!")
            .setMessage("Se acabaron las vidas\n\nPuntuación: $score puntos\nMejor récord: $bestScore")
            .setPositiveButton("🔄 Intentar otra vez") { dialog, _ ->

                dialog.dismiss()
                resetGame()
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
            .setMessage("Tu mejor puntuación se guardará automáticamente")
            .setPositiveButton("Seguir jugando") { dialog, _ -> dialog.dismiss() }
            .setNegativeButton("Salir") { dialog, _ ->
                dialog.dismiss()
                saveBestScore()
                finish()
            }
            .setCancelable(true)
            .show()
    }

    private fun createConfettiEffect() {
        // Efecto de confeti simple
        Toast.makeText(this, "🎉 ¡Correcto!", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        saveBestScore()
    }

    // Extensión para convertir dp a px
    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}