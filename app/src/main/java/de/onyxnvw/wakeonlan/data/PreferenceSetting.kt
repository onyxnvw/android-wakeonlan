package de.onyxnvw.wakeonlan.data

import androidx.datastore.preferences.core.Preferences

data class PreferenceSetting(
    val key: Preferences.Key<String>,
    val name: String,
    var value: String
)
