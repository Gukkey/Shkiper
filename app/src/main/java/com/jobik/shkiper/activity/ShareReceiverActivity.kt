package com.jobik.shkiper.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.jobik.shkiper.database.data.note.NoteMongoRepository
import com.jobik.shkiper.database.models.Note
import com.jobik.shkiper.helpers.IntentHelper
import com.jobik.shkiper.ui.theme.CustomTheme
import com.jobik.shkiper.ui.theme.CustomThemeStyle
import com.jobik.shkiper.ui.theme.ShkiperTheme
import com.jobik.shkiper.util.ThemeUtil
import com.jobik.shkiper.widgets.screens.NoteSelectionScreen.NoteSelectionScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.EmptyCoroutineContext

@AndroidEntryPoint
class ShareReceiverActivity : AppCompatActivity() {

    @Inject
    lateinit var repository: NoteMongoRepository

    private val result = Intent()

    private var receivedText: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupActivity()
        ThemeUtil.restoreSavedTheme(this)
        setContent {
            ShkiperTheme(
                darkTheme = ThemeUtil.isDarkMode.value ?: isSystemInDarkTheme(),
                style = ThemeUtil.themeStyle.value ?: CustomThemeStyle.DarkPurple
            ) {
                Box(
                    Modifier.fillMaxSize().background(CustomTheme.colors.mainBackground)
                ) {
                    NoteSelectionScreen {
                        handleSelectNote(it)
                    }
                }
            }
        }
    }

    private fun setupActivity() {
        setResult(RESULT_CANCELED, result)
        val intent = intent
        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_SEND == action && type != null) {
            if ("text/plain" == type) {
                receivedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            }
        }
    }

    private fun handleSelectNote(note: Note) {
        setResult(RESULT_OK, result)
        finish()
        updateNoteBody(note)
    }

    private fun updateNoteBody(note: Note) {
        val context = this
        CoroutineScope(EmptyCoroutineContext).launch {
            receivedText?.let { text ->
                repository.updateNote(id = note._id) {
                    it.body = newBodyForNote(it.body, text)
                }
            }
            IntentHelper().StartActivityAndOpenNote(context, note._id.toHexString())
        }
    }

    @Keep
    private fun newBodyForNote(body: String, addedText: String): String {
        return "$body \n$addedText"
    }
}