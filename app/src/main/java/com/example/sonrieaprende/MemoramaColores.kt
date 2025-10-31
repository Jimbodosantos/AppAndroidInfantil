package com.example.sonrieaprende

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MemoramaColores : AppCompatActivity() {

    private var score = 0
    private var bestScore = 0
    private var lives = 3
    private var pairsFound = 0
    private var currentLevel = 1
    private val totalPairs = 8

    private var flippedCards = mutableListOf<View>()
    private var matchedPairs = mutableSetOf<String>()
    private var canFlip = false
    private var isNightMode = false

    private lateinit var scoreText: TextView
    private lateinit var bestScoreText: TextView
    private lateinit var livesText: TextView
    private lateinit var pairsFoundText: TextView
    private lateinit var levelText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var memoryGrid: GridView
    private lateinit var closeBtn: ImageButton
    private lateinit var nightModeBtn: Button
    private lateinit var resetBtn: Button
    private lateinit var statsBar: LinearLayout
    private lateinit var mainContainer: LinearLayout

    private var currentSound: MediaPlayer? = null

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

    override fun onDestroy() {
        super.onDestroy()
        stopAllAudio()
        saveBestScore()
    }

    private fun initViews() {
        scoreText = findViewById(R.id.scoreText)
        bestScoreText = findViewById(R.id.bestScoreText)
        livesText = findViewById(R.id.livesText)
        pairsFoundText = findViewById(R.id.pairsFoundText)
        levelText = findViewById(R.id.levelText)
        progressBar = findViewById(R.id.progressBar)
        memoryGrid = findViewById(R.id.memoryGrid)
        closeBtn = findViewById(R.id.closeBtn)
        nightModeBtn = findViewById(R.id.nightModeBtn)
        resetBtn = findViewById(R.id.resetBtn)
        statsBar = findViewById(R.id.statsBar)
        mainContainer = findViewById(R.id.mainContainer)

        memoryGrid.numColumns = 4
        memoryGrid.verticalSpacing = 8.dpToPx()
        memoryGrid.horizontalSpacing = 8.dpToPx()
    }

    private fun playSound(soundResource: Int) {
        try {
            currentSound?.release()
            currentSound = MediaPlayer.create(this, soundResource)
            currentSound?.setOnCompletionListener {
                it.release()
                currentSound = null
            }
            currentSound?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAllAudio() {
        currentSound?.let {
            if (it.isPlaying) it.stop()
            it.release()
            currentSound = null
        }
    }

    private fun setupVisuals() {
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
        currentLevel = 1
        pairsFound = 0
        flippedCards.clear()
        matchedPairs.clear()
        canFlip = false
        updateStats()
        createBoard()
    }

    private fun createBoard() {
        cardsList.clear()
        colors.forEach { color ->
            cardsList.add(color)
            cardsList.add(color.copy())
        }
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

                cardFront.text = "?"
                cardBack.text = colorItem.icon

                val backBackground = GradientDrawable().apply {
                    setColor(colorItem.color)
                    cornerRadius = 12f
                }
                cardBack.background = backBackground

                cardFront.visibility = View.VISIBLE
                cardBack.visibility = View.INVISIBLE
                cardLayout.isEnabled = false
                cardLayout.alpha = 1f
                cardLayout.scaleX = 1f
                cardLayout.scaleY = 1f

                cardLayout.setBackgroundResource(R.drawable.card_background)
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

        memoryGrid.post {
            showAllCardsTemporarily()
        }

        updateProgress()
    }

    private fun showAllCardsTemporarily() {
        for (i in 0 until memoryGrid.childCount) {
            val cardView = memoryGrid.getChildAt(i)
            val cardFront = cardView.findViewById<TextView>(R.id.cardFront)
            val cardBack = cardView.findViewById<TextView>(R.id.cardBack)

            cardFront.visibility = View.INVISIBLE
            cardBack.visibility = View.VISIBLE
        }

        Handler(Looper.getMainLooper()).postDelayed({
            hideAllCards()
            canFlip = true
        }, 3000)
    }

    private fun hideAllCards() {
        for (i in 0 until memoryGrid.childCount) {
            val cardView = memoryGrid.getChildAt(i)
            val cardFront = cardView.findViewById<TextView>(R.id.cardFront)
            val cardBack = cardView.findViewById<TextView>(R.id.cardBack)
            val cardLayout = cardView.findViewById<LinearLayout>(R.id.cardLayout)

            cardFront.visibility = View.VISIBLE
            cardBack.visibility = View.INVISIBLE
            cardLayout.isEnabled = true
        }
    }

    private fun flipCard(cardView: View, colorItem: ColorItem) {
        val cardFront = cardView.findViewById<TextView>(R.id.cardFront)
        val cardBack = cardView.findViewById<TextView>(R.id.cardBack)
        val cardLayout = cardView.findViewById<LinearLayout>(R.id.cardLayout)

        cardFront.visibility = View.INVISIBLE
        cardBack.visibility = View.VISIBLE

        flippedCards.add(cardView)

        if (flippedCards.size == 2) {
            canFlip = false
            Handler(Looper.getMainLooper()).postDelayed({
                checkMatch()
            }, 800)
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

        if (color1 == color2) {
            playSound(R.raw.correct_sound)
            handleMatchFound(card1, card2, color1)
        } else {
            playSound(R.raw.incorrect_sound)
            handleNoMatch(card1, card2)
        }
    }

    private fun handleMatchFound(card1: View, card2: View, colorName: String) {
        pairsFound++
        score += 20
        matchedPairs.add(colorName)

        card1.findViewById<LinearLayout>(R.id.cardLayout).animate()
            .scaleX(0.9f).scaleY(0.9f).alpha(0.7f).setDuration(300).start()
        card2.findViewById<LinearLayout>(R.id.cardLayout).animate()
            .scaleX(0.9f).scaleY(0.9f).alpha(0.7f).setDuration(300).start()

        card1.findViewById<LinearLayout>(R.id.cardLayout).isEnabled = false
        card2.findViewById<LinearLayout>(R.id.cardLayout).isEnabled = false

        updateStats()
        updateProgress()

        showPairFoundDialog()


        flippedCards.clear()

        if (pairsFound == totalPairs) {
            Handler(Looper.getMainLooper()).postDelayed({
                playSound(R.raw.level_complete_sound)
                completeLevel()
            }, 1000)
        } else {
            canFlip = true
        }
    }

    private fun handleNoMatch(card1: View, card2: View) {
        lives--
        updateStats()

        showWrongPairDialog()

        Handler(Looper.getMainLooper()).postDelayed({
            resetFlippedCards()

            if (lives <= 0) {
                Handler(Looper.getMainLooper()).postDelayed({
                    playSound(R.raw.game_over_sound)
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

    private fun startNextLevel() {
        currentLevel++
        lives = 3
        pairsFound = 0
        flippedCards.clear()
        matchedPairs.clear()
        canFlip = false
        updateStats()
        createBoard()
    }

    private fun resetGame() {
        stopAllAudio()
        score = 0
        lives = 3
        currentLevel = 1
        pairsFound = 0
        flippedCards.clear()
        matchedPairs.clear()
        canFlip = false
        updateStats()
        createBoard()
    }

    private fun updateStats() {
        scoreText.text = score.toString()
        bestScoreText.text = bestScore.toString()
        livesText.text = lives.toString()
        pairsFoundText.text = "$pairsFound/$totalPairs"
        levelText.text = "Nivel $currentLevel"
    }

    private fun updateProgress() {
        val progress = (pairsFound.toFloat() / totalPairs) * 100
        progressBar.progress = progress.toInt()
    }

    private fun toggleNightMode() {
        isNightMode = !isNightMode

        if (isNightMode) {
            mainContainer.setBackgroundColor(Color.parseColor("#2C3E50"))
            val textColor = Color.WHITE
            val textViews = listOf(scoreText, bestScoreText, livesText, pairsFoundText, levelText)
            textViews.forEach { it.setTextColor(textColor) }
        } else {
            mainContainer.setBackgroundResource(R.drawable.gradient_background)
            val textColor = Color.BLACK
            val textViews = listOf(scoreText, bestScoreText, livesText, pairsFoundText, levelText)
            textViews.forEach { it.setTextColor(textColor) }
        }

        nightModeBtn.text = if (isNightMode) "☀️ Día" else "🌙 Noche"
    }

    private fun showPairFoundDialog() {
        Toast.makeText(this, "🎉 ¡Pareja encontrada! +20 puntos", Toast.LENGTH_SHORT).show()
    }

    private fun showWrongPairDialog() {
        Toast.makeText(this, "💪 No son pareja - Vidas: $lives", Toast.LENGTH_SHORT).show()
    }

    private fun showLevelCompleteDialog() {
        AlertDialog.Builder(this)
            .setTitle("🏆 ¡Nivel $currentLevel Completado!")
            .setMessage("¡Encontraste todas las parejas!\n\nPuntos ganados: +${currentLevel * 50}\nPuntuación total: $score")
            .setPositiveButton("🎮 Siguiente Nivel") { dialog, _ ->
                dialog.dismiss()
                startNextLevel()
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
        if (score > bestScore) {
            bestScore = score
            saveBestScore()
            bestScoreText.text = bestScore.toString()
        }

        AlertDialog.Builder(this)
            .setTitle("🎮 ¡Game Over!")
            .setMessage("Se acabaron las vidas\n\nPuntuación final: $score puntos\nMejor récord: $bestScore\nNivel alcanzado: $currentLevel")
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
            .setMessage("Tu mejor puntuación se guardará automáticamente\nPuntuación actual: $score puntos")
            .setPositiveButton("Seguir jugando") { dialog, _ -> dialog.dismiss() }
            .setNegativeButton("Salir") { dialog, _ ->
                dialog.dismiss()
                saveBestScore()
                finish()
            }
            .setCancelable(true)
            .show()
    }


    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}