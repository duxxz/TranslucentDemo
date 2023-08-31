package com.demo.translucent

import android.app.Application
import com.opensource.svgaplayer.SVGAParser

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        SVGAParser.shareParser().init(this)
    }
}