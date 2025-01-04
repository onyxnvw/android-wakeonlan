package de.onyxnvw.wakeonlan.data

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import de.onyxnvw.wakeonlan.R

enum class ConnectionState(@StringRes val desc: Int, @ColorRes val color: Int) {
    UNKNOWN(desc = R.string.conn_state_unknown, color = R.color.conn_state_unknown),
    PENDING(desc = R.string.conn_state_pending, color = R.color.conn_state_pending),
    DISCONNECTED(desc = R.string.conn_state_disconnected, color = R.color.conn_state_disconnected),
    CONNECTED(desc = R.string.conn_state_connected, color = R.color.conn_state_connected)
}
