package com.firetv.launcher.model

import android.graphics.drawable.Drawable

/**
 * Data model representing an installed application on FireTV.
 */
data class AppItem(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: Drawable,
    val isLeanback: Boolean
)
