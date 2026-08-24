package com.thiagoventura.horacerta

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.view.WindowCompat
import androidx.core.content.ContextCompat
import com.thiagoventura.horacerta.ui.HoraCertaTheme
import com.thiagoventura.horacerta.ui.MainController
import com.thiagoventura.horacerta.ui.MainRoot
import com.thiagoventura.horacerta.ui.OnboardingFlow

class MainActivity : ComponentActivity() {
    private var onNotificationResult: ((Boolean) -> Unit)? = null
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> onNotificationResult?.invoke(granted) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        val app = application as HoraCertaApplication

        setContent {
            HoraCertaTheme {
                var onboardingComplete by remember {
                    mutableStateOf(app.repository.onboardingCompleted)
                }
                var resumeRevision by remember { mutableIntStateOf(0) }
                val controller = remember { MainController(app) }
                val lifecycleOwner = LocalLifecycleOwner.current
                val notificationsGranted = remember(resumeRevision) {
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                }
                val exactAlarmsGranted = remember(resumeRevision) {
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                        getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
                }
                val fullScreenGranted = remember(resumeRevision) {
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
                        getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
                }

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) resumeRevision++
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                LaunchedEffect(resumeRevision, onboardingComplete) {
                    if (onboardingComplete) controller.refresh()
                }

                if (!onboardingComplete) {
                    OnboardingFlow(
                        initialName = app.repository.userName,
                        notificationsGranted = notificationsGranted,
                        exactAlarmsGranted = exactAlarmsGranted,
                        fullScreenGranted = fullScreenGranted,
                        permissionRevision = resumeRevision,
                        requestNotifications = { finished ->
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                onNotificationResult = finished
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                finished(true)
                            }
                        },
                        requestExactAlarms = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                runCatching {
                                    startActivity(
                                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                            data = Uri.parse("package:$packageName")
                                        },
                                    )
                                }
                            }
                        },
                        requestFullScreen = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                runCatching {
                                    startActivity(
                                        Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                                            data = Uri.parse("package:$packageName")
                                        },
                                    )
                                }
                            }
                        },
                        onFinished = { name ->
                            app.repository.userName = name
                            app.repository.onboardingCompleted = true
                            onboardingComplete = true
                            app.alarmScheduler.rescheduleAll(app.repository)
                        },
                    )
                } else {
                    MainRoot(
                        controller = controller,
                        userName = app.repository.userName,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val app = application as HoraCertaApplication
        if (app.repository.onboardingCompleted) {
            Thread { app.alarmScheduler.rescheduleAll(app.repository) }.start()
        }
    }
}
