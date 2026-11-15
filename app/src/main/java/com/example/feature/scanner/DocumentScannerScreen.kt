package com.example.feature.scanner

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.TextSnippet
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.core.designsystem.GlassPanel
import com.example.core.media.MediaStoreOperations
import com.example.feature.collage.decodeSampledBitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

enum class ScanEnhanceMode(val label: String) {
    ORIGINAL("Original"),
    BW_DOC("B&W Document"),
    GRAYSCALE("Grayscale"),
    CRISP_COLOR("Color Boost"),
}

private data class ScannedPage(
    val uriString: String,
    val enhanceMode: ScanEnhanceMode = ScanEnhanceMode.BW_DOC,
    val ocrText: String = "",
)

@Composable
fun DocumentScannerScreen(
    onBack: () -> Unit,
    onSavedImageToMediaStore: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pages = remember { mutableStateListOf<ScannedPage>() }
    var activeIndex by remember { mutableIntStateOf(0) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }
    var isOcrRunning by remember { mutableStateOf(false) }
    var isExportingPdf by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val captured = pendingCaptureUri
        if (success && captured != null) {
            pages.add(ScannedPage(uriString = captured.toString()))
            activeIndex = pages.lastIndex
        }
    }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(10),
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri -> pages.add(ScannedPage(uriString = uri.toString())) }
            activeIndex = pages.lastIndex
        }
    }

    val exportPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
    ) { destUri ->
        if (destUri != null) {
            scope.launch {
                isExportingPdf = true
                val ok = exportPagesToPdf(context, pages.toList(), destUri)
                isExportingPdf = false
                Toast.makeText(
                    context,
                    if (ok) "PDF exported (${pages.size} page(s))" else "PDF export failed",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    val activePage = pages.getOrNull(activeIndex)

    LaunchedEffect(activePage?.uriString, activePage?.enhanceMode) {
        val page = activePage
        if (page == null) {
            previewBitmap = null
            return@LaunchedEffect
        }
        previewBitmap = withContext(Dispatchers.Default) {
            renderEnhancedScanPage(context, Uri.parse(page.uriString), page.enhanceMode, maxDim = 1200)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("document_scanner_screen"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                    Column {
                        Text("DOCUMENT SCANNER", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = "Multi-page scan, OCR & PDF export",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Button(
                    enabled = pages.isNotEmpty() && !isExportingPdf,
                    onClick = {
                        exportPdfLauncher.launch("Gallery56_Scan_${System.currentTimeMillis()}.pdf")
                    },
                    modifier = Modifier.testTag("scanner_export_pdf"),
                ) {
                    Icon(Icons.Rounded.PictureAsPdf, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isExportingPdf) "PDF..." else "Export PDF")
                }
            }
        }

        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = {
                            val dir = File(context.cacheDir, "scanner").apply { mkdirs() }
                            val file = File(dir, "scan_${System.currentTimeMillis()}.jpg")
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file,
                            )
                            pendingCaptureUri = uri
                            cameraLauncher.launch(uri)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("scanner_capture_camera"),
                    ) {
                        Icon(Icons.Rounded.AddAPhoto, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Scan Camera")
                    }
                    OutlinedButton(
                        onClick = {
                            pickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("scanner_import_photos"),
                    ) {
                        Icon(Icons.Rounded.AddPhotoAlternate, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add Pages")
                    }
                }
            }
        }

        if (activePage == null) {
            item {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                Icons.Rounded.DocumentScanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp),
                            )
                            Text("Capture a document or import pages to begin.")
                        }
                    }
                }
            }
        } else {
            item {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Page ${activeIndex + 1} of ${pages.size}", style = MaterialTheme.typography.titleMedium)
                        Row {
                            OutlinedButton(
                                onClick = {
                                    val bmp = previewBitmap ?: return@OutlinedButton
                                    scope.launch {
                                        val saved = MediaStoreOperations.saveBitmapToMediaStore(
                                            context = context,
                                            bitmap = bmp,
                                            fileName = "Gallery56_ScanPage_${System.currentTimeMillis()}",
                                            subFolder = "Gallery56_Scans",
                                        )
                                        if (saved != null) {
                                            Toast.makeText(context, "Saved page image to Pictures/Gallery56_Scans", Toast.LENGTH_SHORT).show()
                                            onSavedImageToMediaStore()
                                        }
                                    }
                                },
                            ) {
                                Text("Save JPG")
                            }
                            IconButton(
                                onClick = {
                                    pages.removeAt(activeIndex)
                                    activeIndex = activeIndex.coerceAtMost((pages.size - 1).coerceAtLeast(0))
                                },
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove page")
                            }
                        }
                    }

                    if (pages.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            pages.forEachIndexed { idx, _ ->
                                FilterChip(
                                    selected = idx == activeIndex,
                                    onClick = { activeIndex = idx },
                                    label = { Text("Page ${idx + 1}") },
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.75f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        val bmp = previewBitmap
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Scanned page preview",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ScanEnhanceMode.entries.forEach { mode ->
                            FilterChip(
                                selected = activePage.enhanceMode == mode,
                                onClick = {
                                    pages[activeIndex] = activePage.copy(enhanceMode = mode)
                                },
                                label = { Text(mode.label) },
                            )
                        }
                    }
                }
            }

            item {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.TextSnippet, null, tint = MaterialTheme.colorScheme.primary)
                            Text("On-Device OCR", style = MaterialTheme.typography.titleMedium)
                        }
                        Button(
                            enabled = !isOcrRunning && previewBitmap != null,
                            onClick = {
                                val bmp = previewBitmap ?: return@Button
                                scope.launch {
                                    isOcrRunning = true
                                    val text = runMlKitOcrOnBitmap(bmp)
                                    pages[activeIndex] = pages[activeIndex].copy(ocrText = text)
                                    isOcrRunning = false
                                }
                            },
                            modifier = Modifier.testTag("scanner_run_ocr"),
                        ) {
                            Text(if (isOcrRunning) "Reading..." else "Extract Text")
                        }
                    }

                    if (activePage.ocrText.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = activePage.ocrText,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cb.setPrimaryClip(ClipData.newPlainText("Scanned OCR", activePage.ocrText))
                                Toast.makeText(context, "OCR text copied", Toast.LENGTH_SHORT).show()
                            },
                        ) {
                            Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Copy OCR Text")
                        }
                    } else {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Tap Extract Text to recognize printed text on this page offline using ML Kit.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun renderEnhancedScanPage(
    context: Context,
    uri: Uri,
    mode: ScanEnhanceMode,
    maxDim: Int,
): Bitmap? {
    val src = decodeSampledBitmap(context, uri, maxDim) ?: return null
    if (mode == ScanEnhanceMode.ORIGINAL) return src

    val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    val matrix = ColorMatrix()
    when (mode) {
        ScanEnhanceMode.ORIGINAL -> {}
        ScanEnhanceMode.GRAYSCALE -> matrix.setSaturation(0f)
        ScanEnhanceMode.BW_DOC -> {
            val gray = ColorMatrix().apply { setSaturation(0f) }
            val contrast = 1.9f
            val shift = (-0.5f * contrast + 0.5f) * 255f + 16f
            val boost = ColorMatrix(
                floatArrayOf(
                    contrast, 0f, 0f, 0f, shift,
                    0f, contrast, 0f, 0f, shift,
                    0f, 0f, contrast, 0f, shift,
                    0f, 0f, 0f, 1f, 0f,
                ),
            )
            matrix.postConcat(gray)
            matrix.postConcat(boost)
        }
        ScanEnhanceMode.CRISP_COLOR -> {
            val sat = ColorMatrix().apply { setSaturation(1.28f) }
            val contrast = 1.25f
            val shift = (-0.5f * contrast + 0.5f) * 255f
            val boost = ColorMatrix(
                floatArrayOf(
                    contrast, 0f, 0f, 0f, shift,
                    0f, contrast, 0f, 0f, shift,
                    0f, 0f, contrast, 0f, shift,
                    0f, 0f, 0f, 1f, 0f,
                ),
            )
            matrix.postConcat(sat)
            matrix.postConcat(boost)
        }
    }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(matrix)
    }
    canvas.drawBitmap(src, 0f, 0f, paint)
    src.recycle()
    return out
}

