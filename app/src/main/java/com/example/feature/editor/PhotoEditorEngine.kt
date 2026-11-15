package com.example.feature.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import kotlin.math.min

enum class CropAspectPreset(val label: String, val ratio: Float?) {
    FREE("Original", null),
    SQUARE("1:1", 1f),
    RATIO_4_3("4:3", 4f / 3f),
    RATIO_16_9("16:9", 16f / 9f),
    RATIO_3_4("3:4", 3f / 4f),
}

enum class EditorFilterPreset(val label: String) {
    NONE("Original"),
    NOIR("Noir B&W"),
    VIVID("Vivid"),
    WARM_FILM("Warm Film"),
    COOL_CYAN("Cool Cyan"),
    MATTE("Matte Fade"),
    HIGH_CONTRAST("Punch"),
}

data class PhotoEditState(
    val rotationDegrees: Int = 0,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val cropPreset: CropAspectPreset = CropAspectPreset.FREE,
    val brightness: Float = 0f,     // -100..100
    val contrast: Float = 1f,       // 0.5..1.8
    val saturation: Float = 1f,     // 0..2
    val temperature: Float = 0f,    // -50..50
    val filter: EditorFilterPreset = EditorFilterPreset.NONE,
    val overlayText: String = "",
    val overlayTextColor: Int = android.graphics.Color.WHITE,
    val stickerText: String = "",
)

object PhotoEditorEngine {

    fun renderEditedBitmap(source: Bitmap, state: PhotoEditState): Bitmap {
        // 1. Rotate & Flip
        val matrix = Matrix().apply {
            postRotate(state.rotationDegrees.toFloat())
            postScale(
                if (state.flipHorizontal) -1f else 1f,
                if (state.flipVertical) -1f else 1f,
            )
        }
        val transformed = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)

        // 2. Center Crop by Aspect Preset
        val cropped = cropToPreset(transformed, state.cropPreset)
        if (cropped !== transformed && transformed !== source) {
            transformed.recycle()
        }

        // 3. Apply ColorMatrix (Brightness, Contrast, Saturation, Temperature, Filter)
        val out = Bitmap.createBitmap(cropped.width, cropped.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val cm = buildCombinedColorMatrix(state)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(cropped, 0f, 0f, paint)
        if (cropped !== source) {
            cropped.recycle()
        }

        // 4. Overlay Text & Sticker
        val minDim = min(out.width, out.height).toFloat()
        if (state.overlayText.isNotBlank()) {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = state.overlayTextColor
                textSize = (minDim * 0.065f).coerceAtLeast(24f)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                setShadowLayer(8f, 2f, 2f, android.graphics.Color.BLACK)
            }
            canvas.drawText(
                state.overlayText,
                out.width / 2f,
                out.height * 0.86f,
                textPaint,
            )
        }

        if (state.stickerText.isNotBlank()) {
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.argb(190, 12, 16, 24)
            }
            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                textSize = (minDim * 0.045f).coerceAtLeast(18f)
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }
            val pad = minDim * 0.03f
            val textW = labelPaint.measureText(state.stickerText)
            val left = out.width * 0.06f
            val top = out.height * 0.06f
            canvas.drawRoundRect(
                left,
                top,
                left + textW + pad * 2f,
                top + labelPaint.textSize + pad * 1.4f,
                pad,
                pad,
                badgePaint,
            )
            canvas.drawText(
                state.stickerText,
                left + pad,
                top + labelPaint.textSize + pad * 0.45f,
                labelPaint,
            )
        }

        return out
    }

    private fun cropToPreset(bmp: Bitmap, preset: CropAspectPreset): Bitmap {
        val targetRatio = preset.ratio ?: return bmp
        val currentRatio = bmp.width.toFloat() / bmp.height.toFloat().coerceAtLeast(1f)
        if (kotlin.math.abs(currentRatio - targetRatio) < 0.01f) return bmp

        val cropW: Int
        val cropH: Int
        if (currentRatio > targetRatio) {
            cropH = bmp.height
            cropW = (cropH * targetRatio).toInt().coerceIn(1, bmp.width)
        } else {
            cropW = bmp.width
            cropH = (cropW / targetRatio).toInt().coerceIn(1, bmp.height)
        }
        val x = ((bmp.width - cropW) / 2).coerceAtLeast(0)
        val y = ((bmp.height - cropH) / 2).coerceAtLeast(0)
        return Bitmap.createBitmap(bmp, x, y, cropW, cropH)
    }

    private fun buildCombinedColorMatrix(state: PhotoEditState): ColorMatrix {
        val result = ColorMatrix()

        // Saturation
        val sat = ColorMatrix().apply { setSaturation(state.saturation) }
        result.postConcat(sat)

        // Contrast & Brightness
        val c = state.contrast
        val t = (1f - c) * 128f + state.brightness
        val bc = ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, t,
                0f, c, 0f, 0f, t,
                0f, 0f, c, 0f, t,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
        result.postConcat(bc)

        // Temperature (warm/cool R-B shift)
        if (state.temperature != 0f) {
            val shift = state.temperature * 0.9f
            val temp = ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, shift,
                    0f, 1f, 0f, 0f, 0f,
                    0f, 0f, 1f, 0f, -shift,
                    0f, 0f, 0f, 1f, 0f,
                ),
            )
            result.postConcat(temp)
        }

        // Preset Filter
        val filterMatrix = when (state.filter) {
            EditorFilterPreset.NONE -> null
            EditorFilterPreset.NOIR -> ColorMatrix().apply { setSaturation(0f) }
            EditorFilterPreset.VIVID -> ColorMatrix().apply { setSaturation(1.42f) }
            EditorFilterPreset.WARM_FILM -> ColorMatrix(
                floatArrayOf(
                    1.08f, 0.04f, 0f, 0f, 10f,
                    0f, 1.02f, 0f, 0f, 4f,
                    0f, 0f, 0.9f, 0f, -8f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            )
            EditorFilterPreset.COOL_CYAN -> ColorMatrix(
                floatArrayOf(
                    0.92f, 0f, 0f, 0f, -6f,
                    0f, 1.02f, 0.05f, 0f, 4f,
                    0f, 0.04f, 1.12f, 0f, 12f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            )
            EditorFilterPreset.MATTE -> ColorMatrix(
                floatArrayOf(
                    0.9f, 0f, 0f, 0f, 18f,
                    0f, 0.9f, 0f, 0f, 18f,
                    0f, 0f, 0.9f, 0f, 20f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            )
            EditorFilterPreset.HIGH_CONTRAST -> ColorMatrix(
                floatArrayOf(
                    1.32f, 0f, 0f, 0f, -38f,
                    0f, 1.32f, 0f, 0f, -38f,
                    0f, 0f, 1.32f, 0f, -38f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            )
        }
        if (filterMatrix != null) {
            result.postConcat(filterMatrix)
        }
        return result
    }
}
