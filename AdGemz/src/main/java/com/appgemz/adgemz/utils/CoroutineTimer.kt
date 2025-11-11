package com.appgemz.adgemz.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CoroutineTimer(
    private val durationInMs: Int = 0, // Max duration
    private val intervalInMillis: Long = 1000L, // Interval time (1 sec)
    private val onTick: (Int) -> Unit, // Trigger every second
    private val onComplete: () -> Unit // Trigger when completed
) {
    private var job: Job? = null
    private var elapsedMs = 0

    /**
     * Start the coroutine timer
     */
    fun start() {
        job = CoroutineScope(Dispatchers.Main).launch {
            while (elapsedMs < durationInMs) {
                delay(intervalInMillis)
                elapsedMs+=1000
                onTick(elapsedMs) // Callback every second
            }
            onComplete() // Trigger on complete (8 sec reached)
        }
    }

    /**
     * Cancel the coroutine timer anytime before completion
     */
    fun cancel() {
        job?.cancel()
        job = null
        elapsedMs = 0
    }
}