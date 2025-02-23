package com.jobik.shkiper.screens.calendar

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jobik.shkiper.database.data.note.NoteMongoRepository
import com.jobik.shkiper.database.data.reminder.ReminderMongoRepository
import com.jobik.shkiper.database.models.Note
import com.jobik.shkiper.database.models.NotePosition
import com.jobik.shkiper.database.models.Reminder
import com.jobik.shkiper.database.models.RepeatMode
import com.jobik.shkiper.helpers.DateHelper
import com.jobik.shkiper.helpers.DateHelper.Companion.isLocalDateInRange
import com.jobik.shkiper.helpers.DateHelper.Companion.sortReminders
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

data class CalendarScreenState(
    val isNotesInitialized: Boolean = false,
    val notes: List<Note> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
    val targetReminders: List<Reminder> = emptyList(),
    val hashtags: Set<String> = emptySet(),
    val currentHashtag: String? = null,
    val selectedDateRange: Pair<LocalDate, LocalDate> = Pair(LocalDate.now(), LocalDate.now()),
    val fullScreenCalendarOpen: Boolean = false,
    val datesWithIndicator: Set<LocalDate> = emptySet()
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val noteRepository: NoteMongoRepository,
    private val reminderRepository: ReminderMongoRepository,
    private val application: Application,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _screenState = mutableStateOf(CalendarScreenState())
    val screenState: State<CalendarScreenState> = _screenState

    /*******************
     * Notes region
     *******************/

    private var notesFlowJob: Job? = null
    private var remindersFlowJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            getReminders()
            getTargetReminders()
        }
    }

    private fun getReminders() {
        viewModelScope.launch {
            reminderRepository.getReminders().collect() {
                _screenState.value = screenState.value.copy(reminders = it)
                getDatesWithIndicator(it)
            }
        }
    }

    private fun getDatesWithIndicator(reminders: List<Reminder>) {
        val currentDate = LocalDate.now()
        val datesWithIndicator =
            reminders.filter { it.repeat == RepeatMode.NONE && !it.date.isBefore(currentDate) }
                .map { it.date }
        _screenState.value = screenState.value.copy(datesWithIndicator = datesWithIndicator.toSet())
    }

    private fun getTargetReminders() {
        remindersFlowJob?.cancel()

        remindersFlowJob = viewModelScope.launch {
            reminderRepository.getReminders().collect() {
                setFilteredEvents(it)
                getNotes()
            }
        }
    }

    private fun setFilteredEvents(it: List<Reminder>) {
        val targetEvents =
            it.filter {
                val targetReminderDate = DateHelper.nextDateWithRepeating(
                    notificationDate = LocalDateTime.of(it.date, it.time),
                    repeatMode = it.repeat,
                    startingPoint = LocalDateTime.of(
                        screenState.value.selectedDateRange.first,
                        LocalTime.MIN
                    )
                ).toLocalDate()
                isLocalDateInRange(
                    date = targetReminderDate,
                    range = _screenState.value.selectedDateRange
                )
            }

        val sortedReminders = sortReminders(
            reminders = targetEvents,
            pointDate = LocalDateTime.of(screenState.value.selectedDateRange.first, LocalTime.now())
        )

        _screenState.value = screenState.value.copy(targetReminders = sortedReminders)
    }

    private fun getNotes() {
        notesFlowJob?.cancel()

        notesFlowJob = viewModelScope.launch {
            noteRepository.getNotesFlow().collect() { notesList ->
                setFilteredNotes(notesList)
            }
        }
    }

    private fun setFilteredNotes(allNotes: List<Note>) {
        var selectedNotes = selectNotesByEvents(allNotes)
        getHashtags(selectedNotes)
        selectedNotes = selectNotesByTags(selectedNotes)
        removeSelectedTagIfEmpty(selectedNotes)

        _screenState.value = _screenState.value.copy(notes = selectedNotes, isNotesInitialized = true)
    }

    private fun removeSelectedTagIfEmpty(selectedNotes: List<Note>) {
        if (selectedNotes.isEmpty() && screenState.value.currentHashtag != null) setCurrentHashtag(
            null
        )
    }

    private fun selectNotesByTags(selectedNotes: List<Note>): List<Note> {
        if (screenState.value.currentHashtag != null) {
            return selectedNotes.filter { note ->
                note.hashtags.any { it == screenState.value.currentHashtag }
            }
        }
        return selectedNotes
    }

    private fun selectNotesByEvents(allNotes: List<Note>): List<Note> {
        val selectedNotes = screenState.value.targetReminders.associateBy { it.noteId }.keys
            .mapNotNull { reminderId ->
                allNotes.find {
                    it._id == reminderId && it.position != NotePosition.DELETE
                }
            }

        return selectedNotes
    }

    fun selectDate(date: LocalDate) {
        _screenState.value = _screenState.value.copy(selectedDateRange = Pair(date, date))
        getTargetReminders()
    }

    fun selectNextDate(date: LocalDate) {
        val newDateRange = if (date.isAfter(screenState.value.selectedDateRange.first)) Pair(
            screenState.value.selectedDateRange.first,
            date
        ) else Pair(
            date,
            screenState.value.selectedDateRange.first
        )
        _screenState.value = _screenState.value.copy(selectedDateRange = newDateRange)
        getTargetReminders()
    }

    /*******************
     * Hashtag region
     *******************/

    private fun getHashtags(notes: List<Note>) {
        val hashtags = notes.flatMap { it.hashtags.toSet() }.toSet()
        _screenState.value = screenState.value.copy(hashtags = hashtags)
    }

    fun setCurrentHashtag(newHashtag: String?) {
        _screenState.value = screenState.value.copy(
            currentHashtag =
            if (newHashtag == screenState.value.currentHashtag) null else newHashtag
        )
        getNotes()
    }

    fun switchFullScreenCalendarOpen() {
        _screenState.value = screenState.value.copy(
            fullScreenCalendarOpen = screenState.value.fullScreenCalendarOpen.not()
        )
    }
}