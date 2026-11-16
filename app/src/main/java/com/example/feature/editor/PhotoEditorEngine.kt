package com.example.feature.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class CropAspectPreset(val label: String, val ratio: Float?) {
    FREE("Original", null),
    SQUARE("1:1", 1f),
    RATIO_4_5("4:5", 4f / 5f),
    RATIO_4_3("4:3", 4f / 3f),
    RATIO_3_2("3:2", 3f / 2f),
    RATIO_16_9("16:9", 16f / 9f),
    RATIO_9_16("9:16", 9f / 16f),
}

enum class EditorFilterPreset(val label: String) {
    NONE("Original"),
    NOIR("Noir B&W"),
    VIVID("Vivid"),
    WARM_FILM("Warm Film"),
    COOL_CYAN("Cool Cyan"),
    MATTE("Matte Fade"),
    HIGH_CONTRAST("Punch"),
    GOLDEN_HOUR("Golden Hour"),
    CYBER_NEON("Cyber Neon"),
    SEPIA_PRINT("Sepia Print"),
    BLEACH_BYPASS("Bleach Bypass"),
    TEAL_ORANGE("Teal & Orange"),
    DREAMY_PASTEL("Dreamy Pastel"),
    AMOLED_DARK("Deep Noir"),
}

enum class EditorCurvesPreset(val label: String) {
    LINEAR("Linear"),
    S_CURVE("S-Curve"),
    CINEMA_FADE("Cinema Fade"),
    CROSS_PROCESS("Cross Process"),
    HIGH_KEY("High Key"),
}

enum class EditorDuotonePreset(val label: String, val shadowColor: Int, val highlightColor: Int) {
    NONE("None", 0, 0),
    AMBER_NOIR("Amber Noir", 0xFF1A1008.toInt(), 0xFFF5C469.toInt()),
    CYAN_COBALT("Cyan Cobalt", 0xFF071329.toInt(), 0xFF5CE1E6.toInt()),
    ROSE_VIOLET("Rose Violet", 0xFF1D0B2B.toInt(), 0xFFF472B6.toInt()),
    EMERALD_GOLD("Emerald Gold", 0xFF061B14.toInt(), 0xFFA7F3D0.toInt()),
}

enum class EditorLightLeakPreset(val label: String) {
    NONE("None"),
    AMBER_CORNER("Amber Leak"),
    ROSE_FLARE("Rose Flare"),
    PRISM_BEAM("Prism Beam"),
}

enum class EditorFramePreset(val label: String) {
    NONE("No Frame"),
    THIN_WHITE("Gallery Thin"),
    WIDE_WHITE("Museum Mat"),
    POLAROID("Instant Print"),
    DARK_CINEMA("Cinema Dark"),
}

enum class EditorGradientOverlay(val label: String) {
    NONE("None"),
    SUNSET("Sunset"),
    OCEAN("Deep Ocean"),
    CYBER("Cyber Violet"),
    VIGNETTE_TOP("Cinema Top"),
}

enum class EditorShapeOverlay(val label: String) {
    NONE("None"),
    CIRCLE_BADGE("Circle Frame"),
    ROUNDED_BORDER("Inner Border"),
    CINEMA_BARS("2.39:1 Bars"),
}

enum class EditorTextFontStyle(val label: String) {
    SANS("Modern"),
    SERIF("Editorial"),
    MONO("Tech Mono"),
}

enum class EditorTextAlign(val label: String) {
    LEFT("Left"),
    CENTER("Center"),
    RIGHT("Right"),
}

enum class EditorExportFormat(val label: String, val mime: String) {
    JPEG("JPEG", "image/jpeg"),
    PNG("PNG", "image/png"),
    WEBP("WebP", "image/webp"),
}

data class EditorTextLayer(
    val id: String,
    val text: String,
    val colorInt: Int = android.graphics.Color.WHITE,
    val fontStyle: EditorTextFontStyle = EditorTextFontStyle.SANS,
    val align: EditorTextAlign = EditorTextAlign.CENTER,
    val hasShadow: Boolean = true,
    val hasOutline: Boolean = false,
    val sizeScale: Float = 1.0f,
    val xFraction: Float = 0.5f,
    val yFraction: Float = 0.84f,
)

