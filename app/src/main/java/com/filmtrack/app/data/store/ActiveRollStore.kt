package com.filmtrack.app.data.store

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActiveRollStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("filmtrack_prefs", Context.MODE_PRIVATE)

    private val _activeRollId = MutableStateFlow(prefs.getLong("active_roll_id", -1L))
    val activeRollIdFlow: StateFlow<Long> = _activeRollId.asStateFlow()

    val activeRollId: Long get() = _activeRollId.value

    fun setActiveRoll(rollId: Long) {
        prefs.edit().putLong("active_roll_id", rollId).apply()
        _activeRollId.value = rollId
    }
}
