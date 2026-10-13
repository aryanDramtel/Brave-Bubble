package com.example.brave_bubble

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class BubbleTileService : TileService() {
    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return
        
        if (!Settings.canDrawOverlays(this)) {
            tile.state = Tile.STATE_INACTIVE
            tile.updateTile()
            return
        }

        val prefs = getSharedPreferences("BubblePrefs", Context.MODE_PRIVATE)
        val isRunning = prefs.getBoolean("is_bubble_running", false)
        
        val serviceIntent = Intent(this, BubbleService::class.java)
        if (isRunning) {
            stopService(serviceIntent)
            tile.state = Tile.STATE_INACTIVE
        } else {
            startForegroundService(serviceIntent)
            tile.state = Tile.STATE_ACTIVE
        }
        tile.updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        val prefs = getSharedPreferences("BubblePrefs", Context.MODE_PRIVATE)
        val isRunning = prefs.getBoolean("is_bubble_running", false)
        qsTile?.state = if (isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        qsTile?.updateTile()
    }
}