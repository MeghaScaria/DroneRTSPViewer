package com.example.dronertspviewer

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Bundle
import android.util.Rational
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var libVLC: LibVLC
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var videoLayout: VLCVideoLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        videoLayout = findViewById(R.id.videoLayout)

        val options = arrayListOf<String>()
        libVLC = LibVLC(this, options)
        mediaPlayer = MediaPlayer(libVLC)
        mediaPlayer.attachViews(videoLayout, null, false, false)

        // Set up the Play button
        val playButton = findViewById<Button>(R.id.playButton)
        playButton.setOnClickListener {
            playStream("rtsp://192.168.1.3:554/ch0")
        }

        // Set up the Record button
        val recordButton = findViewById<Button>(R.id.recordButton)
        recordButton.setOnClickListener {
            playStream("rtsp://192.168.1.3:554/ch0", record = true)
        }

        // Set up the PiP button
        val pipButton = findViewById<Button>(R.id.pipButton)
        pipButton.setOnClickListener {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                // Use a fixed aspect ratio (e.g., 16:9) for simplicity
                val aspectRatio = Rational(16, 9)
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio)
                    .build()
                enterPictureInPictureMode(params)
            } else {
                Toast.makeText(this, "PiP not supported on this device", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getOutputFilePath(): String {
        val folder = getExternalFilesDir(null)
        val file = File(folder, "recorded_stream.mp4")
        return file.absolutePath
    }

    private fun playStream(url: String, record: Boolean = false) {
        if (url.isNotEmpty()) {
            try {
                val media = Media(libVLC, url.toUri())
                media.setHWDecoderEnabled(true, false)

                if (record) {
                    val outputFilePath = getOutputFilePath()
                    @Suppress("SpellCheckingInspection")
                    val soutOption = ":sout=#duplicate{dst=display,dst=std{access=file,mux=mp4,dst=${outputFilePath}}}"
                    media.addOption(soutOption)
                }

                mediaPlayer.media = media
                media.release()
                mediaPlayer.play()
            } catch (e: Exception) {
                Toast.makeText(this, "Error playing stream: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Please enter a valid RTSP URL", Toast.LENGTH_SHORT).show()
        }
    }

    // Override to handle changes when entering or exiting PiP
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration?) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        // Hide UI controls when in PiP mode
        val playButton = findViewById<Button>(R.id.playButton)
        val recordButton = findViewById<Button>(R.id.recordButton)
        val pipButton = findViewById<Button>(R.id.pipButton)
        if (isInPictureInPictureMode) {
            playButton.visibility = android.view.View.GONE
            recordButton.visibility = android.view.View.GONE
            pipButton.visibility = android.view.View.GONE
        } else {
            playButton.visibility = android.view.View.VISIBLE
            recordButton.visibility = android.view.View.VISIBLE
            pipButton.visibility = android.view.View.VISIBLE
        }
    }

    override fun onStop() {
        super.onStop()
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
        mediaPlayer.detachViews()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
        libVLC.release()
    }
}
