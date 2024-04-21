package com.virendra.cameramodule.screens.player

import android.media.session.MediaSession
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSourceFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerControlView
import androidx.media3.ui.PlayerView
import com.virendra.cameramodule.R
import com.virendra.cameramodule.constants.PageConstants.VIDEO_URI

class PlayerActivity : AppCompatActivity(), Player.Listener {

    private lateinit var playerView: PlayerView
    @UnstableApi
    private lateinit var playerControlView: PlayerControlView
    @Deprecated("Deprecated in Java")
    @UnstableApi
    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        super.onPlayerStateChanged(playWhenReady, playbackState)
        if (playbackState == Player.STATE_READY && playWhenReady) {
            playerView.player?.play()
        }else if (playbackState == Player.EVENT_PLAYER_ERROR){
            Toast.makeText(this, "Unable to play the video", Toast.LENGTH_SHORT).show()
        }
    }

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = ContextCompat.getColor(this, R.color.teal_700)
        setContentView(R.layout.activity_player)

        val videoUri = Uri.parse(intent.getStringExtra(VIDEO_URI))
        val player = ExoPlayer.Builder(this).build()

        playerView = findViewById(R.id.playerView)
        playerControlView = findViewById(R.id.control_view)

        playerView.player = player
        playerControlView.player = player

        val dataSourceFactory = DefaultDataSourceFactory(this)
        val mediaItem = MediaItem.fromUri(videoUri)


        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
        player.prepare(mediaSource)
        player.addListener(this)

        findViewById<ImageButton>(R.id.imgBack).setOnClickListener {
            finish()
        }
    }


}