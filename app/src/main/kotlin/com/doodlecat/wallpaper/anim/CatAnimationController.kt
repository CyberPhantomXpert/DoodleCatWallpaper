package com.doodlecat.wallpaper.anim

import android.graphics.PointF
import com.doodlecat.wallpaper.core.Prefs
import com.doodlecat.wallpaper.core.RandomPositionGenerator
import com.doodlecat.wallpaper.render.CatPose
import com.doodlecat.wallpaper.render.CatPoses
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class CatAnimationController(
    private val prefs: Prefs,
    private val rng: Random = Random.Default
) {

    /* ---------- geometry (written by engine on surface/prefs changes) ---------- */
    var surfaceW: Int = 0
        private set
    var surfaceH: Int = 0
        private set
    var unitScale: Float = 0.05f   // px per design unit
        private set
    var marginPx: Float = 40f
        private set

    /* ---------- render-read state ---------- */
    var state: CatState = CatState.SLEEPING
        private set
    var pose: CatPose = CatPoses.CURLED
        private set
    val center = PointF(0f, 0f)
    var scaleX: Float = 1f; private set
    var scaleY: Float = 1f; private set
    var tiltDeg: Float = 0f; private set
    var bobYUnits: Float = 0f; private set
    var breath: Float = 0f; private set          // 0..1 sine phase
    var tailAngleDeg: Float = 0f; private set
    var earAngleDeg: Float = 0f; private set
    var earIndex: Int = 1; private set           // which ear twitches
    var eyeMode: EyeMode = EyeMode.CLOSED; private set
    var showAnnoyedMark: Boolean = false; private set
    val zSprites: Array<ZSprite> = Array(3) { ZSprite() }

    /** Bounding box of the currently displayed pose, in px. */
    val boxW: Float get() = pose.unitsW * unitScale
    val boxH: Float get() = pose.unitsH * unitScale

    /* ---------- internals ---------- */
    private val posGen = RandomPositionGenerator(rng)
    private val dest = PointF()
    private val from = PointF()
    private var stateStartMs = 0L
    private var stateDurMs = 1L
    private var moveStyle = MoveStyle.SLIDE
    private var pendingSleepPose: CatPose = CatPoses.CURLED
    private var breathPhaseRad = rng.nextFloat() * PI.toFloat() * 2f
    private var nextRareAtMs = Long.MAX_VALUE
    private var activeEvent: RareEvent? = null
    private var eventDir = 1f
    private var running = false
    private var lastTickMs = 0L
    private var annoyance = 0
    private val catTapTimes = ArrayList<Long>(8)

    val isAnimating: Boolean get() = state != CatState.SLEEPING || activeEvent != null

    /* ================= lifecycle ================= */

    fun onSurfaceChanged(w: Int, h: Int) {
        surfaceW = w
        surfaceH = h
        val minDim = minOf(w, h).coerceAtLeast(1)
        marginPx = (minDim * 0.07f).coerceIn(24f, 96f)
        recomputeScale()
        clampCenter()
        if (state == CatState.MOVING) clampPoint(dest)
    }

    fun onPrefsChanged() {
        recomputeScale()
        clampCenter()
    }

    private fun recomputeScale() {
        val minDim = minOf(surfaceW, surfaceH).coerceAtLeast(1)
        val target = minDim * prefs.size.fraction
        // Scale against the largest pose dimension so every pose fits the budget.
        val refUnits = maxOf(CatPoses.SLEEP_POSES.maxOf { maxOf(it.unitsW, it.unitsH) }, 80f)
        var s = target / refUnits
        // Hard guarantee: the cat box can never exceed the usable area.
        val availW = (surfaceW - marginPx * 2f).coerceAtLeast(1f)
        val availH = (surfaceH - marginPx * 2f).coerceAtLeast(1f)
        s = minOf(s, availW / pose.unitsW, availH / pose.unitsH)
        unitScale = s.coerceAtLeast(0.01f)
    }

    /** First appearance: random spot, random pose, already asleep. */
    fun appear(nowMs: Long) {
        pose = CatPoses.SLEEP_POSES[rng.nextInt(CatPoses.SLEEP_POSES.size)]
        recomputeScale()
        posGen.clear()
        center.set(
            marginPx + boxW / 2f + rng.nextFloat() * (surfaceW - 2f * marginPx - boxW).coerceAtLeast(0f),
            marginPx + boxH / 2f + rng.nextFloat() * (surfaceH - 2f * marginPx - boxH).coerceAtLeast(0f)
        )
        clampCenter()
        state = CatState.SLEEPING
        eyeMode = EyeMode.CLOSED
        resetTransforms()
        scheduleNextRare(nowMs, first = true)
        running = true
        lastTickMs = nowMs
    }

    fun resume(nowMs: Long) {
        running = true
        lastTickMs = nowMs
        if (nextRareAtMs == Long.MAX_VALUE && prefs.rareEvents) scheduleNextRare(nowMs, first = true)
    }

    fun pause() {
        running = false
        activeEvent = null
    }

    /* ================= input ================= */

    /** A deliberate tap. Retargets gracefully if already moving; never queues stacks. */
    fun onTap(x: Float, y: Float, nowMs: Long) {
        if (!running) return
        trackCatTap(x, y, nowMs)
        showAnnoyedMark = annoyance >= ANNOY_THRESHOLD
        if (state == CatState.MOVING) {
            beginMove(nowMs)          // interrupt: new destination from live position
        } else {
            state = CatState.REACT
            stateStartMs = nowMs
            stateDurMs = REACT_MS
            eyeMode = EyeMode.OPEN
        }
    }

    private fun trackCatTap(x: Float, y: Float, nowMs: Long) {
        val hit = x > center.x - boxW * 0.6f && x < center.x + boxW * 0.6f &&
                y > center.y - boxH * 0.6f && y < center.y + boxH * 0.6f
        if (!hit) {
            if (catTapTimes.isNotEmpty() && nowMs - catTapTimes.last() > CAT_TAP_WINDOW_MS) {
                catTapTimes.clear(); annoyance = 0
            }
            return
        }
        catTapTimes.removeAll { nowMs - it > CAT_TAP_WINDOW_MS }
        catTapTimes.add(nowMs)
        annoyance = if (catTapTimes.size >= ANNOY_THRESHOLD) 2 else 0
    }

    /* ================= frame update (burst pacing) ================= */

    /** Advance one animation frame. Called at display rate ONLY while [isAnimating]. */
    fun updateFrame(nowMs: Long) {
        val event = activeEvent
        if (event != null && state == CatState.SLEEPING) {
            updateRareEvent(nowMs)
            return
        }
        when (state) {
            CatState.REACT -> {
                val t = progress(nowMs)
                if (t >= 1f) beginMove(nowMs)
                else {
                    val p = sin(PI * t).toFloat()
                    scaleX = 1f + 0.035f * p
                    scaleY = 1f - 0.03f * p
                }
            }
            CatState.MOVING -> {
                val t = progress(nowMs)
                if (t >= 1f) beginSettle(nowMs)
                else applyMove(t)
            }
            CatState.SETTLING -> {
                val t = progress(nowMs)
                if (t >= 1f) enterSleep(nowMs)
                else {
                    val d = (1f - t) * sin(PI * t * 1.5f).toFloat()
                    scaleX = 1f + 0.05f * d
                    scaleY = 1f - 0.05f * d
                }
            }
            CatState.SLEEPING -> Unit
        }
    }

    private fun applyMove(t: Float) {
        val eased = t * t * (3f - 2f * t)   // smoothstep
        center.set(
            from.x + (dest.x - from.x) * eased,
            from.y + (dest.y - from.y) * eased
        )
        when (moveStyle) {
            MoveStyle.SLIDE -> {
                bobYUnits = 0f
                tiltDeg = 1.5f * sin(PI * t).toFloat()
                scaleX = 1.03f; scaleY = 0.98f
            }
            MoveStyle.HOP -> {
                val hops = 2f
                bobYUnits = -HOP_HEIGHT_UNITS * abs(sin(PI * t * hops)).toFloat()
                val squash = Math.pow(abs(cos(PI * t * hops)).toDouble(), 8.0).toFloat()
                scaleY = 1f - 0.08f * squash
                scaleX = 1f + 0.06f * squash
                tiltDeg = 0f
            }
            MoveStyle.SCAMPER -> {
                bobYUnits = -2.4f * abs(sin(PI * t * 4f)).toFloat()
                tiltDeg = 2.2f * sin(2.0 * PI * t).toFloat()
                scaleX = 1f; scaleY = 1f
            }
        }
    }

    /* ================= state transitions ================= */

    private fun beginMove(nowMs: Long) {
        from.set(center)
        pendingSleepPose = chooseNextSleepPose()
        val travelPose = CatPoses.WALK
        pose = travelPose
        recomputeScale()
        val minDim = minOf(surfaceW, surfaceH).coerceAtLeast(1)
        var minTravel = minDim * MIN_TRAVEL_FRACTION
        if (annoyance > 0) minTravel *= 1.8f          // easter egg: flee farther
        posGen.next(
            surfaceW, surfaceH, boxW, boxH, marginPx, minTravel, from.x, from.y, dest
        )
        clampPoint(dest)
        moveStyle = MoveStyle.entries[rng.nextInt(MoveStyle.entries.size)]
        val jitter = 0.85f + rng.nextFloat() * 0.3f
        val annoyFactor = if (annoyance > 0) 0.8f else 1f
        stateDurMs = (prefs.speed.baseMoveMs * jitter * annoyFactor)
            .toLong().coerceIn(220L, 900L)
        stateStartMs = nowMs
        state = CatState.MOVING
        eyeMode = EyeMode.OPEN
    }

    private fun beginSettle(nowMs: Long) {
        pose = pendingSleepPose
        recomputeScale()
        clampCenter()
        state = CatState.SETTLING
        stateStartMs = nowMs
        stateDurMs = SETTLE_MS
        eyeMode = EyeMode.CLOSED
        showAnnoyedMark = false
        annoyance = (annoyance - 1).coerceAtLeast(0)
    }

    private fun enterSleep(nowMs: Long) {
        state = CatState.SLEEPING
        resetTransforms()
        scheduleNextRare(nowMs, first = false)
    }

    private fun resetTransforms() {
        scaleX = 1f; scaleY = 1f; tiltDeg = 0f; bobYUnits = 0f
        tailAngleDeg = 0f; earAngleDeg = 0f
    }

    private fun chooseNextSleepPose(): CatPose {
        if (!prefs.randomPose) return pose.let {
            if (it in CatPoses.SLEEP_POSES) it else CatPoses.CURLED
        }
        var next = CatPoses.SLEEP_POSES[rng.nextInt(CatPoses.SLEEP_POSES.size)]
        if (next == pose) next = CatPoses.SLEEP_POSES[(CatPoses.SLEEP_POSES.indexOf(next) + 1) % CatPoses.SLEEP_POSES.size]
        return next
    }

    /* ================= idle (slow pacing, battery-first) ================= */

    /**
     * Called from the engine's slow idle ticker. Advances breathing and spawns
     * due rare events. Returns true if a full-speed burst must start now.
     */
    fun updateIdle(nowMs: Long): Boolean {
        if (!running) return false
        val dt = (nowMs - lastTickMs).coerceIn(0L, 500L)
        lastTickMs = nowMs
        breathPhaseRad = (breathPhaseRad + dt * 2.0f * PI / BREATH_PERIOD_MS).toFloat()
        breath = (0.5f + 0.5f * sin(breathPhaseRad)).toFloat()

        updateZSprites(nowMs)

        if (prefs.rareEvents && nowMs >= nextRareAtMs && activeEvent == null && state == CatState.SLEEPING) {
            return startRareEvent(nowMs)
        }
        return false
    }

    /**
     * Delay until the next idle tick. -1 means "nothing will ever change":
     * the engine then schedules no work at all until an external event arrives.
     * This is what makes the wallpaper effectively static from a battery view.
     */
    fun nextIdleDelayMs(nowMs: Long): Long {
        if (!running) return -1L
        var delay = Long.MAX_VALUE
        if (breathVisible()) delay = minOf(delay, IDLE_TICK_MS)
        if (zActive()) delay = minOf(delay, IDLE_TICK_MS)
        if (prefs.rareEvents && nextRareAtMs != Long.MAX_VALUE) {
            delay = minOf(delay, (nextRareAtMs - nowMs).coerceAtLeast(1L))
        }
        return if (delay == Long.MAX_VALUE) -1L else delay.coerceAtMost(MAX_IDLE_TICK_MS)
    }

    private fun breathVisible(): Boolean = state == CatState.SLEEPING && activeEvent == null

    private fun zActive(): Boolean = zSprites.any { it.alive }

    /* ================= rare events ================= */

    private fun scheduleNextRare(nowMs: Long, first: Boolean) {
        if (!prefs.rareEvents) { nextRareAtMs = Long.MAX_VALUE; return }
        val range = if (first) 12_000L..30_000L else 25_000L..90_000L
        nextRareAtMs = nowMs + rng.nextLong(range.first, range.last)
    }

    private fun startRareEvent(nowMs: Long): Boolean {
        val pool = if (prefs.sleepingZ) RARE_WITH_Z else RARE_NO_Z
        val event = pool[rng.nextInt(pool.size)]
        eventDir = if (rng.nextBoolean()) 1f else -1f
        earIndex = rng.nextInt(2)
        when (event) {
            RareEvent.ZZZ -> {
                spawnZ(nowMs)
                scheduleNextRare(nowMs, first = false)
                return false            // z animates on the slow ticker
            }
            RareEvent.POSE_CHANGE -> {
                pendingSleepPose = chooseNextSleepPose()
                activeEvent = event
                stateStartMs = nowMs
                stateDurMs = POSE_CHANGE_MS
                return true
            }
            RareEvent.MICRO_SHIFT -> {
                from.set(center)
                val minDim = minOf(surfaceW, surfaceH).coerceAtLeast(1)
                val d = minDim * (0.02f + rng.nextFloat() * 0.03f)
                val a = rng.nextFloat() * 2f * PI
                dest.set(center.x + cos(a).toFloat() * d, center.y + sin(a).toFloat() * d)
                clampPoint(dest)
                activeEvent = event
                stateStartMs = nowMs
                stateDurMs = MICRO_SHIFT_MS
                return true
            }
            else -> {
                activeEvent = event
                stateStartMs = nowMs
                stateDurMs = when (event) {
                    RareEvent.EAR_TWITCH -> EAR_TWITCH_MS
                    RareEvent.TAIL_TWITCH -> TAIL_TWITCH_MS
                    RareEvent.ONE_EYE -> ONE_EYE_MS
                    else -> 300L
                }
                if (event == RareEvent.ONE_EYE) {
                    eyeMode = if (rng.nextBoolean()) EyeMode.LEFT_OPEN else EyeMode.RIGHT_OPEN
                }
                return true
            }
        }
    }

    private fun updateRareEvent(nowMs: Long) {
        val event = activeEvent ?: return
        val t = progress(nowMs)
        if (t >= 1f) {
            activeEvent = null
            tailAngleDeg = 0f
            earAngleDeg = 0f
            if (eyeMode != EyeMode.CLOSED && state == CatState.SLEEPING) eyeMode = EyeMode.CLOSED
            if (event == RareEvent.POSE_CHANGE) resetTransforms()
            return
        }
        when (event) {
            RareEvent.EAR_TWITCH -> earAngleDeg = sin(PI * t).toFloat() * 14f * eventDir
            RareEvent.TAIL_TWITCH -> tailAngleDeg = sin(PI * t * 2.0).toFloat() * 10f * eventDir
            RareEvent.ONE_EYE -> Unit
            RareEvent.MICRO_SHIFT -> {
                val e = t * t * (3f - 2f * t)
                center.set(from.x + (dest.x - from.x) * e, from.y + (dest.y - from.y) * e)
            }
            RareEvent.POSE_CHANGE -> {
                val squash = sin(PI * t).toFloat()
                scaleX = 1f + 0.04f * squash
                scaleY = 1f - 0.05f * squash
                if (t >= 0.45f && pose != pendingSleepPose) {
                    pose = pendingSleepPose
                    recomputeScale()
                    clampCenter()
                }
            }
            RareEvent.ZZZ -> Unit
        }
    }

    /* ================= Z sprites ================= */

    private fun spawnZ(nowMs: Long) {
        val slot = zSprites.firstOrNull { !it.alive } ?: zSprites[0]
        slot.alive = true
        slot.spawnMs = nowMs
        slot.dxUnits = (rng.nextFloat() - 0.3f) * 6f
        slot.jitter = 0.85f + rng.nextFloat() * 0.4f
        scheduleNextRare(nowMs, first = false)
    }

    private fun updateZSprites(nowMs: Long) {
        for (z in zSprites) {
            if (z.alive && nowMs - z.spawnMs > Z_LIFETIME_MS) z.alive = false
        }
    }

    /* ================= helpers ================= */

    private fun progress(nowMs: Long): Float =
        ((nowMs - stateStartMs).toFloat() / stateDurMs).coerceIn(0f, 1f)

    private fun clampCenter() = clampPoint(center)

    private fun clampPoint(p: PointF) {
        val halfW = pose.unitsW * unitScale * 0.5f
        val halfH = pose.unitsH * unitScale * 0.5f
        val minX = marginPx + halfW
        val maxX = surfaceW - marginPx - halfW
        val minY = marginPx + halfH
        val maxY = surfaceH - marginPx - halfH
        p.x = if (maxX <= minX) surfaceW * 0.5f else coerce(p.x, minX, maxX)
        p.y = if (maxY <= minY) surfaceH * 0.5f else coerce(p.y, minY, maxY)
    }

    private fun coerce(v: Float, min: Float, max: Float) = v.coerceIn(min, max)

    private companion object {
        const val REACT_MS = 150L
        const val SETTLE_MS = 240L
        const val EAR_TWITCH_MS = 280L
        const val TAIL_TWITCH_MS = 420L
        const val ONE_EYE_MS = 950L
        const val MICRO_SHIFT_MS = 260L
        const val POSE_CHANGE_MS = 340L
        const val BREATH_PERIOD_MS = 3400f
        const val IDLE_TICK_MS = 80L          // ~11 Hz, only while breathing/z active
        const val MAX_IDLE_TICK_MS = 60_000L
        const val Z_LIFETIME_MS = 2400L
        const val HOP_HEIGHT_UNITS = 5f
        const val MIN_TRAVEL_FRACTION = 0.18f // configurable minimum travel distance
        const val CAT_TAP_WINDOW_MS = 6000L
        const val ANNOY_THRESHOLD = 4
        val RARE_WITH_Z = arrayOf(
            RareEvent.EAR_TWITCH, RareEvent.EAR_TWITCH,
            RareEvent.TAIL_TWITCH, RareEvent.TAIL_TWITCH,
            RareEvent.ZZZ, RareEvent.ONE_EYE,
            RareEvent.MICRO_SHIFT, RareEvent.POSE_CHANGE
        )
        val RARE_NO_Z = arrayOf(
            RareEvent.EAR_TWITCH, RareEvent.TAIL_TWITCH,
            RareEvent.ONE_EYE, RareEvent.MICRO_SHIFT, RareEvent.POSE_CHANGE
        )
    }

    val zLifetimeMs: Long get() = Z_LIFETIME_MS
}
