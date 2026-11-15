package com.example.feature.intelligence.engine

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

data class OnDeviceModelStatus(
    val name: String,
    val capability: String,
    val isAvailable: Boolean,
    val isOptionalDownload: Boolean,
    val statusDetail: String,
)

object ModelAvailabilityChecker {

    fun inspectModels(context: Context, faceAnalysisEnabled: Boolean): List<OnDeviceModelStatus> {
        val aiCoreInstalled = isPackageInstalled(context, "com.google.android.aicore")
        val aiCoreReady = aiCoreInstalled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

        return listOf(
            OnDeviceModelStatus(
                name = "ML Kit Image Labeling",
                capability = "Classification, Semantic Indexing & Smart Albums",
                isAvailable = true,
                isOptionalDownload = false,
                statusDetail = "Bundled offline model active (400+ categories, 0 network calls)",
            ),
            OnDeviceModelStatus(
                name = "ML Kit Object Detection",
                capability = "Object Recognition & Bounding Boxes",
                isAvailable = true,
                isOptionalDownload = false,
                statusDetail = "Bundled offline model active",
            ),
            OnDeviceModelStatus(
                name = "ML Kit Text Recognition v2",
                capability = "On-Device OCR & Document Indexing",
                isAvailable = true,
                isOptionalDownload = false,
                statusDetail = "Bundled offline Latin OCR active",
            ),
            OnDeviceModelStatus(
                name = "ML Kit Face Detection",
                capability = "Privacy-Gated Face Detection & Landmark Clustering",
                isAvailable = true,
                isOptionalDownload = false,
                statusDetail = if (faceAnalysisEnabled) {
                    "Bundled offline model active (Local-only, zero identity DB)"
                } else {
                    "Bundled offline model paused (Disabled by user privacy toggle)"
                },
            ),
            OnDeviceModelStatus(
                name = "Android AICore / Gemini Nano VLM",
                capability = "Generative Natural-Language Image Captioning",
                isAvailable = aiCoreReady,
                isOptionalDownload = true,
                statusDetail = if (aiCoreReady) {
                    "System AICore detected; ready for on-device generative runtime"
                } else {
                    "AICore system model unavailable on this device/API. Using deterministic structured captions built strictly from real ML Kit labels, OCR, objects, and EXIF."
                },
            ),
        )
    }

    private fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }
}
