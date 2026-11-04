package com.music.violence.share

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.music.violence.MainActivity

/**
 * A Quick Settings tile that opens Violence on the Receive page, so a phone being shared to is
 * two swipes and a tap away from anywhere (the phone has to be unlocked: the system asks to
 * unlock before it starts an app from a tile).
 */
class ReceiveTileService : TileService() {

    override fun onStartListening() {
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            updateTile()
        }
    }

    @Suppress("DEPRECATION")
    override fun onClick() {
        val intent = Intent(this, MainActivity::class.java)
            .setAction(ReceiveDeepLink.ACTION)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
            )
        } else {
            startActivityAndCollapse(intent)
        }
    }
}
