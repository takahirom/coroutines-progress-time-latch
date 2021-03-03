package com.github.takahirom.coroutine.progress.time.latch

import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.delay
import kotlin.properties.Delegates

class CoroutinesProgressTimeLatch(
    private val lifecycleScope: LifecycleCoroutineScope,
    private val delayMs: Long = 750,
    private val minShowTime: Long = 500,
    private val currentTimeProvider: (() -> Long) = { System.currentTimeMillis() },
    private val viewRefreshingToggle: (viewRefreshing: Boolean) -> Unit
) {
    private sealed class State(val showProgress: Boolean) {
        object NotRefreshNoProgress : State(false)
        object RefreshNoProgress : State(false)
        class RefreshProgress(val startTime: Long) : State(true)
        object NotRefreshProgress : State(true)
    }

    private var visibility: Boolean? by Delegates.observable(null) { property, oldValue, newValue ->
        if (oldValue != newValue && newValue != null) {
            viewRefreshingToggle(newValue)
        }
    }


    private var state: State = State.NotRefreshNoProgress
        set(value) {
            visibility = value.showProgress
            field = value
        }

    fun refresh(newRefreshing: Boolean) {
        lifecycleScope.launchWhenStarted {
            refreshLogic(newRefreshing)
        }
    }

    @MainThread
    @VisibleForTesting
    internal suspend fun refreshLogic(newRefreshing: Boolean) {
        when (val localState = state) {
            State.NotRefreshNoProgress -> {
                if (newRefreshing) {
                    state = State.RefreshNoProgress
                    delay(delayMs)
                    if (state == State.RefreshNoProgress) {
                        state = State.RefreshProgress(currentTimeProvider())
                    }
                }
            }
            State.RefreshNoProgress -> {
                if (newRefreshing) {
                    // do nothing
                } else {
                    state = State.NotRefreshNoProgress
                }
            }
            is State.RefreshProgress -> {
                if (newRefreshing) {
                    // do nothing
                } else {
                    val shouldWaitTime =
                        minShowTime - (currentTimeProvider() - localState.startTime)
                    val shouldWait = shouldWaitTime > 0
                    state = State.NotRefreshProgress
                    if (shouldWait) {
                        delay(shouldWaitTime)
                    }
                    if (state == State.NotRefreshProgress) {
                        state = State.NotRefreshNoProgress
                    }
                }
            }
            State.NotRefreshProgress -> {
                if (newRefreshing) {
                    state = State.RefreshProgress(currentTimeProvider())
                }
            }
        }
    }
}

