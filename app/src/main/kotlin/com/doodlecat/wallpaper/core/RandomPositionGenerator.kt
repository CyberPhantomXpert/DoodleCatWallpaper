package com.doodlecat.wallpaper.core

import android.graphics.PointF
import kotlin.math.hypot
import kotlin.random.Random

/**
 * Picks genuinely random destinations using the *actual* cat bounding box and the
 * actual surface size. Guarantees:
 *  - the cat box (plus margin) always fits fully on screen, portrait or landscape;
 *  - a configurable minimum travel distance from the current position;
 *  - recency bias suppression: recently used spots are penalised, so no
 *    top-left -> bottom-right -> top-left patterns;
 *  - graceful degradation: if constraints cannot all be met (tiny screen,
 *    huge cat), the least-bad candidate is used instead of looping forever.
 */
class RandomPositionGenerator(private val rng: Random = Random.Default) {

    private val recent = ArrayList<PointF>(RECENT_CAPACITY + 1)

    /**
     * @param surfaceW/H   current wallpaper surface pixels
     * @param boxW/H       scaled cat bounding box pixels for the destination pose
     * @param marginPx     safe inset from every edge
     * @param minTravelPx  minimum distance from [currentX]/[currentY]
     */
    fun next(
        surfaceW: Int,
        surfaceH: Int,
        boxW: Float,
        boxH: Float,
        marginPx: Float,
        minTravelPx: Float,
        currentX: Float,
        currentY: Float,
        out: PointF
    ): PointF {
        val halfW = boxW * 0.5f
        val halfH = boxH * 0.5f
        val minX = marginPx + halfW
        val maxX = surfaceW - marginPx - halfW
        val minY = marginPx + halfH
        val maxY = surfaceH - marginPx - halfH

        // Degenerate case (box larger than surface): pin to centre.
        if (maxX <= minX || maxY <= minY) {
            out.set(surfaceW * 0.5f, surfaceH * 0.5f)
            return out
        }

        var bestX = 0f
        var bestY = 0f
        var bestScore = -1f

        repeat(ATTEMPTS) {
            val cx = rng.nextFloat() * (maxX - minX) + minX
            val cy = rng.nextFloat() * (maxY - minY) + minY

            val travel = hypot(cx - currentX, cy - currentY)
            if (travel < minTravelPx) return@repeat

            var score = travel.coerceAtMost(minTravelPx * 3f)
            for (r in recent) {
                val d = hypot(cx - r.x, cy - r.y)
                if (d < minTravelPx * RECENCY_FACTOR) return@repeat
                score += d.coerceAtMost(minTravelPx * 2f)
            }
            if (score > bestScore) {
                bestScore = score
                bestX = cx
                bestY = cy
            }
            // First candidate that passes every hard constraint and is far from
            // everything: accept immediately for O(1) average cost.
            if (it >= MIN_ATTEMPTS && score > minTravelPx * 4f) {
                return@repeat
            }
        }

        if (bestScore < 0f) {
            // Constraints impossible (very small surface): fall back to the corner
            // quadrant farthest from the current position.
            val fx = if (currentX < surfaceW * 0.5f) maxX else minX
            val fy = if (currentY < surfaceH * 0.5f) maxY else minY
            bestX = fx
            bestY = fy
        }

        out.set(bestX, bestY)
        remember(bestX, bestY)
        return out
    }

    fun clear() = recent.clear()

    private fun remember(x: Float, y: Float) {
        if (recent.size >= RECENT_CAPACITY) recent.removeAt(0)
        recent.add(PointF(x, y))
    }

    private companion object {
        const val ATTEMPTS = 28
        const val MIN_ATTEMPTS = 6
        const val RECENT_CAPACITY = 5
        const val RECENCY_FACTOR = 0.6f
    }
}
