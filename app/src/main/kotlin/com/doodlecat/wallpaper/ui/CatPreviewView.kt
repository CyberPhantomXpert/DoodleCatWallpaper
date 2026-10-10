package com.doodlecat.wallpaper.ui

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import com.doodlecat.wallpaper.render.CatPoses
import com.doodlecat.wallpaper.render.CatRenderer

/** Static doodle for the launch screen. Draws once; never invalidates itself. */
class CatPreviewView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val renderer = CatRenderer()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val scale = minOf(width, height) * 0.55f / 100f
        renderer.drawStatic(canvas, CatPoses.CURLED, width * 0.5f, height * 0.5f, scale)
    }
}