private suspend fun runMlKitOcrOnBitmap(bitmap: Bitmap): String = suspendCancellableCoroutine { cont ->
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val inputImage = InputImage.fromBitmap(bitmap, 0)
    recognizer.process(inputImage)
        .addOnSuccessListener { result ->
            recognizer.close()
            if (cont.isActive) cont.resume(result.text.ifBlank { "No text detected on this page." })
        }
        .addOnFailureListener {
            recognizer.close()
            if (cont.isActive) cont.resume("OCR unavailable on this device.")
        }
}

private suspend fun exportPagesToPdf(
    context: Context,
    pages: List<ScannedPage>,
    destUri: Uri,
): Boolean = withContext(Dispatchers.IO) {
    if (pages.isEmpty()) return@withContext false
    val pdf = PdfDocument()
    return@withContext try {
        pages.forEachIndexed { idx, page ->
            val bmp = renderEnhancedScanPage(context, Uri.parse(page.uriString), page.enhanceMode, maxDim = 1600)
                ?: return@forEachIndexed
            try {
                val pageInfo = PdfDocument.PageInfo.Builder(bmp.width, bmp.height, idx + 1).create()
                val pdfPage = pdf.startPage(pageInfo)
                pdfPage.canvas.drawBitmap(bmp, 0f, 0f, null)
                pdf.finishPage(pdfPage)
            } finally {
                bmp.recycle()
            }
        }
        context.contentResolver.openOutputStream(destUri)?.use { out ->
            pdf.writeTo(out)
        } ?: return@withContext false
        true
    } catch (_: Throwable) {
        false
    } finally {
        pdf.close()
    }
}
