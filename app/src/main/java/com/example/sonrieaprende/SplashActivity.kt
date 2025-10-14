package com.example.sonrieaprende


import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val circle = findViewById<ImageView>(R.id.circle)
        val square = findViewById<ImageView>(R.id.square)
        val triangle = findViewById<ImageView>(R.id.triangle)
        val title = findViewById<TextView>(R.id.title)
        val subtitle = findViewById<TextView>(R.id.subtitle)

        // ANIMACIÓN
        Handler(Looper.getMainLooper()).postDelayed({
            circle.visibility = android.view.View.VISIBLE
            circle.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fall_circle))
        }, 300)

        Handler(Looper.getMainLooper()).postDelayed({
            square.visibility = android.view.View.VISIBLE
            square.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fall_square))
        }, 600)

        Handler(Looper.getMainLooper()).postDelayed({
            triangle.visibility = android.view.View.VISIBLE
            triangle.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fall_triangle))
        }, 900)

        Handler(Looper.getMainLooper()).postDelayed({
            //  Hacer visible el título ANTES de animarlo
            title.alpha = 1f
            title.isVisible = true
            val titleAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in_up)
            title.startAnimation(titleAnimation)
        }, 1500)

        Handler(Looper.getMainLooper()).postDelayed({
            // Hacer visible el subtítulo ANTES de animarlo
            subtitle.alpha = 1f
            subtitle.isVisible = true
            val subtitleAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in_up)
            subtitle.startAnimation(subtitleAnimation)
        }, 1800)

        // Iniciar animaciones continuas después de que todas caigan
        Handler(Looper.getMainLooper()).postDelayed({
            circle.startAnimation(AnimationUtils.loadAnimation(this, R.anim.continuous_pulse))
            circle.startAnimation(AnimationUtils.loadAnimation(this, R.anim.continuous_rotate))
            square.startAnimation(AnimationUtils.loadAnimation(this, R.anim.continuous_pulse))
            square.startAnimation(AnimationUtils.loadAnimation(this, R.anim.continuous_rotate_reverse))
            triangle.startAnimation(AnimationUtils.loadAnimation(this, R.anim.continuous_pulse))
            triangle.startAnimation(AnimationUtils.loadAnimation(this, R.anim.continuous_bounce))
        }, 2000)

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, 5000)
    }
}