package com.example.feature.videos

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import kotlin.math.abs

object VideoProcessingEngine {

    /**
     * Extracts a full-resolution frame bitmap at [timeMicros] and saves it into Pictures/Gallery56_Frames.
     */
    suspend fun extractFrameToMediaStore(
        context: Context,
        videoUri: Uri,
        timeMillis: Long,
    ): Uri? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            val frame = retriever.getFrameAtTime(
                timeMillis.coerceAtLeast(0L) * 1000L,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
            ) ?: return@withContext null
            try {
                com.example.core.media.MediaStoreOperations.saveBitmapToMediaStore(
                    context = context,
                    bitmap = frame,
                    fileName = "G56_Frame_${System.currentTimeMillis()}",
                    subFolder = "Gallery56_Frames",
                )
            } finally {
                frame.recycle()
            }
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    /**
     * Losslessly trims a video container between [startMillis] and [endMillis] using
     * Android MediaExtractor + MediaMuxer and saves directly into Movies/Gallery56_Trimmed.
     */
    suspend fun trimVideoToMediaStore(
        context: Context,
        videoUri: Uri,
        startMillis: Long,
        endMillis: Long,
    ): Uri? = withContext(Dispatchers.IO) {
        if (endMillis <= startMillis + 250L) return@withContext null
        val tempOut = File(context.cacheDir, "trim_${System.currentTimeMillis()}.mp4")
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        try {
            extractor.setDataSource(context, videoUri, null)
            muxer = MediaMuxer(tempOut.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val trackMap = mutableMapOf<Int, Int>()
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    trackMap[i] = muxer.addTrack(format)
                }
            }
            if (trackMap.isEmpty()) return@withContext null

            muxer.start()
            val startUs = startMillis * 1000L
            val endUs = endMillis * 1000L
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            val buffer = ByteBuffer.allocate(1024 * 1024)
            val info = MediaCodec.BufferInfo()
            while (true) {
                val trackIndex = extractor.sampleTrackIndex
                if (trackIndex < 0) break
                val pts = extractor.sampleTime
                if (pts > endUs) break
                val size = extractor.readSampleData(buffer, 0)
                if (size < 0) break
                if (pts >= startUs && trackIndex in trackMap) {
                    info.offset = 0
                    info.size = size
                    info.presentationTimeUs = (pts - startUs).coerceAtLeast(0L)
                    @Suppress("DEPRECATION")
                    info.flags = extractor.sampleFlags
                    muxer.writeSampleData(trackMap.getValue(trackIndex), buffer, info)
                }
                extractor.advance()
            }
            muxer.stop()
            muxer.release()
            muxer = null

            publishVideoFileToMediaStore(
                context = context,
                file = tempOut,
                fileName = "G56_Trim_${System.currentTimeMillis()}.mp4",
                subFolder = "Gallery56_Trimmed",
            )
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { muxer?.release() }
            runCatching { extractor.release() }
            tempOut.delete()
        }
    }

    /**
     * Converts a clip segment of a video into a real animated GIF89a saved in Pictures/Gallery56_GIFs.
     */
    suspend fun exportVideoClipToGif(
        context: Context,
        videoUri: Uri,
        startMillis: Long,
        endMillis: Long,
        frameCount: Int = 12,
        targetWidth: Int = 360,
    ): Uri? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        val frames = mutableListOf<Bitmap>()
        try {
            retriever.setDataSource(context, videoUri)
            val safeDuration = (endMillis - startMillis).coerceIn(500L, 10_000L)
            val step = safeDuration / frameCount.coerceIn(4, 24)
            for (i in 0 until frameCount.coerceIn(4, 24)) {
                val tUs = (startMillis + i * step) * 1000L
                val bmp = retriever.getFrameAtTime(tUs, MediaMetadataRetriever.OPTION_CLOSEST)
                if (bmp != null) {
                    val ratio = bmp.height.toFloat() / bmp.width.toFloat().coerceAtLeast(1f)
                    val targetHeight = (targetWidth * ratio).toInt().coerceAtLeast(64)
                    val scaled = Bitmap.createScaledBitmap(bmp, targetWidth, targetHeight, true)
                    if (scaled !== bmp) bmp.recycle()
                    frames.add(scaled)
                }
            }
            if (frames.isEmpty()) return@withContext null

            val gifBytes = encodeAnimatedGif(frames, delayMs = step.toInt().coerceIn(80, 500))
            saveGifBytesToMediaStore(
                context = context,
                bytes = gifBytes,
                fileName = "G56_Clip_${System.currentTimeMillis()}.gif",
            )
        } catch (_: Throwable) {
            null
        } finally {
            frames.forEach { it.recycle() }
            runCatching { retriever.release() }
        }
    }

    /**
     * Pure-Kotlin valid GIF89a encoder producing real animated GIF byte streams from bitmaps
     * (uniform 6x6x6 web-safe 216-color palette + standard LZW stream).
     */
    fun encodeAnimatedGif(frames: List<Bitmap>, delayMs: Int = 140): ByteArray {
        require(frames.isNotEmpty())
        val width = frames.first().width
        val height = frames.first().height
        val out = ByteArrayOutputStream()

        // GIF89a Header
        out.write("GIF89a".toByteArray(Charsets.US_ASCII))
        writeShortLE(out, width)
        writeShortLE(out, height)
        // GCT flag = 1, color res = 7, sort = 0, GCT size = 7 (256 entries) -> 0xF7
        out.write(0xF7)
        out.write(0) // bg color index
        out.write(0) // pixel aspect ratio

        // Write 256-entry Global Color Table (6x6x6 RGB cube = 216 colors + 40 grayscale)
        for (r in 0 until 6) {
            for (g in 0 until 6) {
                for (b in 0 until 6) {
                    out.write(r * 51)
                    out.write(g * 51)
                    out.write(b * 51)
                }
            }
        }
        for (i in 216 until 256) {
            val gray = ((i - 216) * 255 / 39).coerceIn(0, 255)
            out.write(gray)
            out.write(gray)
            out.write(gray)
        }

        // Netscape 2.0 Application Extension for infinite looping
        out.write(0x21)
        out.write(0xFF)
        out.write(11)
        out.write("NETSCAPE2.0".toByteArray(Charsets.US_ASCII))
        out.write(3)
        out.write(1)
        writeShortLE(out, 0)
        out.write(0)

        val delayCentiseconds = (delayMs / 10).coerceIn(2, 200)
        val pixels = IntArray(width * height)
        val indexed = ByteArray(width * height)

        for (bmp in frames) {
            val resized = if (bmp.width == width && bmp.height == height) {
                bmp
            } else {
                Bitmap.createScaledBitmap(bmp, width, height, true)
            }
            resized.getPixels(pixels, 0, width, 0, 0, width, height)
            for (i in pixels.indices) {
                val c = pixels[i]
                val r = ((c ushr 16) and 0xFF)
                val g = ((c ushr 8) and 0xFF)
                val b = (c and 0xFF)
                val rq = (r * 5 + 127) / 255
                val gq = (g * 5 + 127) / 255
                val bq = (b * 5 + 127) / 255
                indexed[i] = (rq * 36 + gq * 6 + bq).toByte()
            }
            if (resized !== bmp) resized.recycle()

            // Graphic Control Extension
            out.write(0x21)
            out.write(0xF9)
            out.write(4)
            out.write(0x04) // disposal = 1
            writeShortLE(out, delayCentiseconds)
            out.write(0)
            out.write(0)

            // Image Descriptor
            out.write(0x2C)
            writeShortLE(out, 0)
            writeShortLE(out, 0)
            writeShortLE(out, width)
            writeShortLE(out, height)
            out.write(0) // no LCT

            // LZW Image Data (minimum code size 8)
            out.write(8)
            writeUncompressedLzwStream(out, indexed)
            out.write(0) // Block terminator
        }

        // GIF Trailer
        out.write(0x3B)
        return out.toByteArray()
    }

    private fun writeUncompressedLzwStream(out: OutputStream, indices: ByteArray) {
        // Valid LZW stream using 9-bit codes with periodic Clear codes (256) so decoders never overflow
        val clearCode = 256
        val eoiCode = 257
        val subBlock = ByteArrayOutputStream()
        var bitBuffer = 0
        var bitCount = 0

        fun emitCode9(code: Int) {
            bitBuffer = bitBuffer or (code shl bitCount)
            bitCount += 9
            while (bitCount >= 8) {
                subBlock.write(bitBuffer and 0xFF)
                bitBuffer = bitBuffer ushr 8
                bitCount -= 8
                if (subBlock.size() == 255) {
                    val bytes = subBlock.toByteArray()
                    out.write(255)
                    out.write(bytes)
                    subBlock.reset()
                }
            }
        }

        emitCode9(clearCode)
        var emittedSinceClear = 0
        for (b in indices) {
            emitCode9(b.toInt() and 0xFF)
            emittedSinceClear++
            if (emittedSinceClear >= 240) {
                emitCode9(clearCode)
                emittedSinceClear = 0
            }
        }
        emitCode9(eoiCode)
        if (bitCount > 0) {
            subBlock.write(bitBuffer and 0xFF)
        }
        if (subBlock.size() > 0) {
            val bytes = subBlock.toByteArray()
            out.write(bytes.size)
            out.write(bytes)
        }
    }

    private fun writeShortLE(out: OutputStream, value: Int) {
        out.write(value and 0xFF)
        out.write((value ushr 8) and 0xFF)
    }

    private fun publishVideoFileToMediaStore(
        context: Context,
        file: File,
        fileName: String,
        subFolder: String,
    ): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/$subFolder")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return null
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    file.inputStream().use { it.copyTo(out) }
                }
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
                uri
            } else {
                @Suppress("DEPRECATION")
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), subFolder)
                    .apply { mkdirs() }
                val dest = File(dir, fileName)
                file.copyTo(dest, overwrite = true)
                Uri.fromFile(dest)
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun saveGifBytesToMediaStore(
        context: Context,
        bytes: ByteArray,
        fileName: String,
    ): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/gif")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Gallery56_GIFs")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return null
                context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
                uri
            } else {
                @Suppress("DEPRECATION")
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Gallery56_GIFs")
                    .apply { mkdirs() }
                val dest = File(dir, fileName)
                FileOutputStream(dest).use { it.write(bytes) }
                Uri.fromFile(dest)
            }
        } catch (_: Throwable) {
            null
        }
    }
}