data class EditorBrushStroke(
    val points: List<Pair<Float, Float>>, // normalized 0f..1f coordinates
    val colorInt: Int,
    val widthFraction: Float = 0.012f,
    val isEraser: Boolean = false,
)

data class PhotoEditState(
    // Basic geometry
    val rotationDegrees: Int = 0,
    val straightenDegrees: Float = 0f, // -25..25
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val perspectiveX: Float = 0f, // -0.25..0.25
    val perspectiveY: Float = 0f, // -0.25..0.25
    val cropPreset: CropAspectPreset = CropAspectPreset.FREE,
    val freeCropLeft: Float = 0f, // 0..0.4
    val freeCropTop: Float = 0f, // 0..0.4
    val freeCropRight: Float = 1f, // 0.6..1
    val freeCropBottom: Float = 1f, // 0.6..1
    val resizeScale: Float = 1.0f, // 0.25..1.0
    // Pro light & color adjustments
    val exposure: Float = 0f, // -2..2 EV
    val brightness: Float = 0f, // -100..100
    val contrast: Float = 1f, // 0.5..1.8
    val saturation: Float = 1f, // 0..2
    val vibrance: Float = 0f, // -1..1
    val temperature: Float = 0f, // -50..50
    val tint: Float = 0f, // -50..50
    val highlights: Float = 0f, // -1..1
    val shadows: Float = 0f, // -1..1
    val whites: Float = 0f, // -1..1
    val blacks: Float = 0f, // -1..1
    val sharpness: Float = 0f, // 0..1
    val clarity: Float = 0f, // 0..1
    val dehaze: Float = 0f, // 0..1
    val fade: Float = 0f, // 0..1
    val vignette: Float = 0f, // 0..1
    val grain: Float = 0f, // 0..1
    val denoise: Float = 0f, // 0..1
    val hueShiftDegrees: Float = 0f, // -180..180
    val curvesPreset: EditorCurvesPreset = EditorCurvesPreset.LINEAR,
    val splitToneBalance: Float = 0f, // -1 (cool) .. +1 (warm)
    // Creative effects
    val filter: EditorFilterPreset = EditorFilterPreset.NONE,
    val duotone: EditorDuotonePreset = EditorDuotonePreset.NONE,
    val blurAmount: Float = 0f, // 0..1
    val lightLeak: EditorLightLeakPreset = EditorLightLeakPreset.NONE,
    val glowAmount: Float = 0f, // 0..1
    val framePreset: EditorFramePreset = EditorFramePreset.NONE,
    val gradientOverlay: EditorGradientOverlay = EditorGradientOverlay.NONE,
    // Design layers
    val textLayers: List<EditorTextLayer> = emptyList(),
    val stickerText: String = "",
    val shapeOverlay: EditorShapeOverlay = EditorShapeOverlay.NONE,
    val watermarkText: String = "",
    val brushStrokes: List<EditorBrushStroke> = emptyList(),
)

object PhotoEditorEngine {

    fun renderEditedBitmap(source: Bitmap, state: PhotoEditState): Bitmap {
        // 1. Rotate, Straighten & Flip
        val matrix = Matrix().apply {
            val totalAngle = state.rotationDegrees.toFloat() + state.straightenDegrees
            postRotate(totalAngle)
            postScale(
                if (state.flipHorizontal) -1f else 1f,
                if (state.flipVertical) -1f else 1f,
            )
        }
        val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)

        // 2. Perspective Correction if non-zero
        val perspectived = if (abs(state.perspectiveX) > 0.01f || abs(state.perspectiveY) > 0.01f) {
            applyPerspective(rotated, state.perspectiveX, state.perspectiveY).also {
                if (it !== rotated && rotated !== source) rotated.recycle()
            }
        } else {
            rotated
        }

        // 3. Freeform Crop + Aspect Preset Crop
        val freeCropped = applyFreeformCrop(perspectived, state)
        if (freeCropped !== perspectived && perspectived !== source) perspectived.recycle()

        val cropped = cropToPreset(freeCropped, state.cropPreset)
        if (cropped !== freeCropped && freeCropped !== source) freeCropped.recycle()

