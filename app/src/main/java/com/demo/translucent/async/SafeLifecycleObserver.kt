package com.demo.translucent.async

import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

abstract class SafeLifecycleObserver : LifecycleEventObserver {

    @CallSuper
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (source.lifecycle.currentState == Lifecycle.State.DESTROYED) {
            source.lifecycle.removeObserver(this)
        }
        onSafeStateChanged(source, event)
    }

    abstract fun onSafeStateChanged(source: LifecycleOwner, event: Lifecycle.Event)

}