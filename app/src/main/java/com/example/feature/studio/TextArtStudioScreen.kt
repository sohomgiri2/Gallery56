package com.example.feature.studio

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.Gallery56TopBar
import com.example.core.designsystem.GlassPanel
import com.example.core.media.MediaStoreOperations
import com.example.ui.theme.LocalGallery56Tokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class ArtPalettePreset(
    val name: String,
    val startColor: Int,
    val endColor: Int,
    val textColor: Int,
)

private val artPalettes = listOf(
    ArtPalettePreset("Obsidian Cyan", 0xFF07090D.toInt(), 0xFF0E2A38.toInt(), 0xFFF4F7FB.toInt()),
    ArtPalettePreset("Sunset Crimson", 0xFF280B18.toInt(), 0xFF9E2A4B.toInt(), 0xFFFFF1F2.toInt()),
    ArtPalettePreset("Solar Gold", 0xFF1F1606.toInt(), 0xFF8C6214.toInt(), 0xFFFFFBEB.toInt()),
    ArtPalettePreset("Museum Paper", 0xFFF8FAFC.toInt(), 0xFFE2E8F0.toInt(), 0xFF0F172A.toInt()),
    ArtPalettePreset("Cosmic Violet", 0xFF110724.toInt(), 0xFF4C1D95.toInt(), 0xFFF5F3FF.toInt()),
)

@Composable
fun TextArtStudioScreen(
    onBack: () -> Unit,
    onSavedToMediaStore: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokens = LocalGallery56Tokens.current

    var quoteText by remember { mutableStateOf("BEYOND GALLERY.\nBEYOND IMAGINATION.") }
    var captionText by remember { mutableStateOf("GALLERY56 • SOHOM GIRI") }
    var selectedPaletteIndex by remember { mutableIntStateOf(0) }
    var fontType by remember { mutableIntStateOf(0) } // 0 sans, 1 serif, 2 mono
    var isExporting by remember { mutableStateOf(false) }

    val palette = artPalettes[selectedPaletteIndex]
    val previewBitmap = remember(quoteText, captionText, palette, fontType) {
        renderTextArtCard(quoteText, captionText, palette, fontType, sizePx = 760)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("text_art_studio_screen"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Gallery56TopBar(
                title = "TEXT-ON-ART STUDIO",
                subtitle = "Typography cards, quotes & editorial posters",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = tokens.semantic.iconPrimary)
                    }
                },
                primaryActions = {
                    Button(
                        enabled = quoteText.isNotBlank() && !isExporting,
                        onClick = {
                            scope.launch {
                                isExporting = true
                                val hiRes = withContext(Dispatchers.Default) {
                                    renderTextArtCard(quoteText, captionText, palette, fontType, sizePx = 1600)
                                }
                                val saved = try {
                                    MediaStoreOperations.saveBitmapToMediaStore(
                                        context = context,
                                        bitmap = hiRes,
                                        fileName = "Gallery56_Art_${System.currentTimeMillis()}",
                                        subFolder = "Gallery56_TextArt",
                                    )
                                } finally {
                                    hiRes.recycle()
                                }
                                isExporting = false
                                if (saved != null) {
                                    onSavedToMediaStore()
                                    Toast.makeText(context, "Saved to Pictures/Gallery56_TextArt", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Save failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                    ) {
                        Icon(Icons.Rounded.Save, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (isExporting) "..." else "Export")
                    }
                },
            )
        }

        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Image(
                    bitmap = previewBitmap.asImageBitmap(),
                    contentDescription = "Text art preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp)),
                )
            }
        }

        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = quoteText,
                    onValueChange = { quoteText = it },
                    label = { Text("Main Quote / Typography Copy") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = captionText,
                    onValueChange = { captionText = it },
                    label = { Text("Signature / Footer Credit") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Text("Typography Style", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Modern Sans", "Editorial Serif", "Tech Mono").forEachIndexed { idx, label ->
                        FilterChip(
                            selected = fontType == idx,
                            onClick = { fontType = idx },
                            label = { Text(label) },
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Canvas Backdrop", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    artPalettes.forEachIndexed { idx, p ->
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(p.endColor))
                                .border(
                                    width = if (selectedPaletteIndex == idx) 2.5.dp else 1.dp,
                                    color = if (selectedPaletteIndex == idx) MaterialTheme.colorScheme.primary else Color.Gray,
                                    shape = CircleShape,
                                )
                                .clickable { selectedPaletteIndex = idx },
                        )
                    }
                }
            }
        }
    }
}

private fun renderTextArtCard(
    quote: String,
    footer: String,
    palette: ArtPalettePreset,
    fontType: Int,
    sizePx: Int,
): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, sizePx.toFloat(), sizePx.toFloat(),
            palette.startColor, palette.endColor, Shader.TileMode.CLAMP,
        )
    }
    canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = palette.textColor
        alpha = 72
        style = Paint.Style.STROKE
        strokeWidth = (sizePx * 0.005f).coerceAtLeast(2f)
    }
    val margin = sizePx * 0.075f
    canvas.drawRect(margin, margin, sizePx - margin, sizePx - margin, borderPaint)

    val typeface = when (fontType) {
        1 -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
        2 -> Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        else -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = palette.textColor
        textSize = sizePx * 0.072f
        this.typeface = typeface
    }
    val innerWidth = (sizePx - margin * 3f).toInt().coerceAtLeast(100)
    val staticLayout = StaticLayout.Builder
        .obtain(quote.ifBlank { "GALLERY56" }, 0, quote.ifBlank { "GALLERY56" }.length, textPaint, innerWidth)
        .setAlignment(Layout.Alignment.ALIGN_CENTER)
        .setLineSpacing(8f, 1.15f)
        .build()

    canvas.save()
    canvas.translate((sizePx - innerWidth) / 2f, (sizePx - staticLayout.height) / 2f)
    staticLayout.draw(canvas)
    canvas.restore()

    if (footer.isNotBlank()) {
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.textColor
            alpha = 195
            textSize = sizePx * 0.03f
            this.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(footer, sizePx / 2f, sizePx - margin * 1.45f, footerPaint)
    }
    return bmp
}
