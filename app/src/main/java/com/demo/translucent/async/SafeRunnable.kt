package com.demo.translucent.async

import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

class SafeRunnable(lifecycleOwner: LifecycleOwner, private var runnable: Runnable? = null) : Runnable {

    constructor(lifecycleOwner: LifecycleOwner, runnable: () -> Unit) : this(lifecycleOwner, Runnable(runnable))

    init {
        lifecycleOwner.lifecycle.addObserver(object : SafeLifecycleObserver() {
            override fun onSafeStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (source.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                    runnable = null
                }
            }
        })
    }

    @CallSuper
    override fun run() {
        runnable?.run()
    }

}