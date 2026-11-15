package com.example.core.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

enum class MediaAccessLevel {
    FULL_ACCESS,
    PARTIAL_ACCESS,
    DENIED,
}

object MediaPermissionHelper {

    fun checkAccessLevel(context: Context): MediaAccessLevel {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                val hasImages = hasPermission(context, Manifest.permission.READ_MEDIA_IMAGES)
                val hasVideo = hasPermission(context, Manifest.permission.READ_MEDIA_VIDEO)
                val hasPartial = hasPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                when {
                    hasImages && hasVideo -> MediaAccessLevel.FULL_ACCESS
                    hasPartial || hasImages || hasVideo -> MediaAccessLevel.PARTIAL_ACCESS
                    else -> MediaAccessLevel.DENIED
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                val hasImages = hasPermission(context, Manifest.permission.READ_MEDIA_IMAGES)
                val hasVideo = hasPermission(context, Manifest.permission.READ_MEDIA_VIDEO)
                when {
                    hasImages && hasVideo -> MediaAccessLevel.FULL_ACCESS
                    hasImages || hasVideo -> MediaAccessLevel.PARTIAL_ACCESS
                    else -> MediaAccessLevel.DENIED
                }
            }
            else -> {
                if (hasPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    MediaAccessLevel.FULL_ACCESS
                } else {
                    MediaAccessLevel.DENIED
                }
            }
        }
    }

    fun hasMediaLocationAccess(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            hasPermission(context, Manifest.permission.ACCESS_MEDIA_LOCATION)
        } else {
            true
        }
    }

    fun requiredPermissionsForSdk(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                Manifest.permission.ACCESS_MEDIA_LOCATION,
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.ACCESS_MEDIA_LOCATION,
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_MEDIA_LOCATION,
            )
            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
    }
}
