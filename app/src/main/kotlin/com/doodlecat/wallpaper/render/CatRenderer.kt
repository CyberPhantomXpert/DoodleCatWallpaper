package com.doodlecat.wallpaper.render

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.doodlecat.wallpaper.anim.CatAnimationController
import com.doodlecat.wallpaper.anim.EyeMode

class CatRenderer {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = LINE_UNITS
        color = 0xFFFFFFFF.toInt()
    }
    private val facePaint = Paint(linePaint).apply { strokeWidth = FACE_UNITS }
    private val dotPaint = Paint(linePaint).apply { strokeWidth = DOT_UNITS }
    private val zPaint = Paint(linePaint).apply { strokeWidth = Z_UNITS }

    private val zGlyph = android.graphics.Path().apply {
        moveTo(0f, 0f); lineTo(6f, 0f); lineTo(0f, 7f); lineTo(6f, 7f)
    }
    private val annoyedGlyph = android.graphics.Path().apply {
        moveTo(0f, 0f); lineTo(0f, 6f)
        moveTo(0f, 9f); lineTo(0.05f, 9f)
    }
    private val rect = RectF()

    /**
     * Draws background + cat + sprites. Reads only primitive state from [c];
     * allocates nothing.
     */
    fun draw(canvas: Canvas, c: CatAnimationController, bgColor: Int) {
        canvas.drawColor(bgColor)
        if (c.surfaceW <= 0 || c.surfaceH <= 0) return

        val s = c.unitScale
        val breathAmp = 0.013f * c.breath

        canvas.save()
        canvas.translate(c.center.x, c.center.y + c.bobYUnits * s)
        if (c.tiltDeg != 0f) canvas.rotate(c.tiltDeg)
        canvas.scale(s * c.scaleX, s * c.scaleY)
        canvas.translate(-c.pose.unitsW * 0.5f, -c.pose.unitsH * 0.5f)

        val parts = c.pose.parts
        for (i in parts.indices) {
            val part = parts[i]
            canvas.save()
            when (part.group) {
                PartGroup.BODY -> applyBreath(canvas, part, breathAmp, c)
                PartGroup.HEAD -> applyBreath(canvas, part, breathAmp * 0.5f, c)
                PartGroup.EAR -> {
                    applyBreath(canvas, part, breathAmp * 0.5f, c)
                    val idx = earIndexOf(c.pose, part)
                    if (idx == c.earIndex && c.earAngleDeg != 0f) {
                        canvas.rotate(c.earAngleDeg, part.pivotX, part.pivotY)
                    }
                }
                PartGroup.TAIL -> if (c.tailAngleDeg != 0f) {
                    canvas.rotate(c.tailAngleDeg, part.pivotX, part.pivotY)
                }
                PartGroup.FACE -> Unit
            }
            drawPart(canvas, c, part)
            canvas.restore()
        }

        // Annoyed "!" above the head (easter egg feedback).
        if (c.showAnnoyedMark) {
            canvas.save()
            canvas.translate(c.pose.headAnchorX, c.pose.headAnchorY - 8f)
            canvas.scale(0.9f, 0.9f)
            canvas.drawPath(annoyedGlyph, dotPaint)
            canvas.restore()
        }
        canvas.restore()

        drawZSprites(canvas, c, s)
    }

    private fun applyBreath(canvas: Canvas, part: Part, amp: Float, c: CatAnimationController) {
        if (amp == 0f) return
        val px = c.pose.bodyPivotX
        val py = c.pose.bodyPivotY
        canvas.translate(px, py)
        canvas.scale(1f + amp, 1f + amp)
        canvas.translate(-px, -py)
    }

    private fun drawPart(canvas: Canvas, c: CatAnimationController, part: Part) {
        when (part.group) {
            PartGroup.FACE -> {
                // parts order in every pose: closedEyes, openEyes, mouth(, whiskers)
                // resolved by index within FACE group instead of fragile ordering:
                val faceIndex = faceIndexIn(c.pose, part)
                val isOpen = faceIndex == 1
                val isClosed = faceIndex == 0
                val eyesOpenVisible = c.eyeMode == EyeMode.OPEN
                val oneEye = c.eyeMode == EyeMode.LEFT_OPEN || c.eyeMode == EyeMode.RIGHT_OPEN
                when {
                    isClosed && !eyesOpenVisible && !oneEye -> canvas.drawPath(part.path, facePaint)
                    isOpen && eyesOpenVisible -> canvas.drawPath(part.path, dotPaint)
                    isClosed && oneEye -> drawHalfClosed(canvas, part, c)
                    faceIndex >= 2 -> canvas.drawPath(part.path, facePaint) // mouth / whiskers
                }
            }
            else -> canvas.drawPath(part.path, linePaint)
        }
    }

    /** One eye open: draw the open dot on one side, closed arc on the other. */
    private fun drawHalfClosed(canvas: Canvas, part: Part, c: CatAnimationController) {
        // Clip-free trick: draw the closed arc but only for the eye that stays shut.
        val leftOpen = c.eyeMode == EyeMode.LEFT_OPEN
        val mid = closedEyesMidX(c.pose, part)
        canvas.save()
        rect.set(
            if (leftOpen) mid else -1000f,
            -1000f,
            if (leftOpen) 1000f else mid,
            1000f
        )
        canvas.clipRect(rect)
        canvas.drawPath(part.path, facePaint)
        canvas.restore()
        // open dot for the other eye
        val openPart = openEyesPart(c.pose)
        if (openPart != null) {
            canvas.save()
            rect.set(
                if (leftOpen) -1000f else mid,
                -1000f,
                if (leftOpen) mid else 1000f,
                1000f
            )
            canvas.clipRect(rect)
            canvas.drawPath(openPart.path, dotPaint)
            canvas.restore()
        }
    }

    private fun drawZSprites(canvas: Canvas, c: CatAnimationController, s: Float) {
        for (z in c.zSprites) {
            if (!z.alive) continue
            val age = (System.currentTimeMillis() - z.spawnMs).coerceAtLeast(0L)
            val t = (age.toFloat() / c.zLifetimeMs).coerceIn(0f, 1f)
            val alpha = ((1f - t) * 200f).toInt().coerceIn(0, 255)
            zPaint.alpha = alpha
            canvas.save()
            canvas.translate(
                c.center.x + (c.pose.headAnchorX - c.pose.unitsW * 0.5f + z.dxUnits + t * 6f) * s,
                c.center.y + (c.pose.headAnchorY - c.pose.unitsH * 0.5f - 4f - t * 16f) * s
            )
            canvas.scale(s * 0.9f * z.jitter, s * 0.9f * z.jitter)
            canvas.drawPath(zGlyph, zPaint)
            canvas.restore()
        }
        zPaint.alpha = 255
    }

    /** Static one-shot draw for the app preview view. */
    fun drawStatic(canvas: Canvas, pose: CatPose, cx: Float, cy: Float, scale: Float) {
        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(scale, scale)
        canvas.translate(-pose.unitsW * 0.5f, -pose.unitsH * 0.5f)
        for (part in pose.parts) {
            val faceIndex = faceIndexIn(pose, part)
            when {
                part.group != PartGroup.FACE -> canvas.drawPath(part.path, linePaint)
                faceIndex == 0 || faceIndex >= 2 -> canvas.drawPath(part.path, facePaint)
            }
        }
        canvas.restore()
    }

    /* ---- FACE part indexing (0=closed eyes, 1=open eyes, 2+=mouth/whiskers) ---- */
    private val faceIndexCache = HashMap<CatPose, IntArray>()

    private fun faceIndices(pose: CatPose): IntArray =
        faceIndexCache.getOrPut(pose) {
            val arr = IntArray(pose.parts.size)
            var k = 0
            for (i in pose.parts.indices) {
                if (pose.parts[i].group == PartGroup.FACE) { arr[i] = k; k++ } else arr[i] = -1
            }
            arr
        }

    private fun faceIndexIn(pose: CatPose, part: Part): Int {
        val idx = faceIndices(pose)
        val i = pose.parts.indexOf(part)
        return if (i >= 0) idx[i] else 2
    }

    private fun earIndexOf(pose: CatPose, part: Part): Int {
        var k = 0
        for (p in pose.parts) {
            if (p.group == PartGroup.EAR) {
                if (p === part) return k
                k++
            }
        }
        return 0
    }

    private fun closedEyesMidX(pose: CatPose, closedPart: Part): Float {
        val b = android.graphics.RectF()
        closedPart.path.computeBounds(b, true)
        return b.centerX()
    }

    private fun openEyesPart(pose: CatPose): Part? {
        val idx = faceIndices(pose)
        for (i in pose.parts.indices) {
            if (idx[i] == 1) return pose.parts[i]
        }
        return null
    }

    private companion object {
        const val LINE_UNITS = 2.2f
        const val FACE_UNITS = 1.5f
        const val DOT_UNITS = 2.6f
        const val Z_UNITS = 1.7f
    }
}