        // 4. Resize scale if requested
        val scaled = if (state.resizeScale < 0.99f) {
            val targetW = (cropped.width * state.resizeScale).roundToInt().coerceAtLeast(32)
            val targetH = (cropped.height * state.resizeScale).roundToInt().coerceAtLeast(32)
            Bitmap.createScaledBitmap(cropped, targetW, targetH, true).also {
                if (it !== cropped && cropped !== source) cropped.recycle()
            }
        } else {
            cropped
        }

        // 5. Base ColorMatrix Pass (Exposure, Brightness, Contrast, Saturation, Vibrance, Temp, Tint, Hue, Tone, Filter, Curves, Split Tone)
        var working = Bitmap.createBitmap(scaled.width, scaled.height, Bitmap.Config.ARGB_8888)
        run {
            val canvas = Canvas(working)
            val cm = buildCombinedColorMatrix(state)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(cm)
            }
            canvas.drawBitmap(scaled, 0f, 0f, paint)
        }
        if (scaled !== source) scaled.recycle()

        // 6. Duotone remap if active
        if (state.duotone != EditorDuotonePreset.NONE) {
            working = applyDuotone(working, state.duotone)
        }

        // 7. Soft Blur / Denoise / Glow
        if (state.blurAmount > 0.02f || state.denoise > 0.05f) {
            val strength = max(state.blurAmount, state.denoise * 0.45f)
            working = applyFastBoxBlurBlend(working, strength, blendAlpha = if (state.blurAmount > 0.02f) 1f else 0.45f)
        }

        if (state.glowAmount > 0.03f) {
            val bloom = applyFastBoxBlurBlend(working.copy(Bitmap.Config.ARGB_8888, false), 0.7f, 1f)
            val c = Canvas(working)
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                alpha = (state.glowAmount * 115f).toInt().coerceIn(0, 140)
            }
            c.drawBitmap(bloom, 0f, 0f, p)
            bloom.recycle()
        }

        // 8. Sharpness & Clarity (Unsharp Mask convolution approximation)
        val detailAmount = (state.sharpness + state.clarity * 0.65f).coerceIn(0f, 1.5f)
        if (detailAmount > 0.04f) {
            working = applySharpen(working, detailAmount)
        }

        // 9. Creative Overlays (Vignette, Grain, Gradient Overlay, Light Leak, Brush, Shapes, Text, Watermark)
        val overlayCanvas = Canvas(working)
        val w = working.width.toFloat()
        val h = working.height.toFloat()
        val minDim = min(w, h)

        if (state.vignette > 0.02f) {
            val alpha = (state.vignette * 195f).toInt().coerceIn(0, 210)
            val vPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    w / 2f,
                    h / 2f,
                    max(w, h) * 0.72f,
                    intArrayOf(android.graphics.Color.TRANSPARENT, android.graphics.Color.argb(alpha, 0, 0, 0)),
                    floatArrayOf(0.45f, 1f),
                    Shader.TileMode.CLAMP,
                )
            }
            overlayCanvas.drawRect(0f, 0f, w, h, vPaint)
        }

        if (state.grain > 0.02f) {
            drawDeterministicGrain(working, state.grain)
        }

        drawGradientOverlay(overlayCanvas, w, h, state.gradientOverlay)
        drawLightLeak(overlayCanvas, w, h, state.lightLeak)
        drawShapeOverlay(overlayCanvas, w, h, state.shapeOverlay)

        // Brush & Eraser strokes (eraser blends dark soft background cover or neutral gray when standalone)
        if (state.brushStrokes.isNotEmpty()) {
            state.brushStrokes.forEach { stroke ->
                if (stroke.points.size >= 2) {
                    val path = Path()
                    val first = stroke.points.first()
                    path.moveTo(first.first * w, first.second * h)
                    for (idx in 1 until stroke.points.size) {
                        val pt = stroke.points[idx]
                        path.lineTo(pt.first * w, pt.second * h)
                    }
                    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                        strokeWidth = (stroke.widthFraction * minDim).coerceAtLeast(4f)
                        color = if (stroke.isEraser) android.graphics.Color.argb(220, 24, 24, 28) else stroke.colorInt
                    }
                    overlayCanvas.drawPath(path, strokePaint)
                }
            }
        }

        // Multi-layer Text
        state.textLayers.forEach { layer ->
            if (layer.text.isNotBlank()) {
                val baseTypeface = when (layer.fontStyle) {
                    EditorTextFontStyle.SANS -> Typeface.DEFAULT
                    EditorTextFontStyle.SERIF -> Typeface.SERIF
                    EditorTextFontStyle.MONO -> Typeface.MONOSPACE
                }
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = layer.colorInt
                    textSize = (minDim * 0.068f * layer.sizeScale).coerceAtLeast(22f)
                    typeface = Typeface.create(baseTypeface, Typeface.BOLD)
                    textAlign = when (layer.align) {
                        EditorTextAlign.LEFT -> Paint.Align.LEFT
                        EditorTextAlign.CENTER -> Paint.Align.CENTER
                        EditorTextAlign.RIGHT -> Paint.Align.RIGHT
                    }
                    if (layer.hasShadow) {
                        setShadowLayer(8f, 2f, 2f, android.graphics.Color.BLACK)
                    }
                }
                val px = (layer.xFraction * w).coerceIn(16f, w - 16f)
                val py = (layer.yFraction * h).coerceIn(textPaint.textSize, h - 16f)
                if (layer.hasOutline) {
                    val strokePaint = Paint(textPaint).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = (textPaint.textSize * 0.1f).coerceAtLeast(2f)
                        color = android.graphics.Color.BLACK
                    }
                    overlayCanvas.drawText(layer.text, px, py, strokePaint)
                }
                overlayCanvas.drawText(layer.text, px, py, textPaint)
            }
        }

        // Sticker stamp
        if (state.stickerText.isNotBlank()) {
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.argb(196, 12, 16, 24)
            }
            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                textSize = (minDim * 0.045f).coerceAtLeast(18f)
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }
            val pad = minDim * 0.03f
            val textW = labelPaint.measureText(state.stickerText)
            val left = w * 0.05f
            val top = h * 0.05f
            overlayCanvas.drawRoundRect(
                left,
                top,
                left + textW + pad * 2f,
                top + labelPaint.textSize + pad * 1.4f,
                pad,
                pad,
                badgePaint,
            )
            overlayCanvas.drawText(state.stickerText, left + pad, top + labelPaint.textSize + pad * 0.45f, labelPaint)
        }

        // Watermark
        if (state.watermarkText.isNotBlank()) {
            val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.argb(175, 255, 255, 255)
                textSize = (minDim * 0.03f).coerceAtLeast(14f)
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
                setShadowLayer(4f, 1f, 1f, android.graphics.Color.BLACK)
            }
            overlayCanvas.drawText(state.watermarkText, w - minDim * 0.04f, h - minDim * 0.04f, wmPaint)
        }

        // 10. Frame & Border wrap
        if (state.framePreset != EditorFramePreset.NONE) {
            working = applyFrame(working, state.framePreset)
        }

        return working
    }

    private fun applyPerspective(src: Bitmap, px: Float, py: Float): Bitmap {
        val w = src.width.toFloat()
        val h = src.height.toFloat()
        val insetX = (px * w * 0.18f)
        val insetY = (py * h * 0.18f)
        val srcPts = floatArrayOf(0f, 0f, w, 0f, w, h, 0f, h)
        val dstPts = floatArrayOf(
            max(0f, insetX), max(0f, insetY),
            w - max(0f, -insetX), max(0f, -insetY),
            w - max(0f, insetX), h - max(0f, -insetY),
            max(0f, -insetX), h - max(0f, insetY),
        )
        val m = Matrix().apply { setPolyToPoly(srcPts, 0, dstPts, 0, 4) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }

    private fun applyFreeformCrop(bmp: Bitmap, state: PhotoEditState): Bitmap {
        val left = (state.freeCropLeft * bmp.width).roundToInt().coerceIn(0, bmp.width - 16)
        val top = (state.freeCropTop * bmp.height).roundToInt().coerceIn(0, bmp.height - 16)
        val right = (state.freeCropRight * bmp.width).roundToInt().coerceIn(left + 16, bmp.width)
        val bottom = (state.freeCropBottom * bmp.height).roundToInt().coerceIn(top + 16, bmp.height)
        if (left == 0 && top == 0 && right == bmp.width && bottom == bmp.height) return bmp
        return Bitmap.createBitmap(bmp, left, top, (right - left).coerceAtLeast(1), (bottom - top).coerceAtLeast(1))
    }

    private fun cropToPreset(bmp: Bitmap, preset: CropAspectPreset): Bitmap {
        val targetRatio = preset.ratio ?: return bmp
        val currentRatio = bmp.width.toFloat() / bmp.height.toFloat().coerceAtLeast(1f)
        if (abs(currentRatio - targetRatio) < 0.01f) return bmp

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

    private fun applyDuotone(src: Bitmap, preset: EditorDuotonePreset): Bitmap {
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)
        val sR = android.graphics.Color.red(preset.shadowColor)
        val sG = android.graphics.Color.green(preset.shadowColor)
        val sB = android.graphics.Color.blue(preset.shadowColor)
        val hR = android.graphics.Color.red(preset.highlightColor)
        val hG = android.graphics.Color.green(preset.highlightColor)
        val hB = android.graphics.Color.blue(preset.highlightColor)

        for (i in pixels.indices) {
            val c = pixels[i]
            val a = c ushr 24
            val r = (c ushr 16) and 0xFF
            val g = (c ushr 8) and 0xFF
            val b = c and 0xFF
            val lum = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
            val nr = (sR + (hR - sR) * lum).toInt().coerceIn(0, 255)
            val ng = (sG + (hG - sG) * lum).toInt().coerceIn(0, 255)
            val nb = (sB + (hB - sB) * lum).toInt().coerceIn(0, 255)
            pixels[i] = (a shl 24) or (nr shl 16) or (ng shl 8) or nb
        }
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        src.recycle()
        return out
    }

    private fun applyFastBoxBlurBlend(src: Bitmap, strength: Float, blendAlpha: Float): Bitmap {
        val factor = (1f - strength.coerceIn(0f, 1f) * 0.85f).coerceIn(0.08f, 0.9f)
        val smallW = (src.width * factor).roundToInt().coerceAtLeast(8)
        val smallH = (src.height * factor).roundToInt().coerceAtLeast(8)
        val down = Bitmap.createScaledBitmap(src, smallW, smallH, true)
        val blurred = Bitmap.createScaledBitmap(down, src.width, src.height, true)
        down.recycle()
        if (blendAlpha >= 0.98f) {
            src.recycle()
            return blurred
        }
        val canvas = Canvas(src)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = (blendAlpha * 255).toInt().coerceIn(0, 255) }
        canvas.drawBitmap(blurred, 0f, 0f, paint)
        blurred.recycle()
        return src
    }

    private fun applySharpen(src: Bitmap, amount: Float): Bitmap {
        val w = src.width
        val h = src.height
        if (w < 3 || h < 3) return src
        val input = IntArray(w * h)
        val output = IntArray(w * h)
        src.getPixels(input, 0, w, 0, 0, w, h)
        val k = amount.coerceIn(0f, 1.5f) * 0.7f

        for (y in 0 until h) {
            for (x in 0 until w) {
                val idx = y * w + x
                if (x == 0 || y == 0 || x == w - 1 || y == h - 1) {
                    output[idx] = input[idx]
                } else {
                    val center = input[idx]
                    val top = input[(y - 1) * w + x]
                    val bottom = input[(y + 1) * w + x]
                    val left = input[y * w + (x - 1)]
                    val right = input[y * w + (x + 1)]
                    val a = center ushr 24
                    val rC = (center ushr 16) and 0xFF
                    val gC = (center ushr 8) and 0xFF
                    val bC = center and 0xFF
                    val avgR = (((top ushr 16) and 0xFF) + ((bottom ushr 16) and 0xFF) + ((left ushr 16) and 0xFF) + ((right ushr 16) and 0xFF)) / 4f
                    val avgG = (((top ushr 8) and 0xFF) + ((bottom ushr 8) and 0xFF) + ((left ushr 8) and 0xFF) + ((right ushr 8) and 0xFF)) / 4f
                    val avgB = ((top and 0xFF) + (bottom and 0xFF) + (left and 0xFF) + (right and 0xFF)) / 4f

                    val nr = (rC + k * (rC - avgR)).roundToInt().coerceIn(0, 255)
                    val ng = (gC + k * (gC - avgG)).roundToInt().coerceIn(0, 255)
                    val nb = (bC + k * (bC - avgB)).roundToInt().coerceIn(0, 255)
                    output[idx] = (a shl 24) or (nr shl 16) or (ng shl 8) or nb
                }
            }
        }
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(output, 0, w, 0, 0, w, h)
        src.recycle()
        return out
    }

    private fun drawDeterministicGrain(bmp: Bitmap, amount: Float) {
        val w = bmp.width
        val h = bmp.height
        val pixels = IntArray(w * h)
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)
        val amp = (amount.coerceIn(0f, 1f) * 34f).toInt()
        if (amp <= 0) return
        var seed = 0x56A13F29
        for (i in pixels.indices) {
            seed = seed * 1664525 + 1013904223
            val delta = ((seed ushr 24) % (amp * 2 + 1)) - amp
            val c = pixels[i]
            val a = c ushr 24
            val r = (((c ushr 16) and 0xFF) + delta).coerceIn(0, 255)
            val g = (((c ushr 8) and 0xFF) + delta).coerceIn(0, 255)
            val b = ((c and 0xFF) + delta).coerceIn(0, 255)
            pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    private fun drawGradientOverlay(canvas: Canvas, w: Float, h: Float, preset: EditorGradientOverlay) {
        if (preset == EditorGradientOverlay.NONE) return
        val colors = when (preset) {
            EditorGradientOverlay.SUNSET -> intArrayOf(0x55FB923C, 0x33F472B6, 0x00000000)
            EditorGradientOverlay.OCEAN -> intArrayOf(0x00000000, 0x4438BDF8, 0x660F4C81)
            EditorGradientOverlay.CYBER -> intArrayOf(0x55C084FC, 0x00000000, 0x5500F5D4)
            EditorGradientOverlay.VIGNETTE_TOP -> intArrayOf(0x77000000, 0x00000000, 0x55000000)
            EditorGradientOverlay.NONE -> return
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, w, h, colors, floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, w, h, paint)
    }

    private fun drawLightLeak(canvas: Canvas, w: Float, h: Float, preset: EditorLightLeakPreset) {
        when (preset) {
            EditorLightLeakPreset.NONE -> return
            EditorLightLeakPreset.AMBER_CORNER -> {
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        0f, 0f, max(w, h) * 0.65f,
                        intArrayOf(0x99FBBF24.toInt(), 0x44FB7185, 0x00000000),
                        floatArrayOf(0f, 0.45f, 1f),
                        Shader.TileMode.CLAMP,
                    )
                }
                canvas.drawRect(0f, 0f, w, h, p)
            }
            EditorLightLeakPreset.ROSE_FLARE -> {
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        w, h * 0.25f, max(w, h) * 0.55f,
                        intArrayOf(0x99F472B6.toInt(), 0x33FB7185, 0x00000000),
                        floatArrayOf(0f, 0.5f, 1f),
                        Shader.TileMode.CLAMP,
                    )
                }
                canvas.drawRect(0f, 0f, w, h, p)
            }
            EditorLightLeakPreset.PRISM_BEAM -> {
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        0f, h * 0.1f, w * 0.7f, h * 0.9f,
                        intArrayOf(0x00000000, 0x555CE1E6, 0x55C084FC, 0x00000000),
                        floatArrayOf(0f, 0.35f, 0.6f, 1f),
                        Shader.TileMode.CLAMP,
                    )
                }
                canvas.drawRect(0f, 0f, w, h, p)
            }
        }
    }

    private fun drawShapeOverlay(canvas: Canvas, w: Float, h: Float, shape: EditorShapeOverlay) {
        val minDim = min(w, h)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(215, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = (minDim * 0.012f).coerceAtLeast(3f)
        }
        when (shape) {
            EditorShapeOverlay.NONE -> Unit
            EditorShapeOverlay.CIRCLE_BADGE -> canvas.drawCircle(w / 2f, h / 2f, minDim * 0.42f, p)
            EditorShapeOverlay.ROUNDED_BORDER -> {
                val m = minDim * 0.05f
                canvas.drawRoundRect(RectF(m, m, w - m, h - m), minDim * 0.05f, minDim * 0.05f, p)
            }
            EditorShapeOverlay.CINEMA_BARS -> {
                val barPaint = Paint().apply { color = android.graphics.Color.BLACK }
                val barH = h * 0.11f
                canvas.drawRect(0f, 0f, w, barH, barPaint)
                canvas.drawRect(0f, h - barH, w, h, barPaint)
            }
        }
    }

    private fun applyFrame(src: Bitmap, preset: EditorFramePreset): Bitmap {
        val minDim = min(src.width, src.height)
        val borderPx = when (preset) {
            EditorFramePreset.NONE -> return src
            EditorFramePreset.THIN_WHITE -> (minDim * 0.025f).roundToInt().coerceAtLeast(6)
            EditorFramePreset.WIDE_WHITE, EditorFramePreset.POLAROID -> (minDim * 0.07f).roundToInt().coerceAtLeast(16)
            EditorFramePreset.DARK_CINEMA -> (minDim * 0.045f).roundToInt().coerceAtLeast(12)
        }
        val extraBottom = if (preset == EditorFramePreset.POLAROID) borderPx * 2 else 0
        val out = Bitmap.createBitmap(
            src.width + borderPx * 2,
            src.height + borderPx * 2 + extraBottom,
            Bitmap.Config.ARGB_8888,
        )
        val c = Canvas(out)
        c.drawColor(
            if (preset == EditorFramePreset.DARK_CINEMA) 0xFF0A0A0D.toInt() else android.graphics.Color.WHITE,
        )
        c.drawBitmap(src, borderPx.toFloat(), borderPx.toFloat(), null)
        src.recycle()
        return out
    }

    private fun buildCombinedColorMatrix(state: PhotoEditState): ColorMatrix {
        val result = ColorMatrix()

        // Hue shift via rotate
        if (abs(state.hueShiftDegrees) > 0.5f) {
            val hue = ColorMatrix().apply {
                setRotate(0, state.hueShiftDegrees)
                setRotate(1, state.hueShiftDegrees)
                setRotate(2, state.hueShiftDegrees)
            }
            result.postConcat(hue)
        }

        // Saturation + Vibrance
        val combinedSat = (state.saturation + state.vibrance * 0.45f).coerceIn(0f, 2.5f)
        val sat = ColorMatrix().apply { setSaturation(combinedSat) }
        result.postConcat(sat)

        // Contrast, Exposure, Highlights, Shadows, Whites, Blacks, Clarity, Dehaze, Fade
        val exposureScale = Math.pow(2.0, state.exposure.toDouble()).toFloat()
        val toneContrast = (state.contrast * (1f + state.dehaze * 0.25f + state.clarity * 0.15f) - state.fade * 0.22f)
            .coerceIn(0.4f, 2.4f)
        val c = toneContrast * exposureScale
        val tonalShift = state.brightness +
            state.highlights * 18f +
            state.shadows * 22f +
            state.whites * 26f -
            state.blacks * 20f +
            state.fade * 28f -
            state.dehaze * 18f
        val t = (1f - toneContrast) * 128f + tonalShift
        val bc = ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, t,
                0f, c, 0f, 0f, t,
                0f, 0f, c, 0f, t,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
        result.postConcat(bc)

        // Temperature & Tint & Split-Tone Balance
        if (state.temperature != 0f || state.tint != 0f || state.splitToneBalance != 0f) {
            val rShift = state.temperature * 0.9f + state.splitToneBalance * 16f
            val gShift = -state.tint * 0.8f
            val bShift = -state.temperature * 0.9f - state.splitToneBalance * 16f
            result.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        1f, 0f, 0f, 0f, rShift,
                        0f, 1f, 0f, 0f, gShift,
                        0f, 0f, 1f, 0f, bShift,
                        0f, 0f, 0f, 1f, 0f,
                    ),
                ),
            )
        }

        // Curves Preset
        val curveMatrix = when (state.curvesPreset) {
            EditorCurvesPreset.LINEAR -> null
            EditorCurvesPreset.S_CURVE -> ColorMatrix(
                floatArrayOf(
                    1.18f, 0f, 0f, 0f, -20f,
                    0f, 1.18f, 0f, 0f, -20f,
                    0f, 0f, 1.18f, 0f, -20f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            )
            EditorCurvesPreset.CINEMA_FADE -> ColorMatrix(
                floatArrayOf(
                    0.92f, 0f, 0f, 0f, 16f,
                    0f, 0.94f, 0f, 0f, 14f,
                    0f, 0f, 0.98f, 0f, 20f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            )
            EditorCurvesPreset.CROSS_PROCESS -> ColorMatrix(
                floatArrayOf(
                    1.12f, 0.05f, 0f, 0f, -8f,
                    0f, 1.05f, 0f, 0f, 6f,
                    0f, 0.08f, 1.15f, 0f, 10f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            )
            EditorCurvesPreset.HIGH_KEY -> ColorMatrix(
                floatArrayOf(
                    1.05f, 0f, 0f, 0f, 26f,
                    0f, 1.05f, 0f, 0f, 26f,
                    0f, 0f, 1.05f, 0f, 26f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            )
        }
        if (curveMatrix != null) result.postConcat(curveMatrix)

        // Creative Filter Preset
        val filterMatrix = when (state.filter) {
            EditorFilterPreset.NONE -> null
            EditorFilterPreset.NOIR -> ColorMatrix().apply { setSaturation(0f) }
            EditorFilterPreset.VIVID -> ColorMatrix().apply { setSaturation(1.42f) }
            EditorFilterPreset.WARM_FILM -> ColorMatrix(
                floatArrayOf(
                    1.08f, 0.04f, 0f, 0f, 10f,
                    0f, 1.02f, 0f, 0f, 4f,
                    0f, 0f, 0.90f, 0f, -8f,
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
                    0.90f, 0f, 0f, 0f, 18f,
                    0f, 0.90f, 0f, 0f, 18f,
                    0f, 0f, 0.90f, 0f, 20f,
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
            EditorFilterPreset.GOLDEN_HOUR -> ColorMatrix(
                floatArrayOf(
                    1.14f, 0.06f, 0f, 0f, 16f,
                    0f, 1.05f, 0f, 0f, 8f,
                    0f, 0f, 0.84f, 0f, -12f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            )
            EditorFilterPreset.CYBER_NEON -> ColorMatrix(
                floatArrayOf(
                    1.05f, 0f, 0.16f, 0f, -12f,
                    0f, 0.92f, 0.08f, 0f, 6f,
                    0.12f, 0.04f, 1.24f, 0f, 14f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            )
            EditorFilterPreset.SEPIA_PRINT -> ColorMatrix().apply { setScale(1.0f, 0.88f, 0.70f, 1f) }.also {
                it.preConcat(ColorMatrix().apply { setSaturation(0.25f) })
            }
            EditorFilterPreset.BLEACH_BYPASS -> ColorMatrix(
                floatArrayOf(
                    1.22f, 0f, 0f, 0f, -22f,
                    0f, 1.22f, 0f, 0f, -22f,
                    0f, 0f, 1.22f, 0f, -22f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            ).also { it.preConcat(ColorMatrix().apply { setSaturation(0.45f) }) }
            EditorFilterPreset.TEAL_ORANGE -> ColorMatrix(
                floatArrayOf(
                    1.12f, 0f, -0.05f, 0f, 6f,
                    0f, 1.02f, 0.06f, 0f, 2f,
                    -0.06f, 0.08f, 1.12f, 0f, 8f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            )
            EditorFilterPreset.DREAMY_PASTEL -> ColorMatrix(
                floatArrayOf(
                    0.96f, 0.05f, 0f, 0f, 20f,
                    0.03f, 0.96f, 0.03f, 0f, 18f,
                    0f, 0.04f, 1.02f, 0f, 22f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            )
            EditorFilterPreset.AMOLED_DARK -> ColorMatrix(
                floatArrayOf(
                    1.28f, 0f, 0f, 0f, -34f,
                    0f, 1.28f, 0f, 0f, -34f,
                    0f, 0f, 1.28f, 0f, -34f,
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
