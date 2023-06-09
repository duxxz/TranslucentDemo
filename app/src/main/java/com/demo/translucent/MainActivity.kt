package com.demo.translucent

import android.content.Intent
import android.graphics.PointF
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val btnJump by lazy { findViewById<View>(R.id.btn_jump) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_main)

        btnJump.setOnClickListener {
            val intent = Intent(this, SecondActivity::class.java)
            intent.putExtra(SecondActivity.EXTRAS_ZOOM, PointF(500f, 500f))
            startActivity(intent)
        }
    }
}