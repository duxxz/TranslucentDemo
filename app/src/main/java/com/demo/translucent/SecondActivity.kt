package com.demo.translucent

import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.demo.translucent.utils.matchParent
import com.demo.translucent.widget.CircleExplodeLayout

class SecondActivity : AppCompatActivity() {

    companion object {
        const val EXTRAS_ZOOM = "extras_zoom"
    }

    private val zoomInfo by lazy { intent?.getParcelableExtra<PointF>(EXTRAS_ZOOM) }

    private var explodeView: CircleExplodeLayout? = null

    private val btnBack by lazy { findViewById<View>(R.id.btn_back) }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (zoomInfo != null) {
            intent?.removeExtra(EXTRAS_ZOOM)
            setTheme(R.style.NormalTranslucentAct)

            overridePendingTransition(0, 0)
        }
        super.onCreate(savedInstanceState)

        if (zoomInfo != null) {
            explodeView = CircleExplodeLayout(this).apply {
                layoutParams = ViewGroup.LayoutParams(matchParent, matchParent)
            }
            LayoutInflater.from(this).inflate(R.layout.layout_second, explodeView, true)

            setContentView(explodeView)
        } else {
            setContentView(R.layout.layout_second)
        }

        if (zoomInfo != null) {
            explodeView?.startLoad(zoomInfo)
        }

        btnBack.setOnClickListener {
            onBackPressed()
        }
    }

    override fun finish() {
        super.finish()
        if (zoomInfo != null) {
            overridePendingTransition(0, R.anim.alpha_out)
        }
    }
}