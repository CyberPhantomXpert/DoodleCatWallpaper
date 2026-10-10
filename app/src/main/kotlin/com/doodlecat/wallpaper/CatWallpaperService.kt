package com.doodlecat.wallpaper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.Choreographer
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import com.doodlecat.wallpaper.anim.CatAnimationController
import com.doodlecat.wallpaper.core.Prefs
import com.doodlecat.wallpaper.core.TouchController
import com.doodlecat.wallpaper.render.CatRenderer

class CatWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = CatEngine()

    /**
     * Battery contract:
     *  - full-rate Choreographer frames ONLY during sub-second animation bursts;
     *  - sleeping cat ticks at ~11 Hz for breathing (or not at all if nothing can change);
     *  - screen-off / visibility-loss removes every callback and receiver-driven work;
     *  - no threads, no wake locks, no allocations in the draw path.
     */
    inner class CatEngine : Engine() {

        /** The owning service (Engine is a Java inner class, so we expose the outer instance). */
        private val service: CatWallpaperService get() = this@CatWallpaperService

        private val prefs = Prefs.get(service)
        private val controller = CatAnimationController(prefs)
        private val renderer = CatRenderer()
        private val touch = TouchController(service)
        private val handler = Handler(Looper.getMainLooper())
        private val choreographer = Choreographer.getInstance()

        private var visible = false
        private var screenOn = true
        private var framesRunning = false
        private var surfaceReady = false
        private var appeared = false

        private val frameCallback: Choreographer.FrameCallback = Choreographer.FrameCallback {
            val now = SystemClock.uptimeMillis()
            controller.updateFrame(now)
            drawFrame()
            if (controller.isAnimating) {
                choreographer.postFrameCallback(frameCallback)
            } else {
                framesRunning = false
                enterIdle()
            }
        }

        private val idleRunnable = object : Runnable {
            override fun run() {
                if (!visible || !screenOn) return
                val now = SystemClock.uptimeMillis()
                val needsBurst = controller.updateIdle(now)
                if (needsBurst) {
                    startFrames()
                } else {
                    drawFrame()
                }
                enterIdle()
            }
        }

        private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            controller.onPrefsChanged()
            if (visible && screenOn) {
                drawFrame()
                enterIdle()
            }
        }

        private val screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        screenOn = false
                        stopAllWork()
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        screenOn = true
                        if (visible) startEverything()
                    }
                }
            }
        }

        /* ---------------- lifecycle ---------------- */

        override fun onCreate(surfaceHolder: android.view.SurfaceHolder) {
            super.onCreate(surfaceHolder)
            prefs.registerListener(prefListener)
            ContextCompat.registerReceiver(
                this@CatEngine.service as Context? ?: return,
                screenReceiver,
                IntentFilter().apply {
                    addAction(Intent.ACTION_SCREEN_OFF)
                    addAction(Intent.ACTION_SCREEN_ON)
                },
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }

        override fun onSurfaceCreated(holder: android.view.SurfaceHolder) {
            super.onSurfaceCreated(holder)
            surfaceReady = true
            if (!appeared) {
                controller.onSurfaceChanged(holder.surfaceFrame.width(), holder.surfaceFrame.height())
                controller.appear(SystemClock.uptimeMillis())
                appeared = true
            }
            if (visible && screenOn) startEverything()
        }

        override fun onSurfaceChanged(holder: android.view.SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            controller.onSurfaceChanged(width, height)
            if (visible && screenOn) drawFrame()
        }

        override fun onSurfaceDestroyed(holder: android.view.SurfaceHolder) {
            surfaceReady = false
            stopAllWork()
            super.onSurfaceDestroyed(holder)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            this.visible = visible
            if (visible && screenOn && surfaceReady) startEverything()
            else stopAllWork()
        }

        override fun onDestroy() {
            stopAllWork()
            prefs.unregisterListener(prefListener)
            runCatching { (service as Context).unregisterReceiver(screenReceiver) }
            controller.pause()
            super.onDestroy()
        }

        /* ---------------- pacing ---------------- */

        private fun startEverything() {
            controller.resume(SystemClock.uptimeMillis())
            drawFrame()
            if (controller.isAnimating) startFrames() else enterIdle()
        }

        private fun startFrames() {
            if (framesRunning) return
            framesRunning = true
            handler.removeCallbacks(idleRunnable)
            choreographer.postFrameCallback(frameCallback)
        }

        private fun enterIdle() {
            handler.removeCallbacks(idleRunnable)
            if (!visible || !screenOn) return
            val delay = controller.nextIdleDelayMs(SystemClock.uptimeMillis())
            if (delay >= 0) handler.postDelayed(idleRunnable, delay)
            // delay == -1 → perfectly static wallpaper: zero scheduled work.
        }

        private fun stopAllWork() {
            handler.removeCallbacksAndMessages(null)
            if (framesRunning) {
                choreographer.removeFrameCallback(frameCallback)
                framesRunning = false
            }
            controller.pause()
        }

        /* ---------------- input & draw ---------------- */

        override fun onTouchEvent(event: MotionEvent) {
            if (!visible || !screenOn) return
            val tap = touch.onTouch(event) ?: return
            controller.onTap(tap.x, tap.y, SystemClock.uptimeMillis())
            if (controller.isAnimating) startFrames()
        }

        private fun drawFrame() {
            if (!surfaceReady) return
            val canvas = surfaceHolder.lockCanvas() ?: return
            try {
                renderer.draw(canvas, controller, prefs.backgroundColor)
            } finally {
                surfaceHolder.unlockCanvasAndPost(canvas)
            }
        }
    }
}
