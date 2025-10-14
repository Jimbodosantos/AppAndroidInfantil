package com.example.sonrieaprende

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

    // Datos para los juegos destacados
    private val featuredGames = listOf(
        GameItem("🔤", "Abecedario", "Abecedario Mágico"),
        GameItem("🔢", "Números", "Números Locos"),
        GameItem("🎨", "Colores", "Mundo de Colores"),
        GameItem("🔷", "Formas", "Formas Divertidas")
    )

    // Datos EXACTOS para el menú lateral
    private val menuCategories = listOf(
        MenuCategory(
            "📚 Aprendizaje Básico",
            listOf(
                MenuItemData("🔤", "Abecedario Mágico", "2-4 años"),
                MenuItemData("🔢", "Números Locos", "3-5 años"),
                MenuItemData("🎨", "Mundo de Colores", "2-4 años"),
                MenuItemData("🔷", "Formas Divertidas", "3-5 años")
            )
        ),
        MenuCategory(
            "🐾 Animales Divertidos",
            listOf(
                MenuItemData("🐮", "Granja Sonora", "2-4 años"),
                MenuItemData("🐠", "Océano Mágico", "3-5 años"),
                MenuItemData("🐒", "Selva ABC", "3-5 años")
            )
        ),
        MenuCategory(
            "🎵 Creatividad",
            listOf(
                MenuItemData("🎹", "Música Alegre", "3-6 años"),
                MenuItemData("🖌️", "Pintura Mágica", "3-6 años")
            )
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar vistas
        initViews()

        // Configurar el manejo del botón back
        setupBackPressedHandler()

        // Configurar animaciones
        setupAnimations()

        // Configurar GridView de juegos
        setupGamesGrid()

        // Configurar listeners
        setupListeners()

        // Configurar menú lateral EXACTO
        setupNavigationMenuExact()

        // Configurar animaciones del menú EXACTO
        setupMenuAnimationsExact()
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

        // Animación para las formas del header del menú EXACTO
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

                // Animación flotante para los íconos
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
        // Crear el menú programáticamente
        createMenuProgrammatically()
    }

    private fun createMenuProgrammatically() {
        val menuContainer = findViewById<LinearLayout>(R.id.menuContentContainer)
        menuContainer?.removeAllViews()

        // Crear cada categoría con sus items
        menuCategories.forEachIndexed { categoryIndex, category ->
            // Añadir categoría
            val categoryView = layoutInflater.inflate(R.layout.menu_category, menuContainer, false)
            categoryView.findViewById<TextView>(R.id.categoryIcon).text = category.title.substringBefore(" ")
            categoryView.findViewById<TextView>(R.id.categoryTitle).text = category.title
            categoryView.contentDescription = "Categoría: ${category.title}"

            // Animación de bounce para la categoría
            val bounceAnim = AnimationUtils.loadAnimation(this, R.anim.bounce)
            categoryView.findViewById<TextView>(R.id.categoryIcon).startAnimation(bounceAnim)

            menuContainer.addView(categoryView)

            // Añadir items de esta categoría
            category.items.forEachIndexed { itemIndex, itemData ->
                val itemView = layoutInflater.inflate(R.layout.menu_item, menuContainer, false)

                // Configurar datos del item
                itemView.findViewById<TextView>(R.id.itemIcon).text = itemData.icon
                itemView.findViewById<TextView>(R.id.itemTitle).text = itemData.title
                itemView.findViewById<TextView>(R.id.itemAge).text = itemData.ageRange
                itemView.contentDescription = "${itemData.title} - Para ${itemData.ageRange}"

                // Configurar colores alternados
                val backgroundDrawable = if (itemIndex % 2 == 0) {
                    resources.getDrawable(R.drawable.menu_item_odd, null)
                } else {
                    resources.getDrawable(R.drawable.menu_item_even, null)
                }
                itemView.findViewById<LinearLayout>(R.id.menuItemLayout).background = backgroundDrawable

                // Configurar animaciones y clics
                setupMenuItemAnimations(itemView, itemData, categoryIndex * 10 + itemIndex)

                itemView.setOnClickListener {
                    startGame(itemData.title)
                    animateMenuItemExact(itemView)
                }

                menuContainer.addView(itemView)
            }

            // Añadir espacio entre categorías
            if (categoryIndex < menuCategories.size - 1) {
                val space = View(this)
                space.layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    resources.getDimensionPixelSize(R.dimen.menu_category_spacing)
                )
                space.contentDescription = "Espacio entre categorías"
                menuContainer.addView(space)
            }
        }
    }

    private fun setupMenuItemAnimations(view: View, itemData: MenuItemData, index: Int) {
        // Animación flotante para íconos
        val icon = view.findViewById<TextView>(R.id.itemIcon)
        val floatAnim = AnimationUtils.loadAnimation(this, R.anim.icon_float)
        floatAnim.startOffset = (index % 4) * 300L
        icon.startAnimation(floatAnim)

        // Efecto hover
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
        Toast.makeText(this, "🎮 Iniciando: $gameName", Toast.LENGTH_SHORT).show()
    }

    private fun startChallenge() {
        Toast.makeText(this, "🏆 ¡Comenzando el Reto del Día!\nEncuentra 5 animales que empiecen con 'A'", Toast.LENGTH_LONG).show()
    }

    // Data classes
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
}