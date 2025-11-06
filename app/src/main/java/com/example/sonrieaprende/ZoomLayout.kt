package com.example.sonrieaprende

import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.FrameLayout
import android.widget.ImageView
import kotlin.math.max
import kotlin.math.min

class ZoomLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val matrix = Matrix()
    private var scaleDetector: ScaleGestureDetector
    private var currentScale = 1f
    private val minScale = 0.5f
    private val maxScale = 4f

    private var lastFocusX = 0f
    private var lastFocusY = 0f
    private var isDragging = false

    var onMatrixChanged: ((Matrix) -> Unit)? = null

    init {
        scaleDetector = ScaleGestureDetector(context, ScaleListener())
        isClickable = false
        post { notifyMatrixChanged() }
    }

    fun adjustImageToFit() {
        val imageView = getImageView() ?: return
        post {
            val drawable = imageView.drawable ?: return@post
            val viewWidth = width.toFloat()
            val viewHeight = height.toFloat()
            val imgWidth = drawable.intrinsicWidth.toFloat()
            val imgHeight = drawable.intrinsicHeight.toFloat()
            if (viewWidth == 0f || viewHeight == 0f) return@post
            matrix.reset()
            val scale = min(viewWidth / imgWidth, viewHeight / imgHeight)
            currentScale = scale
            matrix.postScale(scale, scale)
            val tx = (viewWidth - imgWidth * scale) / 2f
            val ty = (viewHeight - imgHeight * scale) / 2f
            matrix.postTranslate(tx, ty)
            applyMatrix()
            notifyMatrixChanged()
        }
    }

    private fun getImageView(): ImageView? = findViewById(R.id.baseImage)

    private fun applyMatrix() {
        getImageView()?.imageMatrix = matrix
    }

    private fun notifyMatrixChanged() {
        onMatrixChanged?.invoke(Matrix(matrix))
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return ev.pointerCount > 1 || super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount >= 2) {
                    lastFocusX = (event.getX(0) + event.getX(1)) / 2f
                    lastFocusY = (event.getY(0) + event.getY(1)) / 2f
                    isDragging = true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDragging && event.pointerCount >= 2 && !scaleDetector.isInProgress) {
                    val focusX = (event.getX(0) + event.getX(1)) / 2f
                    val focusY = (event.getY(0) + event.getY(1)) / 2f

                    val dx = focusX - lastFocusX
                    val dy = focusY - lastFocusY

                    matrix.postTranslate(dx, dy)
                    clampToBounds()
                    applyMatrix()
                    notifyMatrixChanged()

                    lastFocusX = focusX
                    lastFocusY = focusY
                }
            }

            MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP -> {
                isDragging = false
            }
        }
        return true
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            val newScale = currentScale * scaleFactor
            val limitedScale = min(max(newScale, minScale), maxScale)
            val adjFactor = limitedScale / currentScale
            matrix.postScale(adjFactor, adjFactor, detector.focusX, detector.focusY)
            currentScale = limitedScale
            clampToBounds()
            applyMatrix()
            notifyMatrixChanged()
            return true
        }

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean = true
    }

    private fun clampToBounds() {
        val imageView = getImageView() ?: return
        val drawable = imageView.drawable ?: return

        val viewW = width.toFloat()
        val viewH = height.toFloat()
        val rect = RectF(0f, 0f, drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())
        matrix.mapRect(rect)

        var dx = 0f
        var dy = 0f

        if (rect.width() > viewW) {
            if (rect.left > 0f) dx = -rect.left
            if (rect.right < viewW) dx = viewW - rect.right
        }

        if (rect.height() > viewH) {
            if (rect.top > 0f) dy = -rect.top
            if (rect.bottom < viewH) dy = viewH - rect.bottom
        }


        if (rect.width() <= viewW) dx = 0f
        if (rect.height() <= viewH) dy = 0f

        if (dx != 0f || dy != 0f) matrix.postTranslate(dx, dy)
    }

    fun resetView() = adjustImageToFit()
}
