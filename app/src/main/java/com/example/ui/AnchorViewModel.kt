package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Memory
import com.example.data.repository.MemoryRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

enum class MemoryFilter {
    ALL,
    ACTIVE,
    COMPLETED
}

data class CaptureUiState(
    val isListening: Boolean = false,
    val listeningTranscript: String = "",
    val ripplePulse: Float = 1f,
    val showCaughtStamp: Boolean = false,
    val isQuickInputExpanded: Boolean = false,
    val inputText: String = "",
    val selectedCategory: String = "🎓 College",
    val selectedDeadline: String = "Monday"
)

class AnchorViewModel(
    private val repository: MemoryRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(MemoryFilter.ALL)
    val filter: StateFlow<MemoryFilter> = _filter.asStateFlow()

    private val _captureState = MutableStateFlow(CaptureUiState())
    val captureState: StateFlow<CaptureUiState> = _captureState.asStateFlow()

    val memories: StateFlow<List<Memory>> = combine(
        repository.allMemories,
        _filter
    ) { all, currentFilter ->
        when (currentFilter) {
            MemoryFilter.ALL -> all
            MemoryFilter.ACTIVE -> all.filter { !it.isCompleted }
            MemoryFilter.COMPLETED -> all.filter { it.isCompleted }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        seedInitialMemoriesIfEmpty()
    }

    private fun seedInitialMemoriesIfEmpty() {
        viewModelScope.launch {
            if (repository.getCount() == 0) {
                repository.insert(
                    Memory(
                        title = "DBMS Record",
                        text = "Submit before Monday",
                        deadline = "Monday",
                        category = "🎓 College",
                        isCompleted = false
                    )
                )
                repository.insert(
                    Memory(
                        title = "Bring project file",
                        text = "Tomorrow for Software Engineering lab",
                        deadline = "Tomorrow",
                        category = "🎓 College",
                        isCompleted = false
                    )
                )
                repository.insert(
                    Memory(
                        title = "Lab manual sign",
                        text = "Get signature from HOD before 4 PM",
                        deadline = "Today",
                        category = "⚡ Urgent",
                        isCompleted = true
                    )
                )
            }
        }
    }

    fun setFilter(newFilter: MemoryFilter) {
        _filter.value = newFilter
    }

    fun updateInputText(text: String) {
        _captureState.value = _captureState.value.copy(inputText = text)
    }

    fun setCategory(category: String) {
        _captureState.value = _captureState.value.copy(selectedCategory = category)
    }

    fun setDeadline(deadline: String) {
        _captureState.value = _captureState.value.copy(selectedDeadline = deadline)
    }

    fun setQuickInputExpanded(expanded: Boolean) {
        _captureState.value = _captureState.value.copy(isQuickInputExpanded = expanded)
    }

    fun startListening() {
        _captureState.value = _captureState.value.copy(
            isListening = true,
            listeningTranscript = ""
        )
    }

    fun updateSpeechTranscript(partial: String) {
        _captureState.value = _captureState.value.copy(
            listeningTranscript = partial
        )
    }

    fun stopListeningAndIntercept(transcript: String? = null) {
        val finalSpeech = transcript ?: _captureState.value.listeningTranscript
        _captureState.value = _captureState.value.copy(
            isListening = false,
            listeningTranscript = ""
        )
        if (finalSpeech.isNotBlank()) {
            interceptRawThought(finalSpeech)
        }
    }

    fun interceptRawThought(
        rawInput: String,
        explicitCategory: String? = null,
        explicitDeadline: String? = null
    ) {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return

        val parsed = parseThought(trimmed)
        val finalCategory = explicitCategory ?: parsed.category
        val finalDeadline = explicitDeadline ?: parsed.deadline

        viewModelScope.launch {
            repository.insert(
                Memory(
                    title = parsed.title,
                    text = trimmed,
                    deadline = finalDeadline,
                    category = finalCategory,
                    isCompleted = false
                )
            )

            // Trigger "CAUGHT!" comic animation & clear input
            _captureState.value = _captureState.value.copy(
                showCaughtStamp = true,
                inputText = "",
                isQuickInputExpanded = false
            )
        }
    }

    fun dismissCaughtStamp() {
        _captureState.value = _captureState.value.copy(showCaughtStamp = false)
    }

    fun toggleComplete(memory: Memory) {
        viewModelScope.launch {
            repository.update(memory.copy(isCompleted = !memory.isCompleted))
        }
    }

    fun deleteMemory(memory: Memory) {
        viewModelScope.launch {
            repository.delete(memory)
        }
    }

    /**
     * Smart lightweight parsing for thought interception:
     * Extracts title, inferred deadline, and category without heavy backend overhead.
     */
    private data class ParsedResult(
        val title: String,
        val deadline: String?,
        val category: String
    )

    private fun parseThought(raw: String): ParsedResult {
        val lower = raw.lowercase(Locale.ROOT)

        // Detect deadline
        val deadline = when {
            lower.contains("before monday") || lower.contains("on monday") || lower.contains("by monday") || lower.contains("monday") -> "Monday"
            lower.contains("before tuesday") || lower.contains("on tuesday") || lower.contains("by tuesday") || lower.contains("tuesday") -> "Tuesday"
            lower.contains("before wednesday") || lower.contains("on wednesday") || lower.contains("wednesday") -> "Wednesday"
            lower.contains("before thursday") || lower.contains("on thursday") || lower.contains("thursday") -> "Thursday"
            lower.contains("before friday") || lower.contains("on friday") || lower.contains("friday") -> "Friday"
            lower.contains("before saturday") || lower.contains("saturday") -> "Saturday"
            lower.contains("sunday") -> "Sunday"
            lower.contains("tomorrow") -> "Tomorrow"
            lower.contains("tonight") -> "Tonight"
            lower.contains("today") -> "Today"
            lower.contains("next week") -> "Next Week"
            lower.contains("urgent") || lower.contains("asap") -> "ASAP"
            else -> null
        }

        // Detect category
        val category = when {
            lower.contains("urgent") || lower.contains("emergency") || lower.contains("asap") || lower.contains("important") -> "⚡ Urgent"
            lower.contains("dbms") || lower.contains("lab") || lower.contains("exam") || lower.contains("record") ||
                    lower.contains("professor") || lower.contains("assignment") || lower.contains("college") ||
                    lower.contains("class") || lower.contains("hod") || lower.contains("lecture") -> "🎓 College"
            lower.contains("buy") || lower.contains("call") || lower.contains("mom") || lower.contains("dad") ||
                    lower.contains("gym") || lower.contains("room") || lower.contains("rent") -> "📦 Personal"
            lower.contains("idea") || lower.contains("project") || lower.contains("app") -> "💡 Idea"
            else -> "🎓 College"
        }

        // Extract a punchy short title
        var title = raw
        // Strip common prefixes
        val prefixes = listOf("submit the ", "submit ", "bring ", "remember to ", "don't forget to ", "dont forget to ", "pick up ", "ask ", "tell ")
        for (prefix in prefixes) {
            if (lower.startsWith(prefix)) {
                title = raw.substring(prefix.length).trim()
                break
            }
        }

        // Take up to first 4 words or 35 characters
        val words = title.split(" ")
        val shortTitle = if (words.size > 4) {
            words.take(4).joinToString(" ")
        } else {
            title
        }

        val capitalizedTitle = shortTitle.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
        }

        return ParsedResult(
            title = capitalizedTitle.take(35),
            deadline = deadline,
            category = category
        )
    }
}

class AnchorViewModelFactory(
    private val repository: MemoryRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnchorViewModel::class.java)) {
            return AnchorViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
