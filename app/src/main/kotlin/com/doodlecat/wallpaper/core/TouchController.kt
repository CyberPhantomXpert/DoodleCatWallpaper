package com.doodlecat.wallpaper.core

import android.view.MotionEvent
import android.view.ViewConfiguration
import kotlin.math.abs

/**
 * Reduces raw motion events to deliberate single-finger taps.
 * Drags, multi-touch and cancelled gestures are ignored so the cat is never
 * "chased" by a finger and rapid input can never stack animations.
 */
class TouchController(context: android.content.Context) {

    class Tap(val x: Float, val y: Float)

    private val slop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()

    private var tracking = false
    private var downX = 0f
    private var downY = 0f
    private var downTime = 0L

    /** Returns a [Tap] on a valid ACTION_UP, else null. Never allocates per event. */
    private var reusableTap = Tap(0f, 0f)

    fun onTouch(event: MotionEvent): Tap? {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                tracking = event.pointerCount == 1
                downX = event.x
                downY = event.y
                downTime = event.eventTime
            }
            MotionEvent.ACTION_POINTER_DOWN -> tracking = false
            MotionEvent.ACTION_MOVE -> {
                if (tracking &&
                    (abs(event.x - downX) > slop || abs(event.y - downY) > slop)
                ) tracking = false
            }
            MotionEvent.ACTION_UP -> {
                if (tracking &&
                    event.eventTime - downTime < MAX_TAP_MS &&
                    abs(event.x - downX) <= slop &&
                    abs(event.y - downY) <= slop
                ) {
                    reusableTap = Tap(event.x, event.y)
                    tracking = false
                    return reusableTap
                }
                tracking = false
            }
            MotionEvent.ACTION_CANCEL -> tracking = false
        }
        return null
    }

    private companion object {
        const val MAX_TAP_MS = 500L
    }
}
