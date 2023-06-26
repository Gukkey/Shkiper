package com.example.notepadapp.screens.NoteScreen

import android.annotation.SuppressLint
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.notepadapp.navigation.AppScreens
import com.example.notepadapp.ui.components.buttons.RoundedButton
import com.example.notepadapp.ui.components.fields.CustomTextField
import com.example.notepadapp.ui.components.fields.HashtagEditor
import com.example.notepadapp.ui.components.modals.CreateReminderDialog
import com.example.notepadapp.ui.components.modals.ReminderDialogProperties
import com.example.notepadapp.ui.theme.CustomAppTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun NoteScreen(navController: NavController, noteViewModel: NoteViewModel = hiltViewModel()) {
    LaunchedEffect(Unit) {
        if (noteViewModel.note == null) navController.popBackStack()
    }
    val scrollState = rememberScrollState()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route ?: ""
    LaunchedEffect(currentRoute) {
        if (currentRoute.substringBefore("/") != AppScreens.Note.route.substringBefore("/")) {
            noteViewModel.setTopAppBarHover(false)
            noteViewModel.setBottomAppBarHover(false)
        }
    }
    val bodyFieldFocusRequester = remember { FocusRequester() }

    Scaffold(
        backgroundColor = CustomAppTheme.colors.mainBackground,
        topBar = { NoteScreenHeader(navController, noteViewModel) },
        bottomBar = { NoteScreenFooter(navController, noteViewModel) },
        modifier = Modifier.imePadding().navigationBarsPadding().fillMaxSize(),
    ) { contentPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(scrollState)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() } // This is mandatory
                ) {
                    bodyFieldFocusRequester.requestFocus()
                }

        ) {
            CustomTextField(
                text = noteViewModel.noteHeader.value,
                onTextChange = { noteViewModel.updateNoteHeader(it) },
                placeholder = "Header",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onAny = {
                        bodyFieldFocusRequester.requestFocus()
                    }
                ),
                textStyle = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.SemiBold, fontSize = 21.sp),
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(bottom = 6.dp, top = 4.dp)
            )
            CustomTextField(
                text = noteViewModel.noteBody.value,
                onTextChange = { noteViewModel.updateNoteBody(it) },
                placeholder = "Text",
                textStyle = MaterialTheme.typography.body1,
                modifier = Modifier.padding(horizontal = 20.dp)
                    .focusRequester(bodyFieldFocusRequester)
            )
            Spacer(Modifier.height(10.dp))
            HashtagEditor(
                Modifier.padding(horizontal = 20.dp),
                noteViewModel.noteHashtags.value,
                noteViewModel::changeNoteHashtags
            )
            Spacer(Modifier.height(25.dp))
        }
    }

    if (noteViewModel.isCreateReminderDialogShow.value) {
        val reminder = remember { noteViewModel.reminder.value }
        val reminderDialogProperties = remember {
            if (reminder != null) ReminderDialogProperties(reminder.date, reminder.time, reminder.repeat)
            else ReminderDialogProperties()
        }
        CreateReminderDialog(
            reminderDialogProperties = reminderDialogProperties,
            onGoBack = noteViewModel::switchReminderDialogShow,
            onDelete = if (reminder != null) noteViewModel::deleteReminder else null,
            onSave = noteViewModel::createReminder,
        )
    }

    LaunchedEffect(scrollState.value) {
        if (scrollState.canScrollBackward || scrollState.canScrollForward) {
            noteViewModel.setTopAppBarHover(scrollState.value > 0)
            noteViewModel.setBottomAppBarHover(scrollState.value < scrollState.maxValue)
        } else {
            noteViewModel.setTopAppBarHover(false)
            noteViewModel.setBottomAppBarHover(false)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            noteViewModel.saveChanges()
            noteViewModel.deleteNoteIfEmpty()
        }
    }
}

