package de.onyxnvw.wakeonlan.data

import androidx.annotation.StringRes
import de.onyxnvw.wakeonlan.R


enum class WakeUpResult(@StringRes val desc: Int) {
    SUCCESS(desc = R.string.wake_up_success),
    FAILURE(desc = R.string.wake_up_failure),
    WIFI_DISCONNECTED(desc = R.string.wake_up_wifi_disconnected),
    SUBNET_MISMATCH(desc = R.string.wake_up_subnet_mismatch)
}