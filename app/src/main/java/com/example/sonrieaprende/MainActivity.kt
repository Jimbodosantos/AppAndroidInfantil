package com.example.sonrieaprende

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var menuButton: ImageButton
    private lateinit var seeMoreButton: Button
    private lateinit var challengeButton: Button
    private lateinit var gamesGrid: GridView
    private lateinit var introCircle: ImageView
    private lateinit var introSquare: ImageView
    private lateinit var introTriangle: ImageView
    private var backgroundMusic: MediaPlayer? = null

    private val featuredGames = listOf(
        GameItem("🔺", "Formas", "Formas Divertidas"),
        GameItem("🐮", "Contar Animales", "Contar Animales"),
        GameItem("\uD83D\uDD2E", "Memorama", "Memorama de Colores"),
        GameItem("🎓", "Inglés", "English Fun")
    )

    private val menuCategories = listOf(
        MenuCategory(
            "🐾 Animales Divertidos",
            listOf(
                MenuItemData("🐮", "Granja Mágica", "3-6 años")
            )
        ),
        MenuCategory(
            "🎨 Juegos de Memoria",
            listOf(
                MenuItemData("🎴", "Memorama", "4-8 años")
            )
        ),
        MenuCategory(
            "🔺 Formas y Colores",
            listOf(
                MenuItemData("🔺", "Formas Divertidas", "3-5 años")
            )
        ),
        MenuCategory(
            "🌍 Aprende Idiomas",
            listOf(
                MenuItemData("🎓", "English Fun", "4-10 años")
            )
        ),
        MenuCategory(
            "🔢 Matemáticas Básicas",
            listOf(
                MenuItemData("🐮", "Granja Animales", "3-6 años")
            )
        ),
        MenuCategory(
            "🎮 Juegos Destacados",
            listOf(
                MenuItemData("🐮", "Granja Mágica", "3-6 años"),
                MenuItemData("🎴", "Memorama", "4-8 años"),
                MenuItemData("🔺", "Formas Divertidas", "3-5 años"),
                MenuItemData("🎓", "English Fun", "4-10 años")
            )
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupBackPressedHandler()
        setupAnimations()
        setupGamesGrid()
        setupListeners()
        setupNavigationMenuExact()
        setupMenuAnimationsExact()
        startBackgroundMusic()
    }

    override fun onResume() {
        super.onResume()
        backgroundMusic?.start()
    }

    override fun onPause() {
        super.onPause()
        backgroundMusic?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        backgroundMusic?.release()
        backgroundMusic = null
    }

    private fun startBackgroundMusic() {
        try {
            backgroundMusic = MediaPlayer.create(this, R.raw.sonidoprincipal)
            backgroundMusic?.isLooping = true
            backgroundMusic?.setVolume(0.3f, 0.3f)
            backgroundMusic?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        menuButton = findViewById(R.id.menuButton)
        seeMoreButton = findViewById(R.id.seeMoreButton)
        challengeButton = findViewById(R.id.challengeButton)
        gamesGrid = findViewById(R.id.gamesGrid)
        introCircle = findViewById(R.id.introCircle)
        introSquare = findViewById(R.id.introSquare)
        introTriangle = findViewById(R.id.introTriangle)

        gamesGrid.numColumns = 2
    }

    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    drawerLayout.closeDrawer(GravityCompat.END)
                } else {
                    showExitConfirmation()
                }
            }
        })
    }

    private fun showExitConfirmation() {
        Toast.makeText(this, "Presiona nuevamente para salir", Toast.LENGTH_SHORT).show()
    }

    private fun setupAnimations() {
        val floatAnimation = AnimationUtils.loadAnimation(this, R.anim.floatt)

        introCircle.startAnimation(floatAnimation)
        Handler(Looper.getMainLooper()).postDelayed({
            introSquare.startAnimation(floatAnimation)
        }, 500)
        Handler(Looper.getMainLooper()).postDelayed({
            introTriangle.startAnimation(floatAnimation)
        }, 1000)
    }

    private fun setupMenuAnimationsExact() {
        val floatAnimation = AnimationUtils.loadAnimation(this, R.anim.floatt)

        findViewById<View>(R.id.headerShape1)?.startAnimation(floatAnimation)
        Handler(Looper.getMainLooper()).postDelayed({
            findViewById<View>(R.id.headerShape2)?.startAnimation(floatAnimation)
        }, 500)
        Handler(Looper.getMainLooper()).postDelayed({
            findViewById<View>(R.id.headerShape3)?.startAnimation(floatAnimation)
        }, 1000)
    }

    private fun setupGamesGrid() {
        val adapter = object : BaseAdapter() {
            override fun getCount(): Int = featuredGames.size
            override fun getItem(position: Int): GameItem = featuredGames[position]
            override fun getItemId(position: Int): Long = position.toLong()

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: layoutInflater.inflate(R.layout.game_item, parent, false)
                val gameItem = getItem(position)

                val gameIcon = view.findViewById<TextView>(R.id.gameIcon)
                val gameName = view.findViewById<TextView>(R.id.gameName)

                gameIcon.text = gameItem.icon
                gameName.text = gameItem.name

                val iconAnimation = AnimationUtils.loadAnimation(this@MainActivity, R.anim.floatt)
                iconAnimation.startOffset = position * 200L
                gameIcon.startAnimation(iconAnimation)

                view.setOnClickListener {
                    startGame(gameItem.fullName)
                    animateView(view)
                }

                return view
            }
        }

        gamesGrid.adapter = adapter

        gamesGrid.post {
            adjustGridViewHeight()
        }
    }

    private fun adjustGridViewHeight() {
        val numRows = Math.ceil(featuredGames.size / 2.0).toInt()
        val itemHeight = 120.dpToPx()
        val verticalSpacing = 15.dpToPx()
        val padding = 40.dpToPx()

        val totalHeight = (numRows * itemHeight) + ((numRows - 1) * verticalSpacing) + padding

        val params = gamesGrid.layoutParams
        params.height = totalHeight
        gamesGrid.layoutParams = params
    }

    private fun setupListeners() {
        menuButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        seeMoreButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        challengeButton.setOnClickListener {
            startChallenge()
        }
    }

    private fun setupNavigationMenuExact() {
        createMenuProgrammatically()
    }

    private fun createMenuProgrammatically() {
        val menuContainer = findViewById<LinearLayout>(R.id.menuContentContainer)
        menuContainer?.removeAllViews()

        menuCategories.forEachIndexed { categoryIndex, category ->
            val categoryView = layoutInflater.inflate(R.layout.menu_category, menuContainer, false)

            val categoryIcon = categoryView.findViewById<TextView>(R.id.categoryIcon)
            val categoryTitle = categoryView.findViewById<TextView>(R.id.categoryTitle)

            val (emoji, titleWithoutEmoji) = extractEmojiAndTitle(category.title)

            categoryIcon.text = emoji
            categoryTitle.text = titleWithoutEmoji
            categoryView.contentDescription = "Categoría: ${category.title}"

            val bounceAnim = AnimationUtils.loadAnimation(this, R.anim.bounce)
            categoryIcon.startAnimation(bounceAnim)

            menuContainer.addView(categoryView)

            category.items.forEachIndexed { itemIndex, itemData ->
                val itemView = layoutInflater.inflate(R.layout.menu_item, menuContainer, false)

                itemView.findViewById<TextView>(R.id.itemIcon).text = itemData.icon
                itemView.findViewById<TextView>(R.id.itemTitle).text = itemData.title
                itemView.findViewById<TextView>(R.id.itemAge).text = itemData.ageRange
                itemView.contentDescription = "${itemData.title} - Para ${itemData.ageRange}"

                val backgroundDrawable = createMenuItemBackground(itemIndex)
                itemView.findViewById<LinearLayout>(R.id.menuItemLayout).background = backgroundDrawable

                setupMenuItemAnimations(itemView, itemData, categoryIndex * 10 + itemIndex)

                itemView.setOnClickListener {
                    startGame(itemData.title)
                    animateMenuItemExact(itemView)
                }

                menuContainer.addView(itemView)
            }

            if (categoryIndex < menuCategories.size - 1) {
                val space = View(this)
                space.layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    20.dpToPx()
                )
                space.contentDescription = "Espacio entre categorías"
                menuContainer.addView(space)
            }
        }
    }

    private fun extractEmojiAndTitle(fullText: String): Pair<String, String> {
        val trimmedText = fullText.trim()
        val emojiEndIndex = findEmojiEndIndex(trimmedText)

        return if (emojiEndIndex > 0) {
            val emoji = trimmedText.substring(0, emojiEndIndex)
            val title = trimmedText.substring(emojiEndIndex).trim()
            Pair(emoji, title)
        } else {
            Pair(trimmedText.take(1), trimmedText.drop(1).trim())
        }
    }

    private fun findEmojiEndIndex(text: String): Int {
        if (text.isEmpty()) return 0

        var index = 0
        while (index < text.length) {
            val char = text[index]
            if (char.isLetterOrDigit() || char == ' ' || char == '\t') {
                break
            }
            index++
        }
        return index
    }

    private fun createMenuItemBackground(itemIndex: Int): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = 15f
            if (itemIndex % 3 == 0) {
                setColor(Color.parseColor("#FFF0F5"))
                setStroke(2.dpToPx(), Color.parseColor("#FFB6C1"))
            } else if (itemIndex % 3 == 1) {
                setColor(Color.parseColor("#F0FFF0"))
                setStroke(2.dpToPx(), Color.parseColor("#98FB98"))
            } else {
                setColor(Color.parseColor("#F0F8FF"))
                setStroke(2.dpToPx(), Color.parseColor("#87CEEB"))
            }
        }
    }

    private fun setupMenuItemAnimations(view: View, itemData: MenuItemData, index: Int) {
        val icon = view.findViewById<TextView>(R.id.itemIcon)
        val floatAnim = AnimationUtils.loadAnimation(this, R.anim.icon_float)
        floatAnim.startOffset = (index % 4) * 300L
        icon.startAnimation(floatAnim)

        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(1.05f).scaleY(1.05f).setDuration(300).start()
                    v.findViewById<TextView>(R.id.sparkle)?.animate()?.alpha(1f)?.setDuration(300)?.start()
                    v.elevation = 8f
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(300).start()
                    v.findViewById<TextView>(R.id.sparkle)?.animate()?.alpha(0f)?.setDuration(300)?.start()
                    v.elevation = 0f
                }
            }
            false
        }
    }

    private fun animateMenuItemExact(view: View) {
        view.animate()
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setDuration(150)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(150)
                    .start()
            }.start()

        showConfettiEffectExact()
    }

    private fun animateView(view: View) {
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(150)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(150)
                    .start()
            }.start()
    }

    private fun showConfettiEffectExact() {
        Toast.makeText(this, "🎮 ¡Iniciando juego!", Toast.LENGTH_SHORT).show()
    }

    private fun startGame(gameName: String) {
        when (gameName) {
            "Granja Mágica", "Contar Animales", "Granja" -> {
                val intent = Intent(this, ContarAnimales::class.java)
                startActivity(intent)
            }
            "Memorama de Colores", "Memorama" -> {
                val intent = Intent(this, MemoramaColores::class.java)
                startActivity(intent)
            }
            "Formas Divertidas", "Formas" -> {
                val intent = Intent(this, ShapesGameActivity::class.java)
                startActivity(intent)
            }
            "English Fun", "Inglés" -> {
                val intent = Intent(this, EnglishGameActivity::class.java)
                startActivity(intent)
            }
            else -> {
                Toast.makeText(this, "🎮 Iniciando: $gameName", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startChallenge() {
        val randomGame = listOf("Granja Mágica", "Memorama de Colores", "Formas Divertidas", "English Fun").random()

        when (randomGame) {
            "Granja Mágica" -> {
                Toast.makeText(this, "🏆 ¡Reto del Día!\nCuenta 10 animales correctamente", Toast.LENGTH_LONG).show()
                Handler(Looper.getMainLooper()).postDelayed({
                    val intent = Intent(this, ContarAnimales::class.java)
                    startActivity(intent)
                }, 2000)
            }
            "Memorama de Colores" -> {
                Toast.makeText(this, "🏆 ¡Reto del Día!\nEncuentra 5 parejas de colores", Toast.LENGTH_LONG).show()
                Handler(Looper.getMainLooper()).postDelayed({
                    val intent = Intent(this, MemoramaColores::class.java)
                    startActivity(intent)
                }, 2000)
            }
            "Formas Divertidas" -> {
                Toast.makeText(this, "🏆 ¡Reto del Día!\nIdentifica 8 formas correctamente", Toast.LENGTH_LONG).show()
                Handler(Looper.getMainLooper()).postDelayed({
                    val intent = Intent(this, ShapesGameActivity::class.java)
                    startActivity(intent)
                }, 2000)
            }
            "English Fun" -> {
                Toast.makeText(this, "🏆 ¡Reto del Día!\nAprende 10 palabras en inglés", Toast.LENGTH_LONG).show()
                Handler(Looper.getMainLooper()).postDelayed({
                    val intent = Intent(this, EnglishGameActivity::class.java)
                    startActivity(intent)
                }, 2000)
            }
        }
    }

    data class GameItem(
        val icon: String,
        val name: String,
        val fullName: String,
        val ageRange: String = ""
    )

    data class MenuItemData(
        val icon: String,
        val title: String,
        val ageRange: String
    )

    data class MenuCategory(
        val title: String,
        val items: List<MenuItemData>
    )

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}