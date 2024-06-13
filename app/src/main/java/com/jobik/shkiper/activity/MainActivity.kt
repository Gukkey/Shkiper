package com.jobik.shkiper.activity

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.jobik.shkiper.NotepadApplication
import com.jobik.shkiper.SharedPreferencesKeys
import com.jobik.shkiper.SharedPreferencesKeys.OnboardingFinishedData
import com.jobik.shkiper.database.models.NotePosition
import com.jobik.shkiper.navigation.Route
import com.jobik.shkiper.screens.layout.AppLayout
import com.jobik.shkiper.services.billing.BillingService
import com.jobik.shkiper.services.inAppUpdates.InAppUpdatesService
import com.jobik.shkiper.services.localization.LocaleHelper
import com.jobik.shkiper.services.review.ReviewService
import com.jobik.shkiper.services.statistics.StatisticsService
import com.jobik.shkiper.ui.components.modals.OfferWriteReview
import com.jobik.shkiper.ui.theme.AppTheme
import com.jobik.shkiper.ui.theme.CustomThemeStyle
import com.jobik.shkiper.ui.theme.ShkiperTheme
import com.jobik.shkiper.util.ContextUtils.adjustFontSize
import com.jobik.shkiper.util.ThemeUtil
import com.jobik.shkiper.util.settings.SettingsManager
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate

@OptIn(ExperimentalAnimationApi::class)
class StartupActivity : MainActivity()

@ExperimentalAnimationApi
@AndroidEntryPoint
open class MainActivity : ComponentActivity() {
    private lateinit var billingClientLifecycle: BillingService
    private lateinit var inAppUpdatesService: InAppUpdatesService

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(
            LocaleHelper.setLocale(newBase, NotepadApplication.currentLanguage)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        installSplashScreen()
        super.onCreate(savedInstanceState)
        adjustFontSize(SettingsManager.settings.value?.fontScale)
        actionBar?.hide()

        // Billing APIs are all handled in the this lifecycle observer.
        billingClientLifecycle = (application as NotepadApplication).billingClientLifecycle
        lifecycle.addObserver(billingClientLifecycle)
        inAppUpdatesService = InAppUpdatesService(this)

        ThemeUtil.restoreSavedTheme(this)
        val startDestination = getStartDestination()
        val canShowOfferReview =
            mutableStateOf(ReviewService(applicationContext).needShowOfferReview())

        checkForUpdates()

        setContent {
            UpdateStatistics()
            ShkiperTheme(
                darkTheme = ThemeUtil.isDarkMode.value ?: isSystemInDarkTheme(),
                style = ThemeUtil.themeStyle.value ?: CustomThemeStyle.PastelPurple
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(AppTheme.colors.background)
                ) {
                    AppLayout(startDestination)
                }
                if (canShowOfferReview.value)
                    OfferWriteReview { canShowOfferReview.value = false }
            }
        }
    }

    @Composable
    private fun UpdateStatistics() {
        val context = LocalContext.current
        val isUpdated = rememberSaveable { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            if (isUpdated.value) return@LaunchedEffect
            isUpdated.value = true
            val statisticsService = StatisticsService(context)

            statisticsService.appStatistics.apply {
                fistOpenDate.increment()
                openAppCount.increment()
                if (LocalDate.now().isBefore(LocalDate.of(2024, 1, 1))) {
                    isPioneer.increment()
                }
            }
            statisticsService.saveStatistics()
        }
    }

    private val updateActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            // Handle the result of the update flow here.
            if (result.resultCode != Activity.RESULT_OK) {
                // If the update flow fails, you can retry it or notify the user.
                // Note: In a flexible update, the user has the option to postpone the update.
            }
        }

    private fun checkForUpdates() {
        if (InAppUpdatesService.isUpdatedChecked) return
        // Initialize the updateActivityResultLauncher.
        inAppUpdatesService.checkForUpdate(updateActivityResultLauncher)
    }

    override fun onResume() {
        super.onResume()
        billingClientLifecycle.queryProductPurchases()
        inAppUpdatesService.checkForDownloadedUpdate()
    }

    private fun getStartDestination(): String {
        val route = getNotificationRoute()
        if (route != null)
            return route
        return getOnboardingRoute(applicationContext)
            ?: Route.NoteList.notePosition(NotePosition.MAIN.name)
    }

    private fun getOnboardingRoute(context: Context): String? {
        val sharedPreferences =
            context.getSharedPreferences(
                SharedPreferencesKeys.ApplicationStorageName,
                Context.MODE_PRIVATE
            )
        val isOnboardingPageFinished =
            sharedPreferences.getString(SharedPreferencesKeys.OnboardingPageFinishedData, "")
        return if (isOnboardingPageFinished == OnboardingFinishedData) null else Route.Onboarding.route
    }

    private fun getNotificationRoute(): String? {
        // Retrieve the extras from the Intent
        val extras = intent.extras ?: return null
        val noteId = extras.getString(SharedPreferencesKeys.NoteIdExtra, null) ?: return null
        return Route.Note.noteId(noteId)
    }
}
