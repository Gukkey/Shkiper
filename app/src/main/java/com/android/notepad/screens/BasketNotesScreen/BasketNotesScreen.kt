package com.android.notepad.screens.BasketNotesScreen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.android.notepad.R
import com.android.notepad.ui.components.cards.NoteCard
import com.android.notepad.ui.components.layouts.CustomTopAppBar
import com.android.notepad.ui.components.layouts.LazyGridNotes
import com.android.notepad.ui.components.layouts.ScreenContentIfNoData
import com.android.notepad.ui.components.layouts.TopAppBarItem
import com.android.notepad.ui.components.modals.ActionDialog
import com.android.notepad.ui.components.modals.CreateReminderDialog
import com.android.notepad.ui.components.modals.ReminderDialogProperties
import com.android.notepad.ui.theme.CustomAppTheme
import com.android.notepad.viewModels.NotesViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BasketNotesScreen(navController: NavController, basketViewModel: NotesViewModel = hiltViewModel()) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route ?: ""

    val lazyGridNotes = rememberLazyStaggeredGridState()

    val actionBarHeight = 56.dp
    val actionBarHeightPx = with(LocalDensity.current) { actionBarHeight.roundToPx().toFloat() }
    val offsetX = remember { Animatable(-actionBarHeightPx) }

    /**
     * LaunchedEffect for cases when the number of selected notes changes.
     */
    LaunchedEffect(basketViewModel.screenState.value.selectedNotes) {
        if (basketViewModel.screenState.value.selectedNotes.isEmpty()) {
            offsetX.animateTo(
                targetValue = -actionBarHeightPx, animationSpec = tween(durationMillis = 200)
            )
        } else {
            offsetX.animateTo(
                targetValue = 0f, animationSpec = tween(durationMillis = 200)
            )
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (basketViewModel.screenState.value.isNotesInitialized && basketViewModel.screenState.value.notes.isEmpty())
            ScreenContentIfNoData(R.string.BasketNotesPageHeader, Icons.Outlined.DeleteSweep)
        else
            ScreenContent(lazyGridNotes, basketViewModel, currentRoute, navController)
        Box(modifier = Modifier) {
            ActionBar(actionBarHeight, offsetX, basketViewModel)
        }
    }

    if (basketViewModel.screenState.value.isDeleteNotesDialogShow)
        ActionDialog(
            title = stringResource(R.string.DeleteSelectedNotesDialogText),
            icon = Icons.Outlined.Warning,
            confirmText = stringResource(R.string.Confirm),
            onConfirm = basketViewModel::deleteSelectedNotes,
            goBackText = stringResource(R.string.Cancel),
            onGoBack = basketViewModel::switchDeleteDialogShow
        )
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScreenContent(
    lazyGridNotes: LazyStaggeredGridState,
    notesViewModel: NotesViewModel,
    currentRoute: String,
    navController: NavController
) {
    LazyGridNotes(
        contentPadding = PaddingValues(10.dp, 10.dp, 10.dp, 80.dp),
        modifier = Modifier.fillMaxSize(),
        gridState = lazyGridNotes
    ) {
        item(span = StaggeredGridItemSpan.FullLine) {
            Column(
                modifier = Modifier.height(56.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.BasketPageHeader),
                    color = CustomAppTheme.colors.textSecondary,
                    style = MaterialTheme.typography.body1.copy(fontSize = 17.sp),
                    modifier = Modifier.padding(horizontal = 10.dp).basicMarquee(),
                    maxLines = 1
                )
            }
        }
        items(items = notesViewModel.screenState.value.notes) { item ->
            NoteCard(item.header,
                item.body,
                reminder = notesViewModel.screenState.value.reminders.find { it.noteId == item._id },
                markedText = notesViewModel.screenState.value.searchText,
                selected = item._id in notesViewModel.screenState.value.selectedNotes,
                onClick = { notesViewModel.clickOnNote(item, currentRoute, navController) },
                onLongClick = { notesViewModel.toggleSelectedNoteCard(item._id) })
        }
    }
}

@Composable
private fun ActionBar(
    actionBarHeight: Dp, offsetX: Animatable<Float, AnimationVector1D>, notesViewModel: NotesViewModel
) {
    val topAppBarElevation = if (offsetX.value.roundToInt() < -actionBarHeight.value.roundToInt()) 0.dp else 2.dp
    Box(
        modifier = Modifier.height(actionBarHeight).offset { IntOffset(x = 0, y = offsetX.value.roundToInt()) },
    ) {
        CustomTopAppBar(
            modifier = Modifier.fillMaxWidth(),
            elevation = topAppBarElevation,
            text = notesViewModel.screenState.value.selectedNotes.count().toString(),
            navigation = TopAppBarItem(
                isActive = false,
                icon = Icons.Default.Close,
                iconDescription = R.string.GoBack,
                onClick = notesViewModel::clearSelectedNote
            ),
            items = listOf(
                TopAppBarItem(
                    isActive = false,
                    icon = Icons.Outlined.History,
                    iconDescription = R.string.Restore,
                    onClick = notesViewModel::removeSelectedNotesFromBasket
                ),
                TopAppBarItem(
                    isActive = false,
                    icon = Icons.Outlined.Delete,
                    iconDescription = R.string.Delete,
                    onClick = notesViewModel::switchDeleteDialogShow
                ),
            )
        )
    }
}
