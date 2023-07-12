package com.android.notepad.navigation

import androidx.compose.animation.*
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.android.notepad.screens.NoteListScreen.NoteListScreen
import com.android.notepad.screens.NoteScreen.NoteScreen
import com.android.notepad.screens.OnboardingScreen.OnBoardingScreen
import com.android.notepad.screens.SettingsScreen.SettingsScreen
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable


@ExperimentalAnimationApi
@Composable
fun SetupAppScreenNavGraph(
    navController: NavHostController,
    startDestination: String
) {
    AnimatedNavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(
            route = AppScreens.NoteList.route,
            enterTransition = {
                when (initialState.destination.route) {
                    AppScreens.Note.route -> null
                    else -> slideInVertically(initialOffsetY = { -40 }) + fadeIn()
                }
            },
            exitTransition = {
                when (targetState.destination.route) {
                    AppScreens.Note.route -> null
                    else -> slideOutVertically(targetOffsetY = { 50 }) + fadeOut()
                }
            }
        ) {
            NoteListScreen(navController)
        }

        composable(
            route = AppScreens.Archive.route,
            enterTransition = {
                when (initialState.destination.route) {
                    AppScreens.Note.route -> null
                    else -> slideInVertically(initialOffsetY = { -40 }) + fadeIn()
                }
            },
            exitTransition = {
                when (targetState.destination.route) {
                    AppScreens.Note.route -> null
                    else -> slideOutVertically(targetOffsetY = { 50 }) + fadeOut()
                }
            }
        ) {

        }

        composable(
            route = AppScreens.Basket.route,
            enterTransition = {
                when (initialState.destination.route) {
                    AppScreens.Note.route -> null
                    else -> slideInVertically(initialOffsetY = { -40 }) + fadeIn()
                }
            },
            exitTransition = {
                when (targetState.destination.route) {
                    AppScreens.Note.route -> null
                    else -> slideOutVertically(targetOffsetY = { 50 }) + fadeOut()
                }
            }
        ) {

        }

        composable(
            route = AppScreens.Settings.route,
            enterTransition = {
                when (initialState.destination.route) {
                    AppScreens.Note.route -> null
                    else -> slideInVertically(initialOffsetY = { -40 }) + fadeIn()
                }
            },
            exitTransition = {
                when (targetState.destination.route) {
                    AppScreens.Note.route -> null
                    else -> slideOutVertically(targetOffsetY = { 50 }) + fadeOut()
                }
            }
        ) {
            SettingsScreen(navController)
        }

        composable(
            route = AppScreens.Note.route,
            arguments = listOf(navArgument(ARGUMENT_NOTE_ID) {
                type = NavType.StringType
            }),
            enterTransition = {
                fadeIn() + scaleIn(initialScale = 0.9f)
            },
            exitTransition = {
                fadeOut() + scaleOut(targetScale = 0.9f)
            }
        ) {
            NoteScreen(navController)
        }

        composable(
            route = AppScreens.Onboarding.route,
            enterTransition = { slideInVertically(initialOffsetY = { -40 }) + fadeIn() },
            exitTransition = { slideOutVertically(targetOffsetY = { 50 }) + fadeOut() }
        ) {
            OnBoardingScreen(navController)
        }
    }
}