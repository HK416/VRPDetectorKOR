package com.hk416.vrpdetector

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.nio.FloatBuffer
import java.util.Collections
import kotlin.math.max
import kotlin.math.min

class PlateDetectPass(val context: Context) {
    private lateinit var session: OrtSession

    companion object {
        private const val THRESHOLD = 0.5f
        private const val CONFIDENCE = 0.65f
        private const val MODEL_FILE_NAME = "yolov8n_plate_detect.onnx"
        private const val BATCH_SIZE = 1
        private const val PIXEL_SIZE = 3
        private const val INPUT_SIZE = 640
        private val INPUT_SHAPE = longArrayOf(
            BATCH_SIZE.toLong(),
            PIXEL_SIZE.toLong(),
            INPUT_SIZE.toLong(),
            INPUT_SIZE.toLong()
        )
        private val OUTPUT_SHAPE = intArrayOf(1, 5, 8400)
    }

    fun loadModel(ortEnvironment: OrtEnvironment) {
        val assetManager = context.assets
        val outputStream = ByteArrayOutputStream()
        assetManager.open(MODEL_FILE_NAME).use { inputStream ->
            inputStream.copyTo(outputStream)
        }
        session = ortEnvironment.createSession(
            outputStream.toByteArray(),
            OrtSession.SessionOptions()
        )
    }

    fun process(ortEnvironment: OrtEnvironment, imageProxy: ImageProxy): ArrayList<DetectObject> {
        val bitmap = imageToScaledBitmap(imageProxy)
        val buffer = bitmapToFloatBuffer(bitmap)
        val inputName = session.inputNames.iterator().next()
        val inputTensor = OnnxTensor.createTensor(ortEnvironment, buffer, INPUT_SHAPE)
        val resultTensor = session.run(Collections.singletonMap(inputName, inputTensor))
        val objects = parseResultTensor(resultTensor)
        return greedyNMS(objects)
    }

    private fun imageToScaledBitmap(imageProxy: ImageProxy): Bitmap {
        return Bitmap.createScaledBitmap(
            imageProxy.toBitmap(),
            INPUT_SIZE,
            INPUT_SIZE,
            true
        )
    }

    private fun bitmapToFloatBuffer(bitmap: Bitmap): FloatBuffer {
        val size = INPUT_SIZE * INPUT_SIZE
        val bitmapData = IntArray(size)
        bitmap.getPixels(bitmapData, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        val floatBuffer = FloatBuffer.allocate(BATCH_SIZE * PIXEL_SIZE * INPUT_SIZE * INPUT_SIZE)
        floatBuffer.rewind()

        var idx = 0
        for (pixel in bitmapData) {
            floatBuffer.put(idx + 0 * size, ((pixel shr 16) and 0x000000FF) / 255.0f) // R
            floatBuffer.put(idx + 1 * size, ((pixel shr 8) and 0x000000FF) / 255.0f) // G
            floatBuffer.put(idx + 2 * size, ((pixel shr 0) and 0x000000FF) / 255.0f) // B
            idx += 1
        }

        floatBuffer.rewind()
        return floatBuffer
    }

    private fun parseResultTensor(resultTensor: OrtSession.Result): ArrayList<DetectObject> {
        val outputs = Array(OUTPUT_SHAPE[0]) { Array(OUTPUT_SHAPE[1]) { FloatArray(OUTPUT_SHAPE[2]) }}
        for (i in 0 until OUTPUT_SHAPE[0]) {
            val tensor = resultTensor[i].value as Array<*>
            for (j in 0 until OUTPUT_SHAPE[1]) {
                val rows = (tensor[0] as Array<*>)[j] as FloatArray
                for (k in 0 until OUTPUT_SHAPE[2]) {
                    outputs[i][j][k] = rows[k]
                }
            }
        }

        val objects = ArrayList<DetectObject>()
        for (i in 0 until OUTPUT_SHAPE[0]) {
            for (k in 0 until OUTPUT_SHAPE[2]) {
                var cls = -1
                var conf = 0.0f;
                for (j in 4 until OUTPUT_SHAPE[1]) {
                    if (conf < outputs[i][j][k]) {
                        cls = j - 4
                        conf = outputs[i][j][k]
                    }
                }

                if (conf >= CONFIDENCE && cls != -1) {
                    val xOrigin = outputs[i][0][k]
                    val yOrigin = outputs[i][1][k]
                    val width = outputs[i][2][k]
                    val height = outputs[i][3][k]
                    val rect = DetectRect(
                        top = max(yOrigin - 0.5f * height, 0.0f) / INPUT_SIZE.toFloat(),
                        left = max(xOrigin - 0.5f * width, 0.0f) / INPUT_SIZE.toFloat(),
                        bottom = min(yOrigin + 0.5f * height, INPUT_SIZE - 1.0f) / INPUT_SIZE.toFloat(),
                        right = min(xOrigin + 0.5f * width, INPUT_SIZE - 1.0f) / INPUT_SIZE.toFloat()
                    )

                    objects.add(DetectObject(cls, offsetX = 0.0f, offsetY = 0.0f, rect, conf))
                }
            }
        }
        return objects
    }

    private fun greedyNMS(objects: ArrayList<DetectObject>): ArrayList<DetectObject> {
        val results = ArrayList<DetectObject>()
        while (objects.isNotEmpty()) {
            objects.sortByDescending { it.conf }
            val maxConfObject = objects.removeFirst()
            results.add(maxConfObject)

            val temp = ArrayList<DetectObject>()
            for (otherObject in objects) {
                if (maxConfObject.iou(otherObject) < THRESHOLD) {
                    temp.add(otherObject)
                }
            }

            objects.clear()
            objects.addAll(temp)
            temp.clear()
        }
        return results
    }
}