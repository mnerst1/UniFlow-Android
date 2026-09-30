package dev.uniflow.app

import android.app.Application
import dev.uniflow.app.data.*

class UniFlowApplication : Application() {
    val database by lazy { UniDatabase.create(this) }
    val preferences by lazy { Preferences(this) }
    val repository by lazy { Repository(database, preferences) }
}
