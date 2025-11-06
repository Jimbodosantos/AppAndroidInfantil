package com.example.sonrieaprende

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class StrokeSerializable(val points: FloatArray, val color: Int, val strokeWidth: Float)


class DrawingCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var currentColor = Color.parseColor("#FF6B6B")
    private var currentBrushSize = 8f

    private var tempPoints: MutableList<Float> = mutableListOf()
    private var tempPath = Path()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private var baseImageBitmap: Bitmap? = null

    // Strokes committed
    private val strokes: MutableList<StrokeSerializable> = mutableListOf()
    private val undone: MutableList<StrokeSerializable> = mutableListOf()

    private val transformMatrix = Matrix()
    private val inverseMatrix = Matrix()

    var onMatrixChanged: ((Matrix) -> Unit)? = null
    var onHistoryChanged: (() -> Unit)? = null

    init { paint.color = currentColor; paint.strokeWidth = currentBrushSize }


    fun setBaseImageBitmap(bitmap: Bitmap) {
        baseImageBitmap = bitmap
        strokes.clear()
        undone.clear()
        onHistoryChanged?.invoke()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Dibujar imagen base transformada
        baseImageBitmap?.let {
            canvas.save()
            canvas.concat(transformMatrix)
            canvas.drawBitmap(it, 0f, 0f, null)
            // Dibujar strokes
            for (s in strokes) {
                paint.color = s.color
                paint.strokeWidth = s.strokeWidth
                canvas.drawPath(pathFromPoints(s.points), paint)
            }
            // Dibujar trazo actual
            if (tempPoints.isNotEmpty()) {
                paint.color = currentColor
                paint.strokeWidth = currentBrushSize
                canvas.drawPath(tempPath, paint)
            }
            canvas.restore()
        } ?: run {
            // Si no hay image base, dibujar sólo strokes en coords de vista
            for (s in strokes) {
                paint.color = s.color
                paint.strokeWidth = s.strokeWidth
                canvas.drawPath(pathFromPoints(s.points), paint)
            }
            if (tempPoints.isNotEmpty()) {
                paint.color = currentColor
                paint.strokeWidth = currentBrushSize
                canvas.drawPath(tempPath, paint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.pointerCount != 1) return false
        // mapear
        if (!transformMatrix.invert(inverseMatrix)) inverseMatrix.reset()
        val pts = floatArrayOf(event.x, event.y)
        inverseMatrix.mapPoints(pts)
        val x = pts[0]; val y = pts[1]

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                tempPoints.clear()
                tempPoints.add(x); tempPoints.add(y)
                tempPath.reset()
                tempPath.moveTo(x, y)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                // agregar puntos
                tempPoints.add(x); tempPoints.add(y)
                val lx = tempPoints[tempPoints.size - 4]
                val ly = tempPoints[tempPoints.size - 3]
                val mx = (lx + x) / 2f
                val my = (ly + y) / 2f
                tempPath.quadTo(lx, ly, mx, my)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                // finalizar stroke
                tempPoints.add(x); tempPoints.add(y)
                val strokeArr = FloatArray(tempPoints.size)
                for (i in tempPoints.indices) strokeArr[i] = tempPoints[i]
                val s = StrokeSerializable(strokeArr, currentColor, currentBrushSize)
                strokes.add(s)
                tempPath.reset()
                tempPoints.clear()
                // al confirmar nuevo stroke limpiar redo y notificar
                undone.clear()
                onHistoryChanged?.invoke()
                invalidate()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                tempPath.reset()
                tempPoints.clear()
                invalidate()
            }
        }
        return false
    }

    // Convierte array de puntos a Path
    private fun pathFromPoints(points: FloatArray): Path {
        val p = Path()
        if (points.isEmpty()) return p
        p.moveTo(points[0], points[1])
        var i = 2
        while (i + 1 < points.size) {
            val x0 = points[i - 2]; val y0 = points[i - 1]
            val x1 = points[i]; val y1 = points[i + 1]
            val mx = (x0 + x1) / 2f
            val my = (y0 + y1) / 2f
            p.quadTo(x0, y0, mx, my)
            i += 2
        }
        return p
    }

    fun setBrushColor(color: Int) { currentColor = color; paint.color = color }
    fun setBrushSize(size: Float) { currentBrushSize = size; paint.strokeWidth = size }

    fun undo(): Boolean {
        if (strokes.isEmpty()) return false
        val last = strokes.removeAt(strokes.size - 1)
        undone.add(last)
        onHistoryChanged?.invoke()
        invalidate()
        return true
    }

    fun redo(): Boolean {
        if (undone.isEmpty()) return false
        val s = undone.removeAt(undone.size - 1)
        strokes.add(s)
        onHistoryChanged?.invoke()
        invalidate()
        return true
    }

    fun clearCanvas() {
        if (strokes.isNotEmpty() || tempPoints.isNotEmpty()) {
            strokes.clear()
            undone.clear()
            tempPoints.clear()
            tempPath.reset()
            onHistoryChanged?.invoke()
            invalidate()
        }
    }

    fun canUndo(): Boolean = strokes.isNotEmpty()
    fun canRedo(): Boolean = undone.isNotEmpty()

    // Renderiza y devuelve bitmap combinado
    fun getCombinedBitmap(): Bitmap? {
        val base = baseImageBitmap ?: return null
        val out = Bitmap.createBitmap(base.width, base.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.drawBitmap(base, 0f, 0f, null)
        val tmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
        for (s in strokes) {
            tmpPaint.color = s.color
            tmpPaint.strokeWidth = s.strokeWidth
            c.drawPath(pathFromPoints(s.points), tmpPaint)
        }
        return out
    }


    fun toJson(): JSONObject {
        val root = JSONObject()
        val arrS = JSONArray()
        for (s in strokes) arrS.put(strokeToJson(s))
        root.put("strokes", arrS)
        val arrU = JSONArray()
        for (s in undone) arrU.put(strokeToJson(s))
        root.put("undone", arrU)
        return root
    }

    private fun strokeToJson(s: StrokeSerializable): JSONObject {
        val jo = JSONObject()
        val pts = JSONArray()
        for (v in s.points) pts.put(v)
        jo.put("points", pts)
        jo.put("color", s.color)
        jo.put("width", s.strokeWidth)
        return jo
    }


    fun fromJson(root: JSONObject) {
        strokes.clear(); undone.clear()
        val arrS = root.optJSONArray("strokes") ?: JSONArray()
        for (i in 0 until arrS.length()) {
            val j = arrS.getJSONObject(i)
            strokes.add(jsonToStroke(j))
        }
        val arrU = root.optJSONArray("undone") ?: JSONArray()
        for (i in 0 until arrU.length()) {
            val j = arrU.getJSONObject(i)
            undone.add(jsonToStroke(j))
        }
        onHistoryChanged?.invoke()
        invalidate()
    }

    private fun jsonToStroke(j: JSONObject): StrokeSerializable {
        val ptsArr = j.getJSONArray("points")
        val pts = FloatArray(ptsArr.length())
        for (k in 0 until ptsArr.length()) pts[k] = ptsArr.getDouble(k).toFloat()
        val color = j.getInt("color")
        val w = j.getDouble("width").toFloat()
        return StrokeSerializable(pts, color, w)
    }

    fun setTransformMatrix(matrix: Matrix) {
        transformMatrix.set(matrix)
        invalidate()
    }

    // Guarda JSON en disco (archivo simple)
    fun saveHistoryToFile(file: File) {
        try {
            val json = toJson()
            file.parentFile?.mkdirs()
            file.writeText(json.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Restaura desde archivo JSON
    fun restoreHistoryFromFile(file: File) {
        try {
            if (!file.exists()) return
            val text = file.readText()
            val jo = JSONObject(text)
            fromJson(jo)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
