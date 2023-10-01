package com.hk416.vrpdetector

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class OutlineView(context: Context, attributeSet: AttributeSet): View(context, attributeSet) {
    private var boxes: ArrayList<DetectObject>? = null
    companion object {
        private val linePaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.FILL
            strokeWidth = 5.0f
        }
    }

    fun setBoxes(boxes: ArrayList<DetectObject>) {
        this.boxes = boxes
    }

    override fun onDraw(canvas: Canvas?) {
        drawBoxes(canvas)
        super.onDraw(canvas)
    }

    private fun drawBoxes(canvas: Canvas?) {
        boxes?.forEach {
            val startX = (it.rect.left + it.offsetX) * width
            val startY = (it.rect.top + it.offsetY) * height
            val stopX = (it.rect.right + it.offsetX) * width
            val stopY = (it.rect.bottom + it.offsetY) * height

            drawLine(canvas, startX, startY, stopX, startY)
            drawLine(canvas, startX, stopY, stopX, stopY)
            drawLine(canvas, startX, startY, startX, stopY)
            drawLine(canvas, stopX, startY, stopX, stopY)
        }
    }

    private fun drawLine(
        canvas: Canvas?,
        startX: Float,
        startY: Float,
        stopX: Float,
        stopY: Float
    ) {
        if (startX > 0 && startY > 0 && stopX > 0 && stopY > 0) {
            canvas?.drawLine(startX, startY, stopX, stopY, linePaint)
        }
    }
}