@Composable
private fun NoteScreenHeader(navController: NavController, noteViewModel: NoteViewModel) {
    val systemUiController = rememberSystemUiController()
    val backgroundColor by animateColorAsState(
        if (noteViewModel.isTopAppBarHover.value) CustomAppTheme.colors.secondaryBackground else CustomAppTheme.colors.mainBackground,
        animationSpec = tween(200),
    )

    SideEffect {
        systemUiController.setStatusBarColor(backgroundColor)
    }

    TopAppBar(
        elevation = if (noteViewModel.isTopAppBarHover.value) 8.dp else 0.dp,
        backgroundColor = backgroundColor,
        contentColor = CustomAppTheme.colors.textSecondary,
        title = { },
        navigationIcon = {
            Spacer(modifier = Modifier.padding(5.dp, 0.dp, 0.dp, 0.dp))
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.size(40.dp).clip(CircleShape).padding(0.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Go back",
                    tint = CustomAppTheme.colors.textSecondary,
                )
            }
        },
        actions = {
            IconButton(
                onClick = { noteViewModel.switchNotePinnedMode() },
                modifier = Modifier.size(40.dp).clip(CircleShape).padding(0.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.PushPin,
                    contentDescription = "Attach a note",
                    tint = if (noteViewModel.noteIsPinned.value) CustomAppTheme.colors.text else CustomAppTheme.colors.textSecondary,
                )
            }
            Spacer(modifier = Modifier.padding(6.dp, 0.dp, 0.dp, 0.dp))
            IconButton(
                onClick = { noteViewModel.switchReminderDialogShow() },
                modifier = Modifier.size(40.dp).clip(CircleShape).padding(0.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.NotificationAdd,
                    contentDescription = "Add to notification",
                    tint = if (noteViewModel.reminder.value == null) CustomAppTheme.colors.textSecondary else CustomAppTheme.colors.text,
                )
            }
            Spacer(modifier = Modifier.padding(6.dp, 0.dp, 0.dp, 0.dp))
            IconButton(
                onClick = { },
                modifier = Modifier.size(40.dp).clip(CircleShape).padding(0.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Archive,
                    contentDescription = "Add to archive",
                    tint = CustomAppTheme.colors.textSecondary,
                )
            }
            Spacer(modifier = Modifier.padding(6.dp, 0.dp, 0.dp, 0.dp))
        },
        modifier = Modifier.fillMaxWidth(),
    )
}


@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("CoroutineCreationDuringComposition")
@Composable
private fun NoteScreenFooter(navController: NavController, noteViewModel: NoteViewModel) {
    val systemUiController = rememberSystemUiController()
    val backgroundColor by animateColorAsState(
        if (noteViewModel.isBottomAppBarHover.value) CustomAppTheme.colors.secondaryBackground else CustomAppTheme.colors.mainBackground,
        animationSpec = tween(200),
    )

    SideEffect {
        systemUiController.setNavigationBarColor(backgroundColor)
    }

    BottomAppBar(
        elevation = if (noteViewModel.isBottomAppBarHover.value) 8.dp else 0.dp,
        backgroundColor = backgroundColor,
        contentColor = CustomAppTheme.colors.textSecondary,
        cutoutShape = CircleShape,
        modifier = Modifier.fillMaxWidth().height(50.dp),
    ) {
        Row(
            Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (noteViewModel.intermediateStates.value.size < 2) {
                Row {
                    Spacer(modifier = Modifier.padding(15.dp, 0.dp, 0.dp, 0.dp))
                    Text(
                        "Changed at ${getUpdatedTime(noteViewModel)}",
                        modifier = Modifier.basicMarquee(),
                        style = MaterialTheme.typography.body1.copy(fontSize = 15.sp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.padding(45.dp, 0.dp, 0.dp, 0.dp))
                Row {
                    IconButton(
                        onClick = { noteViewModel.noteStateGoBack() },
                        modifier = Modifier.size(40.dp).clip(CircleShape).padding(0.dp),
                        enabled = noteViewModel.currentStateIndex.value > 0
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Undo,
                            contentDescription = "Go back",
                            tint = if (noteViewModel.currentStateIndex.value > 0) CustomAppTheme.colors.text else CustomAppTheme.colors.textSecondary,
                        )
                    }
                    Spacer(modifier = Modifier.padding(6.dp, 0.dp, 0.dp, 0.dp))
                    IconButton(
                        onClick = { noteViewModel.noteStateGoNext() },
                        modifier = Modifier.size(40.dp).clip(CircleShape).padding(0.dp),
                        enabled = noteViewModel.currentStateIndex.value < noteViewModel.intermediateStates.value.size - 1
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Redo,
                            contentDescription = "Go forward",
                            tint = if (noteViewModel.currentStateIndex.value < noteViewModel.intermediateStates.value.size - 1) CustomAppTheme.colors.text else CustomAppTheme.colors.textSecondary,
                        )
                    }
                }
            }
            Row {
                IconButton(
                    onClick = { },
                    modifier = Modifier.size(40.dp).clip(CircleShape).padding(0.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Add to basket",
                        tint = CustomAppTheme.colors.textSecondary,
                    )
                }
                Spacer(modifier = Modifier.padding(6.dp, 0.dp, 0.dp, 0.dp))
            }
        }
    }
}


private fun getUpdatedTime(noteViewModel: NoteViewModel): String {
    val duration = Duration.between(noteViewModel.noteUpdatedDate.value, LocalDateTime.now())
    return if (duration.toDays() > 0)
        noteViewModel.noteUpdatedDate.value.format(DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm"))
    else
        noteViewModel.noteUpdatedDate.value.format(DateTimeFormatter.ofPattern("HH:mm"))
}