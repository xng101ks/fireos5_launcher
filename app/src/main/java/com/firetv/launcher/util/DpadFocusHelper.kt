package com.firetv.launcher.util

import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.view.ViewCompat

object DpadFocusHelper {

    private const val FOCUSED_SCALE = 1.10f
    private const val UNFOCUSED_SCALE = 1.0f
    private const val ANIMATION_DURATION_MS = 150L
    private const val FOCUSED_ELEVATION = 12f
    private const val UNFOCUSED_ELEVATION = 2f

    private val interpolator = DecelerateInterpolator()

    /**
     * Attaches smooth scale and elevation animations on focus change, matching ATV Launcher Pro.
     */
    fun attachFocusAnimation(view: View, onFocusChange: ((Boolean) -> Unit)? = null) {
        view.setOnFocusChangeListener { v, hasFocus ->
            animateFocus(v, hasFocus)
            onFocusChange?.invoke(hasFocus)
        }
    }

    fun animateFocus(view: View, hasFocus: Boolean) {
        val targetScale = if (hasFocus) FOCUSED_SCALE else UNFOCUSED_SCALE
        val targetElevation = if (hasFocus) FOCUSED_ELEVATION else UNFOCUSED_ELEVATION

        if (hasFocus) {
            // Only bring to front if not inside a LinearLayout (to avoid reordering navigation flow)
            if (view.parent !is android.widget.LinearLayout) {
                view.bringToFront()
            }
            view.parent?.requestLayout()
            (view.parent as? View)?.invalidate()
        }

        ViewCompat.setElevation(view, targetElevation)

        view.animate()
            .scaleX(targetScale)
            .scaleY(targetScale)
            .setDuration(ANIMATION_DURATION_MS)
            .setInterpolator(interpolator)
            .start()
    }
}
