package com.example.dronertspviewer

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.widget.Button
import android.widget.EditText
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
    private lateinit var urlInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        videoLayout = findViewById(R.id.videoLayout)
        urlInput = findViewById(R.id.rtspUrlInput)

        // Set up VLC
        //val options = arrayListOf<String>()
        val options = arrayListOf(
            "--network-caching=1500",
            "--no-drop-late-frames",
            "--no-skip-frames",
            "--rtsp-tcp"
        )

        libVLC = LibVLC(this, options)
        mediaPlayer = MediaPlayer(libVLC)
        mediaPlayer.attachViews(videoLayout, null, false, false)

        // Play Button
        findViewById<Button>(R.id.playButton).setOnClickListener {
            val url = urlInput.text.toString()
            playStream(url)
        }

        // Record Button
        findViewById<Button>(R.id.recordButton).setOnClickListener {
            val url = urlInput.text.toString()
            playStream(url, record = true)
        }

        // PiP Button
        findViewById<Button>(R.id.pipButton).setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
        return File(folder, "recorded_stream.mp4").absolutePath
    }

    private fun playStream(url: String, record: Boolean = false) {
        if (url.isBlank()) {
            Toast.makeText(this, "Please enter a valid RTSP URL", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val media = Media(libVLC, url.toUri())
            media.setHWDecoderEnabled(true, false)

            if (record) {
                val outputFilePath = getOutputFilePath()
                val soutOption = ":sout=#duplicate{dst=display,dst=std{access=file,mux=mp4,dst=$outputFilePath}}"
                media.addOption(soutOption)
            }

            mediaPlayer.media?.release()
            mediaPlayer.media = media
            mediaPlayer.play()

        } catch (e: Exception) {
            Toast.makeText(this, "Error playing stream: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration?) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)

        val visibility = if (isInPictureInPictureMode) android.view.View.GONE else android.view.View.VISIBLE
        findViewById<Button>(R.id.playButton).visibility = visibility
        findViewById<Button>(R.id.recordButton).visibility = visibility
        findViewById<Button>(R.id.pipButton).visibility = visibility
        urlInput.visibility = visibility
    }

    override fun onStop() {
        super.onStop()
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.detachViews()
        mediaPlayer.release()
        libVLC.release()
    }
}
