package com.demo.translucent

import android.content.Intent
import android.graphics.PointF
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.demo.translucent.utils.createAnimViewBitmap
import com.demo.translucent.widget.AssetsSVGAImageView

class MainActivity : AppCompatActivity() {

    private val btnJump by lazy { findViewById<View>(R.id.btn_jump) }
    private val btnCapture by lazy { findViewById<View>(R.id.btn_capture) }
    private val asiMeet by lazy { findViewById<AssetsSVGAImageView>(R.id.asi_meet) }

    private val ivPreview by lazy { findViewById<ImageView>(R.id.iv_preview) }

    private val flagLoc by lazy { IntArray(2) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_main)

        asiMeet.openAssets("meeting_night.svga")
//        asiMeet.setImageResource(R.drawable.ic_launcher_background)

        btnJump.setOnClickListener {
            it.getLocationOnScreen(flagLoc)
            val cx = flagLoc[0] + it.width / 2f
            val cy = flagLoc[1] + it.height / 2f

            val intent = Intent(this, SecondActivity::class.java)
            intent.putExtra(SecondActivity.EXTRAS_ZOOM, PointF(cx, cy))
            startActivity(intent)
        }

        btnCapture.setOnClickListener {
            val targetView = window?.decorView ?: return@setOnClickListener
            targetView.createAnimViewBitmap()?.let { img ->
                ivPreview.setImageBitmap(img)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        Log.e("MainActivity", "onStop")
    }

    override fun onPause() {
        super.onPause()

        Log.e("MainActivity", "onPause")
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.e("MainActivity", "onDestroy")
    }

    override fun onStart() {
        super.onStart()

        Log.e("MainActivity", "onStart")
    }

    override fun onResume() {
        super.onResume()

        Log.e("MainActivity", "onResume")
    }
}