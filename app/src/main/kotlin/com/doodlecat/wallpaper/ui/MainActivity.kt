package com.doodlecat.wallpaper.ui

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import com.doodlecat.wallpaper.CatWallpaperService
import com.doodlecat.wallpaper.R

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btn_set_wallpaper).setOnClickListener {
            openLiveWallpaperPicker()
        }
        findViewById<Button>(R.id.btn_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun openLiveWallpaperPicker() {
        val component = ComponentName(this, CatWallpaperService::class.java)
        val primary = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component.flattenToShortString())
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { startActivity(primary); return }
        runCatching {
            startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }
        runCatching {
            startActivity(Intent(Settings.ACTION_WALLPAPER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
