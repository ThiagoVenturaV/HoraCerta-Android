package com.thiagoventura.horacerta.ui

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.thiagoventura.horacerta.alarm.ActiveAlarmStore
import com.thiagoventura.horacerta.alarm.AlarmContract
import com.thiagoventura.horacerta.alarm.AlarmPayload
import com.thiagoventura.horacerta.alarm.putPayload
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GroupedAlarmFlowTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val occurrenceIds = listOf(91_001L, 91_002L)

    @After
    fun cleanUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        ActiveAlarmStore(context).removeAll(occurrenceIds)
    }

    @Test
    fun simultaneousDosesAreReviewedAndSelectedIndividually() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val scheduledAt = System.currentTimeMillis()
        val first = payload(occurrenceIds[0], "Losartana", scheduledAt)
        val second = payload(occurrenceIds[1], "Metformina", scheduledAt)
        ActiveAlarmStore(context).apply {
            removeAll(occurrenceIds)
            add(first)
        }

        val intent = Intent(context, AlarmActivity::class.java).apply {
            putPayload(first)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        ActivityScenario.launch<AlarmActivity>(intent).use {
            composeRule.onNodeWithText("Hora do medicamento").assertIsDisplayed()
            ActiveAlarmStore(context).add(second)
            context.sendBroadcast(Intent(AlarmContract.ACTION_ACTIVE_ALARMS_CHANGED).setPackage(context.packageName))

            composeRule.onNodeWithText("2 medicamentos agora").assertIsDisplayed()
            composeRule.onNodeWithText("Losartana").assertIsDisplayed()
            composeRule.onNodeWithText("Metformina").assertIsDisplayed()
            composeRule.onNodeWithText("Desligar e revisar").performClick()

            composeRule.onNodeWithText("Confirmar doses").assertIsDisplayed()
            composeRule.onNodeWithText("Losartana").performClick()
            composeRule.onNodeWithText("Confirmar 1 dose").assertIsDisplayed()
            composeRule.onNodeWithText("Adiar todas 15 min").assertIsDisplayed()
        }
    }

    private fun payload(id: Long, name: String, scheduledAt: Long) = AlarmPayload(
        occurrenceId = id,
        medicationName = name,
        dosage = "1 comprimido",
        scheduledAt = scheduledAt,
        sound = false,
        vibration = false,
    )
}
