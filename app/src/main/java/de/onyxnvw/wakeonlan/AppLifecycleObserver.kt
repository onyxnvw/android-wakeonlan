package de.onyxnvw.wakeonlan

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class AppLifecycleObserver(
    private val onForeground: () -> Unit,
    private val onBackground: () -> Unit
) : DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        onForeground()
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        onBackground()
    }
}