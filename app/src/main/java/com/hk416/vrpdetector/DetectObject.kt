package com.hk416.vrpdetector

import kotlin.math.max
import kotlin.math.min

data class DetectRect(
    var top: Float,
    var left: Float,
    var bottom: Float,
    var right: Float
) {
    fun area(): Float {
        val width = right - left
        val height = bottom - top
        return if (width < 0.0f || height < 0.0f) 0.0f else width * height
    }

    fun intersects(other: DetectRect): DetectRect {
        return DetectRect(
            top = max(this.top, other.top),
            left = max(this.left, other.left),
            bottom = min(this.bottom, other.bottom),
            right = min(this.right, other.right)
        )
    }
}

data class DetectObject(
    val cls: Int,
    var offsetX: Float,
    var offsetY: Float,
    var rect: DetectRect,
    var conf: Float,
) {
    fun iou(other: DetectObject): Float {
        val aBoxArea = this.rect.area()
        val bBoxArea = other.rect.area()
        val intersectsArea = this.rect.intersects(other.rect).area()
        return intersectsArea / (aBoxArea + bBoxArea - intersectsArea)
    }
}