package dev.uniflow.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.uniflow.app.data.*
import dev.uniflow.app.domain.validate
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class StudyState(val courses: List<Course> = emptyList(), val records: List<StudyRecord> = emptyList(), val settings: Settings = Settings(), val loading: Boolean = true)
class StudyViewModel(private val repo: Repository) : ViewModel() {
    private val ready = MutableStateFlow(false)
    val errors = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val state = combine(repo.courses, repo.records, repo.preferences.settings, ready) { c, r, s, loaded -> StudyState(c, r, s, !loaded) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StudyState())
    init { launch { repo.seedIfNeeded(); ready.value = true } }
    private fun launch(block: suspend () -> Unit) { viewModelScope.launch { try { block() } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; errors.emit(Unit) } } }
    fun save(value: Course) { launch { require(value.name.isNotBlank() && value.credits in 1..30); repo.save(value) } }
    fun save(value: StudyRecord) { launch { require(validate(value) == null); repo.save(value) } }
    fun delete(value: Course) { launch { repo.delete(value) } }
    fun delete(value: StudyRecord) { launch { repo.delete(value) } }
    fun settings(value: Settings) { launch { repo.preferences.save(value) } }
    fun reset() { launch { repo.resetDemo() } }
    companion object {
        fun factory(repo: Repository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = StudyViewModel(repo) as T
        }
    }
}
