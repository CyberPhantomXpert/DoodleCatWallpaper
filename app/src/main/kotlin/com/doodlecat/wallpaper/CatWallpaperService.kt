package com.doodlecat.wallpaper

import android.service.wallpaper.WallpaperService

class CatWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = CatEngine(this)
}
