package com.demo.translucent.async

import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

class SafeTask<T>(lifecycleOwner: LifecycleOwner, private var task: Task<T>? = null) : Task<T> {

    constructor(lifecycleOwner: LifecycleOwner, task: ((T) -> Unit)) : this(lifecycleOwner, Task(task))

    init {
        lifecycleOwner.lifecycle.addObserver(object : SafeLifecycleObserver() {
            override fun onSafeStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (source.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                    task = null
                }
            }
        })
    }

    @CallSuper

    override fun run(param: T) {
        task?.run(param)
    }
